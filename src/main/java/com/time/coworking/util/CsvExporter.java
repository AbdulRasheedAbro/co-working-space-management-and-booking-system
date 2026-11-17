package com.time.coworking.util;

import com.time.coworking.dto.ReportRow;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class CsvExporter {

    private CsvExporter() {}

    /**
     * Exports a list of ReportRow to a CSV file.
     * Column order is derived from the first row's key order.
     */
    public static File export(List<ReportRow> rows, File outFile) throws IOException {
        if (rows == null || rows.isEmpty()) {
            throw new IOException("No data to export");
        }

        // Derive column order from first row (LinkedHashMap preserves insertion order)
        List<String> columns = List.copyOf(rows.get(0).values().keySet());

        try (FileWriter fw = new FileWriter(outFile);
             CSVPrinter printer = new CSVPrinter(fw,
                     CSVFormat.DEFAULT.builder()
                             .setHeader(columns.toArray(String[]::new))
                             .build())) {

            for (ReportRow row : rows) {
                Map<String, Object> values = row.values();
                Object[] cells = new Object[columns.size()];
                for (int i = 0; i < columns.size(); i++) {
                    Object v = values.get(columns.get(i));
                    cells[i] = v == null ? "" : v.toString();
                }
                printer.printRecord(cells);
            }
        }

        return outFile;
    }
}