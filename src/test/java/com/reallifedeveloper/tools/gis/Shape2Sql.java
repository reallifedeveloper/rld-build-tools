package com.reallifedeveloper.tools.gis;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;

import org.opengis.feature.Feature;
import org.opengis.feature.Property;

import com.reallifedeveloper.tools.gis.ShapefileProcessor.FeatureProcessor;

/**
 * An example of how to use a {@link ShapefileProcessor} to create SQL insert statements for a
 * particular type of shapefile.
 *
 * @author RealLifeDeveloper
 */
public class Shape2Sql {

    /**
     * The <a href="http://en.wikipedia.org/wiki/SRID">SRID</a> that identifies the coordinate system
     * that is used in the shapefile and also in the SQL file being created.
     * <p>
     * In this example we use <a href="http://epsg.io/3857">WGS 84 / Pseudo-Mercator</a>, the same as
     * is used by Google Maps, so SRID={@value #SRID}.
     */
    public static final int SRID = 3857;

    /**
     * The character encoding used, both in the shapefile and in the SQL file being created,
     * {@value #CHARACTER_ENCODING}.
     */
    public static final String CHARACTER_ENCODING = "UTF-8";

    private static final String SQL_STRING =
            "INSERT INTO bar (name, url, geometry) VALUES ('%s', '%s', geometry::STGeomFromText('%s', %d));";

    /**
     * Reads a shapefile containing a map of bars, with information about the name and home page
     * of each bar, and translates this into SQL insert statements appropriate for SQL Server.
     *
     * @param shapefileUrl the URL where the shapefile can be found
     * @param out an <code>OutputStream</code> where SQL statements will be written
     *
     * @throws IOException if reading from <code>shapefileUrl</code> or writing to <code>out</code> failed
     */
    public void translate(URL shapefileUrl, OutputStream out) throws IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, CHARACTER_ENCODING))) {
            FeatureProcessor sqlWriter = new BarFeatureProcessor(
                    (name, url, geom) -> writer.println(String.format(SQL_STRING, name, url, geom, SRID)));
            ShapefileProcessor shapefileProcessor = new ShapefileProcessor(sqlWriter);
            shapefileProcessor.setCharacterEncoding(CHARACTER_ENCODING);
            shapefileProcessor.processShapefile(shapefileUrl);
        }
    }

    /**
     * Defines some kind of processing of bar information.
     */
    @FunctionalInterface
    public interface BarProcessor {
        /**
         * Processes the given bar information.
         *
         * @param name the name of the bar
         * @param url a URL to the home page of the bar
         * @param geometry the geometric shape of the bar
         */
        void processBar(String name, String url, String geometry);
    }

    /**
     * Interprets a <code>org.opengis.feature.Feature</code> from a shapefile with bar information and
     * processes it with a {@link BarProcessor}.
     */
    public static class BarFeatureProcessor implements FeatureProcessor {

        /**
         * The name of the attribute in the shapefile that contains the name.
         */
        public static final String ATTRIBUTE_NAME = "name";

        /**
         * The name of the attribute in the shapefile that contains the URL.
         */
        public static final String ATTRIBUTE_URL = "url";

        /**
         * The name of the attribute in the shapefile that contains the geometric shape.
         */
        public static final String ATTRIBUTE_GEOMETRY = "the_geom";

        private BarProcessor barProcessor;

        /**
         * Creates a new <code>BarFeatureProcessor</code> that uses the given {@link BarProcessor}
         * to process a <code>Feature</code>.
         *
         * @param barProcessor the <code>BarProcessor</code> to use, must not be <code>null</code>
         */
        public BarFeatureProcessor(BarProcessor barProcessor) {
            if (barProcessor == null) {
                throw new IllegalArgumentException("barProcessor must not be null");
            }
            this.barProcessor = barProcessor;
        }

        /**
         * Reads bar information from a <code>org.opengis.feature.Feature</code> and processes this
         * information with the current {@link BarProcessor}.
         *
         * @param feature the <code>Feature</code> to process with the current <code>BarProcessor</code>
         */
        @Override
        public void processFeature(Feature feature) {
            String name = null;
            String url = null;
            String geometry = null;
            for (Property property : feature.getProperties()) {
                if (property.getName().toString().equals(ATTRIBUTE_NAME)) {
                    name = property.getValue().toString();
                } else if (property.getName().toString().equals(ATTRIBUTE_URL)) {
                    url = property.getValue().toString();
                } else if (property.getName().toString().equals(ATTRIBUTE_GEOMETRY)) {
                    geometry = property.getValue().toString();
                }
            }
            if (name == null || url == null || geometry == null) {
                throw new IllegalArgumentException("Missing property in shape file: " + ATTRIBUTE_NAME + "=" + name
                        + ", " + ATTRIBUTE_URL + "=" + url + ", " + ATTRIBUTE_GEOMETRY + "=" + geometry);
            }
            barProcessor.processBar(name, url, geometry);
        }
    }

}
