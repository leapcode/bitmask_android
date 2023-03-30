package se.leap.bitmaskclient.base;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static utils.CustomInteractions.tryResolve;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.FixMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import se.leap.bitmaskclient.R;

@LargeTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BitmaskTest extends ProviderBaseTest {

    @Override
    public boolean configureProviderIfNeeded() {
        try {
            DataInteraction linearLayout = tryResolve(onData(hasToString(containsString("riseup.net")))
                            .inAdapterView(withId(R.id.provider_list)),
                    2);
            linearLayout.perform(click());
            return true;
        } catch (NoMatchingViewException e) {
            // it might be that the provider was already configured, so we print the stack
            // trace here and try to continue
            e.printStackTrace();
        }
        return false;
    }
}
