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
package se.leap.bitmaskclient.testutils.matchers;

import android.os.Bundle;
import android.os.Parcelable;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by cyberta on 09.01.18.
 */

public class BundleMatcher extends BaseMatcher<Bundle> {

    private final HashMap<String, Integer> expectedIntegers;
    private final HashMap<String, String> expectedStrings;
    private final HashMap<String, Boolean> expectedBooleans;
    private final HashMap<String, Parcelable> expectedParcelables;
    private HashMap<String, Integer> unfoundExpectedInteger  = new HashMap<>();
    private HashMap<String, Boolean> unfoundExpectedBoolean = new HashMap<>();
    private HashMap<String, String> unfoundExpectedString = new HashMap<>();
    private HashMap<String, Parcelable> unfoundExpectedParcelable = new HashMap<>();
    private HashMap<String, Object> unexpectedAdditionalObjects = new HashMap<>();

    public BundleMatcher(HashMap<String, Integer> expectedIntegers, HashMap<String, String> expectedStrings, HashMap<String, Boolean> expectedBooleans, HashMap<String, Parcelable> expectedParcelables) {
        this.expectedBooleans = expectedBooleans;
        this.expectedIntegers = expectedIntegers;
        this.expectedStrings = expectedStrings;
        this.expectedParcelables = expectedParcelables;
    }

    @Override
    public boolean matches(Object item) {
        if (item instanceof Bundle) {
            Bundle actualBundle = (Bundle) item;
            return checkActualBundleHasAllExpectedBooleanValues(actualBundle) &&
                    checkActualBundleHasAllExpectedStringValues(actualBundle) &&
                    checkActualBundleHasAllExpectedIntValues(actualBundle) &&
                    checkActualBundleHasAllExpectedParcelableValues(actualBundle) &&
                    checkUnexpectedAdditionalValuesIn(actualBundle);
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Bundle didn't match expectation!");

        if (!unfoundExpectedInteger.isEmpty()) {
            Iterator<String> iterator = unfoundExpectedInteger.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                if (unfoundExpectedInteger.get(key) == null) {
                    description.appendText("\n unfound Integer in actual Bundle: ").appendValue(iterator.next());
                } else {
                    description.appendText("\n expected Integer for key \"" + key + "\": ").appendValue(expectedIntegers.get(key)).
                            appendText("\n found Integer was: ").appendValue(unfoundExpectedInteger.get(key));
                }
            }
        }
        if (!unfoundExpectedBoolean.isEmpty()) {
            Iterator<String> iterator = unfoundExpectedBoolean.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                if (unfoundExpectedBoolean.get(key) == null) {
                    description.appendText("\n unfound Boolean in actual Bundle: ").appendValue(iterator.next());
                } else {
                    description.appendText("\n expected Boolean for key \"" + key + "\": ").appendValue(expectedBooleans.get(key)).
                            appendText("\n found Boolean was: ").appendValue(unfoundExpectedBoolean.get(key));
                }
            }
        }
        if (!unfoundExpectedString.isEmpty()) {
            Iterator<String> iterator = unfoundExpectedString.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                if (unfoundExpectedString.get(key) == null) {
                    description.appendText("\n unfound String in actual Bundle: ").appendValue(iterator.next());
                } else {
                    description.appendText("\n expected String for key \"" + key + "\": ").appendValue(expectedStrings.get(key)).
                            appendText("\n but found String was: ").appendValue(unfoundExpectedString.get(key));
                }
            }
        }
        if (!unfoundExpectedParcelable.isEmpty()) {
            Iterator<String> iterator = unfoundExpectedInteger.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                if (unfoundExpectedParcelable.get(key) == null) {
                    description.appendText("\n unfound Parcelable in actual Bundle: ").appendValue(iterator.next());
                } else {
                    description.appendText("\n expected Parcelable or key \"" + key + "\": ").appendValue(expectedParcelables.get(key)).
                            appendText("\n found Parcelable was: ").appendValue(unfoundExpectedParcelable.get(key));
                }
            }
        }

        if (!unexpectedAdditionalObjects.isEmpty()) {
            Iterator<String> iterator = unexpectedAdditionalObjects.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                Object value = unexpectedAdditionalObjects.get(key);
                if (value instanceof String) {
                    description.appendText("\n unexpected String found in actual Bundle: ").appendValue(key).appendText(", ").appendValue(value);
                } else if (value instanceof Boolean) {
                    description.appendText("\n unexpected Boolean found in actual Bundle: ").appendValue(key).appendText(", ").appendValue(value);
                } else if (value instanceof Integer) {
                    description.appendText("\n unexpected Integer found in actual Bundle: ").appendValue(key).appendText(", ").appendValue(value);
                } else if (value instanceof Parcelable) {
                    description.appendText("\n unexpected Parcelable found in actual Bundle: ").appendValue(key).appendText(", ").appendValue(value);
                } else {
                    description.appendText("\n unexpected Object found in actual Bundle: ").appendValue(key).appendText(", ").appendValue(value);
                }
            }
        }
    }

    private boolean checkActualBundleHasAllExpectedBooleanValues(Bundle actualBundle) {
        Set<String> booleanKeys = expectedBooleans.keySet();
        for (String key : booleanKeys) {
            Object valueObject = actualBundle.get(key);
            if (!(valueObject instanceof Boolean) ||
                    valueObject != expectedBooleans.get(key)) {
                unfoundExpectedBoolean.put(key, (Boolean) valueObject);
                return false;
            }
        }
        return true;
    }

    private boolean checkActualBundleHasAllExpectedStringValues(Bundle actualBundle) {
        Set<String> stringKeys = expectedStrings.keySet();
        for (String key : stringKeys) {
            Object valueObject = actualBundle.get(key);
            if (!(valueObject instanceof String) ||
                    !valueObject.equals(expectedStrings.get(key))) {
                unfoundExpectedString.put(key, (String) valueObject);
                return false;
            }
        }
        return true;
    }

    private boolean checkActualBundleHasAllExpectedIntValues(Bundle actualBundle) {
        Set<String> stringKeys = expectedIntegers.keySet();
        for (String key : stringKeys) {
            Object valueObject = actualBundle.get(key);
            if (!(valueObject instanceof Integer) ||
                    ((Integer) valueObject).compareTo(expectedIntegers.get(key)) != 0) {
                unfoundExpectedInteger.put(key, (Integer) valueObject);
                return false;
            }
        }
        return true;
    }

    private boolean checkActualBundleHasAllExpectedParcelableValues(Bundle actualBundle) {
        Set<String> stringKeys = expectedParcelables.keySet();
        for (String key : stringKeys) {
            Object valueObject = actualBundle.get(key);
            if (!(valueObject instanceof Parcelable) ||
                    !valueObject.equals(expectedParcelables.get(key))) {
                unfoundExpectedParcelable.put(key, (Parcelable) valueObject);
                return false;
            }
        }
        return true;
    }

    private boolean checkUnexpectedAdditionalValuesIn(Bundle actualBundle) {
        Set<String> keys = actualBundle.keySet();

        for (String key : keys) {
            if (!expectedStrings.containsKey(key) &&
                    !expectedIntegers.containsKey(key) &&
                    !expectedBooleans.containsKey(key) &&
                    !expectedParcelables.containsKey(key)
                    ) {
                unexpectedAdditionalObjects.put(key, actualBundle.getString(key));
            }
        }
        return unexpectedAdditionalObjects.isEmpty();
    }
}
