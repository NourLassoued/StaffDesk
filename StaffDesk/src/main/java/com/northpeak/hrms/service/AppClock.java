package com.northpeak.hrms.service;

import java.time.LocalDate;

/**
 * Reference date of the application.
 * The demo dataset covers July - September 2023, so "today" defaults to 15 September 2023.
 * Run with -Dhrms.today=now to use the real system date, or -Dhrms.today=2023-09-20 for another date.
 */
public final class AppClock {

    private static final LocalDate TODAY = resolve();

    private AppClock() {
    }

    public static LocalDate today() {
        return TODAY;
    }

    private static LocalDate resolve() {
        String value = System.getProperty("hrms.today", "2023-09-15");
        return "now".equalsIgnoreCase(value) ? LocalDate.now() : LocalDate.parse(value);
    }
}
