package com.gayan.utilz;

import com.gayan.entities.TicketPool;
import com.gayan.versions.BlockingQueueTicketPool;
import com.gayan.versions.ReentrantLockTicketPool;
import com.gayan.versions.SynchronizedTicketPool;
import com.gayan.workers.Consumer;
import com.gayan.workers.Producer;
import com.gayan.workers.Reader;
import com.gayan.workers.Writer;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.gayan.utilz.TerminalColorConstant.WHITE_BACKGROUND;

public class SimulationManager {
    private TicketPool ticketPool;
    private int capacity;
    private int syncChoice;
    private String syncMethodName;

    private final List<Thread> producerThreads = new ArrayList<>();
    private final List<Producer> producers = new ArrayList<>();

    private final List<Thread> consumerThreads = new ArrayList<>();
    private final List<Consumer> consumers = new ArrayList<>();

    private final List<Thread> writerThreads = new ArrayList<>();
    private final List<Writer> writers = new ArrayList<>();

    private final List<Thread> readerThreads = new ArrayList<>();
    private final List<Reader> readers = new ArrayList<>();



    public void startSimulation(){

        Scanner scanner = new Scanner(System.in);

        // Get Ticket Pool Capacity
        this.capacity = getValidatedIntegerInput(scanner,
                "Enter the Ticket Pool Capacity (1-1000): ",
                1, 1000, TerminalColorConstant.YELLOW_BOLD);

        // Choose Synchronization Method
        printSyncMethodMenu();
        int syncChoice = getValidatedIntegerInput(scanner,
                "Enter your choice (1-3): ",
                1, 3, TerminalColorConstant.WHITE_BOLD);

        System.out.println("===============================================");
        //Sync Method based switching to instantiate the Ticket Pool
        switch (syncChoice) {
            case 1 -> {
                ticketPool = new SynchronizedTicketPool(capacity);
                syncMethodName = "Synchronized Version";
                System.out.println(TerminalColorConstant.GREEN + "Using " + syncMethodName + " for this simulation." + TerminalColorConstant.RESET);
            }
            case 2 -> {
                ticketPool = new ReentrantLockTicketPool(capacity);
                syncMethodName = "Reentrant Lock Version";
                System.out.println(TerminalColorConstant.GREEN + "Using " + syncMethodName + " for this simulation." + TerminalColorConstant.RESET);
            }
            case 3 -> {
                ticketPool = new BlockingQueueTicketPool(capacity);
                syncMethodName = "Blocking Queue Version";
                System.out.println(TerminalColorConstant.GREEN + "Using " + syncMethodName + " for this simulation." + TerminalColorConstant.RESET);
            }
            default -> {
                System.out.println("\u001B[31mInvalid choice. Exiting...\u001B[0m");
                System.exit(1);
            }
        }
        startDeadlockMonitor();
        boolean running = true;
        while(running){
            printMenu();
            int menuChoice = getValidatedIntegerInput(scanner,
                    "Enter your choice (0-14): ",
                    0, 14, TerminalColorConstant.WHITE_BOLD);

            switch (menuChoice) {
                case 1 -> {
                    addProducer();
                    printHeader("Add Producer");
                }
                case 2 -> {
                    addConsumer();
                    printHeader("Add Consumer");
                }
                case 3 -> {
                    addWriter();
                    printHeader("Add Writer");
                }
                case 4 -> {
                    addReader();
                    printHeader("Add Reader");
                }

                case 5 -> {
                    // Ask for number of tickets
                    String msg = MessageFormat.format(
                            TerminalColorConstant.CYAN + "\nEnter the number of tickets to produce (1-{0}): " + TerminalColorConstant.RESET,
                            capacity);
                    int maxTickets = getValidatedIntegerInput(scanner, msg, 1, capacity, TerminalColorConstant.WHITE_BOLD);

                    // Ask for creation rate
                    System.out.println(TerminalColorConstant.CYAN + "\nChoose ticket creation speed:" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "1. Very Fast (1 second)" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "2. Fast (2 seconds)" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "3. Moderate (3 seconds)" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "4. Slow (4 seconds)" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "5. Very Slow (5 seconds)" + TerminalColorConstant.RESET);

                    int creationRateChoice = getValidatedIntegerInput(scanner,
                            "Select creation speed (1-5): ",
                            1, 5, TerminalColorConstant.WHITE_BOLD);

                    int creationRateMillis = creationRateChoice * 1000; // 1 -> 1000ms, 2 -> 2000ms, etc.

                    // Create and add producer
                    addProducer(creationRateMillis, maxTickets);
                    printHeader("Add Custom Producer");
                }

                case 6 -> {
                    // Ask for number of tickets
                    String msg = MessageFormat.format(
                            TerminalColorConstant.CYAN + "\nEnter max no of tickets to purchase (1-{0}): " + TerminalColorConstant.RESET,
                            capacity);
                    int maxTickets = getValidatedIntegerInput(scanner, msg, 1, capacity, TerminalColorConstant.WHITE_BOLD);

                    // Ask for creation rate
                    System.out.println(TerminalColorConstant.CYAN + "\nChoose ticket purchasing speed:" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "1. Very Fast (1 second)" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "2. Fast (2 seconds)" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "3. Moderate (3 seconds)" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "4. Slow (4 seconds)" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "5. Very Slow (5 seconds)" + TerminalColorConstant.RESET);

                    int creationRateChoice = getValidatedIntegerInput(scanner,
                            "\nSelect purchasing speed (1-5): ",
                            1, 5, TerminalColorConstant.WHITE_BOLD);

                    System.out.println(TerminalColorConstant.CYAN + "\nChoose Allow Cancel Ticket after purchasing ?:" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "1. Yes" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "2. No" + TerminalColorConstant.RESET);

                    int allowCancel = getValidatedIntegerInput(scanner,
                            "\nSelect Allow or not ? (1-2): ",
                            1, 2, TerminalColorConstant.WHITE_BOLD);

                    int creationRateMillis = creationRateChoice * 1000; // 1 -> 1000ms, 2 -> 2000ms, etc.
                    boolean canceled = allowCancel == 1;
                    // Create and add Consumer
                    addConsumer(creationRateMillis, maxTickets, canceled);
                    printHeader("Custom Consumer");
                }

                case 7 -> {
                    // Ask for number of tickets
                    String msg = MessageFormat.format(
                            TerminalColorConstant.CYAN + "\nEnter the number of tickets to update (1-{0}): " + TerminalColorConstant.RESET,
                            capacity);
                    int maxTickets = getValidatedIntegerInput(scanner, msg, 1, capacity, TerminalColorConstant.WHITE_BOLD);

                    // Ask for creation rate
                    System.out.println(TerminalColorConstant.CYAN + "\nChoose ticket update speed:" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "1. Very Fast (1 second)" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "2. Fast (2 seconds)" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "3. Moderate (3 seconds)" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "4. Slow (4 seconds)" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "5. Very Slow (5 seconds)" + TerminalColorConstant.RESET);

                    int creationRateChoice = getValidatedIntegerInput(scanner,
                            "Select update speed (1-5): ",
                            1, 5, TerminalColorConstant.WHITE_BOLD);

                    int creationRateMillis = creationRateChoice * 1000; // 1 -> 1000ms, 2 -> 2000ms, etc.

                    // Create and add producer
                    addWriter(creationRateMillis, maxTickets);
                    printHeader("Custom Writer");
                }

                case 8 -> {
                    // Ask for number of tickets
                    String msg = MessageFormat.format(
                            TerminalColorConstant.CYAN + "\nEnter the number of tickets to read (1-{0}): " + TerminalColorConstant.RESET,
                            capacity);
                    int maxTickets = getValidatedIntegerInput(scanner, msg, 1, capacity, TerminalColorConstant.WHITE_BOLD);

                    // Ask for creation rate
                    System.out.println(TerminalColorConstant.CYAN + "\nChoose ticket reading speed:" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "1. Very Fast (1 second)" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "2. Fast (2 seconds)" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "3. Moderate (3 seconds)" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "4. Slow (4 seconds)" + TerminalColorConstant.RESET);
                    System.out.println(TerminalColorConstant.CYAN + "5. Very Slow (5 seconds)" + TerminalColorConstant.RESET);

                    int creationRateChoice = getValidatedIntegerInput(scanner,
                            "Select reading speed (1-5): ",
                            1, 5, TerminalColorConstant.WHITE_BOLD);

                    int creationRateMillis = creationRateChoice * 1000; // 1 -> 1000ms, 2 -> 2000ms, etc.

                    // Create and add producer
                    addReader(creationRateMillis, maxTickets);
                    printHeader("Custom Reader");
                }

                case 9 -> {
                    removeProducer();
                    printHeader("Remove Producer");
                }
                case 10 -> {
                    removeConsumer();
                    printHeader("Remove Consumer");
                }
                case 11 -> {
                    removeWriter();
                    printHeader("Remove Writer");
                }
                case 12 -> {
                    removeReader();
                    printHeader("Remove Reader");
                }
                case 13 -> {
                    ticketPool.printTicketPoolStatus();
                    printHeader("Show Ticket Pool Status");
                }
                case 14 -> {
                    printWorkersSummary();
                    printHeader("Show Workers & Threads Summary");
                }
                case 0 -> {
                    shutdown();
                    running = false;
                }
                default -> System.out.println(TerminalColorConstant.RED + "Invalid choice. Please try again." + TerminalColorConstant.RESET);
            }
        }
        scanner.close();

    }

