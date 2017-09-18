package se.leap.bitmaskclient.test;

public class TestUserStatusFragment extends BaseTestDashboardFragment {

    public final String TAG = TestUserStatusFragment.class.getName();

    private final String provider = "demo.bitmask.net";
    private final String test_username = "parmegvtest1";
    private final String test_password = " S_Zw3'-";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        changeProviderAndLogIn(provider);
        user_status_controller.clickUserSessionButton();
        user_status_controller.assertLoggedOut();
    }

    public void testLogInAndOut() {
        user_status_controller.clickUserSessionButton();
        Screenshot.take("Login dialog");
        user_status_controller.logIn(test_username, test_password);
        Screenshot.setTimeToSleep(2);
        Screenshot.takeWithSleep("Logging in");
        user_status_controller.logOut();
    }

    public void testFailedLogIn() {
        user_status_controller.clickUserSessionButton();
        user_status_controller.logIn(test_username, TAG, false);
        if(!user_status_controller.assertErrorLogInDialogAppears())
            throw new IllegalStateException();
    }
}
