package io.fair_acc.chartfx.renderer.spi.hexagon;

import java.util.Collection;

import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class GridDrawer {
    private final HexagonMap map;
    private Font font = new Font(13);

    /**
     * @param map the global hex map
     */
    public GridDrawer(final HexagonMap map) {
        this.map = map;
    }

    public void draw(final Canvas canvas) {
        // registerCanvasMouseLiner(canvas); // TODO move elsewhere

        final GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        for (final Hexagon hexagon : map.getAllHexagons()) {
            // draw hexagon according to Node specifications
            hexagon.draw(gc);

            if (map.renderCoordinates) {
                hexagon.renderCoordinates(gc);
            }
        }
    }

    public void draw(final Group root) {
        final Collection<Hexagon> hexagons = map.getAllHexagons();
        for (final Hexagon hexagon : hexagons) {
            hexagon.addEventFilter(MouseEvent.MOUSE_CLICKED, me -> {
                final GridPosition pos = ((Hexagon) me.getSource()).position;
                for (final HexagonCallback callBack : map.onHexClickedCallback) {
                    if (callBack == null) {
                        continue;
                    }
                    final Hexagon hex = map.getHexagon(pos);
                    if (hex != null) {
                        callBack.handle(hex);
                    }
                }
            });
            hexagon.addEventFilter(MouseEvent.MOUSE_ENTERED, me -> {
                final GridPosition pos = ((Hexagon) me.getSource()).position;
                for (final HexagonCallback callBack : map.onHexEnteredCallback) {
                    if (callBack == null) {
                        continue;
                    }
                    final Hexagon hex = map.getHexagon(pos);
                    if (hex != null) {
                        callBack.handle(hex);
                    }
                }
            });

            hexagon.addEventFilter(MouseEvent.MOUSE_EXITED, me -> {
                final GridPosition pos = ((Hexagon) me.getSource()).position;
                for (final HexagonCallback callBack : map.onHexExitCallback) {
                    if (callBack == null) {
                        continue;
                    }
                    final Hexagon hex = map.getHexagon(pos);
                    if (hex != null) {
                        callBack.handle(hex);
                    }
                }
            });

            root.getChildren().add(hexagon);

            if (map.renderCoordinates) {
                final Text text = new Text(hexagon.position.getCoordinates());
                text.setFont(font);
                final double textWidth = text.getBoundsInLocal().getWidth();
                final double textHeight = text.getBoundsInLocal().getHeight();
                text.setX(hexagon.getGraphicsXoffset() - textWidth / 2);
                text.setY(hexagon.getGraphicsYoffset() + textHeight / 4); // Not
                                                                          // sure
                                                                          // why,
                                                                          // but
                                                                          // 4
                                                                          // seems
                                                                          // like
                                                                          // a
                                                                          // good
                                                                          // value

                root.getChildren().add(text);
            }
        }
    }

    public void drawContour(final Canvas canvas) {
        // registerCanvasMouseLiner(canvas); // TODO move elsewhere

        final GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        for (final Hexagon hexagon : map.getAllHexagons()) {
            // draw hexagon contour according to Node specifications
            hexagon.drawContour(gc);

            if (map.renderCoordinates) {
                hexagon.renderCoordinates(gc);
            }
        }
    }

    public void setFont(final Font font) {
        this.font = font;
    }

    /**
     * @param x pixel coordinate
     * @param y pixel coordinate
     * @param hexagonHeight size of hexagons
     * @param xPadding x-padding between tiles
     * @param yPadding y-padding between tiles
     * @return the GridPosition that contains that pixel
     */
    public static GridPosition pixelToPosition(final int x, final int y, final int hexagonHeight, final int xPadding,
            final int yPadding) {
        int xLocal = x - xPadding;
        int yLocal = y - yPadding;
        final double hexagonRadius = (double) hexagonHeight / 2;
        final double q = (1.0 / 3.0 * Math.sqrt(3.0) * xLocal - 1.0 / 3.0 * yLocal) / hexagonRadius;
        final double r = 2.0 / 3.0 * y / hexagonRadius;
        return GridPosition.hexRound(q, r);
    }
}
