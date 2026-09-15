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

/** Checkpoint support for the Adventure Mode fork. */
public final class AdventureSaves {

    public static final String AUTO = "auto";
    public static final int MANUAL_SLOTS = 3;

    private static final String ROOT = "adventure_saves";
    private static final String GAME_FILE = "game.dat";
    private static final String META_SUFFIX = ".meta";
    private static final String TMP_SUFFIX = ".adventure_tmp";
    private static final String BACKUP_SUFFIX = ".adventure_backup";

    private AdventureSaves() {}

    private static String slotRoot(int slot) {
        return ROOT + "/game" + slot;
    }

    private static String checkpointPath(int slot, String id) {
        return slotRoot(slot) + "/" + id;
    }

    private static String checkpointMetaPath(int slot, String id) {
        return slotRoot(slot) + "/" + id + META_SUFFIX;
    }

    private static String manualId(int index) {
        return "manual" + index;
    }

    public static void clearForNewRun(int slot) {
        FileUtils.deleteDir(slotRoot(slot));
    }

    public static boolean manualExists(int index) {
        if (index < 1 || index > MANUAL_SLOTS) return false;
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
        Meta meta = validMetaForCurrentRun(id);
        return meta == null ? -1 : meta.depth;
    }

    public static int checkpointBranch(String id) {
        Meta meta = validMetaForCurrentRun(id);
        return meta == null ? -1 : meta.branch;
    }

    public static int manualDepth(int index) {
        if (index < 1 || index > MANUAL_SLOTS) return -1;
        return checkpointDepth(manualId(index));
    }

