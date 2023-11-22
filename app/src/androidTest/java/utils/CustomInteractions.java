package utils;

import androidx.annotation.Nullable;
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.ViewInteraction;

public class CustomInteractions {

    public static @Nullable
    ViewInteraction tryResolve(ViewInteraction viewInteraction, int maxTries) {
        return tryResolve(viewInteraction, null, maxTries);
    }

    public static @Nullable
    ViewInteraction tryResolve(ViewInteraction viewInteraction, ViewAssertion assertion) {
        return tryResolve(viewInteraction, assertion, 10);
    }

    public static @Nullable ViewInteraction tryResolve(ViewInteraction viewInteraction, ViewAssertion assertion, int maxTries) {
        ViewInteraction resolvedViewInteraction = null;
        int attempt = 0;
        boolean hasFound = false;
        while (!hasFound && attempt < maxTries) {
            try {
                resolvedViewInteraction = viewInteraction;
                if (assertion != null) {
                    resolvedViewInteraction.check(assertion);
                }
                hasFound = true;
            } catch (NoMatchingViewException exception) {
                System.out.println("NoMatchingViewException - attempt: " + attempt + ". " + exception.getLocalizedMessage());
                attempt++;
                if (attempt == maxTries) {
                    throw exception;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        return resolvedViewInteraction;
    }

    public static @Nullable
    DataInteraction tryResolve(DataInteraction dataInteraction, ViewAssertion assertion, int maxTries) {
        DataInteraction resolvedDataInteraction = null;
        int attempt = 0;
        boolean hasFound = false;
        while (!hasFound && attempt < maxTries) {
            try {
                resolvedDataInteraction = dataInteraction;
                if (assertion != null) {
                    resolvedDataInteraction.check(assertion);
                }
                hasFound = true;
            } catch (Exception exception) {
                // TODO: specify expected exception
                attempt++;
                if (attempt == maxTries) {
                    throw exception;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        return resolvedDataInteraction;
    }
    public static @Nullable
    DataInteraction tryResolve(DataInteraction dataInteraction, int maxTries) {
        return tryResolve(dataInteraction, null, maxTries);
    }
}
