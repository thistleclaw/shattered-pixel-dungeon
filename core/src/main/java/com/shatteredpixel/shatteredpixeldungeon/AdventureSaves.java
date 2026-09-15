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
 * Shattered already serializes a complete run into gameN/. AdventureSaves
 * snapshots that directory outside the normal run folder, so regular death
 * cleanup cannot destroy the checkpoints.
 */
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

    /**
     * Called when a save slot starts a genuinely new run. Manual/auto saves are
     * intentionally per-run, so old checkpoints from a previous run in the same
     * slot must not leak into a new game (especially with reused custom seeds).
     */
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
            return; //already have the entry checkpoint for this floor/branch
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

            //Do not immediately re-open the files we have just renamed/written.
            //On some old Android/Linux storage stacks (notably 4.x-era devices)
            //the directory metadata can lag very briefly after moveTo(), making
            //an immediate exists()/length() check report a false negative even
            //though the checkpoint is already present. A later menu refresh sees
            //the exact same checkpoint correctly. If copy + metadata write did
            //not throw, the save operation itself succeeded.
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

    /**
     * Used after final death, when Shattered has already invalidated gameN/.
     * Prefer the floor-entry autosave, but if it is unavailable keep the run
     * recoverable from the first valid manual slot instead of deleting it.
     */
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

        //Keep checkpoint metadata OUTSIDE the copied game directory. This makes
        //the UI/validation independent of reparsing a copied Shattered game.dat
        //and avoids leaking Adventure metadata back into gameN/ on restore.
        writeMeta(slot, id, Dungeon.seed, Dungeon.depth, Dungeon.branch);
    }

    private static void writeMeta(int slot, String id, long seed, int depth, int branch) throws IOException {
        try {
            FileHandle file = FileUtils.getFileHandle(checkpointMetaPath(slot, id));
            FileHandle parent = file.parent();
            if (!parent.exists()) parent.mkdirs();
            file.writeString(seed + "\n" + depth + "\n" + branch + "\n", false, "UTF-8");
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
    }

    private static Meta readMeta(int slot, String id) {
        //Adventure2+ metadata: simple sidecar text file. It is deliberately
        //boring so it behaves consistently even on Android 4.4's old org.json.
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
                //Fall through to game.dat for compatibility with older checkpoints.
            }
        }

        //Compatibility fallback for checkpoints made by early Adventure builds.
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
     * Replaces a directory without deleting the previous copy until the new
     * copy has been written completely. This matters on older devices and also
     * protects a manual checkpoint from a failed overwrite.
     */
    private static void replaceDirectorySafely(FileHandle source, FileHandle target) throws IOException {
        if (source == null || !source.exists() || !source.isDirectory()) {
            throw new IOException("checkpoint directory does not exist");
        }

        FileHandle parent = target.parent();
        FileHandle temp = parent.child(target.name() + TMP_SUFFIX);
        FileHandle backup = parent.child(target.name() + BACKUP_SUFFIX);

        try {
            if (temp.exists()) temp.deleteDirectory();
            temp.mkdirs();
            copyChildren(source, temp);

            if (backup.exists()) backup.deleteDirectory();
            if (target.exists()) target.moveTo(backup);

            try {
                temp.moveTo(target);
            } catch (RuntimeException moveError) {
                if (target.exists()) target.deleteDirectory();
                if (backup.exists()) backup.moveTo(target);
                throw moveError;
            }

            if (backup.exists()) backup.deleteDirectory();

        } catch (RuntimeException e) {
            //Best-effort rollback. Never intentionally delete the only good copy.
            try {
                if (!target.exists() && backup.exists()) backup.moveTo(target);
            } catch (RuntimeException ignored) {
                //The original exception is more useful to report.
            }
            throw new IOException(e);
        } finally {
            if (temp.exists()) temp.deleteDirectory();
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
