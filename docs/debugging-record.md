# E002: `String.split`で末尾の空列が消え、任意メモ付きCSV行を拒否する

## 目的

三列目のメモは任意であり、空文字でも列そのものは存在します。`"SKU-17,Tea,"`をインポートしたとき、サービスは`ProductImportRow("SKU-17", "Tea", "")`を受理・保存し、拒否件数を`0`に保つ必要があります。

## 実行環境と再現境界

このラボはJava 21、Maven、JUnit Jupiter 5.11.4だけを使います。フレームワーク、HTTP、ファイル、データベースは使いません。公開境界は`CsvImportService#importLine(String)`であり、直接の結果として`ImportOutcome`を、最終状態として`acceptedRows()`と`rejectedCount()`を別々に読みます。

固定入力`"SKU-17,Tea,"`には、SKU、商品名、空の任意メモという三つの列が含まれます。すべての入力と状態はインメモリで決定的なため、時刻、乱数、並行処理、`sleep`に依存しません。

## 最初に観測した事実

バグ状態はコミット[`4488fb6`](../commit/4488fb6)です。次のコマンドで、意図したアサーション差分を確認しました。

```bash
git checkout 4488fb6
mvn --batch-mode test -Dtest=CsvImportServiceTest
```

| 観測項目 | 期待 | 実際 | 根拠 |
| --- | --- | --- | --- |
| 直接結果 | `ACCEPTED` | `REJECTED` | `CsvImportServiceTest` |
| 受理済み行 | 空メモの一行 | 空のリスト | `CsvImportService#acceptedRows()` |
| 拒否件数 | `0` | `1` | `CsvImportService#rejectedCount()` |
| 既定`split`の結果 | 三列を期待しがち | `"SKU-17"`, `"Tea"`の二列 | `StringSplitObservationTest` |
| 負のlimit付き`split`の結果 | 三列 | `"SKU-17"`, `"Tea"`, `""` | `StringSplitObservationTest` |

```text
末尾の任意メモが空でもCSV行は受理される ==> expected: <ACCEPTED> but was: <REJECTED>
受理済み行には空メモを持つ一行が残る ==> iterable lengths differ, expected: <1> but was: <0>
末尾空列だけで拒否件数を増やさない ==> expected: <0> but was: <1>
```

完全な失敗出力は[`evidence/01-bug-service-test-output.txt`](../evidence/01-bug-service-test-output.txt)に保存しています。直接の結果だけでなく、受理済み行と拒否件数を最終状態として分けて確認したため、単に列数検証のメッセージが誤っていた可能性は除外できます。

## 競合仮説と検証

| 仮説 | 確認方法 | 結果 |
| --- | --- | --- |
| 末尾メモが空の入力は業務ルール上不正である | 非空メモ行と空メモ行の両方を契約テストで比較する | 非空メモ行は受理される。今回の契約は空メモの受理を明示しているため棄却。 |
| カンマが正規表現として誤解釈され、分割位置が変わる | `split(",")`と`split(",", -1)`でトークン内容を比較する | 先頭二列の内容は一致し、差は末尾の空トークンだけであるため棄却。 |
| 既定limitによって末尾空トークンが破棄される | 同じ文字列で二つの`split`結果の長さと内容を観測する | 既定版は二列、負のlimit版は三列。採用。 |

## 確定した原因

バグ状態のサービスは、行を次のように分割していました。

```java
String[] columns = line.split(",");
```

`String.split(String regex)`は、limitが`0`の二引数版と同じです。limitが`0`の場合、末尾の空文字列は結果配列から除去されます。[1] したがって`"SKU-17,Tea,"`は`["SKU-17", "Tea"]`となり、三列要求の検証で`REJECTED`になります。

この失敗はCSVパーサー、ファイルI/O、データベース、フレームワーク固有の問題ではありません。`StringSplitObservationTest`が、同じ固定入力から生まれるトークン列を直接示しています。

## 最小修正

修正コミットは[`5e29910`](../commit/5e29910)です。変更は一行だけです。

```java
String[] columns = line.split(",", -1);
```

負のlimitを使うと、パターンは可能な限り適用され、末尾の空文字列を含むトークン列を返します。[1] このため、空のメモは三列目の`""`として保持され、三列検証と`ProductImportRow`の生成が契約どおりに進みます。

空トークンを後処理で補う実装や、列数検証を二列まで緩める実装は採用しませんでした。前者は区切り位置の意味を分散させ、後者は三列という入力仕様を失わせるためです。

## 回帰保証

### 再発防止テスト

最初に失敗した`trailingEmptyOptionalMemo_isAcceptedAndStoredAsEmptyString`はそのまま残しています。このテストは、`ACCEPTED`という直接結果、空メモを持つ受理済み行、拒否件数`0`という最終状態を別々に検証します。

| テスト | 回帰として守る契約 |
| --- | --- |
| `trailingEmptyOptionalMemo_isAcceptedAndStoredAsEmptyString` | 末尾の任意メモが空でも、行を受理・保存し、拒否件数を増やさない。 |
| `nonEmptyOptionalMemo_remainsAccepted` | 非空メモを持つ通常行を、修正後も受理し続ける。 |
| `defaultSplitDiscardsTrailingEmptyTokenWhileNegativeLimitPreservesIt` | 既定limitと負のlimitの標準ライブラリ上の差を、実装判断の証拠として維持する。 |

修正後の`mvn --batch-mode clean test`では、3テストがすべて成功しました。完全な出力は[`evidence/03-fixed-full-test-output.txt`](../evidence/03-fixed-full-test-output.txt)に保存しています。

## 再現手順

```bash
git checkout 4488fb6
mvn --batch-mode test -Dtest=CsvImportServiceTest
# expected: <ACCEPTED> but was: <REJECTED>

git checkout main
mvn --batch-mode clean test
# Tests run: 3, Failures: 0, Errors: 0
```

## スコープと注意点

この修正は、末尾の空列が意味を持つ、引用符なし・三列固定のCSV風入力に有効です。実際のCSVに引用符、エスケープ、カンマを含む値、改行、複数文字区切り、異なる文字コードが含まれる場合、`String.split`だけを汎用CSVパーサーとして使うべきではありません。

また、空文字と列の欠落が業務上同じ意味なら、末尾空列を保持する必要はありません。まず入力仕様で「末尾に区切りがある」ことと「列が存在しない」ことを区別すべきかを決めてください。

## References

[1] [Oracle: `String` — `split(String)` and `split(String, int)`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html)
