package uk.gov.pay.adminusers.app.config;

import jakarta.validation.constraints.NotNull;

public class SecondFactorAuthConfiguration {
    @NotNull
    private int timeWindowInSeconds;
    
    @NotNull
    private int validTimeWindows;
    
    public int getTimeWindowInSeconds() {
        return timeWindowInSeconds;
    }

    public int getValidTimeWindows() {
        return validTimeWindows;
    }

    public long getTimeWindowInMillis() {
        return timeWindowInSeconds * 1000L;
    }

    public void setTimeWindowInSeconds(int timeWindowInSeconds) {
        this.timeWindowInSeconds = timeWindowInSeconds;
    }

    public void setValidTimeWindows(int validTimeWindows) {
        this.validTimeWindows = validTimeWindows;
    }
}
