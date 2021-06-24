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
package se.leap.bitmaskclient.providersetup.activities;

import android.os.Bundle;
import androidx.annotation.Nullable;

import butterknife.OnClick;
import se.leap.bitmaskclient.R;

import static android.view.View.VISIBLE;

/**
 * Create an account with a provider
 */

public class SignupActivity extends ProviderCredentialsBaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setProviderHeaderLogo(R.drawable.logo);
        setProviderHeaderText(R.string.create_profile);

        setProgressbarTitle(R.string.signing_up);
        setButtonText(R.string.signup_button);

        passwordVerificationField.setVisibility(VISIBLE);
        passwordVerificationError.setVisibility(VISIBLE);
    }

    @Override
    @OnClick(R.id.button)
    void handleButton() {
        super.handleButton();
        if (getPassword().equals(getPasswordVerification())) {
            signUp(getUsername(), getPassword());
        }
    }
}
