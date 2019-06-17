/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.GPSDirectory;
import com.lightcrafts.image.metadata.ImageMetadata;
import lombok.val;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
 */
public class GPSMetadataEntry extends MetadataEntry {

    private static final String OSM_TILE_URL = "https://tile.openstreetmap.org/";
    private static final String OSM_USER_AGENT =
            "LightZone/1.0 (https://github.com/ktgw0316/LightZone; contact: arctica0316@gmail.com)";

    @Override
    public String getLabel(ImageMetadata meta) {
        return "GPS"; // TODO: LOCALE.get("GPSLabel");
    }

    @Override
    public Object getValue(ImageMetadata meta) {
        final GPSDirectory dir =
                (GPSDirectory) meta.getDirectoryFor(GPSDirectory.class);
        if (dir == null) {
            return "";
        }
        val lat = dir.getGPSLatitude();
        val lon = dir.getGPSLongitude();
        if (lat == null || lon == null) {
            return "";
        }
        val icon = getMapTileImageIcon(lat, lon, 17);
        if (icon != null) {
            return icon;
        }
        return getDMS(dir);
    }

    private String getDMS(GPSDirectory dir) {
        val latDMS = dir.getGPSLatitudeDMS();
        val lonDMS = dir.getGPSLongitudeDMS();
        return (latDMS.isEmpty() || lonDMS.isEmpty())
                ? ""
                : latDMS + ", " + lonDMS;
    }

    @Override
    boolean isEditable(ImageInfo info) {
        return false;
    }

    @Override
    boolean isValidValue(ImageMetadata meta, String value) {
        return true;
    }

    @Override
    void setValue(ImageMetadata meta, String value) {
        // readonly
    }

    @Override
    URI getURI(ImageMetadata meta) {
        final GPSDirectory dir =
                (GPSDirectory) meta.getDirectoryFor(GPSDirectory.class);
        if (dir == null) {
            return null;
        }
        Double latitude  = dir.getGPSLatitude();
        Double longitude = dir.getGPSLongitude();
        if (latitude == null || longitude == null) {
            return null;
        }

        final int zoom = 17;
        try {
            return new URI("https://www.openstreetmap.org/#map="
                    + zoom + "/" + latitude + "/" + longitude);
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    // cf. https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Java
    private static String getOpenStreetMapTileNumber(double lat, double lon, int zoom) {
        int xtile = (int)Math.floor((lon + 180) / 360 * (1<<zoom));
        int ytile = (int)Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat))
                + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom));
        if (xtile < 0)
            xtile = 0;
        if (xtile >= (1 << zoom))
            xtile = (1 << zoom) - 1;
        if (ytile < 0)
            ytile = 0;
        if (ytile >= (1 << zoom))
            ytile = (1 << zoom) - 1;
        return("" + zoom + "/" + xtile + "/" + ytile);
    }

    private static ImageIcon getMapTileImageIcon(double lat, double lon, int zoom) {
        try {
            val url = new URL(OSM_TILE_URL + getOpenStreetMapTileNumber(lat, lon, zoom) + ".png");
            val con = (HttpURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", OSM_USER_AGENT);
            con.setRequestProperty("Accept", "image/png,image/*,*/*;q=0.8");
            con.setRequestProperty("Referer", "https://www.openstreetmap.org/");
            con.setConnectTimeout(2000);
            con.setReadTimeout(2000);
            con.setInstanceFollowRedirects(true);
            val response = con.getResponseCode();
            if (response != HttpURLConnection.HTTP_OK) {
                return null;
            }
            try (val in = con.getInputStream()) {
                val img = ImageIO.read(in);
                return img == null ? null : new AttributedMapIcon(img);
            }
        } catch (IOException ignored) {
            return null;
        }
    }

    private static final class AttributedMapIcon extends ImageIcon {
        private static final String ATTRIBUTION = "© OpenStreetMap contributors";

        private AttributedMapIcon(Image image) {
            super(image);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            super.paintIcon(c, g, x, y);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                int width = getIconWidth();
                int height = getIconHeight();
                Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
                FontMetrics metrics = g2.getFontMetrics(font);
                Rectangle2D textBounds = metrics.getStringBounds(ATTRIBUTION, g2);

                int padX = 8;
                int padY = 4;
                int boxWidth = (int) Math.ceil(textBounds.getWidth()) + padX * 2;
                int boxHeight = (int) Math.ceil(textBounds.getHeight()) + padY * 2;
                int boxX = x + width - boxWidth - 8;
                int boxY = y + height - boxHeight - 8;

                g2.setColor(new Color(255, 255, 255, 180));
                g2.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 6, 6);
                g2.setColor(new Color(32, 32, 32, 210));
                g2.setFont(font);
                g2.drawString(ATTRIBUTION, boxX + padX, boxY + boxHeight - padY - 1);
            } finally {
                g2.dispose();
            }
        }
    }
}
