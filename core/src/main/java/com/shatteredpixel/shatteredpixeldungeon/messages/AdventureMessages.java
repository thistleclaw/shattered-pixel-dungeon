/*
 * Shattered Pixel Dungeon - Adventure Mode additions
 * Copyright (C) 2026
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.shatteredpixel.shatteredpixeldungeon.messages;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.I18NBundle;

/** Small standalone bundle for Adventure Mode strings. Other locales fall back to English. */
public final class AdventureMessages {

    private static final String BUNDLE_PATH = "messages/adventure/adventure";

    private static I18NBundle bundle;
    private static Languages language;

    private AdventureMessages() {}

    public static String get(String key, Object... args) {
        if (bundle == null || language != Messages.lang()) {
            language = Messages.lang();
            bundle = I18NBundle.createBundle(Gdx.files.internal(BUNDLE_PATH), Messages.locale());
        }
        return args.length == 0 ? bundle.get(key) : bundle.format(key, args);
    }
}
