
package com.example.pawpals.ui;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import com.example.pawpals.R;
import com.example.pawpals.ui.community.CommunityActivity;
import com.example.pawpals.ui.main.MainActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

// ----------------------------
// Community UI Test
// ----------------------------
@RunWith(AndroidJUnit4.class)
public class CommunityUITest {

    @Rule
    public ActivityTestRule<CommunityActivity> communityActivityRule =
            new ActivityTestRule<>(CommunityActivity.class);

    @Test
    public void testPostSubmission_success() {
        onView(withId(R.id.postEditText))
                .perform(typeText("פארק פתוח ברחוב הרצל"), closeSoftKeyboard());

        onView(withId(R.id.publishButton)).perform(click());

        onView(withText("פארק פתוח ברחוב הרצל"))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testEmptyPostSubmission_showsError() {
        onView(withId(R.id.publishButton)).perform(click());

        onView(withText("אנא כתוב תוכן"))
                .check(matches(isDisplayed()));
    }

    // ----------------------------
    // Map Interaction UI Test
    // ----------------------------
    @Rule
    public ActivityTestRule<MainActivity> mainActivityRule =
            new ActivityTestRule<>(MainActivity.class);

    @Test
    public void testOpenDogProfile_fromMap() {
        onView(withText("בובי")).perform(click());

        onView(withId(R.id.dogProfileName))
                .check(matches(withText("בובי")));
    }
}
