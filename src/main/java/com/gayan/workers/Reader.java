package com.gayan.workers;

import com.gayan.entities.TicketPool;

public class Reader implements Runnable{

    private int counter = 0;
    private final TicketPool ticketPool;
    private final int readRateAtMillis;
    private volatile boolean running = true;
    private final int maxReadAttempts;

    public Reader(TicketPool ticketPool, int readRateAtMillis, int maxReadAttempts) {
        this.ticketPool = ticketPool;
        this.readRateAtMillis = readRateAtMillis;
        this.maxReadAttempts = maxReadAttempts;
    }

    @Override
    public void run() {
        while (running && counter < maxReadAttempts) {
            try {
                synchronized (ticketPool) {
                    // If pool is empty, wait until producer adds and notifies
                    while (ticketPool.getCurrentSize() == 0) {
                        ticketPool.wait(); // 💤 wait until Producer notifies
                    }
                }

                counter++;

                Thread.sleep(readRateAtMillis);

            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + " was interrupted during reading.");
                Thread.currentThread().interrupt();
                break;
            }
        }
        ticketPool.printTicketPoolStatus();
    }

    // Allow dynamic stop
    public void stop() {
        running = false;
    }
}
