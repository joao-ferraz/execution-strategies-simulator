package data;

import java.time.Instant;

public class TickData {
    private final Instant timestamp;
    private final double ap;
    private final double bp;
    private final double tp;
    private final double volume;
    private final String side;

    public TickData(Instant timestamp, double bp, double ap, double tp, double volume, String side) {
        this.timestamp = timestamp;
        this.ap = ap;
        this.bp = bp;
        this.tp = tp;
        this.volume = volume;
        this.side = side;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public double getAp() {
        return ap;
    }

    public double getBp() {
        return bp;
    }

    public double getTp() {
        return tp;
    }

    public double getVolume() {
        return volume;
    }

    public String getSide() {
        return side;
    }

    @Override
    public String toString() {
        return "TickData{" +
                "timestamp=" + timestamp +
                ", bp=" + bp +
                ", ap=" + ap +
                ", tp=" + tp +
                ", volume=" + volume +
                ", side='" + side + '\'' +
                '}';
    }
}
