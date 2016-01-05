package com.example.dms.inventorytracker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 *  Inventory Tracker
 *  Item.java
 *
 *  Class file for item object
 *
 *  Author: Amar Bhatt (Freelancer)
 *  Organization: Database Management Systems
 *  Last Update: 01/05/2016
 */
public class Item {

    // Class variables
    private String number; // item identification
    private int quantity; // quantity of item
    private int id; // unique identifier
    private String date; // last modified time stamp

    /*
    * Constructor
     */
    public Item(String number, int quantity, int id) {
        this.number = number;
        this.quantity = quantity;
        this.id = id;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date now = Calendar.getInstance().getTime();
        this.date = df.format(now); // set date to NOW


    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNumber() {

        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
    /*
    * Update item date to current time
     */
    public void updateDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date now = Calendar.getInstance().getTime();
        this.date = df.format(now);
    }
}
