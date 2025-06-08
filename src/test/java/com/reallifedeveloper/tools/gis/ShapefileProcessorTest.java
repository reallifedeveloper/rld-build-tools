package com.reallifedeveloper.tools.gis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.api.feature.Feature;
import org.junit.jupiter.api.Test;

import com.reallifedeveloper.tools.gis.ShapefileProcessor.FeatureProcessor;

public class ShapefileProcessorTest {

    private TestFeatureProcessor featureProcessor = new TestFeatureProcessor();
    private ShapefileProcessor shapefileProcessor = new ShapefileProcessor(featureProcessor);

    @Test
    public void shape2sql() throws Exception {
        URL shapefileUrl = getClass().getResource("/shapefile/test.shp");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Shape2Sql shape2Sql = new Shape2Sql();
        shape2Sql.translate(shapefileUrl, out);
        List<String> sqlLines = Arrays.asList(out.toString("UTF-8").split("\\n"));
        assertEquals(3, sqlLines.size(), "Wrong number of SQL statements: ");
        assertTrue(sqlLines.get(0).contains("Above Eleven"), "Missing content in line 1");
        assertTrue(sqlLines.get(1).contains("Levels Club"), "Missing content in line 2");
        assertTrue(sqlLines.get(2).contains("Octave"), "Missing content in line 3");
    }

    @Test
    public void processShapefile() throws Exception {
        URL shapefileUrl = getClass().getResource("/shapefile/test.shp");
        shapefileProcessor.processShapefile(shapefileUrl);
        assertEquals(3, featureProcessor.processedFeatures.size(), "Wrong number of features read: ");
    }

    @Test
    public void processShapefileNonExistingUrl() throws Exception {
        URL shapefileUrl = new URI("file:///no_such_file").toURL();
        assertThrows(FileNotFoundException.class, () -> shapefileProcessor.processShapefile(shapefileUrl));
    }

    @Test
    public void processShapefileNullUrl() throws Exception {
        Exception e = assertThrows(IllegalArgumentException.class, () -> shapefileProcessor.processShapefile(null));
        assertEquals("shapefileUrl must not be null", e.getMessage());
    }

    @Test
    public void constructorNullFeatureProcessor() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> new ShapefileProcessor(null));
        assertEquals("featureProcessor must not be null", e.getMessage());
    }

    private static final class TestFeatureProcessor implements FeatureProcessor {

        private List<Feature> processedFeatures = new ArrayList<>();

        @Override
        public void processFeature(Feature feature) {
            processedFeatures.add(feature);
        }

    }
}
