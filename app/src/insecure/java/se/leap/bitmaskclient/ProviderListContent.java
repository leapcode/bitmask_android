/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributers
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
package se.leap.bitmaskclient;

import java.util.*;
import java.net.*;

/**
 * Models the provider list shown in the ProviderListActivity.
 *
 * @author parmegv
 */
public class ProviderListContent {

    public static List<ProviderItem> ITEMS = new ArrayList<ProviderItem>();

    public static Map<String, ProviderItem> ITEM_MAP = new HashMap<String, ProviderItem>();

    /**
     * Adds a new provider item to the end of the items map, and to the items list.
     *
     * @param item
     */
    public static void addItem(ProviderItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(String.valueOf(ITEMS.size()), item);
    }

    public static void removeItem(ProviderItem item) {
        ITEMS.remove(item);
        ITEM_MAP.remove(item);
    }

    /**
     * A provider item.
     */
    public static class ProviderItem {
        final public static String CUSTOM = "custom";
        final public static String DANGER_ON = "danger_on";
        private String provider_main_url;
        private String name;

        /**
         * @param name              of the provider
         * @param provider_main_url used to download provider.json file of the provider
         */
        public ProviderItem(String name, String provider_main_url) {
            this.name = name;
            this.provider_main_url = provider_main_url;
        }

        public String name() {
            return name;
        }

        public String providerMainUrl() {
            return provider_main_url;
        }

        public String domain() {
            try {
                return new URL(provider_main_url).getHost();
            } catch (MalformedURLException e) {
                return provider_main_url.replaceFirst("http[s]?://", "").replaceFirst("/.*", "");
            }
        }
    }
}
