package com.lightcrafts.platform.macosx;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class MacOSXColorProfileManagerTest {

    @Test
    public void getSystemDisplayProfilePath_returnsExistingPathOnMac_whenNativeAvailable() {
        // Only run this test on macOS
        String os = System.getProperty("os.name").toLowerCase();
        Assumptions.assumeTrue(os.contains("mac"), "Only runs on macOS");

        // Try to load the native library first. If not available, skip the test.
        try {
            System.loadLibrary("MacOSX");
        } catch (UnsatisfiedLinkError e) {
            Assumptions.assumeTrue(false, "Native library MacOSX not available: " + e.getMessage());
            return; // unreachable but keeps compiler happy
        }

        // Ensure the class initializer runs (it also calls System.loadLibrary, but that's fine)
        try {
            Class.forName("com.lightcrafts.platform.macosx.MacOSXColorProfileManager");
        } catch (ClassNotFoundException e) {
            Assumptions.assumeTrue(false, "Class not found: " + e.getMessage());
        }

        String path = MacOSXColorProfileManager.getSystemDisplayProfilePath();
        assertThat(path).as("profile path").isNotNull();
        File f = new File(path);
        assertThat(f.exists()).as("profile file exists").isTrue();
    }
}

