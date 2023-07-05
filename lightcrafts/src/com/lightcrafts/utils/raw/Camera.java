package com.lightcrafts.utils.raw;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Camera {
    static final boolean Debug = System.getProperty("lightcrafts.debug") != null;

    public static String getCompatibleCameraName(@NotNull String name) {
        name = name.replace('*', '_') // For the Pentax *ist
                .replace('/', '_')
                .replace(':', '_');
        final var resource = DCRaw.class.getResourceAsStream("resources/CompatibleCameras.properties");
        if (resource == null) {
            return name;
        }
        final Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(resource, UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            return name;
        }
        return Optional.ofNullable(properties.getProperty(name)).orElse(name);
    }

    public static URL getLztUrl(@NotNull String name) {
        name = Camera.getCompatibleCameraName(name);
        final URL url = Camera.class.getResource("resources/" + name + ".lzt");
        if ((url == null) && Debug) {
            System.err.println("No default Document for \"" + name + "\"");
        }
        return url;
    }
}
