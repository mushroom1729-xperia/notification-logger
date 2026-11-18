# タスク2: バッテリー最適化・バックグラウンド動作の強化

既存プロジェクト（Notification Logger、`com.mushroom.notificationlogger`）への機能追加です。既存の設計・UIパターン（バナー表示、設定ダイアログ、テーマ切替など）を踏襲してください。

## 追加内容

### 1. バッテリー最適化の状態確認と誘導
- `PowerManager.isIgnoringBatteryOptimizations()` で自アプリがバッテリー最適化の対象外になっているか確認する。
- 対象のまま（最適化が効いている）場合は、ホーム画面に既存の通知アクセスバナーと同形式の案内バナーを表示し、タップで `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`（自アプリの package URI 付き）へ遷移させる。遷移に失敗した場合は `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` にフォールバックする。
- 設定ダイアログにもバッテリー最適化の現在の状態（対象外 / 対象）を表示する。

### 2. バックグラウンド制限の確認
- API 28 以上で `ActivityManager.isBackgroundRestricted()` を確認し、制限されている場合はバナーまたは設定画面で警告し、アプリ情報画面への導線を用意する。

### 3. リスナー接続状態の可視化と復旧の強化
- `NotificationListenerService` の接続 / 切断状態をアプリ内で保持し、ホーム画面に状態を表示する（接続中は控えめに、切断中は警告として表示）。
- 切断時は `requestRebind` を行う。さらに WorkManager の定期ワーカー（6時間間隔程度）で接続状態とバッテリー最適化の状態を点検し、切断が続いていれば `requestRebind` を再試行する。
- WorkManager の依存追加は可。ただし **INTERNET 権限は絶対に追加しない**こと。

### 4. その他
- `versionCode` を 2、`versionName` を `1.1.0` に更新する。
- 追加した判定ロジック（最適化状態・制限状態の解釈など、テスト可能な部分）にはユニットテストを追加する。
- README の機能一覧と変更履歴（1.1.0）を更新する。
- `./gradlew assembleDebug testDebugUnitTest` が警告なく通る状態にする。

## 制約
- git の commit / push はしないこと（レビュー後に別途行う）。
- Room のスキーマは変更しないこと（今回は不要なはず）。
- ユーザーへの通知投稿（POST_NOTIFICATIONS）が必要になる設計にはせず、アプリ内表示で完結させること。
- 作業の最後に、実装した内容・ビルド結果・残課題を日本語で簡潔にまとめて報告すること。
