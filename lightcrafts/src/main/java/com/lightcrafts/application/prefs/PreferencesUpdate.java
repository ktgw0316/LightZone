package com.lightcrafts.application.prefs;

import com.lightcrafts.domain.prefs.SaveFormat;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public final class PreferencesUpdate {
    private PreferencesUpdate() {
    }

    public static @NotNull Transition update(
        PreferencesModel model, PreferencesMessage message
    ) {
        Objects.requireNonNull(model);
        Objects.requireNonNull(message);
        final var general = model.general();
        switch (message) {
            case PreferencesMessage.LanguageChanged(String language) -> {
                return unchanged(model.withLanguage(language));
            }
            case PreferencesMessage.MemoryLimitChanged(int value) -> {
                return unchanged(model.withGeneral(
                        general.withMemoryLimit(value)
                ));
            }
            case PreferencesMessage.InteractiveUpdateChanged(
                    com.lightcrafts.domain.prefs.InteractiveUpdateFrequency frequency
            ) -> {
                return unchanged(model.withGeneral(
                        general.withInteractiveUpdateFrequency(frequency)
                ));
            }
            case PreferencesMessage.ScratchDirectoryChanged(String path3) -> {
                return unchanged(model.withGeneral(
                        general.withScratchDirectory(path3)
                ));
            }
            case PreferencesMessage.DisplayProfileChanged(String path2) -> {
                return unchanged(model.withGeneral(
                        general.withDisplayProfilePath(path2)
                ));
            }
            case PreferencesMessage.BrowserCacheScopeChanged(boolean local) -> {
                return unchanged(model.withGeneral(
                        general.withLocalBrowserCache(local)
                ));
            }
            case PreferencesMessage.CheckForUpdateChanged(boolean enabled3) -> {
                return unchanged(model.withGeneral(
                        general.withCheckForUpdate(enabled3)
                ));
            }
            case PreferencesMessage.ExportDirectoryModeChanged(boolean original) -> {
                return unchanged(model.withGeneral(
                        general.withExportByOriginal(original)
                ));
            }
            case PreferencesMessage.SaveDirectoryModeChanged(boolean byOriginal) -> {
                return unchanged(model.withGeneral(
                        general.withSaveByOriginal(byOriginal)
                ));
            }
            case PreferencesMessage.AutoSaveChanged(boolean enabled2) -> {
                return unchanged(model.withGeneral(
                        general.withAutoSave(enabled2)
                ));
            }
            case PreferencesMessage.IntegrationChanged(boolean enabled1) -> {
                return unchanged(model.withGeneral(
                        general.withIntegrationEnabled(enabled1)
                ));
            }
            case PreferencesMessage.ToolTitlesChanged(boolean enabled) -> {
                return unchanged(model.withGeneral(
                        general.withShowToolTitles(enabled)
                ));
            }
            case PreferencesMessage.SaveFormatChanged(@NotNull SaveFormat format) -> {
                return unchanged(model.withSaveFormat(format));
            }
            case PreferencesMessage.CopyrightChanged(String text1) -> {
                return unchanged(model.withCopyright(
                        model.copyright().withCopyrightText(text1)
                ));
            }
            case PreferencesMessage.CreatorChanged(String text) -> {
                return unchanged(model.withCopyright(
                        model.copyright().withCreatorText(text)
                ));
            }
            case PreferencesMessage.RevertCopyrightDefaults _ -> {
                return unchanged(model.withCopyright(model.copyright().reverted()));
            }
            case PreferencesMessage.Apply _ -> {
                return new Transition(
                        model, List.of(new PreferencesEffect.ApplyPreferences(model))
                );
            }
            case PreferencesMessage.ChooseScratchDirectory _ -> {
                return new Transition(
                        model,
                        List.of(new PreferencesEffect.ChooseScratchDirectory(
                                general.scratchDirectory()
                        ))
                );
            }
            case PreferencesMessage.ChooseDisplayProfile _ -> {
                return new Transition(
                        model,
                        List.of(new PreferencesEffect.ChooseDisplayProfile(
                                general.displayProfilePath(),
                                !general.displayProfileEnabled()
                        ))
                );
            }
            case PreferencesMessage.ClearBrowserCache _ -> {
                return new Transition(
                        model, List.of(new PreferencesEffect.ConfirmClearBrowserCache())
                );
            }
            case PreferencesMessage.ClearPreferences _ -> {
                return new Transition(
                        model, List.of(new PreferencesEffect.ConfirmClearPreferences())
                );
            }
            case PreferencesMessage.BrowserCacheClearConfirmed _ -> {
                return new Transition(
                        model, List.of(new PreferencesEffect.ClearGlobalBrowserCache())
                );
            }
            case PreferencesMessage.ClearPreferencesConfirmed _ -> {
                return new Transition(
                        model, List.of(new PreferencesEffect.ClearAllPreferences())
                );
            }
            case PreferencesMessage.ScratchDirectoryChosen(String path1) -> {
                return unchanged(model.withGeneral(
                        general.withScratchDirectory(path1)
                ));
            }
            case PreferencesMessage.DisplayProfileChosen(String path) -> {
                return unchanged(model.withGeneral(
                        general.withDisplayProfilePath(path)
                ));
            }
            case PreferencesMessage.BrowserCacheCleared _,
                 PreferencesMessage.BrowserCacheClearCancelled _,
                 PreferencesMessage.ClearPreferencesCancelled _ -> {
                return unchanged(model);
            }
            default -> throw new IllegalStateException(
                    "Unhandled preferences message: " + message.getClass().getName()
            );
        }
    }

    @Contract("_ -> new")
    private static @NotNull Transition unchanged(PreferencesModel model) {
        return new Transition(model, List.of());
    }

    public record Transition(
        PreferencesModel model, List<PreferencesEffect> effects
    ) {
        public Transition {
            Objects.requireNonNull(model);
            effects = List.copyOf(effects);
        }
    }
}
