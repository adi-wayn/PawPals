
package com.example.pawpals;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

// Self-contained test class for unit testing community features
public class CommunityUnitTests {

    // Simulated Report class
    public static class Report {
        String user;
        String message;
        long timestamp;

        public Report(String user, String message, long timestamp) {
            this.user = user;
            this.message = message;
            this.timestamp = timestamp;
        }

        public String getMessage() {
            return message;
        }
    }

    // Simulated NotificationService class (for mocking)
    public interface NotificationService {
        void send(String message);
    }

    // Simulated Dog class
    public static class Dog {
        String name;
        String temperament;

        public Dog(String name, String temperament) {
            this.name = name;
            this.temperament = temperament;
        }

        public String getTemperament() {
            return temperament;
        }
    }

    // Simulated CompatibilityChecker
    public static class CompatibilityChecker {
        public static boolean isCompatible(Dog d1, Dog d2) {
            return !(d1.getTemperament().equals("Aggressive") || d2.getTemperament().equals("Aggressive"));
        }
    }

    // Simulated CommunityManager class
    public static class CommunityManager {
        private List<Report> reports = new ArrayList<>();
        private NotificationService notificationService;

        public CommunityManager() {}

        public CommunityManager(NotificationService service) {
            this.notificationService = service;
        }

        public boolean addReport(Report report) {
            reports.add(report);
            return true;
        }

        public List<Report> getReports() {
            return reports;
        }

        public void sendAlert(String message) {
            if (notificationService != null) {
                notificationService.send(message);
            }
        }
    }

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
