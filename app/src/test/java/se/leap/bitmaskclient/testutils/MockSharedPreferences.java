/**
 * Copyright (c) 2018 LEAP Encryption Access Project and contributers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package se.leap.bitmaskclient.testutils;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by cyberta on 09.01.18.
 */

public class MockSharedPreferences implements SharedPreferences {
    HashMap<String, String> mockedStringPrefs = new HashMap<>();
    HashMap<String, Integer> mockedIntPrefs = new HashMap<>();
    HashMap<String, Boolean> mockedBooleanPrefs = new HashMap<>();
    HashMap<String, Long> mockedLongPrefs = new HashMap<>();
    HashMap<String, Set<String>> mockedStringSetPrefs = new HashMap<>();

    @Override
    public Map<String, ?> getAll() {
        return null;
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        String value = mockedStringPrefs.get(key);
        return value != null ? value : defValue;
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        return mockedStringSetPrefs.getOrDefault(key, new HashSet<>());
    }

    @Override
    public int getInt(String key, int defValue) {
        Integer value = mockedIntPrefs.get(key);
        return value != null ? value : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        return mockedLongPrefs.getOrDefault(key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return 0;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Boolean value = mockedBooleanPrefs.get(key);
        return value != null ? value : defValue;
    }

    @Override
    public boolean contains(String key) {
        return mockedStringPrefs.containsKey(key) ||
                mockedBooleanPrefs.containsKey(key) ||
                mockedIntPrefs.containsKey(key) ||
                mockedStringSetPrefs.containsKey(key) ||
                mockedLongPrefs.containsKey(key);
    }

    @Override
    public Editor edit() {
        return new Editor() {
            private HashMap<String, String> tempStrings = new HashMap<>(mockedStringPrefs);
            private HashMap<String, Integer> tempIntegers = new HashMap<>(mockedIntPrefs);
            private HashMap<String, Boolean> tempBoolean = new HashMap<>(mockedBooleanPrefs);
            private HashMap<String, Long> tempLongs = new HashMap<>(mockedLongPrefs);
            private HashMap<String, Set<String>> tempStringSets = new HashMap<>(mockedStringSetPrefs);

            @Override
            public Editor putString(String key, @Nullable String value) {
                tempStrings.put(key, value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, @Nullable Set<String> values) {
                tempStringSets.put(key, values);
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                tempIntegers.put(key, value);
                return this;
            }

            @Override
            public Editor putLong(String key, long value) {
                tempLongs.put(key, value);
                return this;
            }

            @Override
            public Editor putFloat(String key, float value) {
                return null;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                tempBoolean.put(key, value);
                return this;
            }

            @Override
            public Editor remove(String key) {
                tempBoolean.remove(key);
                tempStrings.remove(key);
                tempIntegers.remove(key);
                return this;
            }

            @Override
            public Editor clear() {
                tempBoolean.clear();
                tempStrings.clear();
                tempIntegers.clear();
                return this;
            }

            @Override
            public boolean commit() {
                mockedStringPrefs = new HashMap<>(tempStrings);
                mockedBooleanPrefs = new HashMap<>(tempBoolean);
                mockedIntPrefs = new HashMap<>(tempIntegers);
                return true;
            }

            @Override
            public void apply() {
                mockedStringPrefs = new HashMap<>(tempStrings);
                mockedBooleanPrefs = new HashMap<>(tempBoolean);
                mockedIntPrefs = new HashMap<>(tempIntegers);
                mockedStringSetPrefs = new HashMap<>(tempStringSets);
            }
        };
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

    }
}
