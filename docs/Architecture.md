# ChartFX Architecture

Below are some architectural considerations aimed at maintainers

* Rendering Phases
* Concurrency
* Event System

## Rendering Phases

The JavaFX application thread (`FXAT`) is operating in on-demand pulses that work through phases:

1) **Animations**: execution of animations and user actions added to [Platform::runLater](https://docs.oracle.com/javase/8/javafx/api/javafx/application/Platform.html#runLater-java.lang.Runnable-). This is where most of the user code lives.
2) **Pre-Layout Listener**: user actions added to [Scene::addPreLayoutPulseListener](https://docs.oracle.com/javase%2F9%2Fdocs%2Fapi%2F%2F/javafx/scene/Scene.html#addPreLayoutPulseListener-java.lang.Runnable-)
3) **CSS**: application of node styling
4) **Layout**: 'dirty' nodes are laid out and sizes get determined
5) **Post-Layout Listener**: user actions added to [Scene::addPostLayoutPulseListener](https://docs.oracle.com/javase%2F9%2Fdocs%2Fapi%2F%2F/javafx/scene/Scene.html#addPostLayoutPulseListener-java.lang.Runnable-)
6) **Bounds**: update of the bounds for all nodes
7) **Draw**: Node data gets copied to the Rendering thread where it gets rendered to screen. Note that JavaFX uses [retained mode](https://en.wikipedia.org/wiki/Retained_mode), so the application thread might specify a `line(x,y,width,length)`, and the rendering thread will later determine how the primitive maps to individual pixels on the screen.

In order to guarantee a deterministic chart within a single pulse, ChartFX operates in the following way:

* **Animations**: user code
  * general user code / setup
  * modification of state (datasets, plugins etc)
* **Pre-Layout Listener**: computes everything needed for the layout
  * lock all datasets
  * determine dataset range
  * update legend
  * update axis ranges
  * update labels
* **Layout**: determines the size and placement of parts, e.g.,
  * axis ranges
  * axis mappings from values to pixels
  * axis tick marks
  * axis label placement/visibility
* **Post-Layout Listener**: draws content according to the placement determined in the layout
  * draw axes
  * draw canvas
  * draw plugins
  * unlock locked datasets

Users generally do not need to worry about the phases as most user code is handled in the first phase, but maintainers and low-level users should be careful not to change the SceneGraph in methods that get called by the post layout listener.

## Concurrency

All SceneGraph components (axes, plugins, charts) may only be modified on the FXAT. DataSets are the only part that may be modified concurrently from a background thread, and only while getting a `DataSet::lock()` write lock.

All used datasets get locked and unlocked for the entire time from pre-layout to the drawing phases. The FXAT currently uses a reentrant (datasets can be added to multiple charts) read-lock (for compatibility), but it is allowed to do write operations such as clearing the event state described below.

Parallel processing from the FXAT while the datasets are locked (e.g. parallel point reduction) does not require any additional locking of the datasets. The FXAT waits for operations to finish, so it does not make any progress and therefore can't run into race conditions.

## Event System

The event system is based on [bit masks](https://en.wikipedia.org/wiki/Mask_(computing)) where each bit corresponds to a specific part of the chart. Setting a bit dirties the state and registers the draw handlers. Setting the same bit multiple times generally has no additional effect. The state bits get cleared once the drawing is finished. Individual steps may be skipped if the state indicates that nothing has changed. 

Listeners can aggregate state from multiple sources and filter bits that they are interested in. Similar to JavaFX properties, each element can subscribe to others via change-listeners (called if specified bits change from 0 to 1) or invalidation-listeners (called on every event). However, it is generally recommended to only use change-listeners.

Example for an axis that needs to trigger a layout on a property change

```Java
// create an axis state object that knows only about axis events
var state = BitState.initDirty(this, ChartBits.AxisMask);

// changes to the axis padding need to recompute the layout and redraw the canvas
// (the set method has the same signature as a JavaFX listener, but does not require a dependency)
axisPadding.addListener(state.onPropChange(ChartBits.AxisLayout, ChartBits.AxisCanvas)::set);

// trigger JavaFX layouts
state.addChangeListener(ChartBits.AxisLayout, (src, bits) -> requestLayout());
```

Example for a chart that aggregates the state from one or more axes

```Java
// create a chart state object that knows about all events
var state = BitState.initDirty(this, ChartBits.AxisMask);

// merge changes coming from a relevant axis
axis.getBitState().addChangeListener(state);

// trigger a redraw if any axis needs to be drawn
state.addChangeListener(ChartBits.AxisCanvas, (src, bits) -> drawAxesInNextCycle());

// remove an axis that is no longer part of this chart
axis.getBitState().removeChangeListener(state);
```

Example for skipping the draw step if none of the axes has changed

```Java
redrawAxes() {
  if (state.isClean(ChartBits.AxisCanvas) {
    return; // all content is still good
  }
  for (var axis : getAxes) {
    axis.redraw();
  }
}
```

Updates use bitwise operations and batch updates automatically, so the process is very efficient. For example 10 data sets changing 100 values will get merged into 10 dataset events that are then merged into a single chart event.

State bits may only be modified from the FXAT, or in the dataset case from a concurrent thread that holds a write lock. Since dataset updates may come from other threads, the Chart internally keeps a second thread-safe (using CAS operations) accumulation state for datasets, e.g.,

```Java
var state = BitState.initDirty(this);
var dataSetState = BitState.initDirtyMultiThreaded(this, ChartBits.DataSetMask)
    .addChangeListener((src, bits) -> {
        if(Platform.isFxApplicationThread()) {
            // Forward immediately as is
            state.accept(src, bits);
        } else {
            // May be deferred until the bits are already outdated, so set actual state
            Platform.runLater(() -> state.accept(src, src.getBits()));
        }
    });
```

In some cases it can be difficult to find what triggered events, so there are additional debugging tools that provide stack trace information

```Java
// Print every time something needs the canvas content to be updated
state.addInvalidateListener(ChartBits.AxisCanvas, ChartBits.printerWithStackTrace());
```

For example, the screenshot below shows a redraw request triggered by a tick unit change `[15] setTickUnit`. IntelliJ parses the output similar to an exception stack trace and provides clickable links to the lines.

![image](https://github.com/fair-acc/chart-fx/assets/5491587/b0666f90-d990-4b4e-a1a3-a263a8fab63b)

At the low-level the bit masks use `int` and `IntSupplier`, so users could also create custom elements with custom events, e.g.,

```Java
enum CustomEvents implements IntSupplier {
  SomeBit;
  int getAsInt() {
    return bit;
  }
  final static int OFFSET = ChartEvents.values().length;
  final int bit = 1 << (OFFSET + ordinal());
}

state.setDirty(CustomEvents.SomeBit);
```

