package com.example.pawpals;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import model.Community;
import model.CommunityManager;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test using Mockito to verify behavior of Community with a mocked CommunityManager.
 */
public class ExampleUnitTest {

    private Community community;
    private CommunityManager mockManager;

    @Before
    public void setUp() {
        // Create a mock of CommunityManager
        mockManager = mock(CommunityManager.class);

        // Define behavior for the mock
        when(mockManager.isManager()).thenReturn(true);

        // Use the mock in the Community constructor
        community = new Community("MockCommunity", mockManager);
    }

    @Test
    public void testCommunityWithMockedManager() {
        assertEquals("MockCommunity", community.getName());
        assertEquals(mockManager, community.getManager());
        assertTrue(community.getManager().isManager());  // As mocked
    }
}
