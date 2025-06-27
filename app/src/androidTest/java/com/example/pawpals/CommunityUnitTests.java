package com.example.pawpals;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import model.*;

public class CommunityUnitTests {

    private Community community;
    private CommunityManager manager;

    @Before
    public void setUp() {
        manager = new CommunityManager("ManagerName", "MyCommunity");
        community = new Community("MyCommunity", manager);
    }

    @Test
    public void testCommunityName() {
        assertEquals("MyCommunity", community.getName());
    }

    @Test
    public void testManagerAssignment() {
        assertEquals(manager, community.getManager());
        assertTrue(manager.isManager());
    }

    @Test
    public void testAddAndGetReports() {
        Report r1 = new Report("post", "Ali", "Lost Dog", "Please help me find my dog.");
        Report r2 = new Report("complaint", "Sara", "Noise", "Too much barking.");

        community.addReport(r1);
        community.addReport(r2);

        ArrayList<Report> allReports = community.getReports();
        assertEquals(2, allReports.size());

        ArrayList<Report> posts = community.getReportByType("post");
        assertEquals(1, posts.size());
        assertEquals("Ali", posts.get(0).getSenderName());
    }

    @Test
    public void testAddMembers() {
        User user1 = new User("Mohammad", "MyCommunity");
        ArrayList<User> newMembers = new ArrayList<>();
        newMembers.add(user1);

        community.setMembers(newMembers);

        assertEquals(1, community.getMembers().size());
        assertEquals("Mohammad", community.getMembers().get(0).getUserName());
    }
}
