"""
B3 Historical Data Fetcher - Download and parse COTAHIST files.
"""

import os
import requests
import zipfile
import pandas as pd
from pathlib import Path
from typing import Dict, Optional
from datetime import datetime


class B3HistoricalDataFetcher:
    """Downloads and parses B3 COTAHIST historical data files."""

    BASE_URL = "https://bvmf.bmfbovespa.com.br/InstDados/SerHist"

    def __init__(self, cache_dir: str = None):
        """
        Initialize fetcher.

        Args:
            cache_dir: Directory for caching downloaded files
        """
        if cache_dir is None:
            cache_dir = os.path.join(
                os.path.dirname(__file__),
                "data",
                "cotahist"
            )

        self.cache_dir = Path(cache_dir)
        self.parsed_dir = self.cache_dir / "parsed"

        self.cache_dir.mkdir(parents=True, exist_ok=True)
        self.parsed_dir.mkdir(exist_ok=True)

    def get_ticker_stats(self, ticker: str, date: str) -> Optional[Dict]:
        """
        Get statistics for a ticker on a specific date from COTAHIST.

        Args:
            ticker: Ticker symbol (without .SA suffix)
            date: Date in format YYYY-MM-DD

        Returns:
            Dict with ticker statistics or None if not found
        """
        date_obj = datetime.strptime(date, "%Y-%m-%d")
        year = date_obj.year

        df = self.load_cotahist(year)

        if df is None:
            return None

        ticker_clean = ticker.replace(".SA", "")
        date_int = int(date_obj.strftime("%Y%m%d"))

        mask = (df['codneg'] == ticker_clean) & (df['date'] == date_int)
        records = df[mask]

        if records.empty:
            return None

        record = records.iloc[0]

        return {
            'ticker': ticker,
            'date': date,
            'totneg': int(record['totneg']),
            'quatot': int(record['quatot']),
            'voltot': float(record['voltot']),
            'preabe': float(record['preabe']),
            'premax': float(record['premax']),
            'premin': float(record['premin']),
            'premed': float(record['premed']),
            'preult': float(record['preult'])
        }

    def load_cotahist(self, year: int) -> Optional[pd.DataFrame]:
        """
        Load COTAHIST data for a year (with caching).

        Args:
            year: Year to load

        Returns:
            DataFrame with parsed data or None if unavailable
        """
        parquet_path = self.parsed_dir / f"{year}.parquet"

        if parquet_path.exists():
            print(f"Loading cached COTAHIST data for {year}...")
            return pd.read_parquet(parquet_path, engine='fastparquet')

        txt_path = self.cache_dir / f"COTAHIST_A{year}.TXT"

        if not txt_path.exists():
            print(f"Downloading COTAHIST data for {year}...")
            if not self.download_cotahist(year):
                return None

        print(f"Parsing COTAHIST data for {year}...")
        df = self.parse_cotahist(txt_path)

        if df is not None:
            df.to_parquet(parquet_path, engine='fastparquet')
            print(f"Cached to {parquet_path}")

        return df

    def download_cotahist(self, year: int) -> bool:
        """
        Download COTAHIST ZIP file for a year.

        Args:
            year: Year to download

        Returns:
            True if successful, False otherwise
        """
        zip_url = f"{self.BASE_URL}/COTAHIST_A{year}.ZIP"
        zip_path = self.cache_dir / f"COTAHIST_A{year}.ZIP"
        txt_path = self.cache_dir / f"COTAHIST_A{year}.TXT"

        try:
            response = requests.get(zip_url, timeout=60)
            response.raise_for_status()

            with open(zip_path, 'wb') as f:
                f.write(response.content)

            with zipfile.ZipFile(zip_path, 'r') as zip_ref:
                zip_ref.extractall(self.cache_dir)

            zip_path.unlink()

            if txt_path.exists():
                print(f"Downloaded and extracted: {txt_path}")
                return True
            else:
                print(f"Error: TXT file not found after extraction")
                return False

        except Exception as e:
            print(f"Error downloading COTAHIST: {e}")
            return False

    def parse_cotahist(self, file_path: Path) -> Optional[pd.DataFrame]:
        """
        Parse COTAHIST fixed-width TXT file.

        Args:
            file_path: Path to COTAHIST TXT file

        Returns:
            DataFrame with parsed data
        """
        colspecs = [
            (0, 2),       # tipreg
            (2, 10),      # date
            (10, 12),     # codbdi
            (12, 24),     # codneg
            (24, 27),     # tpmerc
            (27, 39),     # nomres
            (56, 69),     # preabe
            (69, 82),     # premax
            (82, 95),     # premin
            (95, 108),    # premed
            (108, 121),   # preult
            (147, 152),   # totneg
            (152, 170),   # quatot
            (170, 188),   # voltot
        ]

        names = [
            'tipreg', 'date', 'codbdi', 'codneg', 'tpmerc', 'nomres',
            'preabe', 'premax', 'premin', 'premed', 'preult',
            'totneg', 'quatot', 'voltot'
        ]

        try:
            df = pd.read_fwf(
                file_path,
                colspecs=colspecs,
                names=names,
                encoding='latin-1'
            )

            df = df[df['tipreg'] == 1]

            df['date'] = pd.to_numeric(df['date'], errors='coerce')
            df['totneg'] = pd.to_numeric(df['totneg'], errors='coerce')
            df['quatot'] = pd.to_numeric(df['quatot'], errors='coerce')
            df['voltot'] = pd.to_numeric(df['voltot'], errors='coerce')

            df['preabe'] = pd.to_numeric(df['preabe'], errors='coerce') / 100
            df['premax'] = pd.to_numeric(df['premax'], errors='coerce') / 100
            df['premin'] = pd.to_numeric(df['premin'], errors='coerce') / 100
            df['premed'] = pd.to_numeric(df['premed'], errors='coerce') / 100
            df['preult'] = pd.to_numeric(df['preult'], errors='coerce') / 100

            df = df.dropna(subset=['date', 'codneg', 'totneg'])

            df['codneg'] = df['codneg'].str.strip()

            print(f"Parsed {len(df)} records from COTAHIST")
            return df

        except Exception as e:
            print(f"Error parsing COTAHIST: {e}")
            return None