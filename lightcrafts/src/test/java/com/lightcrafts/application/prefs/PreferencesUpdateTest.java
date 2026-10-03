package com.lightcrafts.application.prefs;

import com.lightcrafts.domain.prefs.InteractiveUpdateFrequency;
import com.lightcrafts.domain.prefs.SaveFormat;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreferencesUpdateTest {

    @Test
    void settingMessageUpdatesModelWithoutProducingEffects() {
        var initial = model();

        var transition = PreferencesUpdate.update(
            initial, new PreferencesMessage.AutoSaveChanged(false)
        );

        assertTrue(initial.general().autoSave());
        assertFalse(transition.model().general().autoSave());
        assertTrue(transition.model().general().displayProfileAvailable());
        assertTrue(transition.effects().isEmpty());
    }

    @Test
    void applyProducesOneEffectContainingCurrentModel() {
        var current = PreferencesUpdate.update(
            model(), new PreferencesMessage.CheckForUpdateChanged(false)
        ).model();

        var transition = PreferencesUpdate.update(
            current, new PreferencesMessage.Apply()
        );

        assertSame(current, transition.model());
        assertEquals(
            List.of(new PreferencesEffect.ApplyPreferences(current)),
            transition.effects()
        );
    }

    @Test
    void revertingCopyrightRestoresDefaultsInModel() {
        var initial = model();
        var edited = PreferencesUpdate.update(
            initial, new PreferencesMessage.CopyrightChanged("Edited")
        ).model();

        var transition = PreferencesUpdate.update(
            edited, new PreferencesMessage.RevertCopyrightDefaults()
        );

        assertEquals("Default copyright",
            transition.model().copyright().copyrightText());
        assertEquals("Default creator",
            transition.model().copyright().creatorText());
        assertTrue(transition.effects().isEmpty());
    }

    @Test
    void cacheClearRequestRequiresConfirmation() {
        var initial = model();

        var transition = PreferencesUpdate.update(
            initial, new PreferencesMessage.ClearBrowserCache()
        );

        assertSame(initial, transition.model());
        assertEquals(
            List.of(new PreferencesEffect.ConfirmClearBrowserCache()),
            transition.effects()
        );
    }

    @Test
    void confirmedCacheClearProducesEffectWithoutChangingModel() {
        var initial = model();

        var transition = PreferencesUpdate.update(
            initial, new PreferencesMessage.BrowserCacheClearConfirmed()
        );

        assertSame(initial, transition.model());
        assertEquals(
            List.of(new PreferencesEffect.ClearGlobalBrowserCache()),
            transition.effects()
        );
    }

    @Test
    void cancelledCacheClearProducesNoEffect() {
        var initial = model();

        var transition = PreferencesUpdate.update(
            initial, new PreferencesMessage.BrowserCacheClearCancelled()
        );

        assertSame(initial, transition.model());
        assertTrue(transition.effects().isEmpty());
    }

    @Test
    void preferencesResetRequestRequiresConfirmation() {
        var initial = model();

        var transition = PreferencesUpdate.update(
            initial, new PreferencesMessage.ClearPreferences()
        );

        assertSame(initial, transition.model());
        assertEquals(
            List.of(new PreferencesEffect.ConfirmClearPreferences()),
            transition.effects()
        );
    }

    @Test
    void confirmedPreferencesResetProducesEffectWithoutChangingModel() {
        var initial = model();

        var transition = PreferencesUpdate.update(
            initial, new PreferencesMessage.ClearPreferencesConfirmed()
        );

        assertSame(initial, transition.model());
        assertEquals(
            List.of(new PreferencesEffect.ClearAllPreferences()),
            transition.effects()
        );
    }

    @Test
    void cancelledPreferencesResetProducesNoEffect() {
        var initial = model();

        var transition = PreferencesUpdate.update(
            initial, new PreferencesMessage.ClearPreferencesCancelled()
        );

        assertSame(initial, transition.model());
        assertTrue(transition.effects().isEmpty());
    }

    private static PreferencesModel model() {
        return new PreferencesModel(
            "en",
            List.of("", "en", "fr"),
            new PreferencesModel.GeneralSettings(
                256,
                2048,
                1024,
                InteractiveUpdateFrequency.NORMAL,
                "/tmp",
                true,
                "(sRGB default)",
                true,
                false,
                true,
                true,
                true,
                true,
                true,
                true,
                false
            ),
            SaveFormat.JPEG,
            new PreferencesModel.CopyrightSettings(
                "Default copyright",
                "Default creator",
                "Default copyright",
                "Default creator"
            )
        );
    }
}
