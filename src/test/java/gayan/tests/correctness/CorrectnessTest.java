package gayan.tests.correctness;

import com.gayan.entities.Ticket;
import com.gayan.entities.TicketPool;
import gayan.tests.BaseTestConfig;
import gayan.tests.utilz.TestUtilz;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CorrectnessTest extends BaseTestConfig {

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test basic ticket creation and addition")
    void testBasicTicketAddition(TestUtilz.PoolType poolType) {
        // Arrange
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
        Ticket ticket = pool.createTicket("Concert", "Vendor", "London", 99.99);

        // Act
        pool.addTicket(ticket);

        // Assert
        assertEquals(1, pool.getCurrentSize(), "Pool should have exactly one ticket");
        assertEquals(1, pool.getAvailableTicketCount(), "Pool should have one available ticket");
        assertEquals(0, pool.getSoldTicketCount(), "Pool should have zero sold tickets");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test ticket purchase")
    void testTicketPurchase(TestUtilz.PoolType poolType) {
        // Arrange
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
        Ticket ticket = pool.createTicket("Concert", "Vendor", "London", 99.99);
        pool.addTicket(ticket);

        // Act
        Optional<Ticket> purchasedTicket = pool.purchaseTicket();

        // Assert
        assertTrue(purchasedTicket.isPresent(), "Should successfully purchase a ticket");
        assertTrue(purchasedTicket.get().isSold(), "Purchased ticket should be marked as sold");
        assertEquals(1, pool.getSoldTicketCount(), "Pool size should remain the same");
        assertEquals(0, pool.getAvailableTicketCount(), "No available tickets should remain");
        assertEquals(1, pool.getSoldTicketCount(), "One ticket should be marked as sold");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test ticket cancellation")
    void testTicketCancellation(TestUtilz.PoolType poolType) {
        // Arrange
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
        Ticket ticket = pool.createTicket("Concert", "Vendor", "London", 99.99);
        pool.addTicket(ticket);
        Optional<Ticket> purchasedTicket = pool.purchaseTicket();

        // Act
        pool.cancelTicket(purchasedTicket.get());

        // Assert
        assertEquals(1, pool.getAvailableTicketCount(), "One ticket should be available after cancellation");
        assertEquals(0, pool.getSoldTicketCount(), "No tickets should be sold after cancellation");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test ticket update")
    void testTicketUpdate(TestUtilz.PoolType poolType) {
        // Arrange
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
        Ticket ticket = pool.createTicket("Concert", "Vendor", "London", 99.99);
        pool.addTicket(ticket);

        // Act
        pool.updateTicket(ticket.getTicketId(), 149.99, "New York", "Updated Concert");

        // Assert
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
        // Arrange
        int capacity = 5;
        TicketPool pool = TestUtilz.createTicketPool(poolType, capacity);
        List<Ticket> tickets = TestUtilz.createTestTickets(pool, capacity + 5); // Create more tickets than capacity

        // Act & Assert
        for (int i = 0; i < capacity; i++) {
            pool.addTicket(tickets.get(i));
            assertEquals(i + 1, pool.getCurrentSize(), "Pool should have " + (i + 1) + " tickets");
        }

        // This would exceed capacity, implementation should handle this
        // BlockingQueue will block, synchronized and reentrant will wait
        // We'll test this behavior more thoroughly in thread safety tests
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test empty pool behavior")
    void testEmptyPoolBehavior(TestUtilz.PoolType poolType) {
        // Arrange
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);

        // Assert
        assertEquals(0, pool.getCurrentSize(), "New pool should be empty");
        assertEquals(0, pool.getAvailableTicketCount(), "New pool should have no available tickets");
        assertEquals(0, pool.getSoldTicketCount(), "New pool should have no sold tickets");
    }

    @ParameterizedTest
    @EnumSource(TestUtilz.PoolType.class)
    @DisplayName("Test random available ticket selection")
    void testRandomAvailableTicket(TestUtilz.PoolType poolType) throws InterruptedException {
        // Arrange
        TicketPool pool = TestUtilz.createTicketPool(poolType, DEFAULT_CAPACITY);
        TestUtilz.fillPool(pool, 10);

        // Define a runnable that will time out if getRandomAvailableTicket blocks indefinitely
        Thread testThread = new Thread(() -> {
            Optional<Ticket> randomTicket = pool.getRandomAvailableTicket();
            assertTrue(randomTicket.isPresent(), "Should get a random ticket");
            assertFalse(randomTicket.get().isSold(), "Random ticket should not be marked as sold");
        });

        // Act
        testThread.start();
        testThread.join(5000); // Wait up to 5 seconds

        // Assert
        assertFalse(testThread.isAlive(), "Test thread should complete within timeout");
        assertEquals(10, pool.getAvailableTicketCount(), "All ticket should still be available");
        assertEquals(0, pool.getSoldTicketCount(), "No ticket should be sold");
    }
}
