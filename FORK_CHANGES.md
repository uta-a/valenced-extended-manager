# FORK_CHANGES

valenced-extended-manager が上流 [Valenced/valenced-manager](https://github.com/Valenced/valenced-manager) のファイルに加えた変更の一覧。
上流をマージするときは、ここに挙げたファイルの衝突を確認する。

基点: `upstream/main` c6e5736（1.1.2）

| ファイル | 変更 | 理由 |
| --- | --- | --- |
| `app/build.gradle.kts` | `applicationId` を `app.valenced.manager` → `dev.uta.valencedextended.manager` | 上流の Manager と共存させるため。`namespace` と Kotlin パッケージは上流のまま |
| `app/build.gradle.kts` | `versionCode` を `<上流の算出値> * 100 + <ext の番号>` に（`1.1.2-ext.3` → 11203） | 同じ上流の版で fork のリリースを重ねても versionCode が増えるようにするため |
| `app/build.gradle.kts` | `MOD_NAME` を `Valenced Extended`、`REPO` を `uta-a/valenced-extended`、`ORG_LINK` を fork のリポジトリ、`MODDED_APP_PACKAGE_NAME` を `dev.uta.valencedextended.discord` に。`MANAGER_REPO` を追加 | パッチバンドルとモジュールを fork のリリースから取るため。パッチ済み Discord を公式版・上流 Valenced と共存させるため |
| `gradle.properties` | `patchesPublicKey` を fork の鍵に | fork のリリースのパッチバンドルはこの鍵で署名する |
| `app/src/main/java/app/valenced/manager/ui/viewmodel/home/HomeViewModel.kt` | Manager 自身の更新確認先を `Valenced/valenced-manager` → `BuildConfig.MANAGER_REPO` | fork のリリースで Manager を更新するため |
| `app/src/main/java/app/valenced/manager/installer/step/patching/PatchStep.kt` | パッチ後に `InstalledRelease.installing(bundle.release)` を呼ぶ（import 1 行＋1 行） | どのリリースで作った Discord かを覚えるため |
| `app/src/main/java/app/valenced/manager/ui/screen/home/HomeScreen.kt` | 対象の版にリリースのタグを併記。Discord の版が同じでもリリースが新しければボタンを「Update」に | fork のリリースは同じ Discord の版のままモジュールだけ更新されるため |
| `app/src/main/java/app/valenced/manager/updatechecker/worker/UpdateWorker.kt` | Discord の版が同じでもリリースが新しければ通知する | 同上 |
| `app/src/main/res/values/strings.xml` | `app_name` を `Valenced Extended Manager` に | 上流の Manager と見分けるため |
| `README.md` | 冒頭に fork の説明を追記 | fork 利用者向けの案内 |

## 新規（上流と衝突しない）

- `app/src/main/java/dev/uta/valencedextended/manager/InstalledRelease.kt` … インストール済みのリリースのタグを専用の SharedPreferences（`valenced_extended`）に覚える。インストールは確認画面で取り消せるので、パッケージの更新時刻がパッチ後になったときに確定する
- `.github/workflows/release-extended.yml` … 手動で実行して Manager のリリースを作る
- `.github/workflows/upstream-sync.yml` … 毎日上流 `main` を取り込む。衝突しなければ `sync/upstream` でビルドして main への PR を作り、衝突したら `upstream-sync` ラベルの Issue を立てる

## GitHub Actions

- 上流のワークフローはファイルを変えずに、リポジトリ側で無効にしている: `Release`（semantic-release。版の付け方と署名が fork と合わないため）、`Open a PR to main`（`dev` ブランチを使わないため）。`Build pull request` は有効
- `Release: Extended` を Actions から手動で実行してリリースする。版は `<上流の版>-ext.<n>`（例 `1.1.2-ext.1`）。シークレットは `KEYSTORE`（base64）・`KEYSTORE_PASSWORD`・`KEYSTORE_ENTRY_PASSWORD`、変数は `KEYSTORE_ENTRY_ALIAS`。鍵の原本はリポジトリの外に保管している。署名鍵を変えると既存のインストールを更新できなくなる
- `Upstream: Sync` が PR を作れるよう、Settings > Actions > General の「Allow GitHub Actions to create and approve pull requests」をオンにしている。PR を作れない場合は比較画面へのリンク付きの Issue を立てる

## リリースの流れ

1. [valenced-extended](https://github.com/uta-a/valenced-extended) で `Release: Extended` を実行する（例 `1.3.0-ext.1` / `10301`）。`app-release.apk`（モジュール）と `valenced.reseam`（パッチバンドル）が公開される
2. Manager はホーム画面を開いたときと定期チェックで最新リリースを読み、インストール済みのリリースより新しければ「Update」と通知を出す
3. Manager 自体を変えたときは、このリポジトリで `Release: Extended` を実行する。Manager は起動時に新しい版を見つけると更新ダイアログを出す
