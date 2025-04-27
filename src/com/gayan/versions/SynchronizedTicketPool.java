package com.gayan.versions;

import com.gayan.entity.Ticket;
import com.gayan.entity.TicketPool;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class SynchronizedTicketPool implements TicketPool {

    //Shared Resource
    private final Queue<Ticket> tickets;
    private final int capacity;
    private final AtomicLong ticketIdCounter;


    public SynchronizedTicketPool(int capacity) {
        this.capacity = capacity;
        tickets = new LinkedList<>();
        this.ticketIdCounter = new AtomicLong(1);
    }

    //Add Ticket by Writers & Producers
    @Override
    public synchronized void addTicket(Ticket ticket) {
        while(tickets.size() == capacity) {
            try{
                wait();
            }catch(InterruptedException e){
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        tickets.offer(ticket);
        notifyAll();
    }

    //Purchase Tickets by Consumers & Writers
    @Override
    public synchronized Optional<Ticket> purchaseTicket(){
        while(tickets.isEmpty()) {
            try{
                wait();
            }catch(InterruptedException e){
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        //Mark as sold
        Ticket ticket = tickets.poll();
        if(ticket != null) {
            ticket.setSold(true);
        }
        notifyAll();
        return Optional.ofNullable(ticket);
    }

    @Override
    public synchronized void cancelTicket(Ticket ticket){
        if(ticket != null){
            ticket.setSold(false);
            tickets.offer(ticket);
            notifyAll();
        }
    }

    @Override
    public synchronized int getCurrentSize() {
        return this.tickets.size();
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public synchronized int getAvailableTicketCount() {
        return (int) tickets.stream()
                .filter(ticket -> !ticket.isSold())
                .count();
    }

    @Override
    public synchronized int getSoldTicketCount() {
        return (int) tickets.stream()
                .filter(Ticket::isSold)
                .count();
    }

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

    public synchronized Ticket createTicket(
            String eventName,
            String vendorName,
            String location,
            double price) {
        long id = ticketIdCounter.getAndIncrement();
        return new Ticket(id, eventName, vendorName, location, price);
    }

    //Helper Method
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
