package utils;

import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.view.View;

import androidx.annotation.StringRes;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.util.TreeIterables;

import org.hamcrest.Matcher;

public class CustomViewActions {

    public static ViewAction waitForView(int viewId, long timeout) {
        return new ViewAction() {

            @Override
            public String getDescription() {
                return "Wait for view with specific id \n@param viewId: resource ID \n@param timeout: timeout in milli seconds.";
            }

            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();
                long startTime = System.currentTimeMillis();
                long endTime = startTime + timeout;
                Matcher viewMatcher = withId(viewId);

                while (System.currentTimeMillis() < endTime) {
                    // Iterate through all views on the screen and see if the view we are looking for is there already
                    for (View child : TreeIterables.breadthFirstViewTraversal(view)) {
                        // found view with required ID
                        if (viewMatcher.matches(child)) {
                            return;
                        }
                    }
                    // Loops the main thread for a specified period of time.
                    // Control may not return immediately, instead it'll return after the provided delay has passed and the queue is in an idle state again.
                    uiController.loopMainThreadForAtLeast(100);
                }
            }
        };
    }

    public static ViewAction waitForText(@StringRes int textId, long timeout) {
        return new ViewAction() {

            @Override
            public String getDescription() {
                return "Wait for view with specific id \n@param viewId: resource ID \n@param timeout: timeout in milli seconds.";
            }

            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();
                long startTime = System.currentTimeMillis();
                long endTime = startTime + timeout;
                Matcher viewMatcher = withText(textId);

                while (System.currentTimeMillis() < endTime) {
                    // Iterate through all views on the screen and see if the view we are looking for is there already
                    for (View child : TreeIterables.breadthFirstViewTraversal(view)) {
                        // found view with required ID
                        if (viewMatcher.matches(child)) {
                            return;
                        }
                    }
                    // Loops the main thread for a specified period of time.
                    // Control may not return immediately, instead it'll return after the provided delay has passed and the queue is in an idle state again.
                    uiController.loopMainThreadForAtLeast(100);
                }
            }
        };
    }
}