    private int getValidatedIntegerInput(
            Scanner scanner,
            String promptMessage,
            int min,
            int max, String color) {
        int input = -1;
        boolean valid = false;

        while (!valid) {
            try {
                System.out.print(color + promptMessage + TerminalColorConstant.RESET);
                input = Integer.parseInt(scanner.nextLine());
                if (input >= min && input <= max) {
                    valid = true;
                } else {
                    System.out.println(TerminalColorConstant.RED + "Input must be between " + min + " and " + max + "." + TerminalColorConstant.RESET);
                }
            } catch (NumberFormatException e) {
                System.out.println(TerminalColorConstant.RED + "Invalid input. Please enter a number." + TerminalColorConstant.RESET);
            }
        }
        return input;
    }

    private static void printSyncMethodMenu() {
        System.out.println(TerminalColorConstant.YELLOW_BOLD + "Choose Synchronization Method:" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.CYAN + "1. Synchronized Version" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.CYAN + "2. ReentrantLock Version" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.CYAN + "3. BlockingQueue Version" + TerminalColorConstant.RESET);
        System.out.print(TerminalColorConstant.WHITE_BOLD + "Enter your choice (1-3): " + TerminalColorConstant.RESET);

    }

    private void printHeader(String lastAction) {
        System.out.println(TerminalColorConstant.YELLOW_BOLD + "================ Simulation Info ================" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.CYAN + "Max Pool Capacity: " + TerminalColorConstant.RESET + capacity);
        System.out.println(TerminalColorConstant.CYAN + "Synchronization Method: " + TerminalColorConstant.RESET + syncMethodName);
        System.out.println(TerminalColorConstant.CYAN + "Last Action: " + TerminalColorConstant.RESET + lastAction);
        System.out.println(TerminalColorConstant.YELLOW_BOLD + "=================================================" + TerminalColorConstant.RESET);
    }

