package com.luckypets.logistics.e2e;

import org.junit.platform.suite.api.*;

@Suite
@SelectClasses({
        RobustE2ETest.class,           // Robuste Tests zuerst
        BasicWorkflowE2ETest.class,    // Hauptfunktionalität
        ErrorHandlingE2ETest.class,    // Error-Handling
        AnalyticsE2ETest.class         // Analytics (falls vorhanden)
})
@SuiteDisplayName("LuckyPets Logistics E2E Test Suite")
public class EndToEndTestSuite {
    // Verbesserte Test Suite mit logischer Reihenfolge
}
