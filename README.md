# Notification Logger

端末に届く全アプリの通知をローカルに記録し、あとから検索・閲覧できるようにする個人用 Android アプリです。  
すべての記録データおよび画像は端末内のアプリ専用ストレージのみに保存され、外部ネットワークへの送信は一切行いません（`INTERNET` パーミッション非保持）。

## 主な機能

- **全アプリ通知の自動記録 (`NotificationListenerService`)**
  - 初回起動時および権限未付与時に、Android の「通知へのアクセス」設定画面へ直接誘導する導線を搭載
  - 投稿日時、パッケージ名、アプリ名、タイトル、本文（`BigTextStyle` / `InboxStyle` / `MessagingStyle` 対応）、カテゴリを記録
  - 通知に含まれる画像（大きなアイコン `LargeIcon`、`BigPictureStyle` 画像、`MessagingStyle` 添付画像）を内部ストレージ専用ディレクトリ（`files/notification_images/`）に PNG 保存し、レコードと紐付け
- **重複更新・不要通知の適切な処理**
  - 同一通知の更新は `sbn.key` と投稿時刻（`postTime`）の組み合わせで識別し、既存レコードを上書き更新
  - グループまとめ通知（`FLAG_GROUP_SUMMARY`）を自動除外。常駐・進行中通知（`FLAG_FOREGROUND_SERVICE` / `FLAG_ONGOING_EVENT`、着信・アラームを除く）もデフォルトで除外（設定から切替可能）
  - サービス接続時（`onListenerConnected`）のアクティブ通知同期、切断時の `requestRebind`、端末再起動・アプリ更新時（`BOOT_COMPLETED` / `LOCKED_BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`）の再バインド要求によりバックグラウンド稼働の復旧を考慮
- **閲覧・検索・管理 UI（Jetpack Compose + Material 3）**
  - 新しい順の通知一覧（アプリアイコン・アプリ名・タイトル・本文プレビュー・時刻・保存画像サムネイル表示）
  - アプリ単位の絞り込み（フィルタチップおよびアプリ一覧シート）とキーワード検索
  - 詳細画面（全文の選択・クリップボードコピー、保存画像の拡大プレビュー、メタ情報確認）
  - レコードの個別削除・全削除（紐づく内部ストレージ画像ファイルも同時に削除）
  - **外観テーマ切替**: システム連動 / ライト / ダーク の 3 モード切替対応
- **JSON エクスポート**
  - Android 共有シート（`FileProvider` + `Intent.ACTION_SEND`）経由で記録を JSON ファイルとしてエクスポート可能
  - 将来の他デバイス転送実装の土台として、画像は端末固有パスを含めずファイル名（`fileName`）のみで参照する構造を採用

## 技術スタック・仕様

- **パッケージ名**: `com.mushroom.notificationlogger`
- **言語**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **ローカル DB**: Room (`notifications` / `notification_images`) + DataStore Preferences
- **シリアライズ**: `kotlinx-serialization-json`
- **対応 SDK**: `minSdk = 26` (Android 8.0) / `targetSdk = 37`

## ビルド方法

### 前提条件
- JDK 17 以上（JDK 21 推奨）
- Android SDK（API 37 Platform / Build-Tools 37.0.0）

`ANDROID_HOME` 環境変数を設定するか、プロジェクトルートに `local.properties`（`sdk.dir=...`）を配置した状態で以下のコマンドを実行してください。

### デバッグ APK のビルド

```bash
# macOS / Linux
./gradlew assembleDebug

# Windows (PowerShell / Command Prompt)
.\gradlew.bat assembleDebug
```

生成される APK:
`app/build/outputs/apk/debug/app-debug.apk`

### ユニットテストの実行

```bash
./gradlew testDebugUnitTest
```

## 作者・開発クレジット

- **作者**: mushroom1729-xperia
- **開発体制**: 本アプリは **mushroom1729-xperia** と AI（**Gemini / Muse**）との共同開発によって設計・実装されました。
