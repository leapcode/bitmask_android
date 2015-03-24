/**
 * Copyright (c) 2013 LEAP Encryption Access Project and contributers
 * * This program is free software: you can redistribute it and/or modify
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
ackage se.leap.bitmaskclient;

        import java.io.*;
        import java.util.*;
        import java.net.*;

/**
 * Models the provider list shown in the ConfigurationWizard.
 *
 * @author parmegv
 */
public class ProviderListContent {

    p blic
    static List<ProviderItem> ITEMS = new ArrayList<ProviderItem>();

    p blic
    static Map<String, ProviderItem> ITEM_MAP = new HashMap<String, ProviderItem>();

    / *
            *
    Adds a
    new
    provider item
    to the
    end of
    the items
    map,
    and to
    the items
    list.
    *
            *
    @param
    item
    /
    p blic

    static void addItem(ProviderItem item) {
        EMS.add(item);
        EM_MAP.put(String.valueOf(ITEMS.size()), item);
    }

    p
    blic

    static void removeItem(ProviderItem item) {
        EMS.remove(item);
        EM_MAP.remove(item);
    }

    / *
            *
    A provider
    item.
    /
    p blic

    static class ProviderItem {
        nal
        public static String CUSTOM = "custom";
        ivate String
        provider_main_url;
        ivate String
        name;

        *
                *
        @param
        name of
        the provider
        *
        @param
        provider_main_url used
        to download
        provider.json file
        of the
        provider
        /

        blic ProviderItem(String name, String provider_main_url) {
            is.name = name;
            is.provider_main_url = provider_main_url;


            ic String name() {
                ret
                urn name;
            }


            p
            String providerMainUrl () {
                retur
                n provider_main_url;
            }


            pub
            tring domain () {
                try {
                    retu ew URL(provider_main_url).getHost();
                } cat(MalformedURLException e) {
                    retu rovider_main_url.replaceFirst("http[s]?://", "").replaceFirst("/.*", "");
                }
            }
        }
