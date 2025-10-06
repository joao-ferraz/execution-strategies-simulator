"""
Synthetic tick data generation from OHLCV candles.
"""

import pandas as pd
import numpy as np
from datetime import timedelta, datetime, time
from typing import Dict, Optional


class SyntheticTickGenerator:
    """Generates synthetic tick data from OHLCV candles."""

    def __init__(
        self,
        ticks_per_min: int = 120,
        spread_mean: float = 0.0015,
        spread_vol: float = 0.0003,
        vol_noise: float = 0.4,
        trend_weight: float = 0.6,
        seed: int = 42
    ):
        """
        Initialize tick generator.

        Args:
            ticks_per_min: Default number of synthetic ticks per 1-minute candle (used if no B3 data)
            spread_mean: Average bid-ask spread as percentage
            spread_vol: Spread volatility
            vol_noise: Randomness of per-tick volume distribution
            trend_weight: Strength of trend from open to close
            seed: Random seed for reproducibility
        """
        self.ticks_per_min = ticks_per_min
        self.spread_mean = spread_mean
        self.spread_vol = spread_vol
        self.vol_noise = vol_noise
        self.trend_weight = trend_weight
        self.seed = seed

        np.random.seed(seed)

    def generate_ticks(
        self,
        ohlcv_df: pd.DataFrame,
        b3_stats: Optional[Dict] = None
    ) -> pd.DataFrame:
        """
        Generate synthetic tick data from OHLCV candles.

        Args:
            ohlcv_df: DataFrame with columns [Open, High, Low, Close, Volume] and datetime index
            b3_stats: Optional dict with B3 statistics (totneg, quatot, etc)

        Returns:
            DataFrame with columns [timestamp, bid, ask, trade_price, volume, side]
        """
        ohlcv_df = ohlcv_df.dropna(subset=["Open", "High", "Low", "Close", "Volume"])

        if b3_stats and 'totneg' in b3_stats:
            total_ticks_target = b3_stats['totneg']
            tick_distribution = self._calculate_intraday_distribution(
                len(ohlcv_df),
                total_ticks_target
            )
        else:
            tick_distribution = None

        all_ticks = []
        minute_idx = 0

        for timestamp, row in ohlcv_df.iterrows():
            if tick_distribution is not None:
                num_ticks = max(1, int(tick_distribution[minute_idx]))
                minute_idx += 1
            else:
                num_ticks = self.ticks_per_min

            mid_prices = self._generate_midpath(row, num_ticks)
            volumes = self._allocate_volumes(row["Volume"], num_ticks)

            dt = timedelta(seconds=60 / num_ticks)

            for i in range(num_ticks):
                tick_timestamp = timestamp + i * dt
                mid = mid_prices[i]
                bid, ask = self._generate_spread(mid)

                candle_is_bull = row["Close"] > row["Open"]
                prob_buy = 0.55 if candle_is_bull else 0.45
                side = np.random.choice(["buy", "sell"], p=[prob_buy, 1 - prob_buy])

                if side == "buy":
                    trade_price = ask * np.random.uniform(0.999, 1.001)
                else:
                    trade_price = bid * np.random.uniform(0.999, 1.001)

                tick = {
                    "timestamp": tick_timestamp,
                    "bid": round(bid, 4),
                    "ask": round(ask, 4),
                    "trade_price": round(trade_price, 4),
                    "volume": int(volumes[i]),
                    "side": side
                }
                all_ticks.append(tick)

        ticks_df = pd.DataFrame(all_ticks)
        ticks_df.sort_values("timestamp", inplace=True)
        ticks_df.reset_index(drop=True, inplace=True)

        return ticks_df

    def _calculate_intraday_distribution(
        self,
        num_minutes: int,
        total_ticks: int
    ) -> np.ndarray:
        """
        Calculate realistic intraday tick distribution following U-shape pattern.

        Args:
            num_minutes: Number of minutes in trading session
            total_ticks: Total number of ticks to distribute

        Returns:
            Array with number of ticks per minute
        """
        minutes = np.arange(num_minutes)

        u_curve = self._u_shape_curve(minutes, num_minutes)

        distribution = (u_curve / u_curve.sum()) * total_ticks

        return np.maximum(1, distribution)

    def _u_shape_curve(self, minutes: np.ndarray, total_minutes: int) -> np.ndarray:
        """
        Generate U-shape curve for intraday trading activity.

        High activity at market open and close, lower in the middle.

        Args:
            minutes: Array of minute indices
            total_minutes: Total number of minutes

        Returns:
            Array of activity weights
        """
        normalized_time = minutes / total_minutes

        opening_weight = 3.0 * np.exp(-10 * normalized_time)

        closing_weight = 3.0 * np.exp(-10 * (1 - normalized_time))

        midday_weight = 1.0

        curve = opening_weight + closing_weight + midday_weight

        noise = np.random.normal(1.0, 0.1, len(minutes))
        curve = curve * noise

        return np.maximum(0.1, curve)

    def _generate_spread(self, mid_price: float) -> tuple:
        """
        Generate bid and ask prices around mid price.

        Args:
            mid_price: Mid market price

        Returns:
            Tuple of (bid, ask)
        """
        spread_pct = np.random.normal(self.spread_mean, self.spread_vol)
        spread = mid_price * spread_pct
        bid = mid_price - spread / 2
        ask = mid_price + spread / 2
        return bid, ask

    def _generate_midpath(self, row: pd.Series, num_ticks: int) -> np.ndarray:
        """
        Generate random walk between open and close that respects OHLC.

        Args:
            row: OHLCV candle row
            num_ticks: Number of ticks to generate

        Returns:
            Array of mid prices for each tick
        """
        start, end = row["Open"], row["Close"]
        path = np.linspace(start, end, num_ticks)
        noise = np.random.normal(0, (row["High"] - row["Low"]) / 200, num_ticks)
        drift = self.trend_weight * (end - start) * np.linspace(0, 1, num_ticks)
        mid = path + drift + noise
        mid = np.clip(mid, row["Low"], row["High"])
        return mid

    def _allocate_volumes(self, total_volume: float, num_ticks: int) -> np.ndarray:
        """
        Randomly allocate total volume across ticks.

        Args:
            total_volume: Total volume from candle
            num_ticks: Number of ticks to allocate to

        Returns:
            Array of volumes for each tick
        """
        weights = np.abs(np.random.normal(1, self.vol_noise, num_ticks))
        weights /= weights.sum()
        return (weights * total_volume).astype(int)