    //Print the Option Menu
    private void printMenu() {
        // 1. Clear screen first (platform-dependent)
//        System.out.print("\033[H\033[2J");
//        System.out.flush();
        clearScreen();

        // 5. Add Continuous Status Display - compact status line
        System.out.println(TerminalColorConstant.WHITE_BOLD + "STATUS: " +
                "Producers: " + producers.size() + " | " +
                "Consumers: " + consumers.size() + " | " +
                "Writers: " + writers.size() + " | " +
                "Readers: " + readers.size() + " | " +
                "Tickets: " + ticketPool.getCurrentSize() + "/" + capacity +
                TerminalColorConstant.RESET);

        System.out.println(TerminalColorConstant.YELLOW_BOLD + "\n==== Ticket System Simulation Manager ====" + TerminalColorConstant.RESET);

        // 2. Implement Grouped Menu Categories
        // Add Operations Group
        System.out.println(TerminalColorConstant.GREEN_BOLD + "\n-- Add Default Operations --" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.PURPLE + "1. Add Producer" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.PURPLE + "2. Add Consumer" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.PURPLE + "3. Add Writer" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.PURPLE + "4. Add Reader" + TerminalColorConstant.RESET);

        // Add Operations Group
        System.out.println(TerminalColorConstant.GREEN_BOLD + "\n-- Add Custom Operations --" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.BLUE + "5. Add Custom Producer" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.BLUE + "6. Add Custom Consumer" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.BLUE + "7. Add Custom Writer" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.BLUE + "8. Add Custom Reader" + TerminalColorConstant.RESET);


        // Remove Operations Group
        System.out.println(TerminalColorConstant.RED_BOLD + "\n-- Remove Operations --" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.RED + "9. Remove Producer" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.RED + "10. Remove Consumer" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.RED + "11. Remove Writer" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.RED + "12. Remove Reader" + TerminalColorConstant.RESET);

        // Status Operations Group
        System.out.println(TerminalColorConstant.YELLOW_BOLD + "\n-- Status Operations --" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.YELLOW + "13. Show Ticket Pool Status" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.YELLOW + "14. Show Workers & Threads Summary" + TerminalColorConstant.RESET);

        // System Operations
        System.out.println(TerminalColorConstant.RED_BOLD + "\n-- System --" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.RED + "0. Shutdown" + TerminalColorConstant.RESET);

        System.out.println(TerminalColorConstant.WHITE_BOLD + "\nSynchronization Method: " + TerminalColorConstant.RESET + syncMethodName);
        System.out.print(TerminalColorConstant.WHITE_BOLD + "Enter your choice (0-10): " + TerminalColorConstant.RESET);
    }
//    private void printMenu() {
//        System.out.println(TerminalColorConstant.YELLOW_BOLD + "\n==== Simulation Manager ====" + TerminalColorConstant.RESET);
//        System.out.println(TerminalColorConstant.GREEN + "1. Add Producer" + TerminalColorConstant.RESET);
//        System.out.println(TerminalColorConstant.CYAN + "2. Add Consumer" + TerminalColorConstant.RESET);
//        System.out.println(TerminalColorConstant.PURPLE + "3. Add Writer" + TerminalColorConstant.RESET);
//        System.out.println(TerminalColorConstant.BLUE + "4. Add Reader" + TerminalColorConstant.RESET);
//        System.out.println(TerminalColorConstant.RED + "5. Remove Producer" + TerminalColorConstant.RESET);
//        System.out.println(TerminalColorConstant.RED + "6. Remove Consumer" + TerminalColorConstant.RESET);
//        System.out.println(TerminalColorConstant.RED + "7. Remove Writer" + TerminalColorConstant.RESET);
//        System.out.println(TerminalColorConstant.RED + "8. Remove Reader" + TerminalColorConstant.RESET);
//        System.out.println(TerminalColorConstant.YELLOW_BOLD + "9. Show Ticket Pool Status" + TerminalColorConstant.RESET);
//        System.out.println(TerminalColorConstant.YELLOW_BOLD + "10. Show Workers & Threads Summary" + TerminalColorConstant.RESET);
//        System.out.println(TerminalColorConstant.RED_BOLD + "0. Shutdown" + TerminalColorConstant.RESET);
//        System.out.print(TerminalColorConstant.WHITE_BOLD + "Enter your choice: " + TerminalColorConstant.RESET);
//    }

