package com.keemax.model;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 1/6/14
 * Time: 6:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class Order {
    private double rate;
    private double quantity;
    private double total;

    @Override
    public String toString() {
        return "Order{" +
                "rate=" + rate +
                ", quantity=" + quantity +
                ", total=" + total +
                '}';
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
