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
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

public class WndAdventureSaves extends WndOptions {

    public WndAdventureSaves() {
        super(
                "Сохранения",
                "Автосейв создаётся при первом входе на каждый этаж. Ручные слоты можно перезаписывать когда угодно.",
                autoLabel(),
                manualLabel(1),
                manualLabel(2),
                manualLabel(3)
        );
    }

    private static String autoLabel() {
        int depth = AdventureSaves.checkpointDepth(AdventureSaves.AUTO);
        return depth > 0 ? "Автосейв этажа " + depth : "Автосейв этажа (пусто)";
    }

    private static String manualLabel(int slot) {
        int depth = AdventureSaves.manualDepth(slot);
        return depth > 0 ? "Ручной слот " + slot + " — этаж " + depth : "Ручной слот " + slot + " (пусто)";
    }

    @Override
    protected boolean enabled(int index) {
        if (index == 0) return AdventureSaves.autoExists();
        int slot = index;
        return (Dungeon.hero != null && Dungeon.hero.isAlive()) || AdventureSaves.manualExists(slot);
    }

    @Override
    protected void onSelect(int index) {
        if (index == 0) {
            loadAuto();
        } else {
            showManualSlot(index);
        }
    }

    private void showManualSlot(final int slot) {
        final boolean exists = AdventureSaves.manualExists(slot);
        final boolean canSave = Dungeon.hero != null && Dungeon.hero.isAlive();

        GameScene.show(new WndOptions(
                "Ручной слот " + slot,
                exists ? "В этом слоте уже есть сохранение." : "Этот слот пока пуст.",
                "Сохранить сюда",
                "Загрузить"
        ) {
            @Override
            protected boolean enabled(int index) {
                return index == 0 ? canSave : exists;
            }

            @Override
            protected void onSelect(int index) {
                if (index == 0) {
                    if (AdventureSaves.saveManual(slot)) {
                        GLog.p("Игра сохранена в ручной слот " + slot + ".");
                    } else {
                        GLog.w("Не удалось сохранить игру.");
                    }
                } else {
                    loadManual(slot);
                }
            }
        });
    }

    private static void loadAuto() {
        if (AdventureSaves.restoreAuto()) {
            continueFromCheckpoint();
        } else {
            GLog.w("Автосейв недоступен.");
        }
    }

    private static void loadManual(int slot) {
        if (AdventureSaves.restoreManual(slot)) {
            continueFromCheckpoint();
        } else {
            GLog.w("Это сохранение недоступно.");
        }
    }

    private static void continueFromCheckpoint() {
        //Drop references to the dead/current in-memory run. InterlevelScene will
        //load the restored game directory exactly like a normal Continue action.
        Dungeon.hero = null;
        Dungeon.level = null;
        InterlevelScene.mode = InterlevelScene.Mode.CONTINUE;
        ShatteredPixelDungeon.switchScene(InterlevelScene.class);
    }
}
