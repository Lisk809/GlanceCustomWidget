gradle-wrapper.jar 说明
=======================
此目录中应包含 gradle-wrapper.jar 文件，但由于文件较大（约 60KB）且为二进制文件，
无法在此直接生成。

获取方式（任选其一）：

方法 1：在 Android Studio 中打开项目
  → Android Studio 会自动下载并生成 gradlew 和 gradle-wrapper.jar

方法 2：通过已安装的 Gradle 生成
  gradle wrapper --gradle-version=8.7

方法 3：从已有 Android 项目复制
  cp /path/to/other-project/gradle/wrapper/gradle-wrapper.jar \
     ./gradle/wrapper/gradle-wrapper.jar
  cp /path/to/other-project/gradlew ./gradlew
  cp /path/to/other-project/gradlew.bat ./gradlew.bat
  chmod +x gradlew

⚠️ GitHub Actions 的 CI workflow 依赖 gradlew 和 gradle-wrapper.jar，
   请确保这两个文件已提交到仓库。
