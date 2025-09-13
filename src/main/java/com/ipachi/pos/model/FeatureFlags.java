package com.ipachi.pos.model;

import java.util.Objects;

public class FeatureFlags {
    private boolean advanceCash;
    private boolean airtime;
    private boolean electricity;
    private boolean demoMode;

    public FeatureFlags() {}

    public FeatureFlags(boolean advanceCash, boolean airtime, boolean electricity, boolean demoMode) {
        this.advanceCash = advanceCash;
        this.airtime = airtime;
        this.electricity = electricity;
        this.demoMode = demoMode;
    }

    public boolean isAdvanceCash() { return advanceCash; }
    public void setAdvanceCash(boolean advanceCash) { this.advanceCash = advanceCash; }
    public boolean isAirtime() { return airtime; }
    public void setAirtime(boolean airtime) { this.airtime = airtime; }
    public boolean isElectricity() { return electricity; }
    public void setElectricity(boolean electricity) { this.electricity = electricity; }
    public boolean isDemoMode() { return demoMode; }
    public void setDemoMode(boolean demoMode) { this.demoMode = demoMode; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FeatureFlags)) return false;
        FeatureFlags that = (FeatureFlags) o;
        return advanceCash == that.advanceCash && airtime == that.airtime && electricity == that.electricity && demoMode == that.demoMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(advanceCash, airtime, electricity, demoMode);
    }
}
