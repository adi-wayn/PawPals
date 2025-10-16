package com.example.pawpals;

import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.isA;

public class TestUtils {

    /**
     * פעולה שמבצעת sleep בתוך Espresso.
     * שימוש: onView(isRoot()).perform(TestUtils.waitFor(1000));
     */
    public static ViewAction waitFor(final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                // משתמשים ב־isA(View.class) כדי להחזיר Matcher<View>
                return isA(View.class);
            }

            @Override
            public String getDescription() {
                return "מחכה " + millis + " מילישניות.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }
}
