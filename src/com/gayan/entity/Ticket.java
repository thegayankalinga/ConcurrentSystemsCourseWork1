package com.gayan.entity;

public class Ticket {

    private long ticketId;
    private String eventName;
    private String vendorName;
    private String location;
    private double price;
    private boolean sold;

    public Ticket(
            long ticketId,
            String eventName,
            String vendorName,
            String location,
            double price) {
        this.ticketId = ticketId;
        this.eventName = eventName;
        this.vendorName = vendorName;
        this.location = location;
        this.price = price;
        this.sold = false;

    }

    public long getTicketId() {
        return ticketId;
    }

    public boolean isSold() {
        return sold;
    }

    public void setSold(boolean sold) {
        this.sold = sold;
    }

    public void setTicketId(long ticketId) {
        this.ticketId = ticketId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "com.gayan.entity.Ticket{" +
                "ticketId=" + ticketId +
                ", eventName='" + eventName + '\'' +
                ", vendorName='" + vendorName + '\'' +
                ", location='" + location + '\'' +
                ", price=" + price +
                ", sold=" + sold +
                '}';
    }
}
