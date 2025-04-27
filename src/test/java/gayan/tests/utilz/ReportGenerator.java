package gayan.tests.utilz;

import com.gayan.entities.TicketPool;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for generating HTML reports of performance comparisons
 */
public class ReportGenerator {


    public static void main(String[] args) {
        System.out.println("Running performance tests and generating report...");
        Map<String, Map<TestUtilz.PoolType, Long>> results = runPerformanceTests();
        generatePerformanceReport(results);
        System.out.println("Performance testing complete.");
    }
    /**
     * Generates an HTML report comparing the performance of different TicketPool implementations
     * @param results Map of performance results indexed by operation type and implementation
     */
    public static void generatePerformanceReport(Map<String, Map<TestUtilz.PoolType, Long>> results) {
        try {
            // Create reports directory if it doesn't exist
            Path reportsDir = Paths.get("test-reports");
            if (!Files.exists(reportsDir)) {
                Files.createDirectories(reportsDir);
            }

            // Create report file with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String reportFileName = "performance-report-" + timestamp + ".html";
            Path reportFile = reportsDir.resolve(reportFileName);

            try (FileWriter writer = new FileWriter(reportFile.toFile())) {
                writer.write("<!DOCTYPE html>\n");
                writer.write("<html lang=\"en\">\n");
                writer.write("<head>\n");
                writer.write("    <meta charset=\"UTF-8\">\n");
                writer.write("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
                writer.write("    <title>Ticket System Performance Report</title>\n");
                writer.write("    <style>\n");
                writer.write("        body { font-family: Arial, sans-serif; margin: 20px; }\n");
                writer.write("        h1, h2 { color: #333; }\n");
                writer.write("        table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }\n");
                writer.write("        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
                writer.write("        th { background-color: #f2f2f2; }\n");
                writer.write("        tr:nth-child(even) { background-color: #f9f9f9; }\n");
                writer.write("        .chart-container { height: 300px; margin-bottom: 30px; }\n");
                writer.write("        .fastest { background-color: #d4edda; }\n");
                writer.write("        .conclusion { background-color: #f8f9fa; padding: 15px; border-radius: 5px; }\n");
                writer.write("    </style>\n");
                writer.write("    <script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>\n");
                writer.write("</head>\n");
                writer.write("<body>\n");

                writer.write("    <h1>Ticket System Performance Comparison</h1>\n");
                writer.write("    <p>Generated on: " + LocalDateTime.now() + "</p>\n");

                writer.write("    <h2>Performance Results (milliseconds)</h2>\n");

                // Create results table
                writer.write("    <table>\n");
                writer.write("        <tr>\n");
                writer.write("            <th>Operation</th>\n");

                // Add column headers for each implementation
                for (TestUtilz.PoolType type : TestUtilz.PoolType.values()) {
                    writer.write("            <th>" + type + "</th>\n");
                }
                writer.write("        </tr>\n");

                // Add rows for each operation
                for (Map.Entry<String, Map<TestUtilz.PoolType, Long>> entry : results.entrySet()) {
                    String operation = entry.getKey();
                    Map<TestUtilz.PoolType, Long> implementationResults = entry.getValue();

                    // Find the fastest implementation for this operation
                    TestUtilz.PoolType fastest = null;
                    long fastestTime = Long.MAX_VALUE;

                    for (Map.Entry<TestUtilz.PoolType, Long> implEntry : implementationResults.entrySet()) {
                        if (implEntry.getValue() < fastestTime) {
                            fastestTime = implEntry.getValue();
                            fastest = implEntry.getKey();
                        }
                    }

                    writer.write("        <tr>\n");
                    writer.write("            <td>" + operation + "</td>\n");

                    for (TestUtilz.PoolType type : TestUtilz.PoolType.values()) {
                        String cellClass = (type == fastest) ? " class=\"fastest\"" : "";
                        Long time = implementationResults.getOrDefault(type, 0L);
                        writer.write("            <td" + cellClass + ">" + time + "</td>\n");
                    }

                    writer.write("        </tr>\n");
                }

                writer.write("    </table>\n");

                // Add chart script
                writer.write("    <h2>Performance Visualization</h2>\n");
                writer.write("    <div class=\"chart-container\">\n");
                writer.write("        <canvas id=\"performanceChart\"></canvas>\n");
                writer.write("    </div>\n");

                writer.write("    <script>\n");
                writer.write("        const ctx = document.getElementById('performanceChart').getContext('2d');\n");
                writer.write("        const chart = new Chart(ctx, {\n");
                writer.write("            type: 'bar',\n");
                writer.write("            data: {\n");

                // Create labels array
                writer.write("                labels: [");
                boolean first = true;
                for (String operation : results.keySet()) {
                    if (!first) writer.write(", ");
                    writer.write("'" + operation + "'");
                    first = false;
                }
                writer.write("],\n");

                // Create datasets (one for each implementation)
                writer.write("                datasets: [\n");

                String[] colors = {"rgba(255, 99, 132, 0.7)", "rgba(54, 162, 235, 0.7)", "rgba(255, 206, 86, 0.7)"};
                int colorIndex = 0;

                for (TestUtilz.PoolType type : TestUtilz.PoolType.values()) {
                    if (colorIndex > 0) writer.write(",\n");

                    writer.write("                    {\n");
                    writer.write("                        label: '" + type + "',\n");
                    writer.write("                        backgroundColor: '" + colors[colorIndex % colors.length] + "',\n");
                    writer.write("                        borderColor: '" + colors[colorIndex % colors.length].replace("0.7", "1.0") + "',\n");
                    writer.write("                        borderWidth: 1,\n");
                    writer.write("                        data: [");

                    first = true;
                    for (String operation : results.keySet()) {
                        if (!first) writer.write(", ");
                        writer.write(String.valueOf(results.get(operation).getOrDefault(type, 0L)));
                        first = false;
                    }

                    writer.write("]\n");
                    writer.write("                    }");

                    colorIndex++;
                }

                writer.write("\n                ]\n");
                writer.write("            },\n");
                writer.write("            options: {\n");
                writer.write("                responsive: true,\n");
                writer.write("                maintainAspectRatio: false,\n");
                writer.write("                scales: {\n");
                writer.write("                    y: {\n");
                writer.write("                        beginAtZero: true,\n");
                writer.write("                        title: {\n");
                writer.write("                            display: true,\n");
                writer.write("                            text: 'Time (milliseconds)'\n");
                writer.write("                        }\n");
                writer.write("                    }\n");
                writer.write("                },\n");
                writer.write("                plugins: {\n");
                writer.write("                    title: {\n");
                writer.write("                        display: true,\n");
                writer.write("                        text: 'Implementation Performance Comparison'\n");
                writer.write("                    }\n");
                writer.write("                }\n");
                writer.write("            }\n");
                writer.write("        });\n");
                writer.write("    </script>\n");

                // Add implementation comparison section
                writer.write("    <h2>Implementation Analysis</h2>\n");

                writer.write("    <h3>Synchronized Implementation</h3>\n");
                writer.write("    <p>The synchronized implementation uses Java's intrinsic locks to protect the shared ticket pool.</p>\n");
                writer.write("    <h4>Strengths:</h4>\n");
                writer.write("    <ul>\n");
                writer.write("        <li>Simple to implement and understand</li>\n");
                writer.write("        <li>Auto-release of locks when exiting synchronized blocks, even during exceptions</li>\n");
                writer.write("        <li>Good for low-contention scenarios</li>\n");
                writer.write("    </ul>\n");
                writer.write("    <h4>Weaknesses:</h4>\n");
                writer.write("    <ul>\n");
                writer.write("        <li>Limited control (can't try to acquire lock with timeout)</li>\n");
                writer.write("        <li>Less scalable with high thread counts</li>\n");
                writer.write("        <li>Cannot distinguish between read and write locks</li>\n");
                writer.write("    </ul>\n");

                writer.write("    <h3>ReentrantLock Implementation</h3>\n");
                writer.write("    <p>The ReentrantLock implementation uses explicit locks and condition variables for finer-grained control.</p>\n");
                writer.write("    <h4>Strengths:</h4>\n");
                writer.write("    <ul>\n");
                writer.write("        <li>More flexibility than synchronized (timeouts, tryLock, condition variables)</li>\n");
                writer.write("        <li>Can abandon lock acquisition attempts</li>\n");
                writer.write("        <li>Better control over fairness policies</li>\n");
                writer.write("    </ul>\n");
                writer.write("    <h4>Weaknesses:</h4>\n");
                writer.write("    <ul>\n");
                writer.write("        <li>More complex to use correctly</li>\n");
                writer.write("        <li>Requires explicit lock/unlock in try-finally blocks</li>\n");
                writer.write("        <li>Risk of forgetting to release locks</li>\n");
                writer.write("    </ul>\n");

                writer.write("    <h3>BlockingQueue Implementation</h3>\n");
                writer.write("    <p>The BlockingQueue implementation uses built-in thread-safe collections designed for producer-consumer patterns.</p>\n");
                writer.write("    <h4>Strengths:</h4>\n");
                writer.write("    <ul>\n");
                writer.write("        <li>Highest performance in high-concurrency scenarios</li>\n");
                writer.write("        <li>Built specifically for producer-consumer patterns</li>\n");
                writer.write("        <li>Simplifies code by handling blocking behavior internally</li>\n");
                writer.write("    </ul>\n");
                writer.write("    <h4>Weaknesses:</h4>\n");
                writer.write("    <ul>\n");
                writer.write("        <li>Less flexible for custom synchronization needs</li>\n");
                writer.write("        <li>May require additional synchronization for operations not directly supported</li>\n");
                writer.write("        <li>Limited to queue-like behavior</li>\n");
                writer.write("    </ul>\n");

                // Add conclusion section
                writer.write("    <h2>Conclusion</h2>\n");
                writer.write("    <div class=\"conclusion\">\n");
                writer.write("        <p>Based on the performance tests and analysis, we can conclude:</p>\n");
                writer.write("        <ul>\n");
                writer.write("            <li><strong>Best for simple use cases:</strong> Synchronized implementation provides a good balance of simplicity and performance for basic scenarios.</li>\n");
                writer.write("            <li><strong>Best for complex control requirements:</strong> ReentrantLock implementation offers more control and flexibility.</li>\n");
                writer.write("            <li><strong>Best for high throughput:</strong> BlockingQueue implementation provides the best performance in high-concurrency producer-consumer scenarios.</li>\n");
                writer.write("        </ul>\n");
                writer.write("        <p>The choice of implementation should be based on the specific requirements of the application, considering factors like:</p>\n");
                writer.write("        <ul>\n");
                writer.write("            <li>Expected concurrency level</li>\n");
                writer.write("            <li>Need for control over locking behavior</li>\n");
                writer.write("            <li>Complexity of the operations being performed</li>\n");
                writer.write("            <li>Performance requirements</li>\n");
                writer.write("        </ul>\n");
                writer.write("    </div>\n");

                writer.write("</body>\n");
                writer.write("</html>\n");
            }

            System.out.println("Performance report generated: " + reportFile.toAbsolutePath());

        } catch (IOException e) {
            System.err.println("Error generating performance report: " + e.getMessage());
        }
    }

    /**
     * Helper method to collect performance results from tests
     * @return A map of operation types to implementation results
     */
    public static Map<String, Map<TestUtilz.PoolType, Long>> runPerformanceTests() {
        Map<String, Map<TestUtilz.PoolType, Long>> results = new HashMap<>();

        // Initialize results structure
        results.put("Sequential Addition", new HashMap<>());
        results.put("Sequential Purchase", new HashMap<>());
        results.put("Concurrent Operations", new HashMap<>());

        int operations = 10000;
        int threads = 10;

        // Test each implementation
        for (TestUtilz.PoolType type : TestUtilz.PoolType.values()) {
            System.out.println("Testing performance of " + type + " implementation...");

            // Test sequential addition
            long addTime = testSequentialAddition(type, operations);
            results.get("Sequential Addition").put(type, addTime);

            // Test sequential purchase
            long purchaseTime = testSequentialPurchase(type, operations);
            results.get("Sequential Purchase").put(type, purchaseTime);

            // Test concurrent operations
            long concurrentTime = testConcurrentOperations(type, threads, operations / threads);
            results.get("Concurrent Operations").put(type, concurrentTime);
        }

        return results;
    }

    private static long testSequentialAddition(TestUtilz.PoolType type, int operations) {
        TicketPool pool = TestUtilz.createTicketPool(type, operations);

        long startTime = System.nanoTime();
        for (int i = 0; i < operations; i++) {
            pool.addTicket(pool.createTicket("Test Event", "Test Vendor", "Test Location", 100.0));
        }
        long endTime = System.nanoTime();

        return (endTime - startTime) / 1_000_000; // Convert to milliseconds
    }

    private static long testSequentialPurchase(TestUtilz.PoolType type, int operations) {
        TicketPool pool = TestUtilz.createTicketPool(type, operations);
        TestUtilz.fillPool(pool, operations);

        long startTime = System.nanoTime();
        for (int i = 0; i < operations; i++) {
            pool.purchaseTicket();
        }
        long endTime = System.nanoTime();

        return (endTime - startTime) / 1_000_000; // Convert to milliseconds
    }

    private static long testConcurrentOperations(TestUtilz.PoolType type, int threads, int operationsPerThread) {
        TicketPool pool = TestUtilz.createTicketPool(type, threads * operationsPerThread);

        try {
            long startTime = System.nanoTime();
            TestUtilz.runConcurrently(threads, () -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    pool.addTicket(pool.createTicket("Test Event", "Test Vendor", "Test Location", 100.0));
                }
            });

            TestUtilz.runConcurrently(threads, () -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    pool.purchaseTicket();
                }
            });
            long endTime = System.nanoTime();

            return (endTime - startTime) / 1_000_000; // Convert to milliseconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }
}