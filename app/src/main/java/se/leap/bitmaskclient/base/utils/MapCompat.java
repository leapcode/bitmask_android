package se.leap.bitmaskclient.base.utils;

import androidx.annotation.NonNull;
import java.util.Map;

public class MapCompat {
    public static <K, V> V getOrDefault(@NonNull Map<K, V> map, K key, V defaultValue) {
        V value;
        return (((value = map.get(key)) != null) || map.containsKey(key))
                ? value
                : defaultValue;
    }
}