    private void clearScreen() {
        try {
            final String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // If clearing fails, just print extra newlines
            System.out.println("\n\n\n\n\n\n\n\n\n\n");
        }
    }

    //Deadlock Monitor
    private void startDeadlockMonitor() {
        new Thread(() -> {
            ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
            while (true) {
                long[] threadIds = mxBean.findDeadlockedThreads();
                if (threadIds != null) {
                    System.out.println(TerminalColorConstant.RED_BOLD + "\n========= DEADLOCK DETECTED =========" + TerminalColorConstant.RESET);
                    ThreadInfo[] threadInfos = mxBean.getThreadInfo(threadIds);
                    for (ThreadInfo threadInfo : threadInfos) {
                        System.out.println(TerminalColorConstant.RED + "Thread: " + threadInfo.getThreadName() +
                                " (ID: " + threadInfo.getThreadId() + ") is deadlocked." + TerminalColorConstant.RESET);
                    }
                    System.out.println(TerminalColorConstant.RED_BOLD + "======================================" + TerminalColorConstant.RESET);
                    break; // Stop after detecting deadlock once
                }
                try {
                    Thread.sleep(5000); // Check every 5 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "DeadlockMonitorThread").start();
    }

    private void printWorkersSummary() {
        System.out.println(TerminalColorConstant.YELLOW_BOLD + "\n========= WORKERS & THREADS SUMMARY =========" + TerminalColorConstant.RESET);

        // Print producers summary
        System.out.println(TerminalColorConstant.GREEN_BOLD + "\n--- Producers (" + producers.size() + ") ---" + TerminalColorConstant.RESET);
        for (int i = 0; i < producers.size(); i++) {
            Thread thread = producerThreads.get(i);
            System.out.printf("%-20s | %-15s | %-15s\n",
                    thread.getName(),
                    thread.getState(),
                    thread.isAlive() ? "Running" : "Stopped");
        }

        // Print consumers summary
        System.out.println(TerminalColorConstant.CYAN_BOLD + "\n--- Consumers (" + consumers.size() + ") ---" + TerminalColorConstant.RESET);
        for (int i = 0; i < consumers.size(); i++) {
            Thread thread = consumerThreads.get(i);
            System.out.printf("%-20s | %-15s | %-15s\n",
                    thread.getName(),
                    thread.getState(),
                    thread.isAlive() ? "Running" : "Stopped");
        }

        // Print writers summary
        System.out.println(TerminalColorConstant.PURPLE_BOLD + "\n--- Writers (" + writers.size() + ") ---" + TerminalColorConstant.RESET);
        for (int i = 0; i < writers.size(); i++) {
            Thread thread = writerThreads.get(i);
            System.out.printf("%-20s | %-15s | %-15s\n",
                    thread.getName(),
                    thread.getState(),
                    thread.isAlive() ? "Running" : "Stopped");
        }

        // Print readers summary
        System.out.println(TerminalColorConstant.BLUE_BOLD + "\n--- Readers (" + readers.size() + ") ---" + TerminalColorConstant.RESET);
        for (int i = 0; i < readers.size(); i++) {
            Thread thread = readerThreads.get(i);
            System.out.printf("%-20s | %-15s | %-15s\n",
                    thread.getName(),
                    thread.getState(),
                    thread.isAlive() ? "Running" : "Stopped");
        }

        // Print overall statistics
        int totalThreads = producers.size() + consumers.size() + writers.size() + readers.size();
        System.out.println(TerminalColorConstant.YELLOW_BOLD + "\n--- Total Threads: " + totalThreads + " ---" + TerminalColorConstant.RESET);
        System.out.println(TerminalColorConstant.YELLOW_BOLD + "===========================================" + TerminalColorConstant.RESET);
    }

    //========== Worker Methods ==========

    private void addProducer() {
        String threadName = "Producer-" + (producers.size() + 1);
        Producer producer = new Producer(
                ticketPool,
                1000,
                10,
                "Concert",
                threadName,
                "Colombo",
                100.00);
        Thread thread = new Thread(producer, threadName);
        producers.add(producer);
        producerThreads.add(thread);
        thread.start();
        System.out.println(TerminalColorConstant.GREEN + thread.getName() + " started successfully." + TerminalColorConstant.RESET);
    }

    private void addProducer(int creationRateMillis, int maxTickets) {
        String threadName = "Producer-" + (producers.size() + 1);
        Producer producer = new Producer(
                ticketPool,
                creationRateMillis,
                maxTickets,
                "Concert",
                threadName,
                "Colombo",
                100.00);
        Thread thread = new Thread(producer, threadName);
        producers.add(producer);
        producerThreads.add(thread);
        thread.start();
        System.out.println(TerminalColorConstant.GREEN + thread.getName() +
                " started successfully (Rate: " + creationRateMillis + "ms, Max: " +
                maxTickets + " tickets)" + TerminalColorConstant.RESET);
    }

    private void addConsumer() {
        String threadName = "Consumer-" + (consumers.size() + 1);
        Consumer consumer = new Consumer(
                ticketPool,
                8,
                5,
                true);

        Thread thread = new Thread(consumer, threadName);
        consumers.add(consumer);
        consumerThreads.add(thread);
        thread.start();
        System.out.println(TerminalColorConstant.GREEN + thread.getName() + " started successfully." + TerminalColorConstant.RESET);
    }

    private void addConsumer(int purchaseLimit, int purchaseRate, boolean simulateCancel) {
        String threadName = "Consumer-" + (consumers.size() + 1);
        Consumer consumer = new Consumer(
                ticketPool,
                purchaseLimit,
                purchaseRate,
                simulateCancel);

        Thread thread = new Thread(consumer, threadName);
        consumers.add(consumer);
        consumerThreads.add(thread);
        thread.start();
        System.out.println(TerminalColorConstant.GREEN + thread.getName() + " started successfully." + TerminalColorConstant.RESET);
    }

    private void addWriter() {
        Writer writer = new Writer(ticketPool, 2000, 10);
        Thread thread = new Thread(writer, "Writer-" + (writers.size() + 1));
        writers.add(writer);
        writerThreads.add(thread);
        thread.start();
        System.out.println(TerminalColorConstant.GREEN + thread.getName() + " started successfully." + TerminalColorConstant.RESET);
    }

    private void addWriter(int updateAtRateMillis, int maxUpdateAttempts) {
        Writer writer = new Writer(
                ticketPool, updateAtRateMillis,
                maxUpdateAttempts);

        Thread thread = new Thread(writer, "Writer-" + (writers.size() + 1));
        writers.add(writer);
        writerThreads.add(thread);
        thread.start();
        System.out.println(TerminalColorConstant.GREEN + thread.getName() + " started successfully." + TerminalColorConstant.RESET);
    }

    private void addReader() {
        Reader reader = new Reader(ticketPool, 3000, 50);
        Thread thread = new Thread(reader, "Reader-" + (readers.size() + 1));
        readers.add(reader);
        readerThreads.add(thread);
        thread.start();
        System.out.println(TerminalColorConstant.GREEN + thread.getName() + " started successfully." + TerminalColorConstant.RESET);
    }

    private void addReader(int readAtRateMillis, int maxReadAttempts) {
        Reader reader = new Reader(
                ticketPool, readAtRateMillis,
                maxReadAttempts);

        Thread thread = new Thread(reader, "Reader-" + (readers.size() + 1));
        readers.add(reader);
        readerThreads.add(thread);
        thread.start();
        System.out.println(TerminalColorConstant.GREEN + thread.getName() + " started successfully." + TerminalColorConstant.RESET);
    }

    private void removeProducer() {
        if (!producers.isEmpty()) {
            Producer producer = producers.remove(producers.size() - 1);
            Thread thread = producerThreads.remove(producerThreads.size() - 1);
            producer.stop();
            thread.interrupt();
            System.out.println(TerminalColorConstant.RED + thread.getName() + " stopped successfully." + TerminalColorConstant.RESET);
        } else {
            System.out.println(TerminalColorConstant.YELLOW + "No producers to remove." + TerminalColorConstant.RESET);
        }
    }



    private void removeConsumer() {
        if (!consumers.isEmpty()) {
            Consumer consumer = consumers.remove(consumers.size() - 1);
            Thread thread = consumerThreads.remove(consumerThreads.size() - 1);
            consumer.stop();
            thread.interrupt();
            System.out.println(TerminalColorConstant.RED + thread.getName() + " stopped successfully." + TerminalColorConstant.RESET);
        } else {
            System.out.println(TerminalColorConstant.YELLOW + "No consumers to remove." + TerminalColorConstant.RESET);
        }
    }

    private void removeWriter() {
        if (!writers.isEmpty()) {
            Writer writer = writers.remove(writers.size() - 1);
            Thread thread = writerThreads.remove(writerThreads.size() - 1);
            writer.stop();
            thread.interrupt();
            System.out.println(TerminalColorConstant.RED + thread.getName() + " stopped successfully." + TerminalColorConstant.RESET);
        } else {
            System.out.println(TerminalColorConstant.YELLOW + "No writers to remove." + TerminalColorConstant.RESET);
        }
    }

    private void removeReader() {
        if (!readers.isEmpty()) {
            Reader reader = readers.remove(readers.size() - 1);
            Thread thread = readerThreads.remove(readerThreads.size() - 1);
            reader.stop();
            thread.interrupt();
            System.out.println(TerminalColorConstant.RED + thread.getName() + " stopped successfully." + TerminalColorConstant.RESET);
        } else {
            System.out.println(TerminalColorConstant.YELLOW + "No readers to remove." + TerminalColorConstant.RESET);
        }
    }

    public void shutdown() {
        System.out.println(TerminalColorConstant.RED_BOLD + "Shutting down simulation..." + TerminalColorConstant.RESET);

        for (Producer producer : producers) producer.stop();
        for (Thread thread : producerThreads) thread.interrupt();

        for (Consumer consumer : consumers) consumer.stop();
        for (Thread thread : consumerThreads) thread.interrupt();

        for (Writer writer : writers) writer.stop();
        for (Thread thread : writerThreads) thread.interrupt();

        for (Reader reader : readers) reader.stop();
        for (Thread thread : readerThreads) thread.interrupt();

        System.out.println(TerminalColorConstant.GREEN_BOLD + "Simulation stopped successfully." + TerminalColorConstant.RESET);
    }
}
