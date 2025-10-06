"""
Data organization and file system management for market data.
"""

import os
import json
import pandas as pd
from pathlib import Path
from typing import Dict, List
from datetime import datetime


class DataOrganizer:
    """Organizes and persists market data in structured directory format."""

    def __init__(self, base_dir: str = "market_data"):
        """
        Initialize data organizer.

        Args:
            base_dir: Base directory for market data storage
        """
        self.base_dir = Path(base_dir)
        self.tickers_dir = self.base_dir / "tickers"
        self.metadata_path = self.base_dir / "metadata.json"
        self.liquidity_ranking_path = self.base_dir / "liquidity_ranking.json"

        self._ensure_directory_structure()

    def _ensure_directory_structure(self):
        """Create base directory structure if it doesn't exist."""
        self.base_dir.mkdir(parents=True, exist_ok=True)
        self.tickers_dir.mkdir(exist_ok=True)

    def save_ticker_data(
        self,
        ticker: str,
        date: str,
        ticks_df: pd.DataFrame,
        ticker_info: Dict = None
    ):
        """
        Save tick data and info for a ticker on a specific date.

        Args:
            ticker: Ticker symbol
            date: Date string in format YYYY-MM-DD
            ticks_df: DataFrame with tick data
            ticker_info: Optional dict with ticker metadata (liquidity, level, etc)
        """
        ticker_dir = self.tickers_dir / ticker
        ticker_dir.mkdir(exist_ok=True)

        ticks_file = ticker_dir / f"{date}_ticks.csv"
        ticks_df.to_csv(ticks_file, index=False)

        if ticker_info:
            self._update_ticker_info(ticker, ticker_info)

        print(f"Saved {ticker} | {date} | {len(ticks_df)} ticks")

    def _update_ticker_info(self, ticker: str, info: Dict):
        """
        Update or create ticker_info.json for a ticker.

        Args:
            ticker: Ticker symbol
            info: Dict with ticker metadata
        """
        ticker_dir = self.tickers_dir / ticker
        info_file = ticker_dir / "ticker_info.json"

        existing_info = {}
        if info_file.exists():
            with open(info_file, 'r') as f:
                existing_info = json.load(f)

        existing_info.update(info)

        with open(info_file, 'w') as f:
            json.dump(existing_info, f, indent=2)

    def save_liquidity_ranking(self, liquidity_df: pd.DataFrame):
        """
        Save liquidity ranking data.

        Args:
            liquidity_df: DataFrame from LiquidityAnalyzer.analyze_tickers()
        """
        ranking_data = liquidity_df.to_dict(orient='records')

        with open(self.liquidity_ranking_path, 'w') as f:
            json.dump(ranking_data, f, indent=2)

        print(f"Saved liquidity ranking: {len(ranking_data)} tickers")

    def update_metadata(self, metadata: Dict):
        """
        Update global metadata.json.

        Args:
            metadata: Dict with metadata to update/add
        """
        existing_metadata = {}
        if self.metadata_path.exists():
            with open(self.metadata_path, 'r') as f:
                existing_metadata = json.load(f)

        existing_metadata.update(metadata)
        existing_metadata['last_updated'] = datetime.now().isoformat()

        with open(self.metadata_path, 'w') as f:
            json.dump(existing_metadata, f, indent=2)

    def list_available_tickers(self) -> List[str]:
        """
        List all tickers with data.

        Returns:
            List of ticker symbols
        """
        if not self.tickers_dir.exists():
            return []

        return [d.name for d in self.tickers_dir.iterdir() if d.is_dir()]

    def list_dates_for_ticker(self, ticker: str) -> List[str]:
        """
        List all available dates for a ticker.

        Args:
            ticker: Ticker symbol

        Returns:
            List of date strings in format YYYY-MM-DD
        """
        ticker_dir = self.tickers_dir / ticker

        if not ticker_dir.exists():
            return []

        csv_files = ticker_dir.glob("*_ticks.csv")
        dates = [f.stem.replace("_ticks", "") for f in csv_files]
        return sorted(dates)

    def get_ticker_info(self, ticker: str) -> Dict:
        """
        Load ticker info from ticker_info.json.

        Args:
            ticker: Ticker symbol

        Returns:
            Dict with ticker info or None if not found
        """
        info_file = self.tickers_dir / ticker / "ticker_info.json"

        if not info_file.exists():
            return None

        with open(info_file, 'r') as f:
            return json.load(f)

    def load_liquidity_ranking(self) -> pd.DataFrame:
        """
        Load liquidity ranking data.

        Returns:
            DataFrame with liquidity ranking or None if not found
        """
        if not self.liquidity_ranking_path.exists():
            return None

        with open(self.liquidity_ranking_path, 'r') as f:
            data = json.load(f)

        return pd.DataFrame(data)

    def get_summary(self) -> Dict:
        """
        Get summary of available data.

        Returns:
            Dict with summary statistics
        """
        tickers = self.list_available_tickers()

        summary = {
            'total_tickers': len(tickers),
            'tickers': {}
        }

        for ticker in tickers:
            dates = self.list_dates_for_ticker(ticker)
            info = self.get_ticker_info(ticker)

            summary['tickers'][ticker] = {
                'num_dates': len(dates),
                'dates': dates,
                'liquidity_level': info.get('level') if info else None
            }

        return summary