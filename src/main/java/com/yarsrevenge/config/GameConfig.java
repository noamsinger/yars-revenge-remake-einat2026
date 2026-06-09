package com.yarsrevenge.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;

public final class GameConfig {

    private static final Logger log = LoggerFactory.getLogger(GameConfig.class);

    public enum GameMode { NOVICE, NORMAL, REBOUND, ULTIMATE }

    private static final Path CONFIG_DIR  = Path.of(System.getProperty("user.home"), ".config", "yars-revenge-remake-einat2026");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.json");

    private static GameConfig instance;

    // Settings with defaults
    private boolean  audioEnabled = true;
    private int      fpsLimit     = 30;           // 6 | 15 | 30
    private String   resolution   = "FIT";        // "FIT" | "1920x1080" | "1024x576"
    private GameMode gameMode     = GameMode.NORMAL;

    private GameConfig() { load(); }

    public static GameConfig getInstance() {
        if (instance == null) instance = new GameConfig();
        return instance;
    }

    // ---- getters ----

    public boolean  isAudioEnabled() { return audioEnabled; }
    public int      getFpsLimit()    { return fpsLimit; }
    public String   getResolution()  { return resolution; }
    public GameMode getGameMode()    { return gameMode; }

    // ---- setters — each calls save() ----

    public void setAudioEnabled(boolean v)  { audioEnabled = v; save(); }
    public void setFpsLimit(int v)          { fpsLimit = v;     save(); }
    public void setResolution(String v)     { resolution = v;   save(); }
    public void setGameMode(GameMode v)     { gameMode = v;     save(); }

    // ---- resolution helpers ----

    public int getResolutionWidth() {
        return switch (resolution) {
            case "1920x1080" -> 1920;
            case "1024x576"  -> 1024;
            case "3840x2160" -> 3840;
            default          -> -1; // FIT — caller uses screen size
        };
    }

    public int getResolutionHeight() {
        return switch (resolution) {
            case "1920x1080" -> 1080;
            case "1024x576"  ->  576;
            case "3840x2160" -> 2160;
            default          ->  -1;
        };
    }

    // ---- persistence ----

    public void load() {
        if (!Files.exists(CONFIG_FILE)) return;
        try {
            String text = Files.readString(CONFIG_FILE);
            audioEnabled = parseBool(text, "audioEnabled", audioEnabled);
            fpsLimit     = parseInt(text,  "fpsLimit",     fpsLimit);
            resolution   = parseStr(text,  "resolution",   resolution);
            String gm    = parseStr(text,  "gameMode",     gameMode.name());
            // Validate
            if (fpsLimit != 6 && fpsLimit != 15 && fpsLimit != 30) fpsLimit = 30;
            if (!resolution.equals("FIT") && !resolution.equals("1920x1080")
                    && !resolution.equals("1024x576") && !resolution.equals("3840x2160"))
                resolution = "FIT";
            try { gameMode = GameMode.valueOf(gm); } catch (IllegalArgumentException e) { gameMode = GameMode.NORMAL; }
        } catch (IOException e) {
            // Use defaults on any IO error
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_DIR);
            String json = "{\n"
                + "  \"audioEnabled\": " + audioEnabled + ",\n"
                + "  \"fpsLimit\": " + fpsLimit + ",\n"
                + "  \"resolution\": \"" + resolution + "\",\n"
                + "  \"gameMode\": \"" + gameMode.name() + "\"\n"
                + "}\n";
            Files.writeString(CONFIG_FILE, json);
        } catch (IOException e) {
            log.error("Could not save config", e);
        }
    }

    // ---- minimal JSON value extractors ----

    private static boolean parseBool(String json, String key, boolean def) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return def;
        String after = json.substring(idx + key.length() + 2).stripLeading();
        if (after.startsWith(":")) after = after.substring(1).stripLeading();
        if (after.startsWith("true"))  return true;
        if (after.startsWith("false")) return false;
        return def;
    }

    private static int parseInt(String json, String key, int def) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return def;
        String after = json.substring(idx + key.length() + 2).stripLeading();
        if (after.startsWith(":")) after = after.substring(1).stripLeading();
        StringBuilder sb = new StringBuilder();
        for (char c : after.toCharArray()) {
            if (Character.isDigit(c)) sb.append(c);
            else if (!sb.isEmpty()) break;
        }
        if (sb.isEmpty()) return def;
        try { return Integer.parseInt(sb.toString()); } catch (NumberFormatException e) { return def; }
    }

    private static String parseStr(String json, String key, String def) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return def;
        String after = json.substring(idx + key.length() + 2).stripLeading();
        if (after.startsWith(":")) after = after.substring(1).stripLeading();
        if (!after.startsWith("\"")) return def;
        int start = after.indexOf('"') + 1;
        int end   = after.indexOf('"', start);
        if (end < 0) return def;
        return after.substring(start, end);
    }
}
