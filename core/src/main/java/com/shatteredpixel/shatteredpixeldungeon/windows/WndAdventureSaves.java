/*
 * Shattered Pixel Dungeon - Adventure Mode additions
 * Copyright (C) 2026
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.AdventureSaves;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.AdventureMessages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

public class WndAdventureSaves extends WndOptions {

    public WndAdventureSaves() {
        super(
                msg("title"),
                msg("description"),
                autoLabel(),
                manualLabel(1),
                manualLabel(2),
                manualLabel(3)
        );
    }

    private static String msg(String key, Object... args) {
        return AdventureMessages.get(key, args);
    }

    private static String location(int depth, int branch) {
        if (depth <= 0) return "";
        if (branch == 0) return msg("location_floor", depth);

        // Branch 1 is used by both quest sub-dungeons in v4.
        if (branch == 1 && depth >= 11 && depth <= 14) {
            return msg("location_mine", depth);
        }
        if (branch == 1 && depth >= 16 && depth <= 19) {
            return msg("location_vault", depth);
        }
        return msg("location_branch", depth, branch);
    }

    private static String autoLabel() {
        int depth = AdventureSaves.checkpointDepth(AdventureSaves.AUTO);
        int branch = AdventureSaves.checkpointBranch(AdventureSaves.AUTO);
        return depth > 0 ? msg("auto_label", location(depth, branch)) : msg("auto_empty");
    }

    private static String manualLabel(int slot) {
        int depth = AdventureSaves.manualDepth(slot);
        int branch = AdventureSaves.manualBranch(slot);
        return depth > 0 ? msg("manual_label", slot, location(depth, branch)) : msg("manual_empty", slot);
    }

    private static boolean heroAlive() {
        return Dungeon.hero != null && Dungeon.hero.isAlive();
    }

    @Override
    protected boolean enabled(int index) {
        if (index == 0) return AdventureSaves.autoExists();
        int slot = index;
        return heroAlive() || AdventureSaves.manualExists(slot);
    }

    @Override
    protected void onSelect(int index) {
        if (index == 0) {
            if (heroAlive()) {
                confirmLoadAuto();
            } else {
                loadAuto();
            }
        } else {
            int slot = index;
            if (heroAlive()) {
                showManualSlot(slot);
            } else {
                loadManual(slot);
            }
        }
    }

    private void showManualSlot(final int slot) {
        final boolean exists = AdventureSaves.manualExists(slot);
        final int depth = AdventureSaves.manualDepth(slot);
        final int branch = AdventureSaves.manualBranch(slot);

        GameScene.show(new WndOptions(
                msg("slot_title", slot),
                exists ? msg("slot_saved", location(depth, branch)) : msg("slot_empty"),
                exists ? msg("overwrite") : msg("save_here"),
                msg("load")
        ) {
            @Override
            protected boolean enabled(int index) {
                return index == 0 || exists;
            }

            @Override
            protected void onSelect(int index) {
                if (index == 0) {
                    if (exists) {
                        confirmOverwrite(slot);
                    } else {
                        saveManualAndRefresh(slot);
                    }
                } else {
                    confirmLoadManual(slot);
                }
            }
        });
    }

    private void confirmOverwrite(final int slot) {
        GameScene.show(new WndOptions(
                msg("overwrite_title", slot),
                msg("overwrite_body"),
                msg("overwrite"),
                msg("cancel")
        ) {
            @Override
            protected void onSelect(int index) {
                if (index == 0) saveManualAndRefresh(slot);
            }
        });
    }

    private static void confirmLoadAuto() {
        int depth = AdventureSaves.checkpointDepth(AdventureSaves.AUTO);
        int branch = AdventureSaves.checkpointBranch(AdventureSaves.AUTO);
        GameScene.show(new WndOptions(
                msg("load_auto_title"),
                msg("load_auto_body", location(depth, branch)),
                msg("load"),
                msg("cancel")
        ) {
            @Override
            protected void onSelect(int index) {
                if (index == 0) loadAuto();
            }
        });
    }

    private static void confirmLoadManual(final int slot) {
        int depth = AdventureSaves.manualDepth(slot);
        int branch = AdventureSaves.manualBranch(slot);
        GameScene.show(new WndOptions(
                msg("load_manual_title", slot),
                msg("load_manual_body", location(depth, branch)),
                msg("load"),
                msg("cancel")
        ) {
            @Override
            protected void onSelect(int index) {
                if (index == 0) loadManual(slot);
            }
        });
    }

    private void saveManualAndRefresh(int slot) {
        if (AdventureSaves.saveManual(slot)) {
            GLog.p(msg("saved", slot));
            // WndOptions snapshots labels/enabled state in its constructor, so
            // rebuild it after a write to show the new slot immediately.
            hide();
            GameScene.show(new WndAdventureSaves());
        } else {
            GLog.w(msg("save_failed"));
        }
    }

    private static void loadAuto() {
        if (AdventureSaves.restoreAuto()) {
            continueFromCheckpoint();
        } else {
            GLog.w(msg("auto_failed"));
        }
    }

    private static void loadManual(int slot) {
        if (AdventureSaves.restoreManual(slot)) {
            continueFromCheckpoint();
        } else {
            GLog.w(msg("manual_failed"));
        }
    }

    private static void continueFromCheckpoint() {
        // Follow the normal Continue path: throw away the in-memory hero and let
        // InterlevelScene reconstruct the run from the restored save directory.
        Dungeon.hero = null;
        ActionIndicator.clearAction();
        InterlevelScene.mode = InterlevelScene.Mode.CONTINUE;
        ShatteredPixelDungeon.switchScene(InterlevelScene.class);
    }
}
