package ingestion;

public class MarketDataReaderFactory {

    public static MarketDataReader createReader(String sourceType, String source) {
        switch (sourceType.toUpperCase()) {
            case "CSV":
                return new CsvTickDataReader(source);
            // Future: case "DB": return new DatabaseTickDataReader(source);
            // Future: case "API": return new ApiTickDataReader(source);
            default:
                throw new IllegalArgumentException("Unsupported source type: " + sourceType);
        }
    }
}
