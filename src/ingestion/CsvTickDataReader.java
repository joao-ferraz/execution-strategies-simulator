package ingestion;

import data.TickData;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

class CsvTickDataReader implements MarketDataReader {
    private static final String CSV_SPLIT_BY = ",";
    private final DateTimeFormatter dateFormat;
    private final String filePath;

    CsvTickDataReader(String filePath) {
        this.filePath = filePath;
        // Format matching CSV timestamps with optional timezone: "2025-10-24 13:03:00+00:00" or "2024-10-04 14:30:00"
        this.dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[XXX]");
    }

    @Override
    public List<TickData> readTickData() {
        List<TickData> tickDataList = new ArrayList<>();
        String line;
        int lineIndex = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            while ((line = br.readLine()) != null) {
                // Skip header row
                if (lineIndex == 0) {
                    lineIndex++;
                    continue;
                }

                String[] values = line.split(CSV_SPLIT_BY);
                if (values.length >= 6) {
                    try {
                        // CSV format: timestamp,bid,ask,trade_price,volume,side
                        Instant timestamp = parseTimestamp(values[0].trim());
                        double bid = Double.parseDouble(values[1].trim());
                        double ask = Double.parseDouble(values[2].trim());
                        double tradePrice = Double.parseDouble(values[3].trim());
                        double volume = Double.parseDouble(values[4].trim());
                        String side = values[5].trim();

                        TickData tickData = new TickData(timestamp, bid, ask, tradePrice, volume, side);
                        tickDataList.add(tickData);
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing line " + lineIndex + ": " + line);
                        e.printStackTrace();
                    }
                }
                lineIndex++;
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + filePath);
            e.printStackTrace();
        }

        System.out.println("Loaded " + tickDataList.size() + " ticks from " + filePath);
        return tickDataList;
    }

    private Instant parseTimestamp(String timestampStr) {
        // Handle timestamps with microseconds by truncating them
        if (timestampStr.contains(".")) {
            int plusIndex = timestampStr.indexOf('+');
            int minusIndex = timestampStr.lastIndexOf('-');
            // Check if there's a timezone offset (+ or - after the dot)
            if (plusIndex > timestampStr.indexOf('.')) {
                // Has timezone: "2025-10-24 13:03:00.500+00:00"
                String beforeMicros = timestampStr.substring(0, timestampStr.indexOf('.'));
                String timezone = timestampStr.substring(plusIndex);
                timestampStr = beforeMicros + timezone;
            } else if (minusIndex > 10) { // Make sure it's not the date separator
                // Has timezone with minus: "2025-10-24 13:03:00.500-05:00"
                String beforeMicros = timestampStr.substring(0, timestampStr.indexOf('.'));
                String timezone = timestampStr.substring(minusIndex);
                timestampStr = beforeMicros + timezone;
            } else {
                // No timezone: "2025-10-24 13:03:00.500000"
                timestampStr = timestampStr.substring(0, timestampStr.indexOf('.'));
            }
        }

        // Parse with timezone if present, otherwise assume UTC
        if (timestampStr.contains("+") || timestampStr.lastIndexOf('-') > 10) {
            return Instant.parse(timestampStr.replace(" ", "T"));
        } else {
            LocalDateTime localDateTime = LocalDateTime.parse(timestampStr, dateFormat);
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
    }
}
