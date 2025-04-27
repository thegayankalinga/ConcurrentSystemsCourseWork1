package com.gayan.entity;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Consumer implements Runnable{
    private static final ConcurrentHashMap<String, AtomicInteger> consumerTicketCount = new ConcurrentHashMap<>();

    private int counter = 0;

    private final TicketPool ticketPool;
    private final int purchaseLimit; //configuration
    private final int purchaseRateAtMillis; //Rate at which they can buy configuration

    //This is created to mainly simulate the real world allowing to do stress testing
    private static final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final boolean simulateCancel; //configuration

    private volatile boolean running = true; //Dynamically remove the consumer

    public Consumer(TicketPool ticketPool, int purchaseRateAtMillis, int purchaseLimit) {
        this.ticketPool = ticketPool;
        this.purchaseRateAtMillis = purchaseRateAtMillis;
        this.purchaseLimit = purchaseLimit;
        this.simulateCancel = false;
    }

    public Consumer(TicketPool ticketPool, int purchaseLimit, int purchaseRateAtMillis, boolean simulateCancel) {
        this.ticketPool = ticketPool;
        this.purchaseLimit = purchaseLimit;
        this.purchaseRateAtMillis = purchaseRateAtMillis;
        this.simulateCancel = simulateCancel;
    }

    @Override
    public void run() {

        while(running && counter < purchaseLimit) {
            try{
                Optional<Ticket> optionalTicket = ticketPool.purchaseTicket();

                optionalTicket.ifPresent(this::handleTicket);

                Thread.sleep(purchaseRateAtMillis);
            }catch (InterruptedException e){
                System.out.println(Thread.currentThread().getName() + " was interrupted.");
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println(Thread.currentThread().getName() + " finished purchasing tickets.");
        printConsumerSummary();
    }

    private void handleTicket(Ticket ticket) {

           try{
               consumerTicketCount.computeIfAbsent(Thread.currentThread().getName(), k -> new AtomicInteger(0)).incrementAndGet();
               counter++;
               System.out.println(Thread.currentThread().getName() +
                       " purchased ticket: " + ticket.getTicketId());

               //Simulate real world cancellation behavior
               if(simulateCancel) {
                   // (Optional) Hold ticket for a random short period (simulate "using" it)
                   Thread.sleep(ThreadLocalRandom.current().nextInt(500) + 200);

                   boolean willCancel = random.nextInt(100) < 10;
                   if (willCancel) {
                       ticketPool.cancelTicket(ticket);
                       counter--;
                       System.out.println(Thread.currentThread().getName() +
                               " cancelled the ticket:" + ticket.getTicketId());
                   } else {
                       counter++;
                       System.out.println(Thread.currentThread().getName() +
                               " Kept the ticket: " + ticket.getTicketId());
                   }
               }

           }catch (InterruptedException e){
               System.out.println(Thread.currentThread().getName() +
                       " was interrupted during ticket handling.");
               Thread.currentThread().interrupt();
           }

    }

    //Dynamically Stop the consumer (Because the coursework asked to remove dynamically)
    public void stop() {
        running = false;
    }


    public static void printConsumerSummary() {
        System.out.println("\n--- Consumer Ticket Purchase Summary ---");
        System.out.printf("%-20s | %-10s\n", "Consumer Name", "Tickets Purchased");
        System.out.println("------------------------------------------");
        for (var entry : consumerTicketCount.entrySet()) {
            System.out.printf("%-20s | %-10d\n", entry.getKey(), entry.getValue().get());
        }

    }
}
