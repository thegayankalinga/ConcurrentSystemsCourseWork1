package gayan.tests;

//import com.gayan.tests.utilz.ReportGenerator;
//import com.gayan.tests.utilz.TestUtilz;

import gayan.tests.utilz.ReportGenerator;
import gayan.tests.utilz.TestUtilz;

import java.util.Map;

/**
 * Main entry point for running comprehensive tests on the ticket system
 */
public class TicketSystemTester {

    public static void main(String[] args) {
        System.out.println("=== Starting Ticket System Tests ===");

        // Run JUnit tests via TestRunner
        System.out.println("\n=== Running Unit Tests ===");
        TestRunner.main(args);

        // Run performance tests and generate report
        System.out.println("\n=== Running Performance Tests for Report ===");
        Map<String, Map<TestUtilz.PoolType, Long>> performanceResults = ReportGenerator.runPerformanceTests();
        ReportGenerator.generatePerformanceReport(performanceResults);

        System.out.println("\n=== All Tests Complete ===");
    }
}
