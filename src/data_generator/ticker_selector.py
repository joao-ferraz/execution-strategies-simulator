"""
Ticker selection and fetching utilities.
"""

import yfinance as yf
from typing import List
import pandas as pd
import os


class IbovespaTickerFetcher:
    """Fetches list of tickers from Ibovespa index."""

    CSV_PATH = os.path.join(
        os.path.dirname(__file__),
        "data",
        "ibov_composition.csv"
    )

    @staticmethod
    def fetch_tickers(include_ibov: bool = True) -> List[str]:
        """
        Fetch Ibovespa component tickers from B3 CSV.

        Args:
            include_ibov: Whether to include ^BVSP index itself

        Returns:
            List of ticker symbols in yfinance format (XXX.SA)
        """
        tickers = IbovespaTickerFetcher._load_from_csv()

        if not tickers:
            print(f"Warning: No tickers loaded from {IbovespaTickerFetcher.CSV_PATH}")
            return []

        print(f"Loaded {len(tickers)} tickers from B3 composition CSV")

        if include_ibov and "^BVSP" not in tickers:
            tickers.insert(0, "^BVSP")
        elif not include_ibov and "^BVSP" in tickers:
            tickers.remove("^BVSP")

        return tickers

    @staticmethod
    def _load_from_csv() -> List[str]:
        """
        Load tickers from B3 Ibovespa composition CSV.

        Returns:
            List of ticker symbols
        """
        if not os.path.exists(IbovespaTickerFetcher.CSV_PATH):
            print(f"CSV file not found: {IbovespaTickerFetcher.CSV_PATH}")
            return []

        try:
            df = pd.read_csv(
                IbovespaTickerFetcher.CSV_PATH,
                sep=';',
                encoding='latin-1',
                skiprows=1,
            )

            tickers = df["Código"].dropna().index.tolist()

            tickers = [f"{ticker.strip()}.SA" for ticker in tickers]

            return tickers

        except Exception as e:
            print(f"Error loading CSV: {e}")
            return []

    @staticmethod
    def validate_tickers(tickers: List[str], max_workers: int = 10) -> List[str]:
        """
        Validate that tickers exist and have data available.

        Args:
            tickers: List of ticker symbols to validate
            max_workers: Number of parallel workers for validation

        Returns:
            List of valid tickers
        """
        valid_tickers = []

        print(f"Validating {len(tickers)} tickers...")

        for ticker in tickers:
            try:
                stock = yf.Ticker(ticker)
                info = stock.info

                if info and 'regularMarketPrice' in info or 'currentPrice' in info:
                    valid_tickers.append(ticker)
                    print(f"  OK {ticker}")
                else:
                    print(f"  SKIP {ticker} - no price data")

            except Exception as e:
                print(f"  SKIP {ticker} - {str(e)[:50]}")

        print(f"Validation complete: {len(valid_tickers)}/{len(tickers)} tickers valid")
        return valid_tickers