package execution;

import data.TickData;

import java.time.Instant;

public class MarketState {
    private final TickData currentTick;
    private final long currentTimeMs;
    private final double bid;
    private final double ask;
    private final double volume;
    private final double midPrice;

    public MarketState(TickData tick) {
        this.currentTick = tick;
        this.currentTimeMs = tick.getTimestamp().toEpochMilli();
        this.bid = tick.getBp();
        this.ask = tick.getAp();
        this.volume = tick.getVolume();
        this.midPrice = (bid + ask) / 2.0;
    }

    public TickData getCurrentTick() {
        return currentTick;
    }

    public long getCurrentTimeMs() {
        return currentTimeMs;
    }

    public Instant getCurrentTime() {
        return currentTick.getTimestamp();
    }

    public double getBid() {
        return bid;
    }

    public double getAsk() {
        return ask;
    }

    public double getVolume() {
        return volume;
    }

    public double getMidPrice() {
        return midPrice;
    }

    public double getSpread() {
        return ask - bid;
    }

    @Override
    public String toString() {
        return "MarketState{" +
                "time=" + currentTick.getTimestamp() +
                ", bid=" + bid +
                ", ask=" + ask +
                ", mid=" + String.format("%.4f", midPrice) +
                ", volume=" + volume +
                '}';
    }
}