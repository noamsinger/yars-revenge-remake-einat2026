package com.yarsrevenge.app;

import ch.qos.logback.core.PropertyDefinerBase;

/**
 * Resolves the log directory at runtime so logback.xml stays portable.
 * Logs land in ~/Library/Logs/yars-revenge on macOS, ~/.local/share/yars-revenge/logs elsewhere.
 */
public class LogDirDefiner extends PropertyDefinerBase {

    @Override
    public String getPropertyValue() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home", ".");
        if (os.contains("mac")) {
            return home + "/Library/Logs/yars-revenge";
        }
        return home + "/.local/share/yars-revenge/logs";
    }
}
