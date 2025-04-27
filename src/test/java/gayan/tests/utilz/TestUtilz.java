package gayan.tests.utilz;

import com.gayan.entities.Ticket;
import com.gayan.entities.TicketPool;
import com.gayan.versions.BlockingQueueTicketPool;
import com.gayan.versions.ReentrantLockTicketPool;
import com.gayan.versions.SynchronizedTicketPool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Utilities for testing the various implementations of TicketPool
 */
public class TestUtilz {

    /**
     * Creates a TicketPool of the specified type with the given capacity
     * @param type The type of TicketPool to create
     * @param capacity The capacity of the TicketPool
     * @return A new TicketPool instance
     */
    public static TicketPool createTicketPool(PoolType type, int capacity) {
        return switch (type) {
            case SYNCHRONIZED -> new SynchronizedTicketPool(capacity);
            case REENTRANT_LOCK -> new ReentrantLockTicketPool(capacity);
            case BLOCKING_QUEUE -> new BlockingQueueTicketPool(capacity);
        };
    }

    /**
     * Creates test tickets with sequential IDs.
     * @param pool The TicketPool to create tickets for
     * @param count The number of tickets to create
     * @return A list of created tickets
     */
    public static List<Ticket> createTestTickets(TicketPool pool, int count) {
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tickets.add(pool.createTicket(
                    "Test Event " + i,
                    "Test Vendor",
                    "Test Location",
                    100.0 + i
            ));
        }
        return tickets;
    }

    /**
     * Fills a ticket pool with test tickets
     * @param pool The pool to fill
     * @param count The number of tickets to add
     */
    public static void fillPool(TicketPool pool, int count) {
        List<Ticket> tickets = createTestTickets(pool, count);
        for (Ticket ticket : tickets) {
            pool.addTicket(ticket);
        }
    }

    /**
     * Runs tasks concurrently and waits for completion
     * @param taskCount Number of concurrent tasks to run
     * @param task The task to run
     */
    public static void runConcurrently(int taskCount, Runnable task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(taskCount);
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                try {
                    task.run();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();
    }

    /**
     * Measures execution time of a function
     * @param function Function to measure
     * @param input Input to the function
     * @return Execution time in milliseconds
     */
    public static <T, R> long measureExecutionTime(Function<T, R> function, T input) {
        long startTime = System.nanoTime();
        function.apply(input);
        long endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000; // Convert to milliseconds
    }

    /**
     * Enum representing the different TicketPool implementations
     */
    public enum PoolType {
        SYNCHRONIZED,
        REENTRANT_LOCK,
        BLOCKING_QUEUE
    }
}
