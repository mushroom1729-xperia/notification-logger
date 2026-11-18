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
- **バッテリー最適化対策・バックグラウンド接続復旧の強化**
  - `PowerManager.isIgnoringBatteryOptimizations()` でバッテリー最適化の対象外状態を確認し、対象のままの場合はホーム画面の案内バナーおよび設定ダイアログから除外リクエスト画面（`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`、失敗時は `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` にフォールバック）へ誘導
  - API 28 以上で `ActivityManager.isBackgroundRestricted()` を確認し、制限時はホーム画面バナーおよび設定ダイアログで警告してアプリ情報画面（`ACTION_APPLICATION_DETAILS_SETTINGS`）へ誘導
  - `NotificationListenerService` の接続 / 切断状態をリアルタイムに保持し、ホーム画面に可視化（接続中は控えめに表示、切断中は警告バナー＋手動再接続ボタンを表示）
  - サービス接続時（`onListenerConnected`）のアクティブ通知同期、切断時の即時 `requestRebind`、端末再起動・アプリ更新時（`BOOT_COMPLETED` / `LOCKED_BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`）の再バインド要求に加え、WorkManager の定期ワーカー（6時間間隔）で接続・最適化状態を点検し切断継続時は `requestRebind` を自動再試行（通知投稿 `POST_NOTIFICATIONS` や `INTERNET` 権限は不使用）
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
- **ローカル DB / 設定**: Room (`notifications` / `notification_images`) + DataStore Preferences
- **バックグラウンド定期処理**: WorkManager (`androidx.work:work-runtime-ktx`)
- **シリアライズ**: `kotlinx-serialization-json`
- **バージョン**: `1.1.0` (`versionCode = 2`)
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

## 変更履歴

### 1.1.0
- `PowerManager.isIgnoringBatteryOptimizations()` によるバッテリー最適化状態の確認、ホーム画面案内バナーおよび除外リクエスト画面への誘導（フォールバック対応）、設定ダイアログでの状態表示を追加
- API 28 以上での `ActivityManager.isBackgroundRestricted()` によるバックグラウンド制限の確認・警告表示とアプリ情報画面への導線を追加
- `NotificationListenerService` の接続 / 切断状態のホーム画面表示（接続中は控えめ表示、切断中は警告表示）と再バインド導線を追加
- WorkManager の定期ワーカー（6時間間隔）による接続状態・バッテリー最適化状態の定期点検および切断継続時の `requestRebind` 自動再試行を追加
- バッテリー最適化・バックグラウンド制限・リスナー接続状態の判定ロジックに対するユニットテストを追加

### 1.0.0
- 初回リリース（全アプリ通知の自動記録、添付画像の内部ストレージ保存、一覧検索・アプリ単位フィルタリング、詳細表示、個別・全件削除、JSON エクスポート、テーマ切替）

## 作者・開発クレジット

- **作者**: mushroom1729-xperia
- **開発体制**: 本アプリは **mushroom1729-xperia** と AI（**Gemini / Muse**）との共同開発によって設計・実装されました。
