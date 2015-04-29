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
package se.leap.bitmaskclient.userstatus;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import butterknife.*;
import se.leap.bitmaskclient.EipFragment;
import se.leap.bitmaskclient.Provider;
import se.leap.bitmaskclient.R;

/**
 * Implements the log in dialog, currently without progress dialog.
 * <p/>
 * It returns to the previous fragment when finished, and sends username and password to the authenticate method.
 * <p/>
 * It also notifies the user if the password is not valid.
 *
 * @author parmegv
 */
public class SessionDialog extends DialogFragment {


    final public static String TAG = SessionDialog.class.getSimpleName();

    final public static String USERNAME = "username";
    final public static String PASSWORD = "password";

    public static enum ERRORS {
        USERNAME_MISSING,
        PASSWORD_INVALID_LENGTH,
        RISEUP_WARNING
    }

    @InjectView(R.id.user_message)
    TextView user_message;
    @InjectView(R.id.username_entered)
    EditText username_field;
    @InjectView(R.id.password_entered)
    EditText password_field;

    private static boolean is_eip_pending = false;

    public static SessionDialog getInstance(Provider provider, Bundle arguments) {
        SessionDialog dialog = new SessionDialog();
        if (provider.getName().equalsIgnoreCase("riseup")) {
            arguments =
                    arguments == Bundle.EMPTY ?
                            new Bundle() : arguments;
            arguments.putBoolean(SessionDialog.ERRORS.RISEUP_WARNING.toString(), true);
        }
        if (arguments != null && !arguments.isEmpty()) {
            dialog.setArguments(arguments);
        }
        return dialog;
    }

    public AlertDialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.session_dialog, null);
        ButterKnife.inject(this, view);

        Bundle arguments = getArguments();
        if (arguments != Bundle.EMPTY && arguments != null) {
            setUp(arguments);
        }

        builder.setView(view)
                .setPositiveButton(R.string.login_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String username = getEnteredUsername();
                        String password = getEnteredPassword();
                        dialog.dismiss();
                        interface_with_Dashboard.logIn(username, password);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        interface_with_Dashboard.cancelLoginOrSignup();
                    }
                })
                .setNeutralButton(R.string.signup_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String username = getEnteredUsername();
                        String password = getEnteredPassword();
                        dialog.dismiss();
                        interface_with_Dashboard.signUp(username, password);
                    }
                });

        return builder.create();
    }

    private void setUp(Bundle arguments) {
        is_eip_pending = arguments.getBoolean(EipFragment.IS_PENDING, false);
        if (arguments.containsKey(ERRORS.PASSWORD_INVALID_LENGTH.toString()))
            password_field.setError(getString(R.string.error_not_valid_password_user_message));
        else if (arguments.containsKey(ERRORS.RISEUP_WARNING.toString())) {
            user_message.setVisibility(TextView.VISIBLE);
            user_message.setText(R.string.login_riseup_warning);
        }
        if (arguments.containsKey(USERNAME)) {
            String username = arguments.getString(USERNAME);
            username_field.setText(username);
        }
        if (arguments.containsKey(ERRORS.USERNAME_MISSING.toString())) {
            username_field.setError(getString(R.string.username_ask));
        }
        if (arguments.containsKey(getString(R.string.user_message))) {
            user_message.setText(arguments.getString(getString(R.string.user_message)));
            user_message.setVisibility(View.VISIBLE);
        } else if (user_message.getVisibility() != TextView.VISIBLE)
            user_message.setVisibility(View.GONE);

        if (!username_field.getText().toString().isEmpty() && password_field.isFocusable())
            password_field.requestFocus();

    }

    private String getEnteredUsername() {
        return username_field.getText().toString();
    }

    private String getEnteredPassword() {
        return password_field.getText().toString();
    }


    /**
     * Interface used to communicate SessionDialog with Dashboard.
     *
     * @author parmegv
     */
    public interface SessionDialogInterface {
        public void logIn(String username, String password);

        public void signUp(String username, String password);

        public void cancelLoginOrSignup();
    }

    SessionDialogInterface interface_with_Dashboard;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            interface_with_Dashboard = (SessionDialogInterface) activity.getFragmentManager().findFragmentById(R.id.user_session_fragment);;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement LogInDialogListener");
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (is_eip_pending)
            interface_with_Dashboard.cancelLoginOrSignup();
    }
}
