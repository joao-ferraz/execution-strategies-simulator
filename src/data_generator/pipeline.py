"""
Main pipeline for market data generation.
"""

import yfinance as yf
import pandas as pd
from datetime import datetime, timedelta
from typing import List, Dict

from .ticker_selector import IbovespaTickerFetcher
from .liquidity_analyzer import LiquidityAnalyzer
from .tick_generator import SyntheticTickGenerator
from .data_organizer import DataOrganizer
from .b3_data_fetcher import B3HistoricalDataFetcher


class MarketDataPipeline:
    """Orchestrates market data generation pipeline."""

    def __init__(
        self,
        output_dir: str = "market_data",
        num_tickers: int = 9,
        days: int = 5,
        tick_config: Dict = None,
        use_b3_stats: bool = True
    ):
        """
        Initialize pipeline.

        Args:
            output_dir: Output directory for generated data
            num_tickers: Number of tickers to select by liquidity
            days: Number of trading days to generate
            tick_config: Optional config dict for SyntheticTickGenerator
            use_b3_stats: Whether to use B3 COTAHIST data for realistic tick counts
        """
        self.output_dir = output_dir
        self.num_tickers = num_tickers
        self.days = days
        self.use_b3_stats = use_b3_stats

        self.ticker_fetcher = IbovespaTickerFetcher()
        self.liquidity_analyzer = LiquidityAnalyzer(lookback_days=30)
        self.tick_generator = SyntheticTickGenerator(**(tick_config or {}))
        self.data_organizer = DataOrganizer(base_dir=output_dir)
        self.b3_fetcher = B3HistoricalDataFetcher() if use_b3_stats else None

    def run(self):
        """Execute full pipeline."""
        print("=" * 80)
        print("MARKET DATA GENERATION PIPELINE")
        print("=" * 80)

        tickers = self._fetch_tickers()
        liquidity_df = self._analyze_liquidity(tickers)
        selected_tickers = self._select_tickers(liquidity_df)
        self._generate_and_save_data(selected_tickers, liquidity_df)
        self._finalize_metadata(selected_tickers)

        print("\n" + "=" * 80)
        print("PIPELINE COMPLETE")
        print("=" * 80)
        self._print_summary()

    def _fetch_tickers(self) -> List[str]:
        """Fetch all Ibovespa tickers."""
        print("\n[1/5] Fetching Ibovespa tickers...")
        tickers = self.ticker_fetcher.fetch_tickers(include_ibov=True)
        print(f"Fetched {len(tickers)} tickers")
        return tickers

    def _analyze_liquidity(self, tickers: List[str]) -> pd.DataFrame:
        """Analyze liquidity for all tickers."""
        print("\n[2/5] Analyzing liquidity...")
        liquidity_df = self.liquidity_analyzer.analyze_tickers(tickers)
        self.data_organizer.save_liquidity_ranking(liquidity_df)
        return liquidity_df

    def _select_tickers(self, liquidity_df: pd.DataFrame) -> List[str]:
        """Select tickers by liquidity percentiles."""
        print("\n[3/5] Selecting tickers by liquidity...")

        selected = self.liquidity_analyzer.select_by_percentiles(
            liquidity_df,
            self.num_tickers,
            exclude_tickers=["^BVSP"]
        )

        ibov_info = liquidity_df[liquidity_df['ticker'] == '^BVSP']
        if not ibov_info.empty:
            selected.insert(0, "^BVSP")
            print(f"\nAdded ^BVSP (Ibovespa index)")

        return selected

    def _generate_and_save_data(self, tickers: List[str], liquidity_df: pd.DataFrame):
        """Generate and save tick data for all selected tickers."""
        print("\n[4/5] Generating tick data...")

        dates = self._get_date_range()
        total_tasks = len(tickers) * len(dates)
        completed = 0

        for date_str in dates:
            print(f"\nDownloading data for all tickers on {date_str}...")

            bulk_data = self._download_bulk_minute_data(tickers, date_str)

            for ticker in tickers:
                ticker_info = self.liquidity_analyzer.get_ticker_info(liquidity_df, ticker)

                try:
                    ticks_df = self._generate_ticks_from_bulk(ticker, bulk_data, date_str)

                    if ticks_df is not None and not ticks_df.empty:
                        self.data_organizer.save_ticker_data(
                            ticker,
                            date_str,
                            ticks_df,
                            ticker_info
                        )

                    completed += 1
                    print(f"  {ticker:12} | {len(ticks_df) if ticks_df is not None else 0:>6} ticks | Progress: {completed}/{total_tasks}")

                except Exception as e:
                    print(f"  {ticker:12} | Error: {str(e)[:40]}")
                    completed += 1

    def _download_bulk_minute_data(self, tickers: List[str], date_str: str) -> pd.DataFrame:
        """
        Download 1-minute data for all tickers on a specific date.

        Args:
            tickers: List of ticker symbols
            date_str: Date string YYYY-MM-DD

        Returns:
            DataFrame with bulk downloaded data
        """
        date = datetime.strptime(date_str, "%Y-%m-%d")
        end_date = date + timedelta(days=1)

        bulk_data = yf.download(
            tickers,
            start=date,
            end=end_date,
            interval='1m',
            progress=False,
            group_by='ticker',
            auto_adjust=True
        )

        return bulk_data

    def _generate_ticks_from_bulk(
        self,
        ticker: str,
        bulk_data: pd.DataFrame,
        date_str: str
    ) -> pd.DataFrame:
        """
        Generate tick data for a ticker from bulk downloaded data.

        Args:
            ticker: Ticker symbol
            bulk_data: DataFrame with bulk downloaded data
            date_str: Date string YYYY-MM-DD

        Returns:
            DataFrame with tick data or None if no data available
        """
        if bulk_data.empty:
            return None

        if len(bulk_data.columns.levels[0]) > 1:
            ticker_data = bulk_data[ticker]
        else:
            ticker_data = bulk_data

        if ticker_data.empty:
            return None

        b3_stats = None
        if self.b3_fetcher:
            ticker_clean = ticker.replace(".SA", "")
            b3_stats = self.b3_fetcher.get_ticker_stats(ticker_clean, date_str)

        return self.tick_generator.generate_ticks(ticker_data, b3_stats)

    def _get_date_range(self) -> List[str]:
        """
        Get list of trading dates to generate.

        Returns:
            List of date strings in format YYYY-MM-DD
        """
        end_date = datetime.now()
        dates = []

        for i in range(self.days * 2):
            check_date = end_date - timedelta(days=i)

            if check_date.weekday() < 5:
                dates.append(check_date.strftime("%Y-%m-%d"))

            if len(dates) >= self.days:
                break

        return sorted(dates)

    def _finalize_metadata(self, tickers: List[str]):
        """Save final metadata."""
        print("\n[5/5] Finalizing metadata...")

        metadata = {
            'num_tickers': len(tickers),
            'selected_tickers': tickers,
            'num_days': self.days,
            'tick_config': {
                'ticks_per_min': self.tick_generator.ticks_per_min,
                'spread_mean': self.tick_generator.spread_mean,
                'spread_vol': self.tick_generator.spread_vol,
                'vol_noise': self.tick_generator.vol_noise,
                'trend_weight': self.tick_generator.trend_weight,
                'seed': self.tick_generator.seed
            }
        }

        self.data_organizer.update_metadata(metadata)
        print("Metadata saved")

    def _print_summary(self):
        """Print summary of generated data."""
        summary = self.data_organizer.get_summary()

        print(f"\nTotal tickers: {summary['total_tickers']}")
        print(f"\nData generated:")

        for ticker, info in summary['tickers'].items():
            level = f"L{info['liquidity_level']}" if info['liquidity_level'] else "N/A"
            print(f"  {ticker:12} | {level:4} | {info['num_dates']} days")