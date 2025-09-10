package com.example.pawpals;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import model.User;

@RunWith(AndroidJUnit4.class)
public class CommunityUITest {

    private ActivityScenario<CommunityActivity> scenario;

    @Before
    public void setUp() {
        // ניצור User פיקטיבי כדי שה־CommunityActivity יוכל להיטען
        User fakeUser = new User("TestUser", "TestCommunity", "test@contact", "Dogs");
        fakeUser.setUid("testUid");

        Intent intent = new Intent(
                androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                CommunityActivity.class
        );
        intent.putExtra("currentUser", fakeUser);

        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

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
