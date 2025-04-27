package com.gayan.entities;

import java.util.List;
import java.util.Optional;

public interface TicketPool {
    boolean addTicket(Ticket ticket); //vendors/producers to add ticket
    Optional<Ticket> purchaseTicket(); //consumers to purchase ticket
    int getCurrentSize(); //readers to query the current size
    int getCapacity(); //get the max no of tickets the pool can hold
    int getAvailableTicketCount(); // readers to check how many unsold tickets are available
    int getSoldTicketCount(); //readers to check how many tickets are sold.
    List<Ticket> getAllTickets(); //return list of all tickets in the pool
    void printTicketPoolStatus();
    Ticket createTicket(
            String eventName,
            String vendorName,
            String location,
            double price);
    void cancelTicket(Ticket ticket);
    void updateTicket(
            long ticketId,
            double newPrice,
            String newLocation,
            String newEventName
    );
    List<Ticket> getAvailableTickets();
    Optional<Ticket> getRandomAvailableTicket();
    int getAllTicketsCount();
}
