# Minecraft 1.20.1 Forge Mod テンプレート

このフォルダは **Minecraft 1.20.1 + Forge 47.4.10** 用の公式MDKをベースにした、すぐに使えるMod開発プロジェクトです。

## 含まれている内容

- サンプルブロック (`example_block`)
- サンプルアイテム（食べ物） (`example_item`)
- カスタムクリエイティブタブ
- Configシステム
- クライアント/サーバーイベントのサンプル

## 必要な環境

- **JDK 17** 以上（推奨: Eclipse Temurin 17 または 21）
- IntelliJ IDEA または Eclipse（推奨: IntelliJ IDEA Community）

## セットアップ手順

1. このフォルダを好きな場所にコピーしてください（パスに日本語やスペースを含めないことを推奨）。
2. IntelliJ IDEA でこのフォルダを **Open** してください。
3. Gradleの同期を待ちます。
4. ターミナルで以下を実行（またはIDEのGradleタブから）:
   ```
   ./gradlew genIntellijRuns
   ```
5. `runClient` を実行してMinecraftを起動し、Modが動作することを確認してください。

## ビルド方法

```bash
./gradlew build
```

完成したModのJARは `build/libs/` に生成されます。  
このJARをMinecraftの `mods` フォルダに入れて、Forge 1.20.1 環境で遊べます。

## カスタマイズのポイント

### 1. 名前を変更する

`gradle.properties` を編集:

```
mod_id=mymod
mod_name=My Cool Mod
mod_version=1.0.0
mod_group_id=com.yourname.mymod
mod_authors=あなたの名前
mod_description=これは私の最初のModです！
```

あわせて、Javaのパッケージ名と `@Mod` の値も変更してください。

### 2. アイテム・ブロックを追加する

`ExampleMod.java` を参考に `DeferredRegister` を使って登録してください。

### 3. テクスチャを追加する

`src/main/resources/assets/examplemod/textures/` 以下に配置し、モデルJSONを作成します。

## 公式ドキュメント

- https://docs.minecraftforge.net/en/1.20.1/
- https://docs.minecraftforge.net/en/1.20.1/gettingstarted/

何か機能を追加したい場合は、具体的に教えてください！（例: 新しい鉱石、ツール、エンティティなど）
