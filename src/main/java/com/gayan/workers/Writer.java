package com.gayan.workers;

import com.gayan.entities.Ticket;
import com.gayan.entities.TicketPool;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

//This is like an admin in a system for maintenance
public class Writer implements Runnable{

    private final TicketPool ticketPool;
    private int updateCounter = 0;
    private final int updateAtRateMillis;
    private final int maxUpdateAttempts;
    private final boolean isUpdateDataProvided;
    private String newEventName = "";
    private double newPrice = 0.00;
    private String newLocation = "";

    private volatile boolean running = true;

    public Writer(TicketPool ticketPool, int updateAtRateMillis, int maxUpdateAttempts) {
        this.ticketPool = ticketPool;
        this.updateAtRateMillis = updateAtRateMillis;
        this.maxUpdateAttempts = maxUpdateAttempts;
        isUpdateDataProvided = false;
    }

    public Writer(
            TicketPool ticketPool,
            int updateAtRateMillis,
            String newEventName,
            double newPrice, String newLocation,
            int maxUpdateAttempts) {
        this.ticketPool = ticketPool;
        this.updateAtRateMillis = updateAtRateMillis;
        this.newEventName = newEventName;
        this.newPrice = newPrice;
        this.newLocation = newLocation;
        this.maxUpdateAttempts = maxUpdateAttempts;
        isUpdateDataProvided = true;
    }

    @Override
    public void run() {
        while (running && updateCounter < maxUpdateAttempts) {
            try {
                Optional<Ticket> optionalTicket;
                synchronized (ticketPool) {
                    while ((optionalTicket = ticketPool.getRandomAvailableTicket()).isEmpty()) {
                        ticketPool.wait(); // 👈 Wait until Producer notifies
                    }
                }

                // Now you have a ticket to update
                Ticket ticket = optionalTicket.get();
                if (!isUpdateDataProvided) {
                    newPrice = ThreadLocalRandom.current().nextDouble(50.0, 300.0);
                    newLocation = "Location-" + ThreadLocalRandom.current().nextInt(1, 10);
                    newEventName = "Event-" + ThreadLocalRandom.current().nextInt(1, 5);
                }

                ticketPool.updateTicket(ticket.getTicketId(), newPrice, newLocation, newEventName);
                updateCounter++;

                Thread.sleep(updateAtRateMillis); // Delay only after successful update

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

//    @Override
//    public void run() {
//        while(running && updateCounter < maxUpdateAttempts) {
//            try{
//                // Get a random available ticket (this will wait if none available)
//                Optional<Ticket> optionalTicket = ticketPool.getRandomAvailableTicket();
//
////                optionalTicket.ifPresent(ticket -> {
////                    long ticketId = ticket.getTicketId();
////                    if(!isUpdateDataProvided) {
////                        newPrice = ThreadLocalRandom.current().nextDouble(50.0, 300.0);
////                        newLocation = "Location-" + ThreadLocalRandom.current().nextInt(1, 10);
////                        newEventName = "Event-" + ThreadLocalRandom.current().nextInt(1, 5);
////                    }
////
////                    // Update ticket using the pool's synchronized update method
////                    ticketPool.updateTicket(ticketId, newPrice, newLocation, newEventName);
////                    updateCounter++;
////                });
////                Thread.sleep(updateAtRateMillis);
//
//                if (optionalTicket.isPresent()) {
//                    Ticket ticket = optionalTicket.get();
//
//                    if (!isUpdateDataProvided) {
//                        newPrice = ThreadLocalRandom.current().nextDouble(50.0, 300.0);
//                        newLocation = "Location-" + ThreadLocalRandom.current().nextInt(1, 10);
//                        newEventName = "Event-" + ThreadLocalRandom.current().nextInt(1, 5);
//                    }
//
//                    ticketPool.updateTicket(ticket.getTicketId(), newPrice, newLocation, newEventName);
//                    updateCounter++;
//
//                    // Sleep only AFTER a successful update
//                    Thread.sleep(updateAtRateMillis);
//                } else {
//                    // No tickets available, retry after short delay
//                    Thread.sleep(200); // Very short wait to retry
//                }
//
//
//            }catch (InterruptedException e) {
//                System.out.println(Thread.currentThread().getName() + " was interrupted during ticket update.");
//                Thread.currentThread().interrupt();
//                break;
//            }
//        }
//        System.out.println(Thread.currentThread().getName() + " stopped updating tickets.");
//    }

    // Allow dynamic stop of Writer
    public void stop() {
        running = false;
    }
}
