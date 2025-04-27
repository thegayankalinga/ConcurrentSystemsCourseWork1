package gayan.tests.safety;

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
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadSafetyTest extends BaseTestConfig {

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test concurrent ticket purchases")
    void testConcurrentPurchases(TestUtilz.PoolType poolType) throws InterruptedException {
        // Arrange
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
        int ticketCount = 50;
        TestUtilz.fillPool(pool, ticketCount);

        AtomicInteger successfulPurchases = new AtomicInteger(0);
        int threadCount = 20;

        // Act
        TestUtilz.runConcurrently(threadCount, () -> {
            Optional<Ticket> purchasedTicket = pool.purchaseTicket();
            if (purchasedTicket.isPresent()) {
                successfulPurchases.incrementAndGet();
            }
        });

        // Assert
        assertEquals(threadCount, successfulPurchases.get(), "All purchase attempts should succeed");
        assertEquals(ticketCount - threadCount, pool.getAvailableTicketCount(), "Available tickets should decrease");
        assertEquals(threadCount, pool.getSoldTicketCount(), "Sold tickets should increase");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test concurrent ticket additions")
    void testConcurrentAdditions(TestUtilz.PoolType poolType) throws InterruptedException {
        // Arrange
        TicketPool pool = TestUtilz.createTicketPool(poolType, HIGH_CAPACITY);
        List<Ticket> tickets = TestUtilz.createTestTickets(pool, DEFAULT_THREAD_COUNT);

        // Act
        TestUtilz.runConcurrently(DEFAULT_THREAD_COUNT, () -> {
            int threadId = (int)(Thread.currentThread().getId() % DEFAULT_THREAD_COUNT);
            pool.addTicket(tickets.get(threadId));
        });

        // Assert
        assertEquals(DEFAULT_THREAD_COUNT, pool.getCurrentSize(), "All tickets should be added");
        assertEquals(DEFAULT_THREAD_COUNT, pool.getAvailableTicketCount(), "All tickets should be available");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test concurrent producers and consumers")
    void testProducerConsumerPattern(TestUtilz.PoolType poolType) throws InterruptedException {
        // Arrange
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
        AtomicInteger producedCount = new AtomicInteger(0);
        AtomicInteger consumedCount = new AtomicInteger(0);

        int producerCount = 5;
        int consumerCount = 10;
        int ticketsPerProducer = 10;

        CountDownLatch startLatch = new CountDownLatch(1);

        // Start producers
        for (int i = 0; i < producerCount; i++) {
            int producerId = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    for (int j = 0; j < ticketsPerProducer; j++) {
                        Ticket ticket = pool.createTicket(
                                "Event-" + producerId + "-" + j,
                                "Producer-" + producerId,
                                "Location-" + j,
                                100.0 + j
                        );
                        pool.addTicket(ticket);
                        producedCount.incrementAndGet();
                        Thread.sleep(10); // Small delay to increase chances of interleaving
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        // Start consumers
        for (int i = 0; i < consumerCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    while (consumedCount.get() < producerCount * ticketsPerProducer) {
                        Optional<Ticket> ticket = pool.purchaseTicket();
                        if (ticket.isPresent()) {
                            consumedCount.incrementAndGet();
                            Thread.sleep(5); // Small delay to increase chances of interleaving
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        // Start all threads at once
        startLatch.countDown();

        // Wait for completion (with timeout)
        long timeout = System.currentTimeMillis() + 10000; // 10 second timeout
        while (consumedCount.get() < producerCount * ticketsPerProducer && System.currentTimeMillis() < timeout) {
            Thread.sleep(100);
        }

        // Assert
        assertEquals(producerCount * ticketsPerProducer, producedCount.get(), "All tickets should be produced");
        assertEquals(producerCount * ticketsPerProducer, consumedCount.get(), "All tickets should be consumed");
        assertEquals(0, pool.getAvailableTicketCount(), "No tickets should remain available");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test readers and writers concurrency")
    void testReadersWritersConcurrency(TestUtilz.PoolType poolType) throws InterruptedException {
        // Arrange
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
        TestUtilz.fillPool(pool, 20);

        AtomicInteger readCount = new AtomicInteger(0);
        AtomicInteger writeCount = new AtomicInteger(0);

        int readerCount = 15;
        int writerCount = 5;
        int operationsPerThread = 10;

        CyclicBarrier startBarrier = new CyclicBarrier(readerCount + writerCount);

        List<Thread> threads = new ArrayList<>();

        // Create reader threads
        for (int i = 0; i < readerCount; i++) {
            Thread readerThread = new Thread(() -> {
                try {
                    startBarrier.await(); // Wait for all threads to be ready
                    for (int j = 0; j < operationsPerThread; j++) {
                        pool.getAvailableTicketCount();
                        pool.getSoldTicketCount();
                        pool.getAllTickets();
                        readCount.incrementAndGet();
                        Thread.sleep(5);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            threads.add(readerThread);
            readerThread.start();
        }

        // Create writer threads
        for (int i = 0; i < writerCount; i++) {
            Thread writerThread = new Thread(() -> {
                try {
                    startBarrier.await(); // Wait for all threads to be ready
                    for (int j = 0; j < operationsPerThread; j++) {
                        Optional<Ticket> ticket = pool.getRandomAvailableTicket();
                        if (ticket.isPresent()) {
                            long ticketId = ticket.get().getTicketId();
                            pool.updateTicket(ticketId, 200.0, "Updated Location", "Updated Event");
                            writeCount.incrementAndGet();
                        }
                        Thread.sleep(20);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            threads.add(writerThread);
            writerThread.start();
        }

        // Wait for all threads to complete (with timeout)
        for (Thread thread : threads) {
            thread.join(5000);
        }

        // Assert
        assertEquals(readerCount * operationsPerThread, readCount.get(), "All read operations should complete");
        assertTrue(writeCount.get() > 0, "Some write operations should complete");
        assertTrue(writeCount.get() <= writerCount * operationsPerThread, "Write count should not exceed maximum possible");
    }
}
