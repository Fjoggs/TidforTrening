package com.fjoggs.tidfortrening.api;

import java.sql.Time;

public class Workout {
    public Time startTime;
    public Time endTime;

    public double distance;

    public Workout() {
        startTime = null;
        endTime = null;
    }

    public String toString() {
        return "Workout started "+ startTime.toString() + " and ended " + endTime.toString() + "." +
                "Distance: " + distance + ".";
    }
}
