package jp.tonbiattack.debuglab.csv;

/**
 * SKU、商品名、任意メモからなる、引用符なしの簡易CSV入力行です。
 */
public record ProductImportRow(String sku, String name, String memo) {
}
