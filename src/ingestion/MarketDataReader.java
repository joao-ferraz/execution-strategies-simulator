package ingestion;

import data.TickData;
import java.util.List;

public interface MarketDataReader {
    List<TickData> readTickData();
}
