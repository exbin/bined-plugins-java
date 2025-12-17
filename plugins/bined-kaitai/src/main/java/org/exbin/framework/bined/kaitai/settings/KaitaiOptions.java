/*
 * Copyright (C) ExBin Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exbin.framework.bined.kaitai.settings;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.exbin.framework.options.settings.api.SettingsOptions;
import org.exbin.framework.options.api.OptionsStorage;

/**
 * Kaitai settings options.
 */
@ParametersAreNonnullByDefault
public class KaitaiOptions implements SettingsOptions {

    public static final String KEY_LIST_PREFIX = "kaitai.list.";
    public static final String KEY_LIST_SIZE = KEY_LIST_PREFIX + ".size";
    public static final String KEY_NAME_PREFIX = KEY_LIST_PREFIX + "name.";
    public static final String KEY_PATH_PREFIX = KEY_LIST_PREFIX + "path.";

    private final OptionsStorage storage;

    public KaitaiOptions(OptionsStorage storage) {
        this.storage = storage;
    }

    public int getListSize() {
        return storage.getInt(KEY_LIST_SIZE, 0);
    }

    public void setListSize(int size) {
        storage.putInt(KEY_LIST_SIZE, size);
    }

    @Nonnull
    public String getListItemName(int index) {
        return storage.get(KEY_NAME_PREFIX + index, "");
    }

    public void setListItemName(String value, int index) {
        storage.put(KEY_NAME_PREFIX + index, value);
    }

    @Nonnull
    public String getListItemPath(int index) {
        return storage.get(KEY_PATH_PREFIX + index, "");
    }

    public void setListItemPath(String value, int index) {
        storage.put(KEY_PATH_PREFIX + index, value);
    }

    public void remove(int index) {
        storage.remove(KEY_NAME_PREFIX + index);
        storage.remove(KEY_PATH_PREFIX + index);
    }

    @Override
    public void copyTo(SettingsOptions options) {
        KaitaiOptions target = (KaitaiOptions) options;
        int listSize = getListSize();
        target.setListSize(listSize);
        for (int index = 0; index < listSize; index++) {
            target.setListItemName(getListItemName(index), index);
            target.setListItemPath(getListItemPath(index), index);
        }
    }
}
