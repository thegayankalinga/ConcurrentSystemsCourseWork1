package gayan.tests.stress;

import com.gayan.entities.Ticket;
import com.gayan.entities.TicketPool;
import gayan.tests.BaseTestConfig;

import gayan.tests.utilz.TestUtilz;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class StressTest extends BaseTestConfig {

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("High concurrency stress test")
    void highConcurrencyStressTest(TestUtilz.PoolType poolType) throws InterruptedException {
        // Arrange
        int capacity = HIGH_CAPACITY;
        TicketPool pool = TestUtilz.createTicketPool(poolType, capacity);

        int producerCount = HIGH_CONCURRENCY_THREAD_COUNT / 4;
        int consumerCount = HIGH_CONCURRENCY_THREAD_COUNT / 4;
        int writerCount = HIGH_CONCURRENCY_THREAD_COUNT / 4;
        int readerCount = HIGH_CONCURRENCY_THREAD_COUNT / 4;

        int ticketsPerProducer = 100;
        AtomicInteger addedTickets = new AtomicInteger(0);
        AtomicInteger purchasedTickets = new AtomicInteger(0);
        AtomicInteger updatedTickets = new AtomicInteger(0);
        AtomicInteger readOperations = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(
                producerCount + consumerCount + writerCount + readerCount);

        ExecutorService executor = Executors.newFixedThreadPool(
                producerCount + consumerCount + writerCount + readerCount);

        System.out.println("\n=== STRESS TEST: " + poolType + " ===");

        // Create producer threads
        for (int i = 0; i < producerCount; i++) {
            int producerId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < ticketsPerProducer; j++) {
                        try {
                            Ticket ticket = pool.createTicket(
                                    "Event-" + producerId + "-" + j,
                                    "Producer-" + producerId,
                                    "Location-" + j,
                                    100.0 + j);
                            pool.addTicket(ticket);
                            addedTickets.incrementAndGet();
                            Thread.sleep(1); // Small delay
                        } catch (Exception e) {
                            System.err.println("Producer error: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Create consumer threads
        for (int i = 0; i < consumerCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < ticketsPerProducer; j++) {
                        try {
                            Optional<Ticket> ticket = pool.purchaseTicket();
                            if (ticket.isPresent()) {
                                purchasedTickets.incrementAndGet();
                            }
                            Thread.sleep(2); // Small delay
                        } catch (Exception e) {
                            System.err.println("Consumer error: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Create writer threads
        for (int i = 0; i < writerCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < ticketsPerProducer / 2; j++) {
                        try {
                            Optional<Ticket> ticket = pool.getRandomAvailableTicket();
                            if (ticket.isPresent()) {
                                long ticketId = ticket.get().getTicketId();
                                pool.updateTicket(ticketId, 200.0, "Updated Location", "Updated Event");
                                updatedTickets.incrementAndGet();
                            }
                            Thread.sleep(5); // Small delay
                        } catch (Exception e) {
                            System.err.println("Writer error: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Create reader threads
        for (int i = 0; i < readerCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < ticketsPerProducer * 2; j++) {
                        try {
                            pool.getAvailableTicketCount();
                            pool.getSoldTicketCount();
                            pool.getCurrentSize();
                            readOperations.incrementAndGet();
                            Thread.sleep(1); // Small delay
                        } catch (Exception e) {
                            System.err.println("Reader error: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads
        long startTime = System.currentTimeMillis();
        startLatch.countDown();

        // Wait for completion (with timeout)
        boolean completed = completionLatch.await(120, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        executor.shutdownNow();

        // Print results
        System.out.println("Test " + (completed ? "completed" : "timed out") + " in " +
                (endTime - startTime) + "ms");
        System.out.println("Added tickets: " + addedTickets.get());
        System.out.println("Purchased tickets: " + purchasedTickets.get());
        System.out.println("Updated tickets: " + updatedTickets.get());
        System.out.println("Read operations: " + readOperations.get());
        System.out.println("Final pool state:");
        System.out.println("  Total tickets: " + pool.getCurrentSize());
        System.out.println("  Available tickets: " + pool.getAvailableTicketCount());
        System.out.println("  Sold tickets: " + pool.getSoldTicketCount());

        // Assert
        assertTrue(completed, "Test should complete within the timeout");
        assertTrue(addedTickets.get() > 0, "Should have added tickets");
        assertTrue(purchasedTickets.get() > 0, "Should have purchased tickets");
        assertTrue(readOperations.get() > 0, "Should have performed read operations");
        assertEquals(addedTickets.get(), pool.getCurrentSize() + purchasedTickets.get(),
                "Added tickets should equal current size plus purchased");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Full capacity stress test")
    void fullCapacityStressTest(TestUtilz.PoolType poolType) throws InterruptedException {
        // Arrange
        int capacity = 100; // Smaller capacity to test full capacity behavior
        TicketPool pool = TestUtilz.createTicketPool(poolType, capacity);

        // First fill the pool to capacity
        TestUtilz.fillPool(pool, capacity);
        assertEquals(capacity, pool.getAllTicketsCount(), "Pool should be at capacity");

        AtomicInteger additionalAdded = new AtomicInteger(0);
        AtomicInteger rejectedAdds = new AtomicInteger(0);
        AtomicInteger purchasedTickets = new AtomicInteger(0);

        int producerCount = 20;
        int consumerCount = 20;
        int attemptsPerProducer = 25;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(producerCount + consumerCount);

        ExecutorService executor = Executors.newFixedThreadPool(producerCount + consumerCount);

        System.out.println("\n=== FULL CAPACITY TEST: " + poolType + " ===");

        // Create producers that will try to add to a full pool
        for (int i = 0; i < producerCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < attemptsPerProducer; j++) {
                        try {
                            Ticket ticket = pool.createTicket(
                                    "Overflow Event",
                                    "Overflow Producer",
                                    "Location",
                                    100.0);
                            try {
                                pool.addTicket(ticket);
                                additionalAdded.incrementAndGet();
                            } catch (Exception e) {
                                // Some implementations might throw exceptions when full
                                rejectedAdds.incrementAndGet();
                            }
                            Thread.sleep(5); // Small delay
                        } catch (Exception e) {
                            System.err.println("Full capacity producer error: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Create consumers that will make space in the pool
        for (int i = 0; i < consumerCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < attemptsPerProducer / 2; j++) {
                        try {
                            Optional<Ticket> ticket = pool.purchaseTicket();
                            if (ticket.isPresent()) {
                                purchasedTickets.incrementAndGet();
                            }
                            Thread.sleep(2); // Longer delay for consumers to create interesting interleaving
                        } catch (Exception e) {
                            System.err.println("Full capacity consumer error: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads
        long startTime = System.currentTimeMillis();
        startLatch.countDown();

        // Wait for completion (with timeout)
        boolean completed = completionLatch.await(180, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        executor.shutdownNow();

        // Print results
        System.out.println("Test " + (completed ? "completed" : "timed out") + " in " +
                (endTime - startTime) + "ms");
        System.out.println("Additional tickets added: " + additionalAdded.get());
        System.out.println("Rejected adds: " + rejectedAdds.get());
        System.out.println("Purchased tickets: " + purchasedTickets.get());
        System.out.println("Final pool state:");
        System.out.println("  Total tickets: " + pool.getAvailableTicketCount() + "/" + capacity);
        System.out.println("  Available tickets: " + pool.getAvailableTicketCount());
        System.out.println("  Sold tickets: " + pool.getSoldTicketCount());

        // Assert
        assertTrue(completed, "Test should complete within the timeout");
        assertTrue(purchasedTickets.get() > 0, "Should have purchased tickets");
        assertEquals(capacity - purchasedTickets.get() + additionalAdded.get(),
                pool.getCurrentSize(), "Final pool size should be consistent");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Empty pool stress test")
    void emptyPoolStressTest(TestUtilz.PoolType poolType) throws InterruptedException {
        // Arrange
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
        // Pool starts empty

        AtomicInteger addedTickets = new AtomicInteger(0);
        AtomicInteger purchaseAttempts = new AtomicInteger(0);
        AtomicInteger successfulPurchases = new AtomicInteger(0);

        int producerCount = 5;
        int consumerCount = 20;
        int ticketsPerProducer = 20;
        int purchaseAttemptsPerConsumer = 50;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(producerCount + consumerCount);

        ExecutorService executor = Executors.newFixedThreadPool(producerCount + consumerCount);

        System.out.println("\n=== EMPTY POOL TEST: " + poolType + " ===");

        // Create consumers that will try to purchase from an empty pool
        for (int i = 0; i < consumerCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < purchaseAttemptsPerConsumer; j++) {
                        try {
                            purchaseAttempts.incrementAndGet();
                            Optional<Ticket> ticket = pool.purchaseTicket();
                            if (ticket.isPresent()) {
                                successfulPurchases.incrementAndGet();
                            }
                            Thread.sleep(5); // Small delay
                        } catch (Exception e) {
                            System.err.println("Empty pool consumer error: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start producers with a slight delay to ensure consumers encounter empty pool
        Thread.sleep(100);

        // Create producers that will add tickets to the pool
        for (int i = 0; i < producerCount; i++) {
            int producerId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Thread.sleep(500); // Delay producers to ensure empty pool condition
                    for (int j = 0; j < ticketsPerProducer; j++) {
                        try {
                            Ticket ticket = pool.createTicket(
                                    "Late Event " + j,
                                    "Producer-" + producerId,
                                    "Location-" + j,
                                    100.0 + j);
                            pool.addTicket(ticket);
                            addedTickets.incrementAndGet();
                            Thread.sleep(50); // Longer delay to create scarcity
                        } catch (Exception e) {
                            System.err.println("Empty pool producer error: " + e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads
        long startTime = System.currentTimeMillis();
        startLatch.countDown();

        // Wait for completion (with timeout)
        boolean completed = completionLatch.await(180, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        executor.shutdownNow();

        // Print results
        System.out.println("Test " + (completed ? "completed" : "timed out") + " in " +
                (endTime - startTime) + "ms");
        System.out.println("Added tickets: " + addedTickets.get());
        System.out.println("Purchase attempts: " + purchaseAttempts.get());
        System.out.println("Successful purchases: " + successfulPurchases.get());
        System.out.println("Final pool state:");
        System.out.println("  Total tickets: " + pool.getCurrentSize());
        System.out.println("  Available tickets: " + pool.getAvailableTicketCount());
        System.out.println("  Sold tickets: " + pool.getSoldTicketCount());

        // Assert
        assertTrue(completed, "Test should complete within the timeout");
        assertTrue(addedTickets.get() > 0, "Should have added tickets");
        assertTrue(successfulPurchases.get() > 0, "Should have successful purchases");
        assertEquals(addedTickets.get() - successfulPurchases.get(),
                pool.getCurrentSize() - pool.getSoldTicketCount(),
                "Added tickets minus purchased should equal available tickets");
    }
}
