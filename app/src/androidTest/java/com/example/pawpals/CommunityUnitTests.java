package com.example.pawpals;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import model.Community;
import model.CommunityManager;
import model.Report;
import model.User;

public class CommunityUnitTests {

    private Community community;
    private CommunityManager manager;

    @Before
    public void setUp() {
        manager = new CommunityManager("Dor", "ben-devide", "05**", "i love dog");
        // נוסיף lat/lng ברירת מחדל (0,0) כדי שהבנאי יתאים
        community = new Community("ben-devide", 0.0, 0.0, manager);
    }

    @Test
    public void testCommunityName() {
        assertEquals("ben-devide", community.getName());
    }

    @Test
    public void testManagerAssignment() {
        assertEquals(manager, community.getManager());
        assertTrue(manager.isManager());
    }

    @Test
    public void testAddAndGetReports() {
        Report r1 = new Report(Report.TYPE_POST, "ALEX", "Lost Dog", "Please help me find my dog.");
        Report r2 = new Report("Complaint", "Sara", "Noise", "Too much barking.");

        community.addReport(r1);
        community.addReport(r2);

        ArrayList<Report> allReports = community.getReports();
        assertEquals(2, allReports.size());

        ArrayList<Report> posts = community.getReportByType(Report.TYPE_POST);
        assertEquals(1, posts.size());
        assertEquals("ALEX", posts.get(0).getSenderName());
    }

    @Test
    public void testAddMembers() {
        // מחלקת User דורשת 4 פרמטרים: name, community, contact, fields
        User user1 = new User("Ady", "ben-devide", "contact", "dogs");
        ArrayList<User> newMembers = new ArrayList<>();
        newMembers.add(user1);

        community.setMembers(newMembers);

        assertEquals(1, community.getMembers().size());
        assertEquals("Ady", community.getMembers().get(0).getUserName());
    }
}
