package se.leap.bitmaskclient.test;

import android.content.Context;
import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.test.ServiceTestCase;

import se.leap.bitmaskclient.Dashboard;
import se.leap.bitmaskclient.eip.EIP;

public class testEIP extends ServiceTestCase<EIP> {

    private Context context;
    private Intent intent;
    private EIP activity;

    public testEIP(Class<EIP> activityClass) {
        super(activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }


}
