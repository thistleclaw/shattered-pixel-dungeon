/*
 * Shattered Pixel Dungeon - Adventure Mode additions
 * Copyright (C) 2026
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon;

import com.badlogic.gdx.files.FileHandle;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.io.IOException;

/**
 * Checkpoint support for the Adventure Mode fork.
 *
 * Shattered already serializes a complete run into gameN/.  AdventureSaves
 * simply snapshots that directory outside the normal run folder, so regular
 * death cleanup cannot destroy the checkpoints.
 */
public final class AdventureSaves {

    public static final String AUTO = "auto";
    public static final int MANUAL_SLOTS = 3;

    private static final String ROOT = "adventure_saves";
    private static final String GAME_FILE = "game.dat";

    private AdventureSaves() {}

    private static String slotRoot(int slot) {
        return ROOT + "/game" + slot;
    }

    private static String checkpointPath(int slot, String id) {
        return slotRoot(slot) + "/" + id;
    }

    private static String manualId(int index) {
        return "manual" + index;
    }

    public static boolean manualExists(int index) {
        return checkpointExistsForCurrentRun(manualId(index));
    }

    public static boolean autoExists() {
        return checkpointExistsForCurrentRun(AUTO);
    }

    public static boolean anyCheckpointExists() {
        if (autoExists()) return true;
        for (int i = 1; i <= MANUAL_SLOTS; i++) {
            if (manualExists(i)) return true;
        }
        return false;
    }

    public static int checkpointDepth(String id) {
        Meta meta = readMeta(GamesInProgress.curSlot, id);
        if (meta == null || meta.seed != Dungeon.seed) return -1;
        return meta.depth;
    }

    public static int manualDepth(int index) {
        return checkpointDepth(manualId(index));
    }

    /** Called after the normal game save is complete. */
    public static void maybeAutoSave(int slot) {
        if (Dungeon.hero == null || !Dungeon.hero.isAlive() || !GamesInProgress.gameExists(slot)) {
            return;
        }

        Meta old = readMeta(slot, AUTO);
        if (old != null
                && old.seed == Dungeon.seed
                && old.depth == Dungeon.depth
                && old.branch == Dungeon.branch) {
            return; //already have the entry checkpoint for this floor
        }

        try {
            copyCurrentGameTo(slot, AUTO);
        } catch (IOException e) {
            ShatteredPixelDungeon.reportException(e);
        }
    }

    public static boolean saveManual(int index) {
        if (index < 1 || index > MANUAL_SLOTS || Dungeon.hero == null || !Dungeon.hero.isAlive()) {
            return false;
        }

        try {
            //Make sure the snapshot contains the exact current turn.
            Dungeon.saveAll();
            copyCurrentGameTo(GamesInProgress.curSlot, manualId(index));
            return true;
        } catch (IOException e) {
            ShatteredPixelDungeon.reportException(e);
            return false;
        }
    }

    public static boolean restoreAuto() {
        return restore(AUTO);
    }

    public static boolean restoreManual(int index) {
        if (index < 1 || index > MANUAL_SLOTS) return false;
        return restore(manualId(index));
    }

    private static boolean restore(String id) {
        int slot = GamesInProgress.curSlot;
        if (!checkpointExistsForCurrentRun(id)) return false;

        FileHandle source = FileUtils.getFileHandle(checkpointPath(slot, id));
        FileHandle target = FileUtils.getFileHandle(GamesInProgress.gameFolder(slot));

        try {
            replaceDirectory(source, target);
            GamesInProgress.setUnknown(slot);
            return true;
        } catch (IOException e) {
            ShatteredPixelDungeon.reportException(e);
            return false;
        }
    }

    private static boolean checkpointExistsForCurrentRun(String id) {
        Meta meta = readMeta(GamesInProgress.curSlot, id);
        return meta != null && meta.seed == Dungeon.seed;
    }

    private static void copyCurrentGameTo(int slot, String id) throws IOException {
        FileHandle source = FileUtils.getFileHandle(GamesInProgress.gameFolder(slot));
        FileHandle target = FileUtils.getFileHandle(checkpointPath(slot, id));
        replaceDirectory(source, target);
    }

    private static Meta readMeta(int slot, String id) {
        String file = checkpointPath(slot, id) + "/" + GAME_FILE;
        if (!FileUtils.fileExists(file)) return null;

        try {
            Bundle bundle = FileUtils.bundleFromFile(file);
            Meta result = new Meta();
            result.seed = bundle.getLong("seed");
            result.depth = bundle.getInt("depth");
            result.branch = bundle.getInt("branch");
            return result;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void replaceDirectory(FileHandle source, FileHandle target) throws IOException {
        if (source == null || !source.exists() || !source.isDirectory()) {
            throw new IOException("checkpoint directory does not exist");
        }

        try {
            if (target.exists()) target.deleteDirectory();
            target.mkdirs();
            copyChildren(source, target);
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
    }

    private static void copyChildren(FileHandle source, FileHandle target) {
        for (FileHandle child : source.list()) {
            FileHandle out = target.child(child.name());
            if (child.isDirectory()) {
                out.mkdirs();
                copyChildren(child, out);
            } else {
                out.writeBytes(child.readBytes(), false);
            }
        }
    }

    private static class Meta {
        long seed;
        int depth;
        int branch;
    }
}
