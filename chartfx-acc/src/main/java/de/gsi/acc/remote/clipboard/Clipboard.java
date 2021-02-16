package de.gsi.acc.remote.clipboard;

import static de.gsi.acc.remote.BasicRestRoles.ANYONE;
import static de.gsi.acc.remote.RestServer.prefixPath;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.Deflater;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.acc.remote.BasicRestRoles;
import de.gsi.acc.remote.RestCommonThreadPool;
import de.gsi.acc.remote.RestServer;
import de.gsi.acc.remote.util.CombinedHandler;
import de.gsi.acc.remote.util.MessageBundle;
import de.gsi.chart.utils.FXUtils;
import de.gsi.chart.utils.PaletteQuantizer;
import de.gsi.chart.utils.WritableImageCache;
import de.gsi.chart.utils.WriteFxImage;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.EventRateLimiter;
import de.gsi.dataset.event.EventSource;
import de.gsi.dataset.event.UpdateEvent;
import de.gsi.dataset.remote.DataContainer;
import de.gsi.dataset.remote.MimeType;
import de.gsi.dataset.utils.ByteArrayCache;
import de.gsi.dataset.utils.Cache;
import de.gsi.dataset.utils.Cache.CacheBuilder;
import de.gsi.dataset.utils.GenericsHelper;
import de.gsi.math.Math;
import de.gsi.math.MathBase;

import ar.com.hjg.pngj.FilterType;
import io.javalin.core.security.Role;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.sse.SseClient;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiFileUpload;
import io.javalin.plugin.openapi.annotations.OpenApiFormParam;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;

/**
 * Basic implementation of a Restfull image clipboard
 * 
 * RestServer property are mainly controlled via {@link RestServer}
 * 
 * N.B. The '/upload' endpoint access requires write privileges. By default, non
 * logged-in users are mapped to 'anonymous' that by-default have READ_WRITE
 * access roles. This can be reconfigured in the default Password file and/or
 * another custom {@link de.gsi.acc.remote.user.RestUserHandler }
 * implementation.
 * 
 * @author rstein
 *
 */
public class Clipboard implements EventSource, EventListener {
    public static final String ERROR_WHILE_READING_TEST_IMAGE_FROM = "error while reading test image from '{}'";
    private static final Logger LOGGER = LoggerFactory.getLogger(Clipboard.class);
    private static final int DEFAULT_PALETTE_COLOR_COUNT = 32;
    private static final int STATISTICS_INT_COUNT = 250;
    private static final boolean IMAGE_USE_ALPHA = true;
    private static final String TESTIMAGE = "PM5544_test_signal.png";
    private static final String DOT_PNG = ".png";
    private static final String QUERY_UPDATE_PERIOD = "updatePeriod";
    private static final String QUERY_LONG_POLLING = "longpolling";
    private static final String QUERY_SSE = "sse";
    private static final String QUERY_LAST_UPDATE = "lastAccess.";
    private static final String CLIPBOARD_BASE = "/clipboard/";
    private static final String CLIPBOARD_ROOT = "";
    private static final String CLIPBOARD_DEFAULT = "misc/";
    private static final String ENDPOINT_UPLOAD = "/upload";
    private static final String ENDPOINT_CLIPBOARD = CLIPBOARD_BASE + "*";
    private static final String TEMPLATE_UPLOAD = "/velocity/clipboard/upload.vm";
    private static final String TEMPLATE_ALL_IMAGES = "/velocity/clipboard/all.vm";
    private static final String TEMPLATE_ONE_IMAGE_LONG_POLLING = "/velocity/clipboard/one_long.vm";
    private static final String TEMPLATE_ONE_IMAGE_SSE = "/velocity/clipboard/one_sse.vm";
    private static final String CACHE_LIMIT = "clipboardCacheLimit";
    private static final int CACHE_LIMIT_DEFAULT = 25;
    private static final String CACHE_TIME_OUT = "clipboardCacheTimeOut"; // [minutes]
    private static final int CACHE_TIME_OUT_DEFAULT = 60;
    // update source definitions
    private final AtomicBoolean autoNotify = new AtomicBoolean(true);
    private final List<EventListener> updateListeners = Collections.synchronizedList(new LinkedList<>());

