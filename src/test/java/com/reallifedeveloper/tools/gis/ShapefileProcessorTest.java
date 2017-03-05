package com.reallifedeveloper.tools.gis;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.feature.Feature;

import com.reallifedeveloper.tools.gis.ShapefileProcessor.FeatureProcessor;

public class ShapefileProcessorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private TestFeatureProcessor featureProcessor = new TestFeatureProcessor();
    private ShapefileProcessor shapefileProcessor = new ShapefileProcessor(featureProcessor);

    @Test
    public void shape2sql() throws Exception {
        URL shapefileUrl = getClass().getResource("/shapefile/test.shp");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Shape2Sql shape2Sql = new Shape2Sql();
        shape2Sql.translate(shapefileUrl, out);
        List<String> sqlLines = Arrays.asList(out.toString("UTF-8").split("\\n"));
        Assert.assertEquals("Wrong number of SQL statements: ", 3, sqlLines.size());
        Assert.assertTrue("Line 1 should contain 'Above Eleven'", sqlLines.get(0).contains("Above Eleven"));
        Assert.assertTrue("Line 2 should contain 'Above Eleven'", sqlLines.get(1).contains("Levels Club"));
        Assert.assertTrue("Line 3 should contain 'Above Eleven'", sqlLines.get(2).contains("Octave"));
    }

    @Test
    public void processShapefile() throws Exception {
        URL shapefileUrl = getClass().getResource("/shapefile/test.shp");
        shapefileProcessor.processShapefile(shapefileUrl);
        Assert.assertEquals("Wrong number of features read: ", 3, featureProcessor.processedFeatures.size());
    }

    @Test
    public void processShapefileNonExistingUrl() throws Exception {
        URL shapefileUrl = new URL("file:///no_such_file");
        expectedException.expect(FileNotFoundException.class);
        shapefileProcessor.processShapefile(shapefileUrl);
    }

    @Test
    public void processShapefileNullUrl() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("shapefileUrl must not be null");
        shapefileProcessor.processShapefile(null);
    }

    @Test
    public void constructorNullFeatureProcessor() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("featureProcessor must not be null");
        new ShapefileProcessor(null);
    }

    private static final class TestFeatureProcessor implements FeatureProcessor {

        private List<Feature> processedFeatures = new ArrayList<>();

        @Override
        public void processFeature(Feature feature) {
            processedFeatures.add(feature);
        }

    }
}
