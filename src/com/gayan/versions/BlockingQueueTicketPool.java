package com.gayan.versions;

import com.gayan.entities.Ticket;
import com.gayan.entities.TicketPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BlockingQueueTicketPool implements TicketPool {
    private final BlockingQueue<Ticket> tickets;
    private final int capacity;
    private final AtomicLong ticketIdCounter;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public BlockingQueueTicketPool(int capacity) {
        this.capacity = capacity;
        this.tickets = new LinkedBlockingQueue<>(capacity);
        this.ticketIdCounter = new AtomicLong(1);
    }

    @Override
    public void addTicket(Ticket ticket) {
        try {
            tickets.put(ticket); // blocks if full
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted while adding ticket", e);
        }
    }

    @Override
    public Optional<Ticket> purchaseTicket() {
        while (true) {
            try {
                List<Ticket> availableTickets = getAvailableTickets();
                if (!availableTickets.isEmpty()) {
                    Ticket ticket = availableTickets.get(0);
                    lock.writeLock().lock();
                    try {
                        if (!ticket.isSold()) { // Double-check inside lock
                            ticket.setSold(true);
                            return Optional.of(ticket);
                        }
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
                Thread.sleep(50); // reduced wait
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
    }

    @Override
    public void cancelTicket(Ticket ticket) {
        if (ticket != null) {
            lock.writeLock().lock();
            try {
                // Only mark as unsold;
                ticket.setSold(false);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }


    @Override
    public int getCurrentSize() {
        return tickets.size();
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public int getAvailableTicketCount() {
        lock.readLock().lock();
        try {
            return (int) tickets.stream().filter(ticket -> !ticket.isSold()).count();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getSoldTicketCount() {
        lock.readLock().lock();
        try {
            return (int) tickets.stream().filter(Ticket::isSold).count();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Ticket> getAllTickets() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(tickets);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void printTicketPoolStatus() {
        int total = getCurrentSize();
        int available = getAvailableTicketCount();
        int sold = getSoldTicketCount();
        double percentageSold = total > 0 ? (double) sold / total * 100 : 0;

        System.out.println("========== TICKET POOL STATISTICS ==========");
        System.out.println("Total tickets: " + total + "/" + capacity);
        System.out.println("Available tickets: " + available);
        System.out.println("Sold tickets: " + sold);
        System.out.printf("Percentage sold: %.2f%%\n", percentageSold);
        System.out.println("============================================");
    }

    @Override
    public Ticket createTicket(String eventName, String vendorName, String location, double price) {
        long id = ticketIdCounter.getAndIncrement();
        return new Ticket(id, eventName, vendorName, location, price);
    }

    @Override
    public List<Ticket> getAvailableTickets() {
        lock.readLock().lock();
        try {
            List<Ticket> available = new ArrayList<>();
            for (Ticket ticket : tickets) {
                if (!ticket.isSold()) {
                    available.add(ticket);
                }
            }
            return available;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<Ticket> getRandomAvailableTicket() {
        while (true) {
            try {
                List<Ticket> availableTickets = getAvailableTickets();
                if (!availableTickets.isEmpty()) {
                    int randomIndex = ThreadLocalRandom.current().nextInt(availableTickets.size());
                    Ticket ticket = availableTickets.get(randomIndex);
                    lock.writeLock().lock();
                    try {
                        if (!ticket.isSold()) { // Double-check inside lock
                            ticket.setSold(true);
                            return Optional.of(ticket);
                        }
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
                Thread.sleep(50); // reduced wait
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
    }

    @Override
    public void updateTicket(long ticketId, double newPrice, String newLocation, String newEventName) {
        lock.writeLock().lock();
        try {
            for (Ticket ticket : tickets) {
                if (ticket.getTicketId() == ticketId) {
                    ticket.setPrice(newPrice);
                    ticket.setLocation(newLocation);
                    ticket.setEventName(newEventName);
                    return;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
