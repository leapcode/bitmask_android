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

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

public class FragmentManagerEnhanced {

    private FragmentManager generic_fragment_manager;
    
    public FragmentManagerEnhanced(FragmentManager generic_fragment_manager) {
	this.generic_fragment_manager = generic_fragment_manager;
    }
    
    public FragmentTransaction removePreviousFragment(String tag) {
	FragmentTransaction transaction = generic_fragment_manager.beginTransaction();
	Fragment previous_fragment = generic_fragment_manager.findFragmentByTag(tag);
	if (previous_fragment != null) {
	    transaction.remove(previous_fragment);
	}
	transaction.addToBackStack(null);

	return transaction;
    }

    public void replace(int containerViewId, Fragment fragment, String tag) {
	FragmentTransaction transaction = generic_fragment_manager.beginTransaction();
	
	transaction.replace(containerViewId, fragment, tag).commit();
    }

    public Fragment findFragmentByTag(String tag) {
	return generic_fragment_manager.findFragmentByTag(tag);
    }
}
