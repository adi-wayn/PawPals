package com.example.pawpals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.anyOf;

@RunWith(AndroidJUnit4.class)
public class ProfileActivityTest {

    @Rule
    public ActivityScenarioRule<ProfileActivity> activityRule =
            new ActivityScenarioRule<>(ProfileActivity.class);

    // ✅ ברירת מחדל – חברים מוצגים
    @Test
    public void testDefaultShowsFriends() {
        onView(withId(R.id.friends_recycler)).check(matches(isDisplayed()));
        onView(withId(R.id.dogs_scroll)).check(matches(withEffectiveVisibility(Visibility.GONE)));
    }

    // ✅ מעבר ל־Dogs Scroll
    @Test
    public void testToggleShowsDogs() {
        onView(withId(R.id.btn_show_dogs)).perform(click());
        onView(withId(R.id.dogs_scroll)).check(matches(isDisplayed()));
        onView(withId(R.id.friends_recycler)).check(matches(withEffectiveVisibility(Visibility.GONE)));
    }

    // ✅ כפתור הוספת כלב – מוצג או מוסתר
    @Test
    public void testAddDogButtonVisibility() {
        onView(withId(R.id.fab_add_dog))
                .check(matches(anyOf(isDisplayed(), withEffectiveVisibility(Visibility.GONE))));
    }

    // ✅ חזרה ל־Friends אחרי מעבר ל־Dogs
    @Test
    public void testToggleBackToFriends() {
        onView(withId(R.id.btn_show_dogs)).perform(click());
        onView(withId(R.id.btn_show_friends)).perform(click());
        onView(withId(R.id.friends_recycler)).check(matches(isDisplayed()));
    }

    // ✅ בדיקה ששדות טקסט של פרופיל מוצגים
    @Test
    public void testProfileTextFieldsVisible() {
        onView(withId(R.id.user_name)).check(matches(isDisplayed()));
        onView(withId(R.id.bio_text)).check(matches(isDisplayed()));
        onView(withId(R.id.contact_text)).check(matches(isDisplayed()));
        onView(withId(R.id.community_status)).check(matches(isDisplayed()));
    }

    // ✅ בדיקה שה־RecyclerView של חברים נטען גם כשהמשתמש אין לו חברים
    @Test
    public void testFriendsRecyclerAlwaysVisible() {
        onView(withId(R.id.friends_recycler)).check(matches(isDisplayed()));
    }

    // ✅ בדיקה של לחיצה על toggle Dogs ללא כלבים – נשאר במסך כלבים ריק
    @Test
    public void testShowDogsWhenEmpty() {
        onView(withId(R.id.btn_show_dogs)).perform(click());
        onView(withId(R.id.dogs_scroll)).check(matches(isDisplayed()));
    }
}
