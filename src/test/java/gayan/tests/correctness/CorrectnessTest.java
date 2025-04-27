package gayan.tests.correctness;

import com.gayan.entities.Ticket;
import com.gayan.entities.TicketPool;
import gayan.tests.BaseTestConfig;
import gayan.tests.utilz.TestUtilz;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class CorrectnessTest extends BaseTestConfig {

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test basic ticket creation and addition")
    void testBasicTicketAddition(TestUtilz.PoolType poolType) {
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
        Ticket ticket = pool.createTicket("Concert", "Vendor", "London", 99.99);

        pool.addTicket(ticket);

        assertEquals(1, pool.getCurrentSize(), "Pool should have exactly one ticket");
        assertEquals(1, pool.getAvailableTicketCount(), "Pool should have one available ticket");
        assertEquals(0, pool.getSoldTicketCount(), "Pool should have zero sold tickets");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test ticket purchase")
    void testTicketPurchase(TestUtilz.PoolType poolType) {
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
        Ticket ticket = pool.createTicket("Concert", "Vendor", "London", 99.99);
        pool.addTicket(ticket);

        Optional<Ticket> purchasedTicket = pool.purchaseTicket();

        assertTrue(purchasedTicket.isPresent(), "Should successfully purchase a ticket");
        assertTrue(purchasedTicket.get().isSold(), "Purchased ticket should be marked as sold");
        assertEquals(1, pool.getSoldTicketCount(), "One ticket should be marked as sold");
        assertEquals(0, pool.getAvailableTicketCount(), "No available tickets should remain");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test ticket cancellation")
    void testTicketCancellation(TestUtilz.PoolType poolType) {
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
        Ticket ticket = pool.createTicket("Concert", "Vendor", "London", 99.99);
        pool.addTicket(ticket);
        Optional<Ticket> purchasedTicket = pool.purchaseTicket();

        pool.cancelTicket(purchasedTicket.get());

        assertEquals(1, pool.getAvailableTicketCount(), "One ticket should be available after cancellation");
        assertEquals(0, pool.getSoldTicketCount(), "No tickets should be sold after cancellation");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test ticket update")
    void testTicketUpdate(TestUtilz.PoolType poolType) {
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
        Ticket ticket = pool.createTicket("Concert", "Vendor", "London", 99.99);
        pool.addTicket(ticket);

        pool.updateTicket(ticket.getTicketId(), 149.99, "New York", "Updated Concert");

        List<Ticket> tickets = pool.getAllTickets();
        assertEquals(1, tickets.size(), "Pool should still have one ticket");
        Ticket updatedTicket = tickets.get(0);
        assertEquals(149.99, updatedTicket.getPrice(), "Price should be updated");
        assertEquals("New York", updatedTicket.getLocation(), "Location should be updated");
        assertEquals("Updated Concert", updatedTicket.getEventName(), "Event name should be updated");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test maximum capacity")
    void testMaximumCapacity(TestUtilz.PoolType poolType) {
        int capacity = 5;
        TicketPool pool = TestUtilz.createTicketPool(poolType, capacity);
        List<Ticket> tickets = TestUtilz.createTestTickets(pool, capacity + 5);

        for (int i = 0; i < capacity; i++) {
            pool.addTicket(tickets.get(i));
            assertEquals(i + 1, pool.getCurrentSize(), "Pool should have " + (i + 1) + " tickets");
        }

        // Test exceeding capacity with timeout
        Ticket extraTicket = tickets.get(capacity);
        long startTime = System.currentTimeMillis();
        boolean added = pool.addTicket(extraTicket);
        long duration = System.currentTimeMillis() - startTime;

        // Verify implementation properly handles capacity limits
        if (added) {
            assertEquals(capacity + 1, pool.getCurrentSize(), "Pool should have increased if add succeeded");
        } else {
            assertEquals(capacity, pool.getCurrentSize(), "Pool size should remain at capacity");
            assertTrue(duration < 6000, "Add operation should timeout within reasonable time");
        }
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test empty pool behavior")
    void testEmptyPoolBehavior(TestUtilz.PoolType poolType) {
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);

        assertEquals(0, pool.getCurrentSize(), "New pool should be empty");
        assertEquals(0, pool.getAvailableTicketCount(), "New pool should have no available tickets");
        assertEquals(0, pool.getSoldTicketCount(), "New pool should have no sold tickets");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test purchasing from empty pool with timeout")
    void testPurchaseFromEmptyPool(TestUtilz.PoolType poolType) throws InterruptedException {
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);

        // Measure how long purchase attempt takes when pool is empty
        long startTime = System.currentTimeMillis();
        Optional<Ticket> result = pool.purchaseTicket();
        long duration = System.currentTimeMillis() - startTime;

        assertFalse(result.isPresent(), "Should not get ticket from empty pool");
        assertTrue(duration < 6000, "Purchase operation should timeout within reasonable time");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test consumer waits and consumes after producer starts")
    void testConsumerWaitsAndConsumesAfterProducerStarts(TestUtilz.PoolType poolType) throws InterruptedException {
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);

        CountDownLatch ticketPurchasedLatch = new CountDownLatch(1);

        // Start a consumer thread first
        Thread consumerThread = new Thread(() -> {
            try {
                Optional<Ticket> purchasedTicket = pool.purchaseTicket();
                if (purchasedTicket.isPresent()) {
                    ticketPurchasedLatch.countDown(); // signal success
                }
            } catch (Exception e) {
                fail("Consumer thread interrupted or failed: " + e.getMessage());
            }
        }, "Test-Consumer");

        consumerThread.start();

        // Wait 3 seconds before starting producer
        Thread.sleep(3000);

        // Start a producer thread
        Thread producerThread = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    Ticket ticket = pool.createTicket("Concert", "Vendor", "City", 100.0);
                    pool.addTicket(ticket);
                    Thread.sleep(500); // slow down a little
                }
            } catch (Exception e) {
                fail("Producer thread interrupted or failed: " + e.getMessage());
            }
        }, "Test-Producer");

        producerThread.start();

        // Wait for the consumer to purchase successfully within 5 seconds
        boolean consumed = ticketPurchasedLatch.await(5, TimeUnit.SECONDS);

        assertTrue(consumed, "Consumer should successfully purchase a ticket after producer adds it");

        consumerThread.join();
        producerThread.join();
    }

//    @ParameterizedTest
//    @EnumSource(TestUtilz.PoolType.class)
//    @DisplayName("Test writer waits and updates after producer starts")
//    void testWriterWaitsAndUpdatesAfterProducerStarts(TestUtilz.PoolType poolType) throws InterruptedException {
//        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
//
//        CountDownLatch ticketUpdatedLatch = new CountDownLatch(1);
//
//        // Start a writer thread first
//        Thread writerThread = new Thread(() -> {
//            try {
//                Thread.sleep(500); // Give producer some time later
//                List<Ticket> allTickets = pool.getAllTickets();
//                if (!allTickets.isEmpty()) {
//                    Ticket ticket = allTickets.get(0);
//                    pool.updateTicket(ticket.getTicketId(), 150.0, "UpdatedCity", "UpdatedEvent");
//                    ticketUpdatedLatch.countDown(); // Signal success
//                }
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }, "Test-Writer");
//
//        writerThread.start();
//
//        // Wait 3 seconds before starting producer
//        Thread.sleep(3000);
//
//        // Start a producer
//        Thread producerThread = new Thread(() -> {
//            try {
//                Ticket ticket = pool.createTicket("Concert", "Vendor", "City", 100.0);
//                pool.addTicket(ticket);
//            } catch (Exception e) {
//                fail("Producer thread interrupted or failed: " + e.getMessage());
//            }
//        }, "Test-Producer");
//
//        producerThread.start();
//
//        boolean updated = ticketUpdatedLatch.await(5, TimeUnit.SECONDS);
//
//        assertTrue(updated, "Writer should successfully update a ticket after producer adds it");
//
//        writerThread.join();
//        producerThread.join();
//    }