    private final Lock clipboardLock = new ReentrantLock();
    private final Condition clipboardCondition = clipboardLock.newCondition();
    private final Cache<String, Cache<String, DataContainer>> clipboardCacheCategory; // Map<categoryName, Map<categoryName, DataContainer>>
    private final SnapshotParameters snapshotParameters = new SnapshotParameters();
    private final Cache<String, String> userCounterCache = Cache.<String, String>builder().withTimeout(1, TimeUnit.MINUTES).build();
    private final IntegerProperty userCount = new SimpleIntegerProperty(this, "userCount", 0);
    private final IntegerProperty userCountSse = new SimpleIntegerProperty(this, "userCountSse", 0);
    private final String exportRoot;
    private final String exportNameImage;
    private final Region regionToCapture;
    private final long maxUpdatePeriod;
    private final TimeUnit maxUpdatePeriodTimeUnit;
    private final WritableImageCache imageCache = new WritableImageCache();
    private final ByteArrayCache byteArrayCache = new ByteArrayCache();
    private final EventRateLimiter eventRateLimiter;
    private final AtomicInteger threadCount = new AtomicInteger(0);
    private final List<Double> captureDiffs = new ArrayList<>(STATISTICS_INT_COUNT);
    private final List<Double> processingTotal = new ArrayList<>(STATISTICS_INT_COUNT);
    private final List<Double> sizeTotal = new ArrayList<>(STATISTICS_INT_COUNT);
    private boolean usePalette;
    private PaletteQuantizer userPalette = null;
    private final EventListener paletteUpdateListener = evt -> {
        if (evt.getPayLoad() instanceof Image) {
            RestCommonThreadPool.getCommonPool().execute(() -> userPalette = WriteFxImage.estimatePalette((Image) (evt.getPayLoad()), IMAGE_USE_ALPHA, DEFAULT_PALETTE_COLOR_COUNT));
        }
    };
    private EventRateLimiter paletteUpdateRateLimiter = new EventRateLimiter(paletteUpdateListener, TimeUnit.SECONDS.toMillis(20));

    @OpenApi(
            description = "endpoint to provide html form data to upload clipboard data",
            summary = "GET",
            tags = { "Clipboard" },
            path = ENDPOINT_UPLOAD,
            method = HttpMethod.GET,
            responses = {
                @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/html"))
                ,
                        @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/json"))
            })
    private final Handler uploadHandlerGet
            = new CombinedHandler(ctx -> {
                  final Map<String, Object> model = MessageBundle.baseModel(ctx);
                  ctx.render(TEMPLATE_UPLOAD, model);
              }) {};
    private final Function<? super String, ? extends Cache<String, DataContainer>> categoryMappingFunction = category -> {
        CacheBuilder<String, DataContainer> clipboardCacheBuilder = Cache.<String, DataContainer>builder().withLimit(getCacheLimit());
        if (getCacheTimeOut() > 0) {
            clipboardCacheBuilder.withTimeout(getCacheTimeOut(), getCacheTimeOutUnit());
        }
        final BiConsumer<String, DataContainer> cacheRecoverAction = (final String k, final DataContainer v) -> // too long for one line
                RestCommonThreadPool.getCommonScheduledPool().schedule(() -> v.getData().forEach(d -> byteArrayCache.add(d.getDataByteArray())), 200, TimeUnit.MILLISECONDS);
        return clipboardCacheBuilder.withPostListener(cacheRecoverAction).build();
    };

