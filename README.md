# `String.split`が末尾の空列を捨て、CSVインポートが任意列を拒否する

Java標準ライブラリの`String.split`を題材に、**末尾が空の任意列を持つCSV風の一行が不正として拒否される**問題を、失敗するテスト、原因の直接観測、最小修正、回帰テストの順に追うデバッグ教材です。既定ブランチの`main`は成功状態に保ち、意図的に失敗する状態はGit履歴に独立して残します。

## この題材で守る契約

> `"SKU-17,Tea,"`を読み込む場合、空のメモを持つ`ProductImportRow("SKU-17", "Tea", "")`を受理・保存し、拒否件数を増やさない。

| 段階 | 実施内容 | 確認すること |
| --- | --- | --- |
| 再現 | 末尾メモが空の固定行をインポートする | 結果が`ACCEPTED`ではなく`REJECTED`となり、受理済み行が空・拒否件数が`1`となる |
| 観測 | 同じ文字列を二つの`split`呼び出しで分解する | 1引数版は二列、負のlimit版は空文字を含む三列を返す |
| 修正 | `split(",", -1)`を使う | 末尾の空トークンを保持したまま三列検証できる |
| 回帰防止 | 同じサービステストを再実行する | 空メモ行は受理され、非空メモ行も従来どおり受理される |

## 必要な環境

| 項目 | バージョン |
| --- | --- |
| JDK | 21 |
| Maven | 3.8以上 |
| テストランナー | JUnit Jupiter 5.11.4 |
| アプリケーションフレームワーク | 不使用 |

## 最短の開始手順

```bash
mvn --batch-mode clean test
```

検証済みの`main`では、3テストがすべて成功します。

## バグを再現する

```bash
git checkout 4488fb6
mvn --batch-mode test -Dtest=CsvImportServiceTest
# expected: <ACCEPTED> but was: <REJECTED>
# 受理済み行: expected 1, but was 0
# 拒否件数: expected 0, but was 1

git checkout main
mvn --batch-mode clean test
# Tests run: 3, Failures: 0, Errors: 0
```

バグコミットではコンパイルや設定ではなく、末尾空列を受理するという契約だけが失敗します。完全な出力は[`evidence/01-bug-service-test-output.txt`](evidence/01-bug-service-test-output.txt)に保存しています。

## 原因の要点

`String.split(String regex)`は、`limit`が`0`の二引数版と同じです。`limit`が`0`の場合、末尾の空文字列は結果から破棄されます。[1] したがって、`"SKU-17,Tea,".split(",")`は二列だけを返し、三列固定の入力検証が行を拒否します。

本教材では、末尾空列を意味のある「空のメモ」と定義しているため、負の`limit`を指定してすべてのトークンを残します。`String.split(regex, limit)`の`limit`が負の場合、パターンは可能な限り適用され、空文字列を含む結果を返します。[1]

## プロジェクト構成

```text
.
├── docs/
│   ├── debugging-record.md      # 観測・仮説・原因・修正・回帰保証
│   ├── novelty-report.md        # 既存Java記事との四軸比較
│   └── topic-brief.md           # 実装前に固定した契約と再現境界
├── evidence/
│   ├── 01-bug-service-test-output.txt
│   ├── 02-split-observation-output.txt
│   └── 03-fixed-full-test-output.txt
├── src/main/java/.../csv/
│   ├── CsvImportService.java
│   ├── ImportOutcome.java
│   └── ProductImportRow.java
└── src/test/java/.../csv/
    ├── CsvImportServiceTest.java
    └── StringSplitObservationTest.java
```

詳細な調査手順は[デバッグ記録](docs/debugging-record.md)、既存コンテンツとの差分は[題材重複調査レポート](docs/novelty-report.md)を参照してください。

## スコープ

この教材は、引用符を扱わない三列固定のCSV風入力だけを対象にします。カンマを含む値、引用符、改行、文字コード、ストリーミングファイルI/Oは対象外です。実運用のCSV仕様がより広い場合は、仕様に合うパーサーやライブラリを別途検討してください。

## References

[1] [Oracle: `String` — `split(String)` and `split(String, int)`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html)
