package com.example.pawpals;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.action.ViewActions.click;

@RunWith(AndroidJUnit4.class)
public class MapInteractionUITest {

    @Rule
    public ActivityScenarioRule<CommunityActivity> activityRule =
            new ActivityScenarioRule<>(CommunityActivity.class); // או הפעילות שמובילה להצגת פרופיל כלב

    @Test
    public void testOpenDogProfile_fromCommunity() {
        // לוחץ על שם הכלב "בובי"
        onView(withText("בובי")).perform(click());

        // מאמת שהשם מוצג בפרופיל
        onView(withId(R.id.dog_name)).check(matches(withText("בובי")));
    }
}