    private final Runnable convertImage = () -> {
        final long start = System.nanoTime(); // NOPMD -- needed for time-keeping
        final int width = (int) getRegionToCapture().getWidth();
        final int height = (int) getRegionToCapture().getHeight();
        if (threadCount.get() > 1 || width == 0 || height == 0) {
            return;
        }
        threadCount.incrementAndGet();

        final WritableImage imageCopyIn = imageCache.getImage(width, height);
        WritableImage imageCopyOut;
        try {
            imageCopyOut = FXUtils.runAndWait(() -> getRegionToCapture().snapshot(snapshotParameters, imageCopyIn));
        } catch (final Exception e) { // NOPMD
            LOGGER.atError().setCause(e).log("snapshotListener -> Node::snapshot(..)");
            threadCount.decrementAndGet();
            return;
        }
        captureDiffs.add(((System.nanoTime() - start) / 1e6));

        if (imageCopyOut == null) {
            LOGGER.atDebug().addArgument(width).addArgument(height).log("snapshotListener - return image is null - requested '{}x{}'");
            threadCount.decrementAndGet();
            return;
        }
        final long mid = System.nanoTime();
        final int size2 = WriteFxImage.getCompressedSizeBound(width, height, true);
        final byte[] rawByteBuffer = byteArrayCache.getArray(size2);
        final ByteBuffer imageBuffer = ByteBuffer.wrap(rawByteBuffer);
        // WriteFxImage.encodeAlt(imageCopyOut, imageBuffer, useAlpha, Deflater.BEST_SPEED, null)
        if (usePalette) {
            // updatePalette(imageCopyOut)
            WriteFxImage.encodePalette(imageCopyOut, imageBuffer, IMAGE_USE_ALPHA, Deflater.BEST_SPEED, FilterType.FILTER_NONE, userPalette);
        } else {
            WriteFxImage.encode(imageCopyOut, imageBuffer, IMAGE_USE_ALPHA, Deflater.BEST_SPEED, FilterType.FILTER_NONE);
        }

        sizeTotal.add((double) imageBuffer.limit());
        imageCache.add(imageCopyIn);
        imageCache.add(imageCopyOut);

        LOGGER.atDebug().addArgument(getExportNameImage()).addArgument(getExportNameImage()) //
                .log("new image '{}' for export name '{}' generated -> notify listener");
        final int maxUpdatePeriodMillis = (int) getMaxUpdatePeriodTimeUnit().toMillis(getMaxUpdatePeriod());
        addClipboardData(new DataContainer(getExportNameImage(), maxUpdatePeriodMillis, imageBuffer.array(), imageBuffer.limit()));
        processingTotal.add(((System.nanoTime() - mid) / 1e6));

        printDiffs("capture", "ms", captureDiffs);
        printDiffs("processingTotal", "ms", processingTotal);
        printDiffs("sizeTotal", "bytes", sizeTotal);

        final int threads = threadCount.decrementAndGet();
        if (threads > 1) {
            LOGGER.atWarn().addArgument(threads).log("thread-pile-up = {}");
        }
    };
    @OpenApi(
            description = "clipboard root",
            summary = "My Summary",
            tags = { "Clipboard" },
            path = ENDPOINT_CLIPBOARD,
            method = HttpMethod.GET,
            headers = { @OpenApiParam(name = "my-custom-header") },
            // pathParams = { @OpenApiParam(name = "*", type = String.class, description = "the <sub-category>/<data-item file name") },
            responses = { @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/html"))
                          ,
                                  @OpenApiResponse(status = "200", content = @OpenApiContent(type = "image/png")),
                                  @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/event-stream")) })
    private final Handler exportHandler
            = new CombinedHandler(ctx -> {
                  final long maxUpdateMillis = getMaxUpdatePeriodTimeUnit().toMillis(getMaxUpdatePeriod());
                  final int maxUpdateRate = 1000 / (int) maxUpdateMillis;
                  RestServer.applyRateLimit(ctx, 2 * maxUpdateRate, TimeUnit.SECONDS); // rate limit on query exportNameImage landing page
                  RestServer.suppressCaching(ctx);

                  // parse path behind CLIPBOARD_BASE root
                  final String landingPage = ctx.path().replaceFirst(CLIPBOARD_BASE, "");
                  final String category = fixPreAndPost(getCategoryFromPath(landingPage));

                  // check if landing page exists
                  Cache<String, DataContainer> categoryMap = getClipboardCache().get(category);
                  if (categoryMap == null) {
                      ctx.status(404).result(categoryNotFound(category));
                      return;
                  }

                  final String imageDataTag = landingPage.replaceFirst(getCategoryFromPath(landingPage), "");
                  if (imageDataTag.isBlank() || !imageDataTag.contains(".")) {
                      if (imageDataTag.isBlank()) {
                          // serve overview page
                          serveCategoryOverview(ctx, category);
                          return;
                      }

                      // serve launch page, long-polling or SSE landing pages
                      final Entry<String, DataContainer> matchingEntry = categoryMap.entrySet().stream().filter(kv -> kv.getValue().getExportName().equals(imageDataTag)).findFirst().orElse(null);
                      if (matchingEntry == null) {
                          // landing page does not (yet) exist - export category overview page
                          serveCategoryOverview(ctx, category);
                          return;
                      }

                      final DataContainer matchedClipboardData = matchingEntry.getValue();
                      serveImageDataLandingPage(ctx, category, matchedClipboardData);
                      return;
                  }

                  serveImageData(ctx, category, imageDataTag);
              }) {};
    @OpenApi(
            description = "endpoint for posting clipboard data",
            summary = "submit new clipboard data",
            tags = { "Clipboard" },
            path = ENDPOINT_UPLOAD,
            method = HttpMethod.POST,
            formParams = { @OpenApiFormParam(name = "clipboardExportName")
                           ,
                                   @OpenApiFormParam(name = "clipboardCategoryName") },
            fileUploads = { @OpenApiFileUpload(name = "clipboardData", isArray = true) },
            requestBody = @OpenApiRequestBody(content = { @OpenApiContent(type = "text/html")
                                                          ,
                                                                  @OpenApiContent(type = "application/binary-new-protocol"),
                                                                  @OpenApiContent(type = "application/binary-legacy"),
                                                                  @OpenApiContent(from = DataContainer.class) }),
            responses = { @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/html"))
                          ,
                                  @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/json")),
                                  @OpenApiResponse(status = "200", content = { @OpenApiContent(from = DataContainer.class) }) })
    private final Handler uploadHandlerPost
            = new CombinedHandler(ctx -> {
                  final String exportName = ctx.formParam("clipboardExportName");
                  final String rawCategory = ctx.formParam("clipboardCategoryName");
                  final String category = fixPreAndPost(rawCategory == null || rawCategory.isBlank() ? CLIPBOARD_DEFAULT : rawCategory);
                  LOGGER.atDebug().addArgument(exportName).log("received export name = '{}'");

                  LOGGER.atInfo().addArgument(exportName).log("received export name = '{}'");
                  LOGGER.atInfo().addArgument(ctx.formParam("clipboardCategoryName")).log("received category name = '{}'");

                  ctx.uploadedFiles("clipboardData").forEach(file -> {
                      final String fileName = file.getFilename();
                      if (exportName == null || exportName.isBlank() || !fileName.contains(".")) {
                          try {
                              final byte[] fileData = file.getContent().readAllBytes();
                              addClipboardData(new DataContainer(category + fileName, -1, fileData, fileData.length));
                          } catch (final IOException e) {
                              LOGGER.atError().setCause(e).addArgument(fileName).log(ERROR_WHILE_READING_TEST_IMAGE_FROM);
                          }
                          LOGGER.atInfo().addArgument(fileName).log("upload received: '{}'");
                          return;
                      }

                      // export name specified
                      int p = fileName.lastIndexOf('/');
                      if (p < 0) {
                          p = 0;
                      }
                      final String[] exportNameData = fileName.substring(p).replace("/", "").split("\\.");
                      String export = exportName.replace(" ", "_") + "." + exportNameData[1];
                      try {
                          final byte[] fileData = file.getContent().readAllBytes();
                          addClipboardData(new DataContainer(category + export, -1, fileData, fileData.length));
                      } catch (final IOException e) {
                          LOGGER.atError().setCause(e).addArgument(file.getFilename()).log(ERROR_WHILE_READING_TEST_IMAGE_FROM);
                      }
                      LOGGER.atInfo().addArgument(fileName).addArgument(export).log("upload received: '{}' as '{}'");
                  });
                  ctx.redirect(this.getExportRoot());
                  // alt: ctx.html("Upload complete")
              }) {};
    @OpenApi(
            description = "landing page",
            summary = "root export",
            tags = { "Clipboard" },
            path = "/",
            method = HttpMethod.GET,
            responses = {
                @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/html"))
                ,
                        @OpenApiResponse(status = "200", content = @OpenApiContent(type = "text/json"))
            })
    private final Handler rootHandler
            = new CombinedHandler(ctx -> serveCategoryOverview(ctx, fixPreAndPost(CLIPBOARD_ROOT))) {};

    public Clipboard(final String exportRoot, final String exportName, final Region regionToCapture, final long maxUpdatePeriod, final TimeUnit maxUpdatePeriodTimeUnit, final boolean allowUploads) {
        this.exportRoot = exportRoot;
        exportNameImage = exportName + DOT_PNG;
        this.regionToCapture = regionToCapture;
        this.maxUpdatePeriod = maxUpdatePeriod;
        this.maxUpdatePeriodTimeUnit = maxUpdatePeriodTimeUnit;

        // Map<categoryName, Map<categoryName, DataContainer>>
        CacheBuilder<String, Cache<String, DataContainer>> clipboardCacheBuilder = Cache.<String, Cache<String, DataContainer>>builder().withLimit(getCacheLimit());
        if (getCacheTimeOut() > 0) {
            clipboardCacheBuilder.withTimeout(getCacheTimeOut(), getCacheTimeOutUnit());
        }
        clipboardCacheCategory = clipboardCacheBuilder.build();

        eventRateLimiter = new EventRateLimiter(evt -> RestCommonThreadPool.getCommonPool().execute(convertImage), maxUpdatePeriodTimeUnit.toMillis(maxUpdatePeriod));

        // add default routes
        Set<Role> accessRoles = Collections.singleton(ANYONE);
        RestServer.getInstance().get(exportRoot, rootHandler, accessRoles);
        RestServer.getInstance().get(prefixPath(ENDPOINT_CLIPBOARD), exportHandler, accessRoles);

        if (allowUploads) {
            RestServer.getInstance().get(exportRoot + ENDPOINT_UPLOAD, uploadHandlerGet, Set.of(BasicRestRoles.ADMIN, BasicRestRoles.READ_WRITE));
            RestServer.getInstance().post(exportRoot + ENDPOINT_UPLOAD, uploadHandlerPost, Set.of(BasicRestRoles.ADMIN, BasicRestRoles.READ_WRITE));
        }
    }

    /**
     * Adds new Clipboard data (non-blocking) to the cache and notifies potential listeners.
     * 
     * @param data data Container
     */
    public void addClipboardData(@NotNull final DataContainer data) {
        RestCommonThreadPool.getCommonPool().execute(() -> {
            try {
                clipboardLock.lock();
                final String category = data.getCategory() == null ? CLIPBOARD_ROOT : data.getCategory();
                final Cache<String, DataContainer> categoryMap = getClipboardCache(category);
                final DataContainer ret = categoryMap.put(data.getExportNameData(), data);
                LOGGER.atDebug().addArgument(data.getCategory()).addArgument(data.getExportName()).addArgument(data.getExportNameData()).addArgument(ret) //
                        .log("adding c = '{}' ex = '{}' exData = '{}' previous data = {}");
                data.updateAccess();
                updateListener(CLIPBOARD_BASE + data.getCategory() + data.getExportNameData(), data.getTimeStampCreation());
                clipboardCondition.signalAll();
            } finally {
                clipboardLock.unlock();
            }
        });
    }

    public void addTestImageData() {
        try (InputStream in = Clipboard.class.getResourceAsStream(TESTIMAGE)) {
            byte[] fileContent = in.readAllBytes();
            addClipboardData(new DataContainer("test.png", -1L, fileContent, fileContent.length));
            addClipboardData(new DataContainer(CLIPBOARD_DEFAULT + "test0.png", -1L, fileContent, fileContent.length));
            addClipboardData(new DataContainer(CLIPBOARD_DEFAULT + CLIPBOARD_DEFAULT + "test1.png", -1L, fileContent, fileContent.length));
            addClipboardData(new DataContainer(CLIPBOARD_DEFAULT + CLIPBOARD_DEFAULT + "test2.bin", -1L, fileContent, fileContent.length));
        } catch (final IOException e) {
            final URL res = DataContainer.class.getResource(TESTIMAGE);
            LOGGER.atError().setCause(e).addArgument(res == null ? null : res.getPath()).log(ERROR_WHILE_READING_TEST_IMAGE_FROM);
        }
    }

    @Override
    public AtomicBoolean autoNotification() {
        return autoNotify;
    }

    /**
     * This returns a nested Map of first 'categoryName', then 'exportName' keys to DataContainer objects
     * @return the underlying clipboard data cache N.B. use preferably
     *         {@link #addClipboardData} for adding data since this does not block
     *         and also notifies listeners
     */
    public Cache<String, Cache<String, DataContainer>> getClipboardCache() {
        return clipboardCacheCategory;
    }

    public Cache<String, DataContainer> getClipboardCache(final String category) {
        return clipboardCacheCategory.computeIfAbsent(fixPreAndPost(category), categoryMappingFunction);
    }

    public String getExportNameImage() {
        return exportNameImage;
    }

    public String getExportRoot() {
        return exportRoot;
    }

    public URI getLocalURI() {
        return URI.create(Objects.requireNonNull(RestServer.getLocalURI()).toString() + prefixPath(getExportRoot()));
    }

    public long getMaxUpdatePeriod() {
        return maxUpdatePeriod;
    }

    public TimeUnit getMaxUpdatePeriodTimeUnit() {
        return maxUpdatePeriodTimeUnit;
    }

    public EventRateLimiter getPaletteUpdateRateLimiter() {
        return paletteUpdateRateLimiter;
    }

    public URI getPublicURI() {
        return URI.create(Objects.requireNonNull(RestServer.getPublicURI()).toString() + prefixPath(getExportRoot()));
    }

    public Region getRegionToCapture() {
        return regionToCapture;
    }

    @Override
    public void handle(final UpdateEvent event) {
        eventRateLimiter.handle(event);
    }

    public boolean isUsePalette() {
        return usePalette;
    }

    public void setPaletteUpdateRateLimiter(final long timeOut, final TimeUnit timeUnit) {
        paletteUpdateRateLimiter = new EventRateLimiter(paletteUpdateListener, timeUnit.toMillis(timeOut));
    }

    public void setUsePalette(final boolean usePalette) {
        this.usePalette = usePalette;
    }

    @Override
    public List<EventListener> updateEventListener() {
        return updateListeners;
    }

    public void updateListener(@NotNull final String eventSource, final long eventTimeStamp) {
        final Queue<SseClient> sseClients = RestServer.getEventClients(eventSource);
        FXUtils.runFX(() -> userCountSse.set(sseClients.size()));
        sseClients.forEach((final SseClient client) -> client.sendEvent("new '" + eventSource + "' @" + eventTimeStamp));
    }

    public ReadOnlyIntegerProperty userCountProperty() {
        return userCount;
    }

    public ReadOnlyIntegerProperty userCountSseProperty() {
        return userCountSse;
    }

    protected void updatePalette(Image imageCopyOut) {
        paletteUpdateRateLimiter.handle(new UpdateEvent(this, "update palette", WriteFxImage.clone(imageCopyOut)));
    }

    private String categoryNotFound(final String category) {
        return "category = " + category + " not found";
    }

    private void serveCategoryOverview(Context ctx, final String category) {
        if (getClipboardCache().get(category) == null) {
            ctx.status(404).result(categoryNotFound(category));
            return;
        }
        final Map<String, Object> model = MessageBundle.baseModel(ctx);
        model.put("root", getExportRoot());
        model.put("category", category);
        final Predicate<String> categoryFilter = cat -> cat.startsWith(category) && !cat.equals(category);
        final List<String> subCategories = getClipboardCache().keySet().stream().filter(categoryFilter).collect(Collectors.toList());
        model.put("categories", subCategories);

        final Predicate<DataContainer> nonDisplayableDataFilter = cat -> MimeType.getEnum(cat.getMimeType()).isNonDisplayableData();
        model.put("images", getClipboardCache(category).values().stream().filter(nonDisplayableDataFilter.negate()).collect(Collectors.toList()));
        model.put("data", getClipboardCache(category).values().stream().filter(nonDisplayableDataFilter).collect(Collectors.toList()));

        ctx.render(TEMPLATE_ALL_IMAGES, model);
    }

    private void serveImageData(Context ctx, final String category, final String imageDataTag) {
        final Cache<String, DataContainer> categoryMap = getClipboardCache(category);

        DataContainer cbData = categoryMap.get(imageDataTag);
        if (cbData == null) {
            // image/data does not exist
            ctx.status(404).result("category = " + category + " and imageDataTag " + imageDataTag + " not found");
            return;
        }

        final boolean isLongPolling = ctx.queryParam(QUERY_LONG_POLLING) != null;
        final String identifier = ctx.req.getRemoteAddr(); // find perhaps a better metric
        userCounterCache.put(identifier, ctx.req.getProtocol());
        FXUtils.runFX(() -> userCount.set(userCounterCache.size()));

        Long sessionUpdate = ctx.sessionAttribute(QUERY_LAST_UPDATE + ctx.path());
        final long lastUpdate = sessionUpdate == null ? 0 : sessionUpdate;
        ctx.contentType(MimeType.PNG.toString());

        while (cbData.getTimeStampCreation() <= lastUpdate && isLongPolling /* && cbData.getMaxUpdatePeriod() > 0 */) {
            try {
                final long waitPeriod = MathBase.max(TimeUnit.SECONDS.toMillis(1), 4 * cbData.getUpdatePeriod());
                clipboardLock.lock();
                final boolean condition1 = !clipboardCondition.await(waitPeriod, TimeUnit.MILLISECONDS) && LOGGER.isInfoEnabled();
                if (condition1) {
                    LOGGER.atInfo().log("aborted a possibly too long long-polling await");
                }
                clipboardLock.unlock();
            } catch (final InterruptedException e) {
                clipboardLock.unlock();
                LOGGER.atError().setCause(e).addArgument(imageDataTag).log("waiting for new image '{}' to be updated");
                Thread.currentThread().interrupt();
            }
            cbData = categoryMap.get(imageDataTag);
            if (cbData == null) {
                // image does not exist
                return;
            }
        }

        ctx.sessionAttribute(QUERY_LAST_UPDATE + ctx.path(), cbData.getTimeStampCreation());
        ctx.res.setContentType(cbData.getMimeType());
        RestServer.writeBytesToContext(ctx, cbData.getDataByteArray(), cbData.getDataByteArraySize());
    }

    private void serveImageDataLandingPage(Context ctx, final String category, final DataContainer data) {
        final String updatePeriodString = ctx.queryParam(QUERY_UPDATE_PERIOD, "1000");
        long updatePeriod = 500;
        if (updatePeriodString != null) {
            try {
                updatePeriod = Long.parseLong(updatePeriodString);
            } catch (final NumberFormatException e) {
                final String clientIp = ctx.req.getRemoteHost();
                LOGGER.atError().setCause(e).addArgument(updatePeriodString).addArgument(clientIp) //
                        .log("could not parse 'updatePeriod'={} argument sent by client {}");
            }
        }
        updatePeriod = MathBase.max(getMaxUpdatePeriod(), updatePeriod);
        final Map<String, Object> model = MessageBundle.baseModel(ctx);
        model.put("indexRoot", CLIPBOARD_BASE + category);
        model.put(QUERY_UPDATE_PERIOD, updatePeriod);
        model.put("title", data.getExportName());
        model.put("imageLanding", CLIPBOARD_BASE + data.getExportName() + "?updatePeriod=" + data.getUpdatePeriod());
        model.put("imageSource", CLIPBOARD_BASE + category + data.getExportNameData());
        model.put(QUERY_LONG_POLLING, QUERY_LONG_POLLING);
        if (ctx.queryParam(QUERY_SSE) == null) {
            ctx.render(TEMPLATE_ONE_IMAGE_LONG_POLLING, model);
        } else {
            ctx.render(TEMPLATE_ONE_IMAGE_SSE, model);
        }
    }

    public static int getCacheLimit() {
        final String property = System.getProperty(CACHE_LIMIT, Integer.toString(CACHE_LIMIT_DEFAULT));
        try {
            return Integer.parseInt(property);
        } catch (final NumberFormatException e) {
            LOGGER.atError().addArgument(CACHE_LIMIT).addArgument(property).addArgument(CACHE_LIMIT_DEFAULT).log("could not parse {}='{}' return default limit {}");
            return CACHE_LIMIT_DEFAULT;
        }
    }

    public static int getCacheTimeOut() {
        final String property = System.getProperty(CACHE_TIME_OUT, Integer.toString(CACHE_TIME_OUT_DEFAULT));
        try {
            return Integer.parseInt(property);
        } catch (final NumberFormatException e) {
            LOGGER.atError().addArgument(CACHE_TIME_OUT).addArgument(property).addArgument(CACHE_TIME_OUT_DEFAULT).log("could not parse {}='{}' return default timeout {} [minutes]");
            return CACHE_TIME_OUT_DEFAULT;
        }
    }

    public static TimeUnit getCacheTimeOutUnit() {
        return TimeUnit.MINUTES;
    }

    private static String fixPreAndPost(final String name) {
        final String fixedPrefix = (name.startsWith("/") ? name : '/' + name);
        return fixedPrefix.endsWith("/") ? fixedPrefix : fixedPrefix + '/';
    }

    private static String getCategoryFromPath(final String name) {
        if (name.isBlank()) {
            return name;
        }
        final int p = name.lastIndexOf('/');
        if (p < 0) {
            return "";
        }
        return name.substring(0, p + 1);
    }

    private static void printDiffs(final String title, final String unit, final List<Double> diffArray) {
        final double[] values = GenericsHelper.toDoublePrimitive(diffArray.toArray(new Double[0]));
        if (diffArray.size() >= STATISTICS_INT_COUNT) {
            if (LOGGER.isDebugEnabled()) {
                final double mean = Math.mean(values);
                if (mean > 40.0) {
                    final double rms = Math.rms(values);
                    final String msg = String.format("processing delays: %-15s  (%3d): dT = %4.1f +- %4.1f %s", title, diffArray.size(), mean, rms, unit);

                    LOGGER.atDebug().log(msg);
                }
            }
            diffArray.clear();
        }
    }
}
