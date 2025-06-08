package com.reallifedeveloper.tools.gis;

import java.awt.Color;
import java.awt.Graphics2D;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.mockito.Mockito;

public class GeometryDrawingPanelTest {

    @Test
    public void emptyDrawingPanelShouldPaintNothing() throws Exception {
        Graphics2D graphics = createGrahpicsMock();
        GeometryDrawingPanel panel = new GeometryDrawingPanel();
        panel.paintComponent(graphics);
        Mockito.verify(graphics, Mockito.never()).draw(Mockito.any());
    }

    @Test
    public void drawingPanelWithThreeGeometriesShouldPaintThreeObjects() throws Exception {
        Graphics2D graphics = createGrahpicsMock();
        GeometryDrawingPanel panel = createTestDrawingPanel();
        panel.paintComponent(graphics);
        Mockito.verify(graphics, Mockito.times(3)).setPaint(Color.BLUE);
        Mockito.verify(graphics, Mockito.times(3)).draw(Mockito.any());
    }

    @Test
    public void drawingPanelShouldPaintNothingAfterClear() throws Exception {
        Graphics2D graphics = createGrahpicsMock();
        GeometryDrawingPanel panel = createTestDrawingPanel();
        panel.clearGeometries();
        panel.paintComponent(graphics);
        Mockito.verify(graphics, Mockito.never()).draw(Mockito.any());
    }

    private Graphics2D createGrahpicsMock() throws ParseException {
        Graphics2D graphics = Mockito.mock(Graphics2D.class, Mockito.withSettings().verboseLogging());
        Mockito.when(graphics.create()).thenReturn(Mockito.mock(Graphics2D.class));
        return graphics;
    }

    private GeometryDrawingPanel createTestDrawingPanel() throws ParseException {
        GeometryDrawingPanel panel = new GeometryDrawingPanel();

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        WKTReader wktReader = new WKTReader(geometryFactory);

        LineString line = (LineString) wktReader
                .read("LINESTRING(20 20, 20 25, 25 25, " + "25 15, 15 15, 15 30, 30 30, 30 10, 10 10, 10 35, 35 35, 35 5)");
        panel.addGeometry(line);

        line = (LineString) wktReader.read("LINESTRING(-10 40, 5 50, 20 40, 35 50, 50 40)");
        panel.addGeometry(line);

        Polygon polygon = (Polygon) wktReader.read("POLYGON((-10 -10, 0 0, 40 0, 50 -10, 40 -20, 0 -20, -10 -10), "
                + "(0 -10, 5 -5, 10 -10, 5 -15, 0 -10), (30 -10, 35 -5, 40 -10, 35 -15, 30 -10))");
        panel.addGeometry(polygon);
        return panel;
    }
}
