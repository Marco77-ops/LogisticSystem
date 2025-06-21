package com.luckypets.logistics.e2e;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        SimpleE2ETest.class,           // Einfache Tests
        BasicWorkflowE2ETest.class,    // Vollständige Tests
        AnalyticsE2ETest.class,        // Analytics Tests
        ErrorHandlingE2ETest.class     // Error Tests
})
public class EndToEndTestSuite {
    // Test Suite Runner - führt alle E2E Tests aus
}