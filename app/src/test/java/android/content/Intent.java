package android.content;

import android.os.Bundle;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Intent {
    final String[] action = new String[1];
    final Map<String, Object> fakeExtras = new HashMap<>();
    final List<String> categories = new ArrayList<>();

    public Intent setAction(String action) {
        this.action[0] = action;
        return this;
    }

    public String getAction() {
        return action[0];
    }

    public Intent putExtra(String key, Bundle bundle) {
        fakeExtras.put(key, bundle);
        return this;
    }

    public Bundle getBundleExtra(String key) {
        Object o = fakeExtras.get(key);
        if (o != null) {
            return (Bundle) o;
        }
        return null;
    }

    public Intent putExtra(String key, Parcelable extra) {
        fakeExtras.put(key, extra);
        return this;
    }

    public Parcelable getParcelableExtra(String key) {
        Object o = fakeExtras.get(key);
        if (o != null) {
            return (Parcelable) o;
        }
        return null;
    }

    public Intent addCategory(String key) {
        categories.add(key);
        return this;
    }

    public Set<String> getCategories() {
        return new HashSet<>(categories);
    }
}
