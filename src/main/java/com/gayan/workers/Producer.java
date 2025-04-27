package com.gayan.workers;

import com.gayan.entities.Ticket;
import com.gayan.entities.TicketPool;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Producer implements Runnable {

    private static final ConcurrentHashMap<String, AtomicInteger> vendorTicketCount = new ConcurrentHashMap<>();

    private int ticketCounter = 0;

    private final TicketPool ticketPool;
    private final int creationRateAtMillis; //configuration
    private final int maxNoOfTickets; //configuration

    private volatile boolean running = true; //to stop the producer dynamically

    //Ticket Information
    private final String eventName;
    private final String vendorName;
    private final String location;
    private final double price;

    public Producer(
            TicketPool ticketPool,
            int creationRateAtMillis,
            int maxNoOfTickets,
            String eventName,
            String vendorName,
            String location,
            double price) {
        this.ticketPool = ticketPool;
        this.creationRateAtMillis = creationRateAtMillis;
        this.maxNoOfTickets = maxNoOfTickets;
        this.eventName = eventName;
        this.vendorName = vendorName;
        this.location = location;
        this.price = price;
    }

    @Override
    public void run() {
        int retryCount = 0;
        final int maxRetries = 3;

        while(running && ticketCounter < maxNoOfTickets) {
            try{
                Ticket ticket = ticketPool.createTicket(eventName, vendorName, location, price);
                boolean result = ticketPool.addTicket(ticket);
                if(!result) {
                    retryCount++;
                    if(retryCount >= maxRetries) {
                        retryCount = 0;
                        running = false;
                    }else{
                        Thread.sleep(500);
                    }

                }

                ticketCounter++;
                vendorTicketCount.computeIfAbsent(vendorName, k -> new AtomicInteger(0)).incrementAndGet();

//                System.out.println(Thread.currentThread().getName() +
//                        " Added a ticket: " + ticket.getTicketId());

                Thread.sleep(creationRateAtMillis);
            }catch (InterruptedException e){
                System.out.println(Thread.currentThread().getName() + " interrupted.");
                Thread.currentThread().interrupt();
                break; // break the while loop
            }
        }
//        System.out.println(Thread.currentThread().getName() +
//                        " finished producing " + ticketCounter +
//                        " tickets for event: " + eventName +
//                        " by vendor: " + vendorName);
        printVendorTicketSummary();

    }

    //Print table view when we have multiple vendors
    public static void printVendorTicketSummary() {
        System.out.println("\n--- Vendor Ticket Production Summary ---");
        System.out.printf("%-20s | %-10s\n", "Vendor Name", "Tickets Produced");
        System.out.println("------------------------------------------");
        for (var entry : vendorTicketCount.entrySet()) {
            System.out.printf("%-20s | %-10d\n", entry.getKey(), entry.getValue().get());
        }
    }

    //Dynamically Stop the producer (Because the coursework asked to remove dynamically)
    public void stop() {
        running = false;
    }
}
