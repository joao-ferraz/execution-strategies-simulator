import yfinance as yf
import pandas as pd
import numpy as np
from datetime import timedelta

def download_data():

    ticker = 'PETR4.SA'
    period="1d"
    interval="1m"
    df = yf.download(ticker, period=period, interval=interval)
    df.to_csv(f'{ticker}-{period}-{interval}.csv')


# ====================================================
# CONFIGURATION
# ====================================================

TICKS_PER_MIN = 120          # synthetic ticks per 1min candle
SPREAD_MEAN = 0.0015         # ~0.15% average spread
SPREAD_VOL = 0.0003          # spread volatility
VOL_NOISE = 0.4              # randomness of per-tick volume
TREND_WEIGHT = 0.6           # strength of trend open→close
SEED = 42                    # reproducibility

np.random.seed(SEED)

# ====================================================
# 1. LOAD DATA FROM CSV
# ====================================================

ticker = "PETR4.SA"
period = "1d"
interval = "1m"

df = pd.read_csv(f"{ticker}-{period}-{interval}.csv", parse_dates=["Datetime"], index_col="Datetime")
breakpoint()
df = df.dropna(subset=["Open", "High", "Low", "Close", "Volume"])

# ====================================================
# 2. HELPER FUNCTIONS
# ====================================================

def generate_spread(mid_price):
    """Return (bid, ask) given a mid price and random spread."""
    spread_pct = np.random.normal(SPREAD_MEAN, SPREAD_VOL)
    spread = mid_price * spread_pct
    bid = mid_price - spread / 2
    ask = mid_price + spread / 2
    return bid, ask

def generate_midpath(row):
    """Generate a random walk between open and close that respects OHLC."""
    start, end = row["Open"], row["Close"]
    path = np.linspace(start, end, TICKS_PER_MIN)
    noise = np.random.normal(0, (row["High"] - row["Low"]) / 200, TICKS_PER_MIN)
    drift = TREND_WEIGHT * (end - start) * np.linspace(0, 1, TICKS_PER_MIN)
    mid = path + drift + noise
    mid = np.clip(mid, row["Low"], row["High"])
    return mid

def allocate_volumes(total_volume):
    """Randomly allocate total volume across ticks."""
    weights = np.abs(np.random.normal(1, VOL_NOISE, TICKS_PER_MIN))
    weights /= weights.sum()
    return (weights * total_volume).astype(int)

# ====================================================
# 3. MAIN LOOP — GENERATE SYNTHETIC TICKS
# ====================================================

all_ticks = []

for t, row in df.iterrows():
    #breakpoint()
    mid_prices = generate_midpath(row)
    volumes = allocate_volumes(row["Volume"])

    # Timestamp spacing
    dt = timedelta(seconds=60 / TICKS_PER_MIN)

    for i in range(TICKS_PER_MIN):
        timestamp = t + i * dt
        mid = mid_prices[i]
        bid, ask = generate_spread(mid)

        # Define side & trade price
        candle_is_bull = row["Close"] > row["Open"]
        prob_buy = 0.55 if candle_is_bull else 0.45
        side = np.random.choice(["buy", "sell"], p=[prob_buy, 1 - prob_buy])

        if side == "buy":
            trade_price = ask * np.random.uniform(0.999, 1.001)
        else:
            trade_price = bid * np.random.uniform(0.999, 1.001)

        tick = {
            "timestamp": timestamp,
            "bid": round(bid, 4),
            "ask": round(ask, 4),
            "trade_price": round(trade_price, 4),
            "volume": int(volumes[i]),
            "side": side
        }
        all_ticks.append(tick)

# ====================================================
# 4. BUILD FINAL DATAFRAME
# ====================================================

ticks_df = pd.DataFrame(all_ticks)
ticks_df.sort_values("timestamp", inplace=True)
ticks_df.reset_index(drop=True, inplace=True)

# ====================================================
# 5. SAVE TO CSV
# ====================================================

out_file = f"synthetic_ticks_{ticker}.csv"
ticks_df.to_csv(out_file, index=False)
print(f"✅ Synthetic tick data saved to: {out_file}")
print(ticks_df.head())
