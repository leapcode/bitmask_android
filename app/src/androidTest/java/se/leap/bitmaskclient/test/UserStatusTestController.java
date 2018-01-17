package se.leap.bitmaskclient.test;

import android.view.*;

import com.robotium.solo.*;

import se.leap.bitmaskclient.R;

import static junit.framework.Assert.assertTrue;

public class UserStatusTestController {
    private final Solo solo;

    public UserStatusTestController(Solo solo) {
        this.solo = solo;
    }

    void clickUserSessionButton() {
        solo.clickOnView(getUserSessionButton());
    }

    View getUserSessionButton() throws IllegalStateException {
        View view = solo.getView(R.id.user_status_button);
        if(view == null)
            throw new IllegalStateException();

        return view;
    }

    void logIn(String username, String password, boolean expectSuccess) {
        solo.enterText(0, username);
        solo.enterText(1, password);
        // https://stackoverflow.com/questions/33560746/how-to-test-alertdialog-item-click-in-robotium-for-android-studio
        solo.clickOnView(solo.getView(android.R.id.button1));
        assertTrue(solo.waitForDialogToClose());

        if (expectSuccess) {
            assertLoggedIn();
        } else {
            solo.waitForText(solo.getString(R.string.cancel));
            assertTrue(solo.waitForText(solo.getString(R.string.login_button)));
        }
    }

    void logIn(String username, String password) {
       logIn(username, password, true);
    }

    private void assertLoggedIn() {
        String log_out = solo.getString(R.string.logout_button);
        assertTrue(solo.waitForText(log_out));
    }

    void assertLoggedOut() {
        String log_in = solo.getString(R.string.login_button);
        assertTrue(solo.waitForText(log_in));
    }

    void logOut() {
        assertLoggedIn();
        clickUserSessionButton();

        solo.clickOnActionBarItem(R.string.logout_button);
        solo.waitForDialogToClose();
        assertLoggedOut();
    }

    boolean assertErrorLogInDialogAppears() {
        assertTrue(solo.waitForDialogToOpen());

        String username_hint = solo.getEditText(0).getHint().toString();
        String correct_username_hint = solo.getString(R.string.username_hint);
        String password_hint = solo.getEditText(1).getHint().toString();
        String correct_password_hint = solo.getString(R.string.password_hint);
        String user_message = solo.getText(0).toString();
        String riseup_user_message = solo.getString(R.string.login_riseup_warning);

        return username_hint.equalsIgnoreCase(correct_username_hint)
                && password_hint.equalsIgnoreCase(correct_password_hint)
                && !user_message.equalsIgnoreCase(riseup_user_message)
                && !user_message.isEmpty();
    }
}
