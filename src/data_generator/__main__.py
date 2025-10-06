"""
CLI entry point for market data generator.

Usage:
    python -m data_generator --days 5 --tickers 9
    python -m data_generator --output market_data --days 10 --tickers 12
"""

import argparse
from .pipeline import MarketDataPipeline


def main():
    parser = argparse.ArgumentParser(
        description='Generate synthetic market data for multiple tickers'
    )

    parser.add_argument(
        '--output',
        type=str,
        default='market_data',
        help='Output directory (default: market_data)'
    )

    parser.add_argument(
        '--tickers',
        type=int,
        default=9,
        help='Number of tickers to select by liquidity (default: 9)'
    )

    parser.add_argument(
        '--days',
        type=int,
        default=5,
        help='Number of trading days to generate (default: 5)'
    )

    parser.add_argument(
        '--ticks-per-min',
        type=int,
        default=120,
        help='Number of ticks per minute (default: 120)'
    )

    parser.add_argument(
        '--spread-mean',
        type=float,
        default=0.0015,
        help='Average bid-ask spread percentage (default: 0.0015)'
    )

    parser.add_argument(
        '--seed',
        type=int,
        default=42,
        help='Random seed for reproducibility (default: 42)'
    )

    args = parser.parse_args()

    tick_config = {
        'ticks_per_min': args.ticks_per_min,
        'spread_mean': args.spread_mean,
        'seed': args.seed
    }

    pipeline = MarketDataPipeline(
        output_dir=args.output,
        num_tickers=args.tickers,
        days=args.days,
        tick_config=tick_config
    )

    pipeline.run()


if __name__ == '__main__':
    main()