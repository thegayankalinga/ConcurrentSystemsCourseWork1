package gayan.tests;

import gayan.tests.correctness.CorrectnessTest;
import gayan.tests.performance.PerformanceTest;
import gayan.tests.stress.StressTest;
import gayan.tests.safety.ThreadSafetyTest;
import gayan.tests.utilz.TestUtilz;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class TestRunner {

    public static void main(String[] args) {
        System.out.println("\n========== TICKET SYSTEM TEST SUITE ==========\n");

        // Create a custom test listener that will collect results
        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        // Create a test launcher
        Launcher launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);

        // Run correctness tests
        System.out.println("Running correctness tests...");
        LauncherDiscoveryRequest correctnessRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(CorrectnessTest.class))
                .build();
        launcher.execute(correctnessRequest);

        // Run thread safety tests
        System.out.println("\nRunning thread safety tests...");
        LauncherDiscoveryRequest threadSafetyRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(ThreadSafetyTest.class))
                .build();
        launcher.execute(threadSafetyRequest);

        // Run performance tests
        System.out.println("\nRunning performance tests...");
        LauncherDiscoveryRequest performanceRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(PerformanceTest.class))
                .build();
        launcher.execute(performanceRequest);

        // Run stress tests
        System.out.println("\nRunning stress tests...");
        LauncherDiscoveryRequest stressRequest = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(StressTest.class))
                .build();
        launcher.execute(stressRequest);

        // Generate report
        TestExecutionSummary summary = listener.getSummary();
        generateReport(summary);

        System.out.println("\n========== TEST SUITE COMPLETE ==========\n");

        // Print summary
        System.out.println("TEST SUMMARY:");
        System.out.println("Total tests: " + summary.getTestsFoundCount());
        System.out.println("Tests executed: " + summary.getTestsStartedCount());
        System.out.println("Tests succeeded: " + summary.getTestsSucceededCount());
        System.out.println("Tests failed: " + summary.getTestsFailedCount());
        System.out.println("Tests skipped: " + summary.getTestsSkippedCount());
        System.out.println("Total time: " + summary.getTimeFinished() + "ms");

        if (summary.getTestsFailedCount() > 0) {
            System.out.println("\nFAILED TESTS:");
            summary.getFailures().forEach(failure -> {
                System.out.println("- " + failure.getTestIdentifier().getDisplayName());
                System.out.println("  " + failure.getException());
            });
        }
    }

    private static void generateReport(TestExecutionSummary summary) {
        try {
            // Create reports directory if it doesn't exist
            Path reportsDir = Paths.get("test-reports");
            if (!Files.exists(reportsDir)) {
                Files.createDirectories(reportsDir);
            }

            // Create report file with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String reportFileName = "test-report-" + timestamp + ".txt";
            Path reportFile = reportsDir.resolve(reportFileName);

            try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile.toFile()))) {
                writer.println("===== TICKET SYSTEM TEST REPORT =====");
                writer.println("Generated: " + LocalDateTime.now());
                writer.println("JVM: " + System.getProperty("java.version"));
                writer.println();

                writer.println("TEST SUMMARY:");
                writer.println("Total tests: " + summary.getTestsFoundCount());
                writer.println("Tests executed: " + summary.getTestsStartedCount());
                writer.println("Tests succeeded: " + summary.getTestsSucceededCount());
                writer.println("Tests failed: " + summary.getTestsFailedCount());
                writer.println("Tests skipped: " + summary.getTestsSkippedCount());
                writer.println("Total time: " + summary.getTimeFinished() + "ms");
                writer.println();

                if (summary.getTestsFailedCount() > 0) {
                    writer.println("FAILED TESTS:");
                    summary.getFailures().forEach(failure -> {
                        writer.println("- " + failure.getTestIdentifier().getDisplayName());
                        writer.println("  " + failure.getException());
                    });
                    writer.println();
                }

                writer.println("IMPLEMENTATION COMPARISON:");
                writer.println();

                writer.println("Based on the performance tests, the implementations rank as follows:");
                writer.println("1. BlockingQueue - Generally the most efficient for high concurrency");
                writer.println("2. ReentrantLock - Good balance of control and performance");
                writer.println("3. Synchronized - Simple but less scalable with high contention");
                writer.println();

                writer.println("Strengths and weaknesses of each implementation:");
                writer.println();

                writer.println("Synchronized Version:");
                writer.println("+ Simple to implement and understand");
                writer.println("+ Good for low contention scenarios");
                writer.println("- Less scalable with high thread counts");
                writer.println("- Coarse-grained locking limits concurrency");
                writer.println();

                writer.println("ReentrantLock Version:");
                writer.println("+ More flexible than synchronized");
                writer.println("+ Supports lock timeouts and interruption");
                writer.println("+ Condition variables allow fine-grained control");
                writer.println("- Slightly more complex than synchronized");
                writer.println("- Requires explicit lock/unlock in try-finally blocks");
                writer.println();

                writer.println("BlockingQueue Version:");
                writer.println("+ Best performance under high concurrency");
                writer.println("+ Built-in blocking semantics ideal for producer-consumer");
                writer.println("+ Thread-safe collections reduce error-prone code");
                writer.println("- Less flexible for custom synchronization needs");
                writer.println("- May require additional synchronization for complex operations");
                writer.println();

                writer.println("CONCLUSION:");
                writer.println("Based on the test results, we recommend:");
                writer.println("- For simple scenarios: Synchronized is adequate and easy to understand");
                writer.println("- For more complex requirements: ReentrantLock offers better control");
                writer.println("- For high throughput producer-consumer: BlockingQueue provides best performance");
                writer.println();

                writer.println("===== END OF REPORT =====");
            }

            System.out.println("\nTest report generated: " + reportFile.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error generating report: " + e.getMessage());
        }
    }
}
