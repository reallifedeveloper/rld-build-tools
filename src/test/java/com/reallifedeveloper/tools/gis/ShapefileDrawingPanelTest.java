package com.reallifedeveloper.tools.gis;

import java.net.URL;

import javax.swing.JFrame;

import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeature;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

import com.reallifedeveloper.tools.gis.ShapefileProcessor.FeatureProcessor;

/**
 * A "test" showing how to draw the geometries in a shapefile using a {@link GeometryDrawingPanel}.
 *
 * @author RealLifeDeveloper
 */
@Disabled("Since this 'test' requires a headful Java installation and waits for you to close it manually")
public class ShapefileDrawingPanelTest {

    @Test
    public void test() throws Exception {
        JFrame frame = new JFrame("GeometryDrawingPanelIT");
        GeometryDrawingPanel panel = new GeometryDrawingPanel();
        ShapefileProcessor shapefileProcessor = new ShapefileProcessor(new GeometryAddingFeatureProcessor(panel));
        URL shapefileUrl = getClass().getResource("/shapefile/test.shp");
        shapefileProcessor.processShapefile(shapefileUrl);
        frame.add(panel);
        frame.setSize(500, 500);
        frame.setVisible(true);
        // You should see three small polygons, two in the top-left corner and one in the botton-right corner.
        // Exit by closing the framw.
        while (frame.isActive()) {
            Thread.sleep(100);
        }
    }

    private static final class GeometryAddingFeatureProcessor implements FeatureProcessor {

        private GeometryDrawingPanel geometryDrawingPanel;

        GeometryAddingFeatureProcessor(GeometryDrawingPanel geometryDrawingPanel) {
            this.geometryDrawingPanel = geometryDrawingPanel;
        }

        @Override
        public void processFeature(Feature feature) {
            SimpleFeature simpleFeature = (SimpleFeature) feature;
            for (Object attribute : simpleFeature.getAttributes()) {
                if (attribute instanceof Geometry) {
                    Geometry geometry = (Geometry) attribute;
                    geometryDrawingPanel.addGeometry(geometry);
                }
            }
        }

    }

}
