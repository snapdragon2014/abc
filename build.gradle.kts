name: Build Mod
on:
  push:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Find and unzip the mod project
        run: |
          mkdir -p mod
          echo "Repodaki dosyalar:"
          ls -la
          find . -iname "*.zip" -not -path "./mod/*" -print0 | while IFS= read -r -d '' z; do
            echo "Zip bulundu: $z"
            unzip -o "$z" -d mod < /dev/null || true
          done
          G=$(find mod -name build.gradle.kts | head -1)
          if [ -z "$G" ]; then
            echo "HATA: build.gradle.kts bulunamadi. Mod zip dosyasi repoda yok ya da bozuk."
            exit 1
          fi
          D=$(dirname "$G")
          echo "Proje klasoru: $D"
          echo "D=$D" >> "$GITHUB_ENV"

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build
        working-directory: ${{ env.D }}
        run: |
          chmod +x gradlew
          ./gradlew build --stacktrace

      - name: Upload mod JAR
        uses: actions/upload-artifact@v4
        with:
          name: PirateShopAccess-mod
          path: ${{ env.D }}/build/libs/*.jar
