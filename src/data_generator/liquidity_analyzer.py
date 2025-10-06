"""
Liquidity analysis and ticker selection by percentile.
"""

import yfinance as yf
import pandas as pd
import numpy as np
from typing import List, Dict
from datetime import datetime, timedelta


class LiquidityAnalyzer:
    """Analyzes liquidity of multiple tickers and selects by percentile."""

    def __init__(self, lookback_days: int = 30):
        """
        Initialize analyzer.

        Args:
            lookback_days: Number of days to analyze for liquidity calculation
        """
        self.lookback_days = lookback_days

    def analyze_tickers(self, tickers: List[str]) -> pd.DataFrame:
        """
        Analyze liquidity for all tickers.

        Args:
            tickers: List of ticker symbols

        Returns:
            DataFrame with columns: ticker, avg_volume, avg_price, liquidity, percentile, level
        """
        print(f"\nAnalyzing liquidity for {len(tickers)} tickers ({self.lookback_days} days)...")
        print("Downloading data for all tickers in bulk...")

        end_date = datetime.now()
        start_date = end_date - timedelta(days=self.lookback_days + 10)

        bulk_data = yf.download(
            tickers,
            start=start_date,
            end=end_date,
            interval='1d',
            progress=True,
            group_by='ticker',
            auto_adjust=True
        )

        print("\nCalculating liquidity metrics...")

        liquidity_data = []

        for ticker in tickers:
            try:
                liquidity_info = self._calculate_ticker_liquidity(ticker, bulk_data)
                if liquidity_info:
                    liquidity_data.append(liquidity_info)
                    print(f"  {ticker:12} | "
                          f"Vol: {liquidity_info['avg_volume']:>12,.0f} | "
                          f"Price: ${liquidity_info['avg_price']:>8.2f} | "
                          f"Liquidity: ${liquidity_info['liquidity']:>15,.0f}")
            except Exception as e:
                print(f"  {ticker:12} | Error: {str(e)[:50]}")

        if not liquidity_data:
            raise RuntimeError("No valid liquidity data collected")

        df = pd.DataFrame(liquidity_data)
        df = df.sort_values('liquidity', ascending=False).reset_index(drop=True)

        df['percentile'] = df['liquidity'].rank(pct=True) * 100
        df['level'] = pd.qcut(df['liquidity'], q=10, labels=False, duplicates='drop') + 1

        print(f"\nLiquidity analysis complete: {len(df)} tickers")
        return df

    def _calculate_ticker_liquidity(self, ticker: str, bulk_data: pd.DataFrame) -> Dict:
        """
        Calculate liquidity metrics for a single ticker from bulk downloaded data.
        Liquidity = Average(Daily Volume × Average Price)

        Args:
            ticker: Ticker symbol
            bulk_data: DataFrame with bulk downloaded data for all tickers

        Returns:
            Dict with liquidity metrics or None if insufficient data
        """
        if len(bulk_data.columns.levels[0]) > 1:
            ticker_data = bulk_data[ticker]
        else:
            ticker_data = bulk_data

        if ticker_data.empty or len(ticker_data) < 5:
            return None

        ticker_data = ticker_data.dropna(subset=['Close', 'Volume'])

        if len(ticker_data) < 5:
            return None

        avg_price = ticker_data['Close'].mean()
        avg_volume = ticker_data['Volume'].mean()

        ticker_data['dollar_volume'] = ticker_data['Volume'] * ticker_data['Close']
        liquidity = ticker_data['dollar_volume'].mean()

        return {
            'ticker': ticker,
            'avg_volume': avg_volume,
            'avg_price': avg_price,
            'liquidity': liquidity
        }

    def select_by_percentiles(
        self,
        liquidity_df: pd.DataFrame,
        num_tickers: int,
        exclude_tickers: List[str] = None
    ) -> List[str]:
        """
        Select N tickers uniformly distributed across liquidity percentiles.

        Args:
            liquidity_df: DataFrame from analyze_tickers()
            num_tickers: Number of tickers to select
            exclude_tickers: Tickers to exclude from selection (e.g., ^BVSP)

        Returns:
            List of selected ticker symbols
        """
        if exclude_tickers:
            df = liquidity_df[~liquidity_df['ticker'].isin(exclude_tickers)].copy()
        else:
            df = liquidity_df.copy()

        if len(df) < num_tickers:
            print(f"Warning: Only {len(df)} tickers available, selecting all")
            return df['ticker'].tolist()

        percentiles = np.linspace(10, 90, num_tickers)

        selected = []
        for target_percentile in percentiles:
            df['distance'] = abs(df['percentile'] - target_percentile)
            closest = df.loc[df['distance'].idxmin()]

            selected.append(closest['ticker'])
            df = df[df['ticker'] != closest['ticker']]

        print(f"\nSelected {num_tickers} tickers across percentiles:")
        for i, ticker in enumerate(selected):
            ticker_info = liquidity_df[liquidity_df['ticker'] == ticker].iloc[0]
            print(f"  {i+1}. {ticker:12} | "
                  f"P{ticker_info['percentile']:>5.1f} | "
                  f"L{ticker_info['level']:>2.0f} | "
                  f"${ticker_info['liquidity']:>15,.0f}")

        return selected

    def get_ticker_info(self, liquidity_df: pd.DataFrame, ticker: str) -> Dict:
        """
        Get liquidity info for a specific ticker.

        Args:
            liquidity_df: DataFrame from analyze_tickers()
            ticker: Ticker symbol

        Returns:
            Dict with ticker info or None if not found
        """
        row = liquidity_df[liquidity_df['ticker'] == ticker]
        if row.empty:
            return None
        return row.iloc[0].to_dict()