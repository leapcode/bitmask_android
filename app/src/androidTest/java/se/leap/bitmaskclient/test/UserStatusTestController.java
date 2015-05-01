package se.leap.bitmaskclient.test;

import android.view.View;

import com.robotium.solo.Solo;

import se.leap.bitmaskclient.R;

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

    void logIn(String username, String password) {
        solo.enterText(0, username);
        solo.enterText(1, password);
        solo.clickOnText(solo.getString(R.string.login_button));
        solo.waitForDialogToClose();
        assertLoggedIn();
    }

    private void assertLoggedIn() {
        String log_out = solo.getString(R.string.logout_button);
        solo.waitForText(log_out);
    }

    void assertLoggedOut() {
        String log_in = solo.getString(R.string.login_button);
        solo.waitForText(log_in);
    }

    void logOut() {
        assertLoggedIn();
        clickUserSessionButton();

        solo.clickOnActionBarItem(R.string.logout_button);
        solo.waitForDialogToClose();
        assertLoggedOut();
    }
}
