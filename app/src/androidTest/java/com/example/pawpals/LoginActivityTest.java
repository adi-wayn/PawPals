package com.example.pawpals;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    // ✅ בדיקה ששדות ריקים מחזירים שגיאה
    @Test
    public void testEmptyFieldsShowsError() {
        onView(withId(R.id.button_login)).perform(click());
        onView(withId(R.id.input_email)).check(matches(hasErrorText("Invalid email")));
    }

    // ✅ בדיקה שאימייל לא תקין מציג שגיאה
    @Test
    public void testInvalidEmailShowsError() {
        onView(withId(R.id.input_email)).perform(typeText("notAnEmail"), closeSoftKeyboard());
        onView(withId(R.id.input_password)).perform(typeText("123456"), closeSoftKeyboard());
        onView(withId(R.id.button_login)).perform(click());
        onView(withId(R.id.input_email)).check(matches(hasErrorText("Invalid email")));
    }

    // ✅ בדיקה שסיסמה קצרה מידי מחזירה שגיאה
    @Test
    public void testShortPasswordShowsError() {
        onView(withId(R.id.input_email)).perform(typeText("test@test.com"), closeSoftKeyboard());
        onView(withId(R.id.input_password)).perform(typeText("123"), closeSoftKeyboard());
        onView(withId(R.id.button_login)).perform(click());
        onView(withId(R.id.input_password)).check(matches(hasErrorText("Password too short")));
    }

    // ✅ בדיקה שקישור לרישום מעביר ל־RegisterActivity
    @Test
    public void testNavigateToRegister() {
        onView(withId(R.id.link_register)).perform(click());
        onView(withId(R.id.button_register)).check(matches(isDisplayed())); // בודקים מעבר למסך רישום
    }

    @Test
    public void testButtonsAreVisible() {
        onView(withId(R.id.button_login)).check(matches(isDisplayed()));
        onView(withId(R.id.btnGoogleSignIn)).check(matches(isDisplayed()));
    }

    // ✅ בדיקה של enable/disable של כפתורי התחברות
    @Test
    public void testButtonsEnableDisable() {
        // נוסיף אימייל + סיסמה חוקיים כדי שהלוגיקה תגיע ל־disableButtons()
        onView(withId(R.id.input_email)).perform(typeText("test@test.com"), closeSoftKeyboard());
        onView(withId(R.id.input_password)).perform(typeText("123456"), closeSoftKeyboard());

        onView(withId(R.id.button_login)).perform(click());
        onView(withId(R.id.button_login)).check(matches(not(isEnabled())));
    }

    // ✅ בדיקה של כישלון התחברות → נשארים במסך
    @Test
    public void testLoginFailureStaysOnLoginScreen() {
        onView(withId(R.id.input_email)).perform(typeText("wrong@test.com"), closeSoftKeyboard());
        onView(withId(R.id.input_password)).perform(typeText("123456"), closeSoftKeyboard());
        onView(withId(R.id.button_login)).perform(click());

        onView(withId(R.id.input_email)).check(matches(isDisplayed()));
    }
}
