package com.gayan.entity;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

//This is like a admin in a system for maintenance
public class Writer implements Runnable{

    private final TicketPool ticketPool;
    private int updateCounter = 0;
    private final int updateAtRateMillis;
    private final int maxUpdateAttempts;
    private final boolean isUpdateDataProvided;
    private final String writerName;
    private String newEventName = "";
    private double newPrice = 0.00;
    private String newLocation = "";

    private volatile boolean running = true;

    public Writer(TicketPool ticketPool, int updateAtRateMillis, String writerName, int maxUpdateAttempts) {
        this.ticketPool = ticketPool;
        this.updateAtRateMillis = updateAtRateMillis;
        this.writerName = writerName;
        this.maxUpdateAttempts = maxUpdateAttempts;
        isUpdateDataProvided = false;
    }

    public Writer(
            TicketPool ticketPool,
            int updateAtRateMillis,
            String writerName,
            String newEventName,
            double newPrice, String newLocation,
            int maxUpdateAttempts) {
        this.ticketPool = ticketPool;
        this.updateAtRateMillis = updateAtRateMillis;
        this.writerName = writerName;
        this.newEventName = newEventName;
        this.newPrice = newPrice;
        this.newLocation = newLocation;
        this.maxUpdateAttempts = maxUpdateAttempts;
        isUpdateDataProvided = true;
    }

    @Override
    public void run() {
        while(running && updateCounter < maxUpdateAttempts) {
            try{
                // Get a random available ticket (this will wait if none available)
                Optional<Ticket> optionalTicket = ticketPool.getRandomAvailableTicket();

                optionalTicket.ifPresent(ticket -> {
                    long ticketId = ticket.getTicketId();
                    if(!isUpdateDataProvided) {
                        newPrice = ThreadLocalRandom.current().nextDouble(50.0, 300.0);
                        newLocation = "Location-" + ThreadLocalRandom.current().nextInt(1, 10);
                        newEventName = "Event-" + ThreadLocalRandom.current().nextInt(1, 5);
                    }

                    // Update ticket using the pool's synchronized update method
                    ticketPool.updateTicket(ticketId, newPrice, newLocation, newEventName);
                    updateCounter++;
                });
                Thread.sleep(updateAtRateMillis);


            }catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + " was interrupted during ticket update.");
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println(Thread.currentThread().getName() + " stopped updating tickets.");
    }

    // Allow dynamic stop of Writer
    public void stop() {
        running = false;
    }
}
