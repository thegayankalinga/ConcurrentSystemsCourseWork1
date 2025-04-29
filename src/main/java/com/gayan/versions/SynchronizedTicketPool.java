package com.gayan.versions;

import com.gayan.entities.Ticket;
import com.gayan.entities.TicketPool;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SynchronizedTicketPool implements TicketPool {

    private final int TIME_OUT = 10000;
    //Shared Resource
    private final Queue<Ticket> tickets;
    //private Queue<Ticket> availableTickets;
    private Queue<Ticket> soldTickets;
    private final int capacity;
    private final AtomicLong ticketIdCounter;

    private volatile boolean poolClosed = false;

    //Constructor
    public SynchronizedTicketPool(int capacity) {
        this.capacity = capacity;
        tickets = new LinkedList<>();
        //availableTickets = new LinkedList<>();
        soldTickets = new LinkedList<>();
        this.ticketIdCounter = new AtomicLong(1);
    }

    @Override
    public boolean addTicket(Ticket ticket) {
        try {
            boolean success = tickets.offer(ticket);
            if (success) {
                synchronized (this) {
                    notifyAll(); // 👈 Wake up any waiting Readers
                }
            } else {
                System.out.println(Thread.currentThread().getName() + " could not add ticket - pool full after waiting.");
            }
            return success;
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " was interrupted while adding ticket.");
            return false;
        }
    }

    //Get all the available tickets if required
    public List<Ticket> getAvailableTickets() {
        return new ArrayList<>(tickets);

    }

    @Override
    public synchronized Optional<Ticket> getRandomAvailableTicket() {
        while (true) {
            List<Ticket> availableTickets = getAvailableTickets();

            if (!availableTickets.isEmpty()) {
                int randomIndex = ThreadLocalRandom.current().nextInt(availableTickets.size());
                return Optional.of(availableTickets.get(randomIndex));
            }

            try {
                wait(TIME_OUT);
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + " was interrupted while waiting for available tickets.");
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
    }

    @Override
    public synchronized Optional<Ticket> purchaseTicket() {
        long startTime = System.currentTimeMillis();
        long totalTimeout = TIME_OUT;

        while (System.currentTimeMillis() - startTime < totalTimeout) {
            Ticket ticket = tickets.poll();
            if(ticket != null) {
                ticket.setSold(true);
                this.soldTickets.offer(ticket);
                notifyAll();
                return Optional.of(ticket);
            }

            // Calculate remaining timeout
            long elapsed = System.currentTimeMillis() - startTime;
            long remainingTime = totalTimeout - elapsed;

            if (remainingTime <= 0) {
                return Optional.empty();
            }

            // No ticket found, wait
            try {
                wait(remainingTime);
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + " was interrupted during purchase.");
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public synchronized void updateTicket(
            long ticketId,
            double newPrice,
            String newLocation,
            String newEventName
    ){

        for (Ticket ticket : tickets) {
            if(ticket.getTicketId() == ticketId) {
                ticket.setPrice(newPrice);
                ticket.setLocation(newLocation);
                ticket.setEventName(newEventName);
                System.out.println("Updated ticket: " + ticket.getTicketId() +
                        " | New Price: " + newPrice +
                        " | New Location: " + newLocation +
                        " | New Event: " + newEventName);
                notifyAll();
                return;
            }
        }
        System.out.println("Failed to update ticket Ticket ID: " + ticketId + " not found");
    }

    //Cancel Ticket by Consumer
    @Override
    public synchronized void cancelTicket(Ticket ticket) {
        if (ticket != null) {
            ticket.setSold(false);

            // Remove from sold tickets queue
            this.soldTickets.remove(ticket);

            // Add back to unsold tickets queue
            tickets.offer(ticket);

            notifyAll();
        }
    }

    //Get the ticket pool queue current usage size.
    @Override
    public synchronized int getCurrentSize() {
        return this.tickets.size() ;
    }

    //Get the Ticket Pool Queue Capacity
    @Override
    public int getCapacity() {
        return capacity;
    }

    //Get all Available Tickets
    @Override
    public synchronized int getAvailableTicketCount() {
        return tickets.size();
    }

    //Get Total Sold Tickets
    @Override
    public synchronized int getSoldTicketCount() {
        return this.soldTickets.size();
    }

    //close pool
    public synchronized void closePool() {
        poolClosed = true;
        notifyAll(); // wake up waiting consumers
    }

    //Get All Tickets
    @Override
    public List<Ticket> getAllTickets() {
        List<Ticket> allTickets = new ArrayList<>(tickets);         // unsold tickets
        allTickets.addAll(soldTickets);                        // add sold tickets
        return allTickets;
    }

    public int getAllTicketsCount() {
        return (tickets.size() + soldTickets.size());
    }

    //Print the Statistics
    @Override
    public synchronized void printTicketPoolStatus() {
        int total = getAllTicketsCount();
        int available = getAvailableTicketCount();
        int sold = getSoldTicketCount();
        double percentageSold = total > 0 ? (double) sold / total * 100 : 0;

        // ANSI color codes for terminal output
        final String RESET = "\u001B[0m";
        final String BOLD = "\u001B[1m";
        final String GREEN = "\u001B[32m";
        final String BLUE = "\u001B[34m";
        final String YELLOW = "\u001B[33m";
        final String RED = "\u001B[31m";

        System.out.println();
        System.out.println(BOLD + "╔══════════════ TICKET POOL STATISTICS ══════════════╗" + RESET);

        // Progress bar for capacity utilization
        int barLength = 30;
        int filledBlocks = total > 0 ? (int)((double)total/capacity * barLength) : 0;
        int soldBlocks = total > 0 ? (int)((double)sold/capacity * barLength) : 0;

        StringBuilder capacityBar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < soldBlocks) {
                capacityBar.append(RED + "█" + RESET); // Sold tickets
            } else if (i < filledBlocks) {
                capacityBar.append(GREEN + "█" + RESET); // Available tickets
            } else {
                capacityBar.append("░"); // Empty space
            }
        }
        capacityBar.append("] ");
        capacityBar.append(String.format("%d/%d", total, capacity));

        System.out.println("║ " + BOLD + "Capacity: " + RESET + capacityBar);
        System.out.println("║ " + BOLD + "Available: " + RESET + GREEN + available + RESET +
                " | " + BOLD + "Sold: " + RESET + RED + sold + RESET +
                " | " + BOLD + "Percentage sold: " + RESET +
                (percentageSold > 75 ? RED : percentageSold > 50 ? YELLOW : GREEN) +
                String.format("%.2f%%", percentageSold) + RESET);

        System.out.println(BOLD + "╚══════════════════════════════════════════════════════╝" + RESET);
        System.out.println();
    }

    //Create Ticket
    public synchronized Ticket createTicket(
            String eventName,
            String vendorName,
            String location,
            double price) {
        long id = ticketIdCounter.getAndIncrement();
        return new Ticket(id, eventName, vendorName, location, price);
    }

    //Helper Method for String Truncation
    private String truncate(String str, int length) {
        if (str.length() <= length) {
            return str;
        }
        return str.substring(0, length - 3) + "...";
    }

    @Override
    public String toString() {
        return "SynchronizedTicketPool{" +
                "tickets=" + tickets +
                ", capacity=" + capacity +
                ", ticketIdCounter=" + ticketIdCounter +
                '}';
    }
}
