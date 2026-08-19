package jp.tonbiattack.debuglab.csv;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class CsvImportServiceTest {

    @Test
    void trailingEmptyOptionalMemo_isAcceptedAndStoredAsEmptyString() {
        CsvImportService service = new CsvImportService();

        ImportOutcome outcome = service.importLine("SKU-17,Tea,");

        assertAll(
                () -> assertEquals(ImportOutcome.ACCEPTED, outcome,
                        "末尾の任意メモが空でもCSV行は受理される"),
                () -> assertIterableEquals(
                        List.of(new ProductImportRow("SKU-17", "Tea", "")),
                        service.acceptedRows(),
                        "受理済み行には空メモを持つ一行が残る"),
                () -> assertEquals(0, service.rejectedCount(),
                        "末尾空列だけで拒否件数を増やさない")
        );
    }

    @Test
    void nonEmptyOptionalMemo_remainsAccepted() {
        CsvImportService service = new CsvImportService();

        ImportOutcome outcome = service.importLine("SKU-18,Coffee,seasonal");

        assertAll(
                () -> assertEquals(ImportOutcome.ACCEPTED, outcome),
                () -> assertIterableEquals(
                        List.of(new ProductImportRow("SKU-18", "Coffee", "seasonal")),
                        service.acceptedRows()),
                () -> assertEquals(0, service.rejectedCount())
        );
    }
}
