package com.example.pawpals;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.example.pawpals.logic.CommunityManager;
import com.example.pawpals.logic.NotificationService;
import com.example.pawpals.logic.Report;
import com.example.pawpals.logic.CompatibilityChecker;
import com.example.pawpals.model.Dog;

import org.junit.Test;

public class CommunityUnitTests {

    @Test
    public void testAddReportSuccessfully() {
        CommunityManager manager = new CommunityManager();
        Report report = new Report("User123", "פארק סגור ליד הדואר", System.currentTimeMillis());
        boolean result = manager.addReport(report);
        assertTrue(result);
        assertTrue(manager.getReports().contains(report));
    }

    @Test
    public void testDogsCompatibility() {
        Dog d1 = new Dog("Luna", "Friendly");
        Dog d2 = new Dog("Max", "Aggressive");
        boolean result = CompatibilityChecker.isCompatible(d1, d2);
        assertFalse(result);
    }

    @Test
    public void testNotificationSent() {
        NotificationService mockService = mock(NotificationService.class);
        CommunityManager manager = new CommunityManager(mockService);
        manager.sendAlert("כלב אבוד באבן גבירול");
        verify(mockService).send("כלב אבוד באבן גבירול");
    }
}