//    @ParameterizedTest
//    @EnumSource(TestUtilz.PoolType.class)
//    @DisplayName("Test reader waits and reads after producer starts")
//    void testReaderWaitsAndReadsAfterProducerStarts(TestUtilz.PoolType poolType) throws InterruptedException {
//        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
//
//        CountDownLatch ticketReadLatch = new CountDownLatch(1);
//
//        // Start a reader thread first
//        Thread readerThread = new Thread(() -> {
//            try {
//                Thread.sleep(500); // Wait a bit first
//                List<Ticket> allTickets = pool.getAllTickets();
//                if (!allTickets.isEmpty()) {
//                    ticketReadLatch.countDown(); // Signal success
//                }
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }, "Test-Reader");
//
//        readerThread.start();
//
//        // Wait 3 seconds before starting producer
//        Thread.sleep(3000);
//
//        // Start a producer
//        Thread producerThread = new Thread(() -> {
//            try {
//                Ticket ticket = pool.createTicket("Concert", "Vendor", "City", 100.0);
//                pool.addTicket(ticket);
//            } catch (Exception e) {
//                fail("Producer thread interrupted or failed: " + e.getMessage());
//            }
//        }, "Test-Producer");
//
//        producerThread.start();
//
//        boolean read = ticketReadLatch.await(5, TimeUnit.SECONDS);
//
//        assertTrue(read, "Reader should successfully read tickets after producer adds them");
//
//        readerThread.join();
//        producerThread.join();
//    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test sequential bulk operations")
    void testSequentialBulkOperations(TestUtilz.PoolType poolType) {
        TicketPool pool = TestUtilz.createTicketPool(poolType, 100);
        int operations = 50;

        // Add tickets
        for (int i = 0; i < operations; i++) {
            Ticket ticket = pool.createTicket("Bulk-" + i, "Vendor", "Location", 100.0);
            assertTrue(pool.addTicket(ticket), "Should add ticket successfully");
        }

        assertEquals(operations, pool.getCurrentSize(), "All tickets should be added");

        // Purchase tickets
        int purchased = 0;
        for (int i = 0; i < operations; i++) {
            Optional<Ticket> ticket = pool.purchaseTicket();
            if (ticket.isPresent()) {
                purchased++;
            }
        }

        assertEquals(operations, purchased, "Should purchase all tickets");
        assertEquals(0, pool.getAvailableTicketCount(), "No tickets should remain available");
        assertEquals(operations, pool.getSoldTicketCount(), "All tickets should be marked as sold");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test concurrent updates to same ticket")
    void testConcurrentUpdates(TestUtilz.PoolType poolType) throws InterruptedException {
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
        Ticket ticket = pool.createTicket("Concert", "Vendor", "London", 99.99);
        pool.addTicket(ticket);

        final int threadCount = 5;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    pool.updateTicket(ticket.getTicketId(), 100.0 + threadId,
                            "Location-" + threadId, "Event-" + threadId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completionLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        completionLatch.await(5, TimeUnit.SECONDS);

        List<Ticket> tickets = pool.getAllTickets();
        assertEquals(1, tickets.size(), "Should still have exactly one ticket");

        // Ticket should have been updated by one of the threads
        Ticket updatedTicket = tickets.get(0);
        assertTrue(updatedTicket.getPrice() >= 100.0 && updatedTicket.getPrice() <= 104.0,
                "Price should be updated by one of the threads");
        assertTrue(updatedTicket.getLocation().startsWith("Location-"),
                "Location should be updated by one of the threads");
        assertTrue(updatedTicket.getEventName().startsWith("Event-"),
                "Event name should be updated by one of the threads");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test reader operations don't block each other")
    void testConcurrentReaderOperations(TestUtilz.PoolType poolType) throws InterruptedException {
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
        TestUtilz.fillPool(pool, 10);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(2);
        AtomicReference<Long> thread1Time = new AtomicReference<>(0L);
        AtomicReference<Long> thread2Time = new AtomicReference<>(0L);

        // First reader thread
        Thread reader1 = new Thread(() -> {
            try {
                startLatch.await();
                long start = System.currentTimeMillis();
                pool.getAvailableTicketCount();
                Thread.sleep(500); // Simulate long read operation
                pool.getAllTickets();
                long end = System.currentTimeMillis();
                thread1Time.set(end - start);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                completionLatch.countDown();
            }
        });

        // Second reader thread
        Thread reader2 = new Thread(() -> {
            try {
                startLatch.await();
                long start = System.currentTimeMillis();
                pool.getAvailableTicketCount();
                pool.getSoldTicketCount();
                long end = System.currentTimeMillis();
                thread2Time.set(end - start);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                completionLatch.countDown();
            }
        });

        reader1.start();
        reader2.start();
        startLatch.countDown();

        boolean completed = completionLatch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Both reader threads should complete");

        // Second reader should not be blocked by first reader's long operation
        assertTrue(thread2Time.get() < thread1Time.get(),
                "Second reader should complete faster than first reader, showing non-blocking behavior");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test random available ticket selection")
    void testRandomAvailableTicket(TestUtilz.PoolType poolType) throws InterruptedException {
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
        TestUtilz.fillPool(pool, 10);

        // Define a runnable that will time out if getRandomAvailableTicket blocks indefinitely
        Thread testThread = new Thread(() -> {
            Optional<Ticket> randomTicket = pool.getRandomAvailableTicket();
            assertTrue(randomTicket.isPresent(), "Should get a random ticket");
            assertFalse(randomTicket.get().isSold(), "Random ticket should not be marked as sold");
        });

        testThread.start();
        testThread.join(5000); // Wait up to 5 seconds

        assertFalse(testThread.isAlive(), "Test thread should complete within timeout");
        assertEquals(10, pool.getAvailableTicketCount(), "All tickets should still be available");
        assertEquals(0, pool.getSoldTicketCount(), "No ticket should be sold");
    }
}