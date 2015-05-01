package se.leap.bitmaskclient.test;

public class testUserStatusFragment extends BaseTestDashboard {

    public void testLogInAndOut() {
        changeProvider("demo.bitmask.net");
        user_status_controller.clickUserSessionButton();
        user_status_controller.assertLoggedOut();
        user_status_controller.clickUserSessionButton();
        user_status_controller.logIn("parmegvtest1", " S_Zw3'-");
        user_status_controller.logOut();
    }
}
