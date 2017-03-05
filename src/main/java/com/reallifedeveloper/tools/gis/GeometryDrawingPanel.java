package com.reallifedeveloper.tools.gis;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.geotools.geometry.jts.LiteShape;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * A <code>JPanel</code> that draws <code>com.vividsolutions.jts.geom.Geometry</code> objects.
 *
 * @author RealLifeDeveloper
 */
public class GeometryDrawingPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int MARGIN = 5;

    private List<Geometry> geometries = new ArrayList<Geometry>();
    private AffineTransform geomToScreen;

    /**
     * Adds a <code>Geometry</code> object to be drawn.
     *
     * @param geometry the <code>Geometry</code> object to add
     */
    public void addGeometry(Geometry geometry) {
        geometries.add(geometry);
    }

    /**
     * Removes all <code>Geometry</code> objects so that none are drawn.
     */
    public void clearGeometries() {
        geometries.clear();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (!geometries.isEmpty()) {
            setTransform();

            Graphics2D g2d = (Graphics2D) g;
            Paint defaultPaint = Color.BLUE;

            for (Geometry geom : geometries) {
                LiteShape shape = new LiteShape(geom, geomToScreen, false);

                g2d.setPaint(defaultPaint);
                g2d.draw(shape);
            }
        }
    }

    private void setTransform() {
        Envelope env = getGeometryBounds();
        Rectangle visRect = getVisibleRect();
        Rectangle drawingRect =
                new Rectangle(visRect.x + MARGIN, visRect.y + MARGIN, visRect.width - 2 * MARGIN, visRect.height
                        - 2 * MARGIN);

        double scale = Math.min(drawingRect.getWidth() / env.getWidth(), drawingRect.getHeight() / env.getHeight());
        double xoff = MARGIN - scale * env.getMinX();
        double yoff = MARGIN + env.getMaxY() * scale;
        geomToScreen = new AffineTransform(scale, 0, 0, -scale, xoff, yoff);
    }

    private Envelope getGeometryBounds() {
        Envelope env = new Envelope();
        for (Geometry geom : geometries) {
            Envelope geomEnv = geom.getEnvelopeInternal();
            env.expandToInclude(geomEnv);
        }

        return env;
    }
}
