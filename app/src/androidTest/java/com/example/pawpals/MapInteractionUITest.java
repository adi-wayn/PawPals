package com.example.pawpals;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import model.Dog;
import model.User;

@RunWith(AndroidJUnit4.class)
public class MapInteractionUITest {

    private ActivityScenario<CommunityActivity> scenario;

    @Before
    public void setUp() {
        // Create a fake User with a dog named "Bobby"
        User fakeUser = new User("TestUser", "TestCommunity", "contact", "dogs");
        fakeUser.setUid("testUid");

        Dog dog = new Dog();
        dog.setName("Bobby");
        dog.setBreed("Husky");
        dog.setAge(3);

        fakeUser.addDogLocal(dog);

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
    public void testOpenDogProfile_fromCommunity() {
        // Click on the dog name "Bobby"
        onView(withText("Bobby")).perform(click());

        // Verify that the dog name is displayed in the profile
        onView(withId(R.id.dog_name)).check(matches(withText("Bobby")));
        onView(withId(R.id.dog_name)).check(matches(isDisplayed()));
    }
}
