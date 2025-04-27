package com.gayan.versions;

import com.gayan.entity.Ticket;
import com.gayan.entity.TicketPool;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class SynchronizedTicketPool implements TicketPool {

    //Shared Resource
    private final Queue<Ticket> tickets;
    private Queue<Ticket> availableTickets;
    private final int capacity;
    private final AtomicLong ticketIdCounter;


    //Constructor
    public SynchronizedTicketPool(int capacity) {
        this.capacity = capacity;
        tickets = new LinkedList<>();
        availableTickets = new LinkedList<>();
        this.ticketIdCounter = new AtomicLong(1);
    }

    //Add Ticket by Writers & Producers
    @Override
    public synchronized void addTicket(Ticket ticket) {
        while(tickets.size() == capacity) {
            try{
                wait();
            }catch(InterruptedException e){
                System.out.println(e.getMessage());
                throw new RuntimeException(e);
            }
        }
        tickets.offer(ticket);
        refreshAvailableTicket();

        notifyAll();
    }

    //Update Available Tickets List after purchase and Add (optional)
    private void refreshAvailableTicket(){
        availableTickets = tickets.stream()
                .filter(ticket -> !ticket.isSold())
                .collect(LinkedList::new, LinkedList::add, LinkedList::addAll);
    }

    //Get all the available tickets if required
    public List<Ticket> getAvailableTickets() {
        return new ArrayList<>(tickets.stream()
                .filter(ticket -> !ticket.isSold())
                .collect(LinkedList::new, LinkedList::add, LinkedList::addAll));
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
                wait();
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + " was interrupted while waiting for available tickets.");
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
    }

    //Purchase Tickets by Consumers & Writers
    @Override
    public synchronized Optional<Ticket> purchaseTicket(){

        while (true) {
            // Try to find an available (unsold) ticket
            Optional<Ticket> optionalTicket = tickets.stream()
                    .filter(ticket -> !ticket.isSold())
                    .findFirst();

            if (optionalTicket.isPresent()) {
                Ticket ticket = optionalTicket.get();
                ticket.setSold(true);
                notifyAll();
                return Optional.of(ticket);
            }

            // No available ticket found, wait
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + " was interrupted during purchase.");
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }

//        refreshAvailableTicket();
//        while(availableTickets.isEmpty()) {
//            try{
//                wait();
//            }catch(InterruptedException e){
//                e.printStackTrace();
//                return Optional.empty();
//            }
//        }
//        //Mark as sold
//
//        Optional<Ticket> optionalTicket = tickets.stream()
//                .filter(ticket -> !ticket.isSold())
//                .findFirst();
//
//        if (optionalTicket.isPresent()) {
//            Ticket ticket = optionalTicket.get();
//            ticket.setSold(true);
//            notifyAll();
//            return Optional.of(ticket);
//        }
//
////        Ticket ticket = tickets.poll();
////        if(ticket != null) {
////            ticket.setSold(true);
////        }
////        notifyAll();
////
//        return optionalTicket;
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
    public synchronized void cancelTicket(Ticket ticket){
        if(ticket != null){
            ticket.setSold(false);
            tickets.offer(ticket);
            notifyAll();
        }
    }

    //Get the ticket pool queue current usage size.
    @Override
    public synchronized int getCurrentSize() {
        return this.tickets.size();
    }

    //Get the Ticket Pool Queue Capacity
    @Override
    public int getCapacity() {
        return capacity;
    }

    //Get all Available Tickets
    @Override
    public synchronized int getAvailableTicketCount() {
        return (int) tickets.stream()
                .filter(ticket -> !ticket.isSold())
                .count();
    }

    //Get Total Sold Tickets
    @Override
    public synchronized int getSoldTicketCount() {
        return (int) tickets.stream()
                .filter(Ticket::isSold)
                .count();
    }

    //Get All Tickets
    @Override
    public List<Ticket> getAllTickets() {
        return List.of();
    }

    //Print the Statistics
    @Override
    public synchronized void printTicketPoolStatus() {
        int total = tickets.size();
        int available = getAvailableTicketCount();
        int sold = getSoldTicketCount();
        double percentageSold = total > 0 ? (double) sold / total * 100 : 0;

        System.out.println("========== TICKET POOL STATISTICS ==========");
        System.out.println("Total tickets: " + total + "/" + capacity);
        System.out.println("Available tickets: " + available);
        System.out.println("Sold tickets: " + sold);
        System.out.printf("Percentage sold: %.2f%%\n", percentageSold);

        //Print Each Ticket Details if Required
//        if (total > 0) {
//            System.out.println("\nTicket details:");
//            System.out.println("-------------------------------------------------");
//            System.out.printf("%-10s %-20s %-15s %-20s %-10s %-10s\n",
//                    "ID", "Event", "Vendor", "Location", "Price", "Status");
//            System.out.println("-------------------------------------------------");
//
//            for (Ticket ticket : tickets) {
//                System.out.printf("%-10d %-20s %-15s %-20s $%-9.2f %s\n",
//                        ticket.getTicketId(),
//                        truncate(ticket.getEventName(), 20),
//                        truncate(ticket.getVendorName(), 15),
//                        truncate(ticket.getLocation(), 20),
//                        ticket.getPrice(),
//                        ticket.isSold() ? "SOLD" : "AVAILABLE");
//            }
//        }
        System.out.println("==============================================");
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
