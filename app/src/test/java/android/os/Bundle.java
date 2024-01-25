package android.os;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Bundle {

    /** An unmodifiable {@code Bundle} that is always {@link #isEmpty() empty}. */
    public static final Bundle EMPTY;

    /**
     * Special extras used to denote extras have been stripped off.
     * @hide
     */
    public static final Bundle STRIPPED;

    static {
        EMPTY = new Bundle();

        STRIPPED = new Bundle();
        STRIPPED.putInt("STRIPPED", 1);
    }

    final Map<String, Boolean> fakeBooleanBundle = new HashMap<>();
    final Map<String, String> fakeStringBundle = new HashMap<>();
    final Map<String, Integer> fakeIntBundle = new HashMap<>();
    final Map<String, Parcelable> fakeParcelableBundle = new HashMap<>();


    public void putString(String key, String value) {
        fakeStringBundle.put(key, value);
    }

    public String getString(String key) {
        return fakeStringBundle.get(key);
    }

    public void putBoolean(String key, boolean value) {
        fakeBooleanBundle.put(key, value);
    }

    public boolean getBoolean(String key) {
        return fakeBooleanBundle.getOrDefault(key, false);
    }

    public void putInt(String key, int value) {
        fakeIntBundle.put(key, value);
    }

    public int getInt(String key) {
        return fakeIntBundle.getOrDefault(key, 0);
    }

    public void putParcelable(String key, Parcelable value) {
        fakeParcelableBundle.put(key, value);
    }

    public Parcelable getParcelable(String key) {
        return fakeParcelableBundle.get(key);
    }

    public Object get(String key) {
        if (fakeBooleanBundle.containsKey(key)) {
            return fakeBooleanBundle.get(key);
        } else if (fakeIntBundle.containsKey(key)) {
            return fakeIntBundle.get(key);
        } else if (fakeStringBundle.containsKey(key)) {
            return fakeStringBundle.get(key);
        } else {
            return fakeParcelableBundle.get(key);
        }
    }

    public Set<String> keySet() {
        //this whole approach as a drawback:
        //you should not add the same keys for values of different types
        HashSet<String> keys = new HashSet<String>();
        keys.addAll(fakeBooleanBundle.keySet());
        keys.addAll(fakeIntBundle.keySet());
        keys.addAll(fakeStringBundle.keySet());
        keys.addAll(fakeParcelableBundle.keySet());
        return keys;
    }

    public boolean containsKey(String key) {
        return fakeBooleanBundle.containsKey(key) ||
                fakeStringBundle.containsKey(key) ||
                fakeIntBundle.containsKey(key) ||
                fakeParcelableBundle.containsKey(key);
    }

    public void remove(String key) {
        fakeBooleanBundle.remove(key);
        fakeIntBundle.remove(key);
        fakeParcelableBundle.remove(key);
        fakeStringBundle.remove(key);
    }

}
