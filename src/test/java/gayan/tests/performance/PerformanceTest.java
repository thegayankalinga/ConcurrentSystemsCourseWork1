package gayan.tests.performance;

import com.gayan.entities.TicketPool;
import gayan.tests.BaseTestConfig;
import gayan.tests.utilz.TestUtilz;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceTest extends BaseTestConfig {

    private static final int WARMUP_ITERATIONS = 3;
    private static final int TEST_ITERATIONS = 5;
    private static final int OPERATIONS_PER_ITERATION = 10000;

    @Test
    @DisplayName("Measure and compare performance of different implementations")
    void compareImplementationPerformance() throws InterruptedException {
        // Results storage
        Map<TestUtilz.PoolType, Long> additionTimes = new HashMap<>();
        Map<TestUtilz.PoolType, Long> purchaseTimes = new HashMap<>();
        Map<TestUtilz.PoolType, Long> concurrentTimes = new HashMap<>();

        System.out.println("\n=== PERFORMANCE TEST RESULTS ===\n");

        // Test each implementation
        for (TestUtilz.PoolType type : TestUtilz.PoolType.values()) {
            System.out.println("Testing " + type + " implementation...");

            // Warm-up
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                runAdditionTest(type, 1000);
                runPurchaseTest(type, 1000);
                runConcurrentTest(type, 5, 5, 1000);
            }

            // Measure addition performance
            long addTime = 0;
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                addTime += runAdditionTest(type, OPERATIONS_PER_ITERATION);
            }
            additionTimes.put(type, addTime / TEST_ITERATIONS);

            // Measure purchase performance
            long purchaseTime = 0;
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                purchaseTime += runPurchaseTest(type, OPERATIONS_PER_ITERATION);
            }
            purchaseTimes.put(type, purchaseTime / TEST_ITERATIONS);

            // Measure concurrent performance
            long concurrentTime = 0;
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                concurrentTime += runConcurrentTest(type, 10, 10, OPERATIONS_PER_ITERATION / 10);
            }
            concurrentTimes.put(type, concurrentTime / TEST_ITERATIONS);
        }

        // Print results
        System.out.println("\n=== Sequential Addition Performance ===");
        System.out.println("Operations: " + OPERATIONS_PER_ITERATION + " ticket additions");
        printResults(additionTimes);

        System.out.println("\n=== Sequential Purchase Performance ===");
        System.out.println("Operations: " + OPERATIONS_PER_ITERATION + " ticket purchases");
        printResults(purchaseTimes);

        System.out.println("\n=== Concurrent Operations Performance ===");
        System.out.println("Operations: 10 producers + 10 consumers with " + (OPERATIONS_PER_ITERATION / 10) + " operations each");
        printResults(concurrentTimes);
    }

    private long runAdditionTest(TestUtilz.PoolType type, int operations) {
        TicketPool pool = TestUtilz.createTicketPool(type, operations);

        long startTime = System.nanoTime();
        for (int i = 0; i < operations; i++) {
            pool.addTicket(pool.createTicket("Event", "Vendor", "Location", 100.0));
        }
        long endTime = System.nanoTime();

        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }

    private long runPurchaseTest(TestUtilz.PoolType type, int operations) {
        TicketPool pool = TestUtilz.createTicketPool(type, operations);
        TestUtilz.fillPool(pool, operations);

        long startTime = System.nanoTime();
        for (int i = 0; i < operations; i++) {
            pool.purchaseTicket();
        }
        long endTime = System.nanoTime();

        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }

    private long runConcurrentTest(TestUtilz.PoolType type, int producers, int consumers, int operationsPerThread) throws InterruptedException {
        TicketPool pool = TestUtilz.createTicketPool(type, producers * operationsPerThread);
        ExecutorService executor = Executors.newFixedThreadPool(producers + consumers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(producers + consumers);
        AtomicLong duration = new AtomicLong(0);

        // Create producer tasks
        for (int i = 0; i < producers; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    long start = System.nanoTime();

                    for (int j = 0; j < operationsPerThread; j++) {
                        pool.addTicket(pool.createTicket("Event", "Vendor", "Location", 100.0));
                    }

                    long end = System.nanoTime();
                    duration.addAndGet(end - start);
                    endLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        // Create consumer tasks
        for (int i = 0; i < consumers; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    long start = System.nanoTime();

                    for (int j = 0; j < operationsPerThread; j++) {
                        Optional<?> ticket = pool.purchaseTicket();
                        if (!ticket.isPresent()) {
                            // Wait briefly if no ticket is available
                            Thread.sleep(1);
                        }
                    }

                    long end = System.nanoTime();
                    duration.addAndGet(end - start);
                    endLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        // Start all threads at once
        long totalStart = System.nanoTime();
        startLatch.countDown();

        // Wait for completion
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        long totalEnd = System.nanoTime();

        executor.shutdownNow();

        // If not all threads completed within timeout, return the elapsed time
        return completed ? TimeUnit.NANOSECONDS.toMillis(totalEnd - totalStart) : 30000;
    }

    private void printResults(Map<TestUtilz.PoolType, Long> results) {
        // Find the fastest implementation
        TestUtilz.PoolType fastest = results.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // Print results with comparison to fastest
        for (Map.Entry<TestUtilz.PoolType, Long> entry : results.entrySet()) {
            TestUtilz.PoolType type = entry.getKey();
            long time = entry.getValue();

            String comparison = type == fastest ?
                    "FASTEST" :
                    String.format("%.2fx slower", (double) time / results.get(fastest));

            System.out.printf("%-20s: %5d ms (%s)\n", type, time, comparison);
        }
    }
}
