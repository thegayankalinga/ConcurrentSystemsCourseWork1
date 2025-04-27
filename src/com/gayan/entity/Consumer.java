package com.gayan.entity;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Consumer implements Runnable{
    private static final ConcurrentHashMap<String, AtomicInteger> consumerTicketCount = new ConcurrentHashMap<>();

    private int counter = 0;
    private final TicketPool ticketPool;
    private final int purchaseLimit; //configuration
    private final int purchaaseRateAtMillis; //Rate at which they can buy configuration
    private volatile boolean running = true; //Dynamically remove the consumer

    //This is created to mainly simulate the realworld allowing to do stress testing
    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    public Consumer(TicketPool ticketPool, int purchaaseRateAtMillis, int purchaseLimit) {
        this.ticketPool = ticketPool;
        this.purchaaseRateAtMillis = purchaaseRateAtMillis;
        this.purchaseLimit = purchaseLimit;
    }

    @Override
    public void run() {

        while(running && counter < purchaseLimit) {
            try{
                Optional<Ticket> optionalTicket = ticketPool.purchaseTicket();
                optionalTicket.ifPresent(ticket ->{



                    consumerTicketCount.computeIfAbsent(Thread.currentThread().getName(), k -> new AtomicInteger(0)).incrementAndGet();
                    counter++;
                    System.out.println(Thread.currentThread().getName() + " purchased ticket: " + ticket.getTicketId());
                });
                Thread.sleep(purchaaseRateAtMillis);
            }catch (InterruptedException e){
                System.out.println(Thread.currentThread().getName() + " was interrupted.");
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println(Thread.currentThread().getName() + " finished purchasing tickets.");
        printConsumerSummary();
    }

    private void handleTicket(Optional<Ticket> optionalTicket) {
        optionalTicket.ifPresent(ticket -> {
           try{
               System.out.println(Thread.currentThread().getName() +
                       " purchased ticket: " + ticket.getTicketId());

               // (Optional) Hold ticket for a random short period (simulate "using" it)
               Thread.sleep(ThreadLocalRandom.current().nextInt(500) + 200);

               boolean willCancel = random.nextInt(100) < 10;
               if(willCancel) {
                   //TODO: Implement the cancel ticket logic in the ticketpool interface.
                   ticketPool.cancelTicket(ticket);
                   System.out.println(Thread.currentThread().getName() +
                           " cancelled the ticket:" + ticket.getTicketId());
               }else{
                   System.out.println(Thread.currentThread().getName() +
                           " Kept the ticket: " + ticket.getTicketId());
               }

           }catch (InterruptedException e){
               System.out.println(Thread.currentThread().getName() +
                       " was interrupted during ticket handling.");
               Thread.currentThread().interrupt();
           }
        });
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
