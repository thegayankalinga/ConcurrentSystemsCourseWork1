package com.gayan.versions;

import com.gayan.entities.Ticket;
import com.gayan.entities.TicketPool;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockTicketPool implements TicketPool {
    private final Queue<Ticket> tickets;
    private final int capacity;
    private final AtomicLong ticketIdCounter;
    private final Lock lock;
    private final Condition notFull;
    private final Condition notEmpty;

    public ReentrantLockTicketPool(int capacity) {
        this.capacity = capacity;
        this.tickets = new LinkedList<>();
        this.ticketIdCounter = new AtomicLong(1);
        this.lock = new ReentrantLock();
        this.notFull = lock.newCondition();
        this.notEmpty = lock.newCondition();
    }

    @Override
    public void addTicket(Ticket ticket) {
        lock.lock();
        try {
            while (tickets.size() == capacity) {
                notFull.await();
            }
            tickets.offer(ticket);
            notEmpty.signalAll();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<Ticket> purchaseTicket() {
        lock.lock();
        try {
            while (true) {
                Optional<Ticket> optionalTicket = tickets.stream()
                        .filter(ticket -> !ticket.isSold())
                        .findFirst();
                if (optionalTicket.isPresent()) {
                    Ticket ticket = optionalTicket.get();
                    ticket.setSold(true);
                    notFull.signalAll();
                    return Optional.of(ticket);
                }
                notEmpty.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void cancelTicket(Ticket ticket) {
        lock.lock();
        try {
            if (ticket != null) {
                ticket.setSold(false);
                tickets.offer(ticket);
                notEmpty.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getCurrentSize() {
        lock.lock();
        try {
            return tickets.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public int getAvailableTicketCount() {
        lock.lock();
        try {
            return (int) tickets.stream().filter(ticket -> !ticket.isSold()).count();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getSoldTicketCount() {
        lock.lock();
        try {
            return (int) tickets.stream().filter(Ticket::isSold).count();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Ticket> getAllTickets() {
        lock.lock();
        try {
            return new ArrayList<>(tickets);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void printTicketPoolStatus() {
        lock.lock();
        try {
            int total = tickets.size();
            int available = getAvailableTicketCount();
            int sold = getSoldTicketCount();
            double percentageSold = total > 0 ? (double) sold / total * 100 : 0;

            System.out.println("========== TICKET POOL STATISTICS ==========");
            System.out.println("Total tickets: " + total + "/" + capacity);
            System.out.println("Available tickets: " + available);
            System.out.println("Sold tickets: " + sold);
            System.out.printf("Percentage sold: %.2f%%\n", percentageSold);
            System.out.println("============================================");
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Ticket createTicket(String eventName, String vendorName, String location, double price) {
        long id = ticketIdCounter.getAndIncrement();
        return new Ticket(id, eventName, vendorName, location, price);
    }

    @Override
    public List<Ticket> getAvailableTickets() {
        lock.lock();
        try {
            List<Ticket> available = new ArrayList<>();
            for (Ticket ticket : tickets) {
                if (!ticket.isSold()) {
                    available.add(ticket);
                }
            }
            return available;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<Ticket> getRandomAvailableTicket() {
        lock.lock();
        try {
            while (true) {
                List<Ticket> availableTickets = getAvailableTickets();
                if (!availableTickets.isEmpty()) {
                    int randomIndex = new Random().nextInt(availableTickets.size());
                    return Optional.of(availableTickets.get(randomIndex));
                }
                notEmpty.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void updateTicket(long ticketId, double newPrice, String newLocation, String newEventName) {
        lock.lock();
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
            lock.unlock();
        }
    }
}
