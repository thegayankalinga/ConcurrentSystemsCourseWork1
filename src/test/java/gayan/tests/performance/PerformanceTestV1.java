package gayan.tests.performance;
//
//public class PerformanceTestV1 {
//}

//package com.gayan.testing;

import com.gayan.entities.Ticket;
import com.gayan.entities.TicketPool;
import com.gayan.versions.BlockingQueueTicketPool;
import com.gayan.versions.ReentrantLockTicketPool;
import com.gayan.versions.SynchronizedTicketPool;
import com.gayan.workers.Consumer;
import com.gayan.workers.Producer;
import com.gayan.workers.Reader;
import com.gayan.workers.Writer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Comprehensive performance testing suite for the ticket system.
 * Tests the three implementations of TicketPool under various conditions
 * and collects performance metrics.
 */

@Disabled("Disabled temporarily due to long time taken by the test")
@DisplayName("Enhanced Performance Tests")
public class PerformanceTestV1 {

    private static final int WARMUP_RUNS = 2;
    private static final int TEST_RUNS = 5;
    private static final String CSV_FILE_NAME = "performance_results.csv";

    // Configuration parameters
    private static final int DEFAULT_POOL_SIZE = 100;
    private static final int DEFAULT_PRODUCER_COUNT = 5;
    private static final int DEFAULT_CONSUMER_COUNT = 10;
    private static final int DEFAULT_READER_COUNT = 3;
    private static final int DEFAULT_WRITER_COUNT = 2;
    private static final int DEFAULT_TICKETS_PER_PRODUCER = 50;
    private static final int DEFAULT_TICKETS_PER_CONSUMER = 20;
    private static final int DEFAULT_PRODUCER_RATE_MS = 50;
    private static final int DEFAULT_CONSUMER_RATE_MS = 100;
    private static final int DEFAULT_READER_RATE_MS = 200;
    private static final int DEFAULT_WRITER_RATE_MS = 300;
    private static final int DEFAULT_MAX_READ_ATTEMPTS = 20;
    private static final int DEFAULT_MAX_WRITE_ATTEMPTS = 10;

    // For high concurrency tests
    private static final int HIGH_PRODUCER_COUNT = 10;
    private static final int HIGH_CONSUMER_COUNT = 20;
    private static final int HIGH_READER_COUNT = 5;
    private static final int HIGH_WRITER_COUNT = 3;
    private static final int HIGH_PRODUCER_RATE_MS = 20;
    private static final int HIGH_CONSUMER_RATE_MS = 30;

    // For low concurrency tests
    private static final int LOW_PRODUCER_COUNT = 2;
    private static final int LOW_CONSUMER_COUNT = 4;
    private static final int LOW_READER_COUNT = 1;
    private static final int LOW_WRITER_COUNT = 1;
    private static final int LOW_PRODUCER_RATE_MS = 200;
    private static final int LOW_CONSUMER_RATE_MS = 300;

    // Test scenarios
    private enum TestScenario {
        BALANCED("Balanced Load"),
        HIGH_CONCURRENCY("High Concurrency"),
        LOW_CONCURRENCY("Low Concurrency"),
        PRODUCER_HEAVY("Producer Heavy"),
        CONSUMER_HEAVY("Consumer Heavy"),
        EMPTY_POOL("Empty Pool (Consumer First)"),
        FULL_POOL("Full Pool (Producer Blocked)"),
        STRESS_TEST("Extended Stress Test");

        private final String description;

