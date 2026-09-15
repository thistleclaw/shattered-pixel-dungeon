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
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

public class WndAdventureSaves extends WndOptions {

    public WndAdventureSaves() {
        super(
                "Сохранения",
                "Хранится один последний автосейв — момент входа на текущий этаж. Ручные слоты сохраняют точный текущий ход.",
                autoLabel(),
                manualLabel(1),
                manualLabel(2),
                manualLabel(3)
        );
    }

    private static String location(int depth, int branch) {
        if (depth <= 0) return "пусто";
        if (branch == 0) return "этаж " + depth;
        if (branch == 1) return "этаж " + depth + " · шахта";
        return "этаж " + depth + " · ветка " + branch;
    }

    private static String autoLabel() {
        int depth = AdventureSaves.checkpointDepth(AdventureSaves.AUTO);
        int branch = AdventureSaves.checkpointBranch(AdventureSaves.AUTO);
        return depth > 0 ? "Автосейв — " + location(depth, branch) : "Автосейв (пусто)";
    }

    private static String manualLabel(int slot) {
        int depth = AdventureSaves.manualDepth(slot);
        int branch = AdventureSaves.manualBranch(slot);
        return depth > 0 ? "Ручной " + slot + " — " + location(depth, branch) : "Ручной " + slot + " (пусто)";
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
                //After death saving is impossible, so one tap on a populated
                //manual slot should simply restore it.
                loadManual(slot);
            }
        }
    }

    private void showManualSlot(final int slot) {
        final boolean exists = AdventureSaves.manualExists(slot);
        final int depth = AdventureSaves.manualDepth(slot);
        final int branch = AdventureSaves.manualBranch(slot);

        GameScene.show(new WndOptions(
                "Ручной слот " + slot,
                exists ? "Сохранено: " + location(depth, branch) + "." : "Этот слот пока пуст.",
                exists ? "Перезаписать" : "Сохранить сюда",
                "Загрузить"
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
                        saveManual(slot);
                    }
                } else {
                    confirmLoadManual(slot);
                }
            }
        });
    }

    private static void confirmOverwrite(final int slot) {
        GameScene.show(new WndOptions(
                "Перезаписать слот " + slot + "?",
                "Старое ручное сохранение будет заменено текущим состоянием игры.",
                "Перезаписать",
                "Отмена"
        ) {
            @Override
            protected void onSelect(int index) {
                if (index == 0) saveManual(slot);
            }
        });
    }

    private static void confirmLoadAuto() {
        int depth = AdventureSaves.checkpointDepth(AdventureSaves.AUTO);
        int branch = AdventureSaves.checkpointBranch(AdventureSaves.AUTO);
        GameScene.show(new WndOptions(
                "Загрузить автосейв?",
                "Откатиться к моменту входа: " + location(depth, branch) + "? Текущий прогресс после него будет потерян.",
                "Загрузить",
                "Отмена"
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
                "Загрузить ручной слот " + slot + "?",
                "Откатиться к сохранению: " + location(depth, branch) + "? Текущий прогресс после него будет потерян.",
                "Загрузить",
                "Отмена"
        ) {
            @Override
            protected void onSelect(int index) {
                if (index == 0) loadManual(slot);
            }
        });
    }

    private static void saveManual(int slot) {
        if (AdventureSaves.saveManual(slot)) {
            GLog.p("Игра сохранена в ручной слот " + slot + ".");
        } else {
            GLog.w("Не удалось сохранить игру.");
        }
    }

    private static void loadAuto() {
        if (AdventureSaves.restoreAuto()) {
            continueFromCheckpoint();
        } else {
            GLog.w("Автосейв недоступен или повреждён.");
        }
    }

    private static void loadManual(int slot) {
        if (AdventureSaves.restoreManual(slot)) {
            continueFromCheckpoint();
        } else {
            GLog.w("Это сохранение недоступно или повреждено.");
        }
    }

    private static void continueFromCheckpoint() {
        //Mirror the game's normal Continue path: drop the in-memory hero and
        //let InterlevelScene rebuild the run from the restored save directory.
        Dungeon.hero = null;
        ActionIndicator.clearAction();
        InterlevelScene.mode = InterlevelScene.Mode.CONTINUE;
        ShatteredPixelDungeon.switchScene(InterlevelScene.class);
    }
}
