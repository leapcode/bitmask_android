package se.leap.bitmaskclient.providersetup.models;

import com.google.gson.Gson;

/**
 * Created by cyberta on 23.10.17.
 */

public class SrpCredentials {

    /**
     * Parameter A of SRP authentication
     */
    private String A;
    private String login;

    public SrpCredentials(String username, String A) {
        this.login = username;
        this.A = A;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