        TestScenario(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private enum ImplementationType {
        SYNCHRONIZED("Synchronized"),
        REENTRANT_LOCK("ReentrantLock"),
        BLOCKING_QUEUE("BlockingQueue");

        private final String name;

        ImplementationType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static class TestResult {
        private final ImplementationType implementation;
        private final TestScenario scenario;
        private final long executionTime;
        private final double throughput;
        private final int totalProduced;
        private final int totalConsumed;
        private final boolean successful;
        private final String errorMessage;

        public TestResult(ImplementationType implementation, TestScenario scenario,
                          long executionTime, double throughput, int totalProduced,
                          int totalConsumed, boolean successful, String errorMessage) {
            this.implementation = implementation;
            this.scenario = scenario;
            this.executionTime = executionTime;
            this.throughput = throughput;
            this.totalProduced = totalProduced;
            this.totalConsumed = totalConsumed;
            this.successful = successful;
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            return String.format("%-15s | %-20s | %7d ms | %8.2f ops/s | %5d | %5d | %s%s",
                    implementation.getName(), scenario.getDescription(), executionTime, throughput,
                    totalProduced, totalConsumed, successful ? "SUCCESS" : "FAILED",
                    errorMessage != null ? " (" + errorMessage + ")" : "");
        }

        public String toCsvString() {
            return String.format("%s,%s,%d,%.2f,%d,%d,%s,%s",
                    implementation.getName(), scenario.name(), executionTime, throughput,
                    totalProduced, totalConsumed, successful ? "SUCCESS" : "FAILED",
                    errorMessage != null ? errorMessage.replace(",", ";") : "");
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Ticket System Performance Test Suite ===");
        System.out.println("Running " + WARMUP_RUNS + " warmup runs followed by " + TEST_RUNS + " test runs per scenario");
        System.out.println();

        List<TestResult> allResults = new ArrayList<>();

        // Run all tests for all implementations
        for (TestScenario scenario : TestScenario.values()) {
            System.out.println("======= Testing Scenario: " + scenario.getDescription() + " =======");

            for (ImplementationType implType : ImplementationType.values()) {
                System.out.println("--- Implementation: " + implType.getName() + " ---");

                // Warmup runs (results discarded)
                for (int i = 0; i < WARMUP_RUNS; i++) {
                    runTest(implType, scenario, true);
                }

                // Actual test runs
                List<TestResult> results = new ArrayList<>();
                for (int i = 0; i < TEST_RUNS; i++) {
                    TestResult result = runTest(implType, scenario, false);
                    if (result != null) {
                        results.add(result);
                        System.out.println("Run " + (i + 1) + ": " + result);
                    }
                }

                // Calculate and display average results
                if (!results.isEmpty()) {
                    double avgTime = results.stream().mapToLong(r -> r.executionTime).average().orElse(0);
                    double avgThroughput = results.stream().mapToDouble(r -> r.throughput).average().orElse(0);
                    boolean allSuccessful = results.stream().allMatch(r -> r.successful);

                    TestResult avgResult = new TestResult(
                            implType, scenario, (long) avgTime, avgThroughput,
                            results.get(0).totalProduced, results.get(0).totalConsumed,
                            allSuccessful, allSuccessful ? null : "Some tests failed"
                    );

                    System.out.println("Average: " + avgResult);
                    allResults.add(avgResult);
                }

                System.out.println();
            }
        }

        // Generate CSV report
        writeResultsToCsv(allResults);

        // Display summary
        printSummary(allResults);
    }

    private static TestResult runTest(ImplementationType implType, TestScenario scenario, boolean isWarmup) {
        TestConfiguration config = configureTest(scenario);
        TicketPool ticketPool = createTicketPool(implType, config.poolSize);

        if (ticketPool == null) {
            return null;
        }

        ConcurrentHashMap<String, Integer> producerResults = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Integer> consumerResults = new ConcurrentHashMap<>();
        AtomicInteger totalProducedTickets = new AtomicInteger(0);
        AtomicInteger totalConsumedTickets = new AtomicInteger(0);

        List<Thread> allThreads = new ArrayList<>();
        List<Producer> producers = new ArrayList<>();
        List<Consumer> consumers = new ArrayList<>();
        List<Reader> readers = new ArrayList<>();
        List<Writer> writers = new ArrayList<>();

        ExecutorService executor = Executors.newCachedThreadPool();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(
                config.producerCount + config.consumerCount + config.readerCount + config.writerCount);

        AtomicBoolean errorOccurred = new AtomicBoolean(false);
        StringBuilder errorMessages = new StringBuilder();

        try {
            // Pre-populate pool for certain scenarios
            if (scenario == TestScenario.FULL_POOL || scenario == TestScenario.CONSUMER_HEAVY) {
                prepopulatePool(ticketPool, config.poolSize);
            }

            // Create and start Producer threads
            for (int i = 0; i < config.producerCount; i++) {
                String eventName = "Event-" + i;
                String vendorName = "Vendor-" + i;
                String location = "Location-" + i;
                double price = 50.0 + (i * 10.0);

                Producer producer = new Producer(
                        ticketPool, config.producerRateMs, config.ticketsPerProducer,
                        eventName, vendorName, location, price);
                producers.add(producer);

                Thread thread = new Thread(() -> {
                    try {
                        startLatch.await();
                        producer.run();
                    } catch (Exception e) {
                        errorOccurred.set(true);
                        errorMessages.append("Producer error: ").append(e.getMessage()).append("; ");
                    } finally {
                        finishLatch.countDown();
                    }
                }, "Producer-" + i);

                allThreads.add(thread);
                thread.start();
            }

            // Create and start Consumer threads
            for (int i = 0; i < config.consumerCount; i++) {
                Consumer consumer = new Consumer(
                        ticketPool, config.ticketsPerConsumer, config.consumerRateMs, config.simulateCancel);
                consumers.add(consumer);

                Thread thread = new Thread(() -> {
                    try {
                        startLatch.await();
                        consumer.run();
                    } catch (Exception e) {
                        errorOccurred.set(true);
                        errorMessages.append("Consumer error: ").append(e.getMessage()).append("; ");
                    } finally {
                        finishLatch.countDown();
                    }
                }, "Consumer-" + i);

                allThreads.add(thread);
                thread.start();
            }

            // Create and start Reader threads
            for (int i = 0; i < config.readerCount; i++) {
                Reader reader = new Reader(ticketPool, config.readerRateMs, config.maxReadAttempts);
                readers.add(reader);

                Thread thread = new Thread(() -> {
                    try {
                        startLatch.await();
                        reader.run();
                    } catch (Exception e) {
                        errorOccurred.set(true);
                        errorMessages.append("Reader error: ").append(e.getMessage()).append("; ");
                    } finally {
                        finishLatch.countDown();
                    }
                }, "Reader-" + i);

                allThreads.add(thread);
                thread.start();
            }

            // Create and start Writer threads
            for (int i = 0; i < config.writerCount; i++) {
                Writer writer = new Writer(ticketPool, config.writerRateMs, config.maxWriteAttempts);
                writers.add(writer);

                Thread thread = new Thread(() -> {
                    try {
                        startLatch.await();
                        writer.run();
                    } catch (Exception e) {
                        errorOccurred.set(true);
                        errorMessages.append("Writer error: ").append(e.getMessage()).append("; ");
                    } finally {
                        finishLatch.countDown();
                    }
                }, "Writer-" + i);

                allThreads.add(thread);
                thread.start();
            }

            // Start the test (all threads begin simultaneously)
            long startTime = System.currentTimeMillis();
            startLatch.countDown();

            // For stress tests, add a timeout
            boolean allFinished;
            if (scenario == TestScenario.STRESS_TEST) {
                allFinished = finishLatch.await(120, TimeUnit.SECONDS);
                if (!allFinished) {
                    errorOccurred.set(true);
                    errorMessages.append("Stress test timed out after 120 seconds; ");
                }
            } else {
                allFinished = finishLatch.await(60, TimeUnit.SECONDS);
                if (!allFinished) {
                    errorOccurred.set(true);
                    errorMessages.append("Test timed out after 60 seconds; ");
                }
            }

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;

            // Verify results
            int producedTickets = ticketPool.getAllTicketsCount();
            int consumedTickets = ticketPool.getSoldTicketCount();

            boolean correctResults = true;
            if (scenario != TestScenario.EMPTY_POOL && scenario != TestScenario.FULL_POOL) {
                // In normal scenarios, we expect production and consumption to be working
                if (producedTickets == 0 || (scenario != TestScenario.PRODUCER_HEAVY && consumedTickets == 0)) {
                    correctResults = false;
                    errorMessages.append("No tickets produced or consumed; ");
                }
            }

            // In the EMPTY_POOL test, we expect consumers to be initially blocked
            if (scenario == TestScenario.EMPTY_POOL && consumedTickets > producedTickets) {
                correctResults = false;
                errorMessages.append("More tickets consumed than produced; ");
            }

            if (scenario == TestScenario.FULL_POOL) {
                // For full pool, we prepopulate and expect consumers to succeed
                if (consumedTickets == 0) {
                    correctResults = false;
                    errorMessages.append("No tickets consumed from full pool; ");
                }
            }

            // Data races check - this would be a serious thread safety issue
            if (consumedTickets > producedTickets) {
                errorOccurred.set(true);
                errorMessages.append("DATA RACE: More tickets consumed than produced; ");
            }

            // Calculate throughput (operations per second)
            double totalOperations = producedTickets + consumedTickets;
            double throughput = (totalOperations * 1000.0) / totalTime;  // ops/second

            if (!isWarmup) {
                return new TestResult(
                        implType, scenario, totalTime, throughput,
                        producedTickets, consumedTickets,
                        correctResults && !errorOccurred.get(),
                        errorOccurred.get() ? errorMessages.toString() : null
                );
            }

            return null;

        } catch (Exception e) {
            System.err.println("Test error: " + e.getMessage());
            e.printStackTrace();

            if (!isWarmup) {
                return new TestResult(
                        implType, scenario, 0, 0, 0, 0,
                        false, "Exception: " + e.getMessage()
                );
            }

            return null;
        } finally {
            // Ensure cleanup
            producers.forEach(Producer::stop);
            consumers.forEach(Consumer::stop);
            readers.forEach(Reader::stop);
            writers.forEach(Writer::stop);

            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static TicketPool createTicketPool(ImplementationType type, int capacity) {
        switch (type) {
            case SYNCHRONIZED:
                return new SynchronizedTicketPool(capacity);
            case REENTRANT_LOCK:
                return new ReentrantLockTicketPool(capacity);
            case BLOCKING_QUEUE:
                return new BlockingQueueTicketPool(capacity);
            default:
                System.err.println("Unknown implementation type: " + type);
                return null;
        }
    }

    private static void prepopulatePool(TicketPool pool, int count) {
        System.out.println("Pre-populating pool with " + count + " tickets...");
        for (int i = 0; i < count; i++) {
            Ticket ticket = pool.createTicket("Pre-Event", "Pre-Vendor", "Pre-Location", 100.0);
            pool.addTicket(ticket);
        }
    }

    private static TestConfiguration configureTest(TestScenario scenario) {
        TestConfiguration config = new TestConfiguration();

        // Default configuration
        config.poolSize = DEFAULT_POOL_SIZE;
        config.producerCount = DEFAULT_PRODUCER_COUNT;
        config.consumerCount = DEFAULT_CONSUMER_COUNT;
        config.readerCount = DEFAULT_READER_COUNT;
        config.writerCount = DEFAULT_WRITER_COUNT;
        config.ticketsPerProducer = DEFAULT_TICKETS_PER_PRODUCER;
        config.ticketsPerConsumer = DEFAULT_TICKETS_PER_CONSUMER;
        config.producerRateMs = DEFAULT_PRODUCER_RATE_MS;
        config.consumerRateMs = DEFAULT_CONSUMER_RATE_MS;
        config.readerRateMs = DEFAULT_READER_RATE_MS;
        config.writerRateMs = DEFAULT_WRITER_RATE_MS;
        config.maxReadAttempts = DEFAULT_MAX_READ_ATTEMPTS;
        config.maxWriteAttempts = DEFAULT_MAX_WRITE_ATTEMPTS;
        config.simulateCancel = false;

        // Adjust configuration based on scenario
        switch (scenario) {
            case HIGH_CONCURRENCY:
                config.poolSize = 200;
                config.producerCount = HIGH_PRODUCER_COUNT;
                config.consumerCount = HIGH_CONSUMER_COUNT;
                config.readerCount = HIGH_READER_COUNT;
                config.writerCount = HIGH_WRITER_COUNT;
                config.producerRateMs = HIGH_PRODUCER_RATE_MS;
                config.consumerRateMs = HIGH_CONSUMER_RATE_MS;
                break;

            case LOW_CONCURRENCY:
                config.poolSize = 50;
                config.producerCount = LOW_PRODUCER_COUNT;
                config.consumerCount = LOW_CONSUMER_COUNT;
                config.readerCount = LOW_READER_COUNT;
                config.writerCount = LOW_WRITER_COUNT;
                config.producerRateMs = LOW_PRODUCER_RATE_MS;
                config.consumerRateMs = LOW_CONSUMER_RATE_MS;
                break;

            case PRODUCER_HEAVY:
                config.producerCount = HIGH_PRODUCER_COUNT;
                config.consumerCount = LOW_CONSUMER_COUNT;
                config.producerRateMs = HIGH_PRODUCER_RATE_MS;
                config.consumerRateMs = LOW_CONSUMER_RATE_MS;
                break;

            case CONSUMER_HEAVY:
                config.producerCount = LOW_PRODUCER_COUNT;
                config.consumerCount = HIGH_CONSUMER_COUNT;
                config.producerRateMs = LOW_PRODUCER_RATE_MS;
                config.consumerRateMs = HIGH_CONSUMER_RATE_MS;
                break;

            case EMPTY_POOL:
                config.poolSize = 100;
                config.producerCount = 3;
                config.consumerCount = 5;
                config.producerRateMs = 200;  // Slower producers
                config.consumerRateMs = 50;   // Faster consumers
                break;

            case FULL_POOL:
                // For full pool test, we'll pre-populate the pool
                config.poolSize = 100;
                config.producerCount = 5;
                config.consumerCount = 3;
                config.producerRateMs = 50;   // Fast producers
                config.consumerRateMs = 200;  // Slow consumers
                break;

            case STRESS_TEST:
                config.poolSize = 500;
                config.producerCount = 20;
                config.consumerCount = 50;
                config.readerCount = 10;
                config.writerCount = 5;
                config.ticketsPerProducer = 200;
                config.ticketsPerConsumer = 80;
                config.producerRateMs = 10;
                config.consumerRateMs = 20;
                config.simulateCancel = true;  // Add cancellations to stress test
                break;

            default:
                // Use defaults for BALANCED scenario
                break;
        }

        return config;
    }

    private static void writeResultsToCsv(List<TestResult> results) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = timestamp + "_" + CSV_FILE_NAME;

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Write header
            writer.println("Implementation,Scenario,ExecutionTime(ms),Throughput(ops/s),TotalProduced,TotalConsumed,Status,ErrorMessage");

            // Write results
            for (TestResult result : results) {
                writer.println(result.toCsvString());
            }

            System.out.println("Results written to " + filename);
        } catch (IOException e) {
            System.err.println("Error writing results to CSV: " + e.getMessage());
        }
    }

    private static void printSummary(List<TestResult> results) {
        System.out.println("\n====== PERFORMANCE TEST SUMMARY ======");

        // Group by scenario
        Map<TestScenario, Map<ImplementationType, TestResult>> scenarioResults = new HashMap<>();

        for (TestResult result : results) {
            scenarioResults
                    .computeIfAbsent(result.scenario, k -> new HashMap<>())
                    .put(result.implementation, result);
        }

        // Print summary for each scenario
        for (TestScenario scenario : TestScenario.values()) {
            System.out.println("\n--- " + scenario.getDescription() + " ---");

            Map<ImplementationType, TestResult> impls = scenarioResults.get(scenario);
            if (impls == null) {
                System.out.println("No results available");
                continue;
            }

            System.out.println("Implementation      | Execution Time | Throughput | Success");
            System.out.println("--------------------|----------------|------------|--------");

            // Find the fastest implementation for this scenario
            Optional<Map.Entry<ImplementationType, TestResult>> fastest = impls.entrySet().stream()
                    .filter(e -> e.getValue().successful)
                    .min(Comparator.comparingLong(e -> e.getValue().executionTime));

            // Find highest throughput for this scenario
            Optional<Map.Entry<ImplementationType, TestResult>> highestThroughput = impls.entrySet().stream()
                    .filter(e -> e.getValue().successful)
                    .max(Comparator.comparingDouble(e -> e.getValue().throughput));

            for (ImplementationType implType : ImplementationType.values()) {
                TestResult result = impls.get(implType);
                if (result != null) {
                    String fastestMarker = fastest.isPresent() && fastest.get().getKey() == implType ? " ⚡" : "";
                    String throughputMarker = highestThroughput.isPresent() && highestThroughput.get().getKey() == implType ? " 🚀" : "";

                    System.out.printf("%-18s | %8d ms %s | %9.2f ops/s %s | %s\n",
                            implType.getName(),
                            result.executionTime,
                            fastestMarker,
                            result.throughput,
                            throughputMarker,
                            result.successful ? "✓" : "✗");
                }
            }
        }

        // Overall performance ranking
        System.out.println("\n=== OVERALL PERFORMANCE RANKING ===");

        // Average execution time across all scenarios
        Map<ImplementationType, Double> avgExecutionTimes = new HashMap<>();
        Map<ImplementationType, Double> avgThroughputs = new HashMap<>();
        Map<ImplementationType, Integer> successCounts = new HashMap<>();

        for (ImplementationType implType : ImplementationType.values()) {
            List<TestResult> implResults = results.stream()
                    .filter(r -> r.implementation == implType)
                    .collect(java.util.stream.Collectors.toList());

            double avgTime = implResults.stream()
                    .mapToLong(r -> r.executionTime)
                    .average()
                    .orElse(0);

            double avgThroughput = implResults.stream()
                    .mapToDouble(r -> r.throughput)
                    .average()
                    .orElse(0);

            int successCount = (int) implResults.stream()
                    .filter(r -> r.successful)
                    .count();

            avgExecutionTimes.put(implType, avgTime);
            avgThroughputs.put(implType, avgThroughput);
            successCounts.put(implType, successCount);
        }

        System.out.println("Implementation      | Avg Time (ms) | Avg Throughput | Success Rate");
        System.out.println("--------------------|---------------|----------------|-------------");

        // Sort implementations by average throughput (higher is better)
        List<ImplementationType> sortedImpls = Arrays.asList(ImplementationType.values());
        sortedImpls.sort(Comparator.comparingDouble(avgThroughputs::get).reversed());

        for (ImplementationType implType : sortedImpls) {
            System.out.printf("%-18s | %13.2f | %14.2f | %d/%d\n",
                    implType.getName(),
                    avgExecutionTimes.get(implType),
                    avgThroughputs.get(implType),
                    successCounts.get(implType),
                    TestScenario.values().length);
        }

        System.out.println("\n====== TEST COMPLETED ======");
    }

    private static class TestConfiguration {
        int poolSize;
        int producerCount;
        int consumerCount;
        int readerCount;
        int writerCount;
        int ticketsPerProducer;
        int ticketsPerConsumer;
        int producerRateMs;
        int consumerRateMs;
        int readerRateMs;
        int writerRateMs;
        int maxReadAttempts;
        int maxWriteAttempts;
        boolean simulateCancel;
    }
}