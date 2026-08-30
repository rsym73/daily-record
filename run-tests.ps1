# 一键跑测试：`pwsh ./run-tests.ps1`（在仓库根目录）
$root = $PSScriptRoot
$localJdk = Join-Path $root '.toolchain\jdk'
if (Test-Path $localJdk) { $env:JAVA_HOME = $localJdk }
& (Join-Path $root 'gradlew.bat') test --console=plain
