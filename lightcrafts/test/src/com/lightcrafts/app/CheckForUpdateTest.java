/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.app;

import org.junit.Test;

import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CheckForUpdateTest {
    private final URL resource = getClass().getClassLoader().getResource("appcast-4.1.8.xml");

    @Test
    public void checkIfUpdateIsAvailable() {
        assertTrue(CheckForUpdate.checkIfUpdateIsAvailable("4.1.7", resource));
        assertFalse(CheckForUpdate.checkIfUpdateIsAvailable("4.1.8", resource));
        assertFalse(CheckForUpdate.checkIfUpdateIsAvailable("4.1.9", resource));
    }

    @Test
    public void checkIfUpdateIsAvailableForBetas() {
        assertTrue(CheckForUpdate.checkIfUpdateIsAvailable("4.1.7alpha1", resource));
        assertTrue(CheckForUpdate.checkIfUpdateIsAvailable("4.1.7beta1", resource));
        assertTrue(CheckForUpdate.checkIfUpdateIsAvailable("4.1.7rc1", resource));

        assertTrue(CheckForUpdate.checkIfUpdateIsAvailable("4.1.8alpha1", resource));
        assertTrue(CheckForUpdate.checkIfUpdateIsAvailable("4.1.8beta1", resource));
        assertTrue(CheckForUpdate.checkIfUpdateIsAvailable("4.1.8rc1", resource));

        assertFalse(CheckForUpdate.checkIfUpdateIsAvailable("4.1.9alpha1", resource));
        assertFalse(CheckForUpdate.checkIfUpdateIsAvailable("4.1.9beta1", resource));
        assertFalse(CheckForUpdate.checkIfUpdateIsAvailable("4.1.9rc1", resource));
    }
}