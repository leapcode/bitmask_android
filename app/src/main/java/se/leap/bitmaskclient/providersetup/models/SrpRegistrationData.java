package se.leap.bitmaskclient.providersetup.models;

import com.google.gson.Gson;

/**
 * Created by cyberta on 23.10.17.
 */

public class SrpRegistrationData {


    private User user;

    public SrpRegistrationData(String username, String passwordSalt, String passwordVerifier) {
        user = new User(username, passwordSalt, passwordVerifier);
    }


    @Override
    public String toString() {
       return new Gson().toJson(this);
    }


    public class User {

        String login;
        String password_salt;
        String password_verifier;

        public User(String login, String password_salt, String password_verifier) {
            this.login = login;
            this.password_salt = password_salt;
            this.password_verifier = password_verifier;
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }
    }
}
