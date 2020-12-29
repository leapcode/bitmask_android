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
package se.leap.bitmaskclient.base;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class FragmentManagerEnhanced {

    private FragmentManager genericFragmentManager;

    public FragmentManagerEnhanced(FragmentManager genericFragmentManager) {
        this.genericFragmentManager = genericFragmentManager;
    }

    public FragmentTransaction removePreviousFragment(String tag) {
        FragmentTransaction transaction = genericFragmentManager.beginTransaction();
        Fragment previousFragment = genericFragmentManager.findFragmentByTag(tag);
        if (previousFragment != null) {
            transaction.remove(previousFragment);
        }

        return transaction;
    }

    public void replace(int containerViewId, Fragment fragment, String tag) {
        try {
            if (genericFragmentManager.findFragmentByTag(tag) != null) {
                FragmentTransaction transaction = genericFragmentManager.beginTransaction();
                transaction.replace(containerViewId, fragment, tag).commit();
            } else {
                genericFragmentManager.beginTransaction().add(containerViewId, fragment, tag).commit();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

    }

    public Fragment findFragmentByTag(String tag) {
        return genericFragmentManager.findFragmentByTag(tag);
    }
}