    public static int manualBranch(int index) {
        if (index < 1 || index > MANUAL_SLOTS) return -1;
        return checkpointBranch(manualId(index));
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
                && old.branch == Dungeon.branch
                && checkpointHasRequiredFiles(slot, AUTO, old)) {
            return;
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
            Dungeon.saveAll();
            copyCurrentGameTo(GamesInProgress.curSlot, manualId(index));
            return checkpointExistsForCurrentRun(manualId(index));
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

    public static boolean restoreBestAvailable() {
        if (restore(AUTO)) return true;
        for (int i = 1; i <= MANUAL_SLOTS; i++) {
            if (restore(manualId(i))) return true;
        }
        return false;
    }

    private static boolean restore(String id) {
        int slot = GamesInProgress.curSlot;
        if (!checkpointExistsForCurrentRun(id)) return false;

        FileHandle source = FileUtils.getFileHandle(checkpointPath(slot, id));
        FileHandle target = FileUtils.getFileHandle(GamesInProgress.gameFolder(slot));

        try {
            replaceDirectorySafely(source, target);
            GamesInProgress.setUnknown(slot);
            return true;
        } catch (IOException e) {
            ShatteredPixelDungeon.reportException(e);
            return false;
        }
    }

    private static boolean checkpointExistsForCurrentRun(String id) {
        return validMetaForCurrentRun(id) != null;
    }

    private static Meta validMetaForCurrentRun(String id) {
        int slot = GamesInProgress.curSlot;
        Meta meta = readMeta(slot, id);
        if (meta == null || meta.seed != Dungeon.seed) return null;
        if (!checkpointHasRequiredFiles(slot, id, meta)) return null;
        return meta;
    }

    private static boolean checkpointHasRequiredFiles(int slot, String id, Meta meta) {
        String base = checkpointPath(slot, id);
        if (!FileUtils.fileExists(base + "/" + GAME_FILE)) return false;

        String depthFile;
        if (meta.branch == 0) {
            depthFile = "depth" + meta.depth + ".dat";
        } else {
            depthFile = "depth" + meta.depth + "-branch" + meta.branch + ".dat";
        }
        return FileUtils.fileExists(base + "/" + depthFile);
    }

    private static void copyCurrentGameTo(int slot, String id) throws IOException {
        FileHandle source = FileUtils.getFileHandle(GamesInProgress.gameFolder(slot));
        FileHandle target = FileUtils.getFileHandle(checkpointPath(slot, id));
        replaceDirectorySafely(source, target);
        writeMeta(slot, id, Dungeon.seed, Dungeon.depth, Dungeon.branch);
    }

    private static void writeMeta(int slot, String id, long seed, int depth, int branch) throws IOException {
        try {
            FileHandle file = FileUtils.getFileHandle(checkpointMetaPath(slot, id));
            file.writeString(seed + "\n" + depth + "\n" + branch + "\n", false, "UTF-8");
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
    }

    private static Meta readMeta(int slot, String id) {
        FileHandle sidecar = FileUtils.getFileHandle(checkpointMetaPath(slot, id));
        if (sidecar.exists() && !sidecar.isDirectory() && sidecar.length() > 0) {
            try {
                String[] lines = sidecar.readString("UTF-8").trim().split("\\n");
                if (lines.length >= 3) {
                    Meta result = new Meta();
                    result.seed = Long.parseLong(lines[0].trim());
                    result.depth = Integer.parseInt(lines[1].trim());
                    result.branch = Integer.parseInt(lines[2].trim());
                    return result;
                }
            } catch (Exception ignored) {
                // Fall through to game.dat for compatibility with older checkpoints.
            }
        }

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

    /**
     * Safely replaces a directory without FileHandle.moveTo().
     *
     * libGDX's moveTo() falls back to copyTo() for FileType.Local. For a directory,
     * copyTo() copies the source directory itself below the destination, which adds
     * an unwanted extra path component. Shattered uses FileType.Local on Android,
     * so checkpoints such as manual1 ended up as manual1/manual1.adventure_tmp/... .
     */
    private static void replaceDirectorySafely(FileHandle source, FileHandle target) throws IOException {
        if (source == null || !source.exists() || !source.isDirectory()) {
            throw new IOException("checkpoint directory does not exist: " + source);
        }

        FileHandle parent = target.parent();
        FileHandle temp = parent.child(target.name() + TMP_SUFFIX);
        FileHandle backup = parent.child(target.name() + BACKUP_SUFFIX);
        boolean hadTarget = target.exists();
        boolean backupReady = false;

        try {
            deleteRequired(temp);
            temp.mkdirs();
            if (!temp.exists() || !temp.isDirectory()) {
                throw new IOException("cannot create temp checkpoint directory: " + temp);
            }
            copyChildren(source, temp);
            if (!temp.child(GAME_FILE).exists()) {
                throw new IOException("temp checkpoint is missing " + GAME_FILE);
            }

            deleteRequired(backup);
            if (hadTarget) {
                backup.mkdirs();
                if (!backup.exists() || !backup.isDirectory()) {
                    throw new IOException("cannot create checkpoint backup: " + backup);
                }
                copyChildren(target, backup);
                backupReady = true;
            }

            deleteRequired(target);
            target.mkdirs();
            if (!target.exists() || !target.isDirectory()) {
                throw new IOException("cannot create checkpoint target: " + target);
            }
            copyChildren(temp, target);
            if (!target.child(GAME_FILE).exists()) {
                throw new IOException("checkpoint target is missing " + GAME_FILE);
            }

            deleteRequired(backup);
            backupReady = false;

        } catch (IOException | RuntimeException e) {
            try {
                deleteRequired(target);
                if (backupReady && backup.exists()) {
                    target.mkdirs();
                    copyChildren(backup, target);
                }
            } catch (Exception rollbackError) {
                e.addSuppressed(rollbackError);
            }

            if (e instanceof IOException) throw (IOException)e;
            throw new IOException(e);

        } finally {
            try {
                deleteRequired(temp);
            } catch (IOException ignored) {
                // Best effort cleanup only.
            }
            if (!backupReady) {
                try {
                    deleteRequired(backup);
                } catch (IOException ignored) {
                    // Best effort cleanup only.
                }
            }
        }
    }

    private static void deleteRequired(FileHandle file) throws IOException {
        if (file == null || !file.exists()) return;

        boolean deleted = file.isDirectory() ? file.deleteDirectory() : file.delete();
        if (!deleted && file.exists()) {
            throw new IOException("cannot delete: " + file);
        }
    }

    private static void copyChildren(FileHandle source, FileHandle target) {
        if (!target.exists()) target.mkdirs();
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
