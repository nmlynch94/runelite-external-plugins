package com.tempoross;

import javax.inject.Singleton;

@Singleton
public class TimerSwaps {
    public enum TimerModes {
        OFF,
        PIE,
        TICKS,
        SECONDS
    }
}
