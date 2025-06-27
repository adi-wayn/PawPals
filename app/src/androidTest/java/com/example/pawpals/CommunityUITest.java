package com.example.pawpals;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CommunityUITest {

    @Rule
    public ActivityScenarioRule<CommunityActivity> activityRule =
            new ActivityScenarioRule<>(CommunityActivity.class);

    @Test
    public void testCommunityButtonsVisible() {
        onView(withId(R.id.buttonAreaMap)).check(matches(isDisplayed()));
        onView(withId(R.id.buttonReportSystem)).check(matches(isDisplayed()));
        onView(withId(R.id.buttonMembers)).check(matches(isDisplayed()));
        onView(withId(R.id.buttonChat)).check(matches(isDisplayed()));
    }

    @Test
    public void testRecyclerViewIsVisible() {
        onView(withId(R.id.feedRecyclerView)).check(matches(isDisplayed()));
    }
}
