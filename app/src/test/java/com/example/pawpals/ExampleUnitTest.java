package com.example.pawpals;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import model.Community;
import model.CommunityManager;
import model.User;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Unit test using Mockito to verify behavior of Community with a mocked CommunityManager.
 */
public class ExampleUnitTest {

    private Community community;
    private CommunityManager mockManager;

    @Before
    public void setUp() {
        // יצירת Mock של CommunityManager
        mockManager = mock(CommunityManager.class);

        // נגדיר התנהגות: תמיד יחזיר true עבור isManager
        when(mockManager.isManager()).thenReturn(true);

        // נשתמש בבנאי הקיים שדורש lat/lng בנוסף לשם ול־manager
        community = new Community("MockCommunity", 0.0, 0.0, mockManager);
    }

    @Test
    public void testCommunityWithMockedManager() {
        assertEquals("MockCommunity", community.getName());
        assertEquals(mockManager, community.getManager());
        assertTrue(community.getManager().isManager());  // As mocked
    }

    @Test
    public void testAddMockedMember() {
        // יצירת אובייקט מדומה של User בעזרת Mockito
        User mockUser = mock(User.class);

        // קביעת התנהגות: כאשר נקרא getUserName על האובייקט, יוחזר "Ady"
        when(mockUser.getUserName()).thenReturn("Ady");

        ArrayList<User> mockedMembers = new ArrayList<>();
        mockedMembers.add(mockUser);

        community.setMembers(mockedMembers);

        assertEquals(1, community.getMembers().size());
        assertEquals("Ady", community.getMembers().get(0).getUserName());
    }
}
