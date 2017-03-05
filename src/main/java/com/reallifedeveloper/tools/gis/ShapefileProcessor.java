package com.reallifedeveloper.tools.gis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.Feature;

/**
 * Reads a <a href="http://en.wikipedia.org/wiki/Shapefile">shapefile</a> and processes it in some way.
 * <p>
 * One example of use is to create a version that converts a shapefile into SQL insert statements.
 *
 * @author RealLifeDeveloper
 */
public class ShapefileProcessor {

    /**
     * The character encoding that is used by default, {@value #DEFAULT_CHARACTER_ENCODING}.
     */
    public static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";

    private FeatureProcessor featureProcessor;
    private String characterEncoding = DEFAULT_CHARACTER_ENCODING;

    /**
     * Creates a new <code>ShapefileProcessor</code> that processes features in a shapefile using
     * the given {@link FeatureProcessor}.
     *
     * @param featureProcessor the <code>FeatureProcessor</code> to use, must not be <code>null</code>
     */
    public ShapefileProcessor(FeatureProcessor featureProcessor) {
        if (featureProcessor == null) {
            throw new IllegalArgumentException("featureProcessor must not be null");
        }
        this.featureProcessor = featureProcessor;
    }

    /**
     * Processes a shapefile read from the given URL with the given {@link FeatureProcessor} that
     * determines what to do with each individual <code>org.opengis.feature.Feature</code> in the file.
     *
     * @param shapefileUrl a URL to the shapefile

     * @throws IOException if there was a problem reading the shapefile
     */
    public void processShapefile(URL shapefileUrl) throws IOException {
        if (shapefileUrl == null) {
            throw new IllegalArgumentException("shapefileUrl must not be null");
        }
        Map<String, Object> connectParams = new HashMap<String, Object>();
        connectParams.put("url", shapefileUrl);
        connectParams.put("charset", characterEncoding);

        DataStore dataStore = DataStoreFinder.getDataStore(connectParams);
        if (dataStore == null) {
            throw new FileNotFoundException(shapefileUrl.toString());
        }
        String[] typeNames = dataStore.getTypeNames();
        String typeName = typeNames[0];

        SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
        SimpleFeatureCollection collection = featureSource.getFeatures();
        SimpleFeatureIterator iterator = collection.features();

        try {
            while (iterator.hasNext()) {
                featureProcessor.processFeature(iterator.next());
            }
        } finally {
            iterator.close();
        }
    }

    /**
     * Sets the character encoding to use in the shapefile that is created.
     *
     * @param newCharacterEncoding the new character encoding
     */
    public void setCharacterEncoding(String newCharacterEncoding) {
        this.characterEncoding = newCharacterEncoding;
    }

    /**
     * Defines some kind of processing av an <code>org.opengis.feature.Feature</code>. A <code>Feature</code>
     * represents a composite object in a shapefile that contains both a geographical or geometrical object
     * and also other attributes.
     */
    public interface FeatureProcessor {
        /**
         * Processes the given <code>org.opengis.feature.Feature</code> in some way.
         *
         * @param feature the <code>Feature</code> to process
         */
        void processFeature(Feature feature);
    }
}
