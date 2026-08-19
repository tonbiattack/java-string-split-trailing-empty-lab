package jp.tonbiattack.debuglab.csv;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class StringSplitObservationTest {

    @Test
    void defaultSplitDiscardsTrailingEmptyTokenWhileNegativeLimitPreservesIt() {
        String line = "SKU-17,Tea,";

        String[] defaultSplit = line.split(",");
        String[] preservingSplit = line.split(",", -1);

        assertAll(
                () -> assertArrayEquals(
                        new String[]{"SKU-17", "Tea"},
                        defaultSplit,
                        "1引数のsplitは末尾の空トークンを返さない"),
                () -> assertArrayEquals(
                        new String[]{"SKU-17", "Tea", ""},
                        preservingSplit,
                        "負のlimitを指定すると末尾の空トークンも返す")
        );
    }
}
