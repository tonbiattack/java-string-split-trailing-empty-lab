package jp.tonbiattack.debuglab.csv;

import java.util.ArrayList;
import java.util.List;

/**
 * 引用符を扱わない、三列固定のCSV風入力を受理するサービスです。
 */
public class CsvImportService {

    private static final int EXPECTED_COLUMN_COUNT = 3;

    private final List<ProductImportRow> acceptedRows = new ArrayList<>();
    private int rejectedCount;

    public ImportOutcome importLine(String line) {
        String[] columns = line.split(",", -1);
        if (columns.length != EXPECTED_COLUMN_COUNT) {
            rejectedCount++;
            return ImportOutcome.REJECTED;
        }

        acceptedRows.add(new ProductImportRow(columns[0], columns[1], columns[2]));
        return ImportOutcome.ACCEPTED;
    }

    public List<ProductImportRow> acceptedRows() {
        return List.copyOf(acceptedRows);
    }

    public int rejectedCount() {
        return rejectedCount;
    }
}
