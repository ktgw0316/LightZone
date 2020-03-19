/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.app;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckForUpdateTest {
    private final URL resource = getClass().getClassLoader().getResource("appcast-4.1.8.xml");

    @Test
    public void checkIfUpdateIsAvailable() {
        assertThat(CheckForUpdate.checkIfUpdateIsAvailable("4.1.7", resource)).isTrue();
        assertThat(CheckForUpdate.checkIfUpdateIsAvailable("4.1.8", resource)).isFalse();
        assertThat(CheckForUpdate.checkIfUpdateIsAvailable("4.1.9", resource)).isFalse();
    }

    @Test
    public void checkIfUpdateIsAvailableForBetas() {
        assertThat(CheckForUpdate.checkIfUpdateIsAvailable("4.1.7alpha1", resource)).isTrue();
        assertThat(CheckForUpdate.checkIfUpdateIsAvailable("4.1.7beta1", resource)).isTrue();
        assertThat(CheckForUpdate.checkIfUpdateIsAvailable("4.1.7rc1", resource)).isTrue();

        assertThat(CheckForUpdate.checkIfUpdateIsAvailable("4.1.8alpha1", resource)).isTrue();
        assertThat(CheckForUpdate.checkIfUpdateIsAvailable("4.1.8beta1", resource)).isTrue();
        assertThat(CheckForUpdate.checkIfUpdateIsAvailable("4.1.8rc1", resource)).isTrue();

        assertThat(CheckForUpdate.checkIfUpdateIsAvailable("4.1.9alpha1", resource)).isFalse();
        assertThat(CheckForUpdate.checkIfUpdateIsAvailable("4.1.9beta1", resource)).isFalse();
        assertThat(CheckForUpdate.checkIfUpdateIsAvailable("4.1.9rc1", resource)).isFalse();
    }
}