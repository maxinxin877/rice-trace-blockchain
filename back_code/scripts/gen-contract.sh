#!/usr/bin/env bash
# =====================================================================
# 生成 Evidence 存证合约的 Java 包装类（用 FISCO BCOS Java SDK 的 CodeGen）
# 适用场景：修改了 Evidence.sol 后重新生成，替换 bcos/contract/Evidence.java
# 要求：Linux / WSL / MacOS（CodeGen 内置 solc 编译器），已下载控制台
# 用法：bash scripts/gen-contract.sh <console目录>
#   例：bash scripts/gen-contract.sh ~/fisco/console
# =====================================================================
set -e

CONSOLE_DIR="${1:?用法: bash scripts/gen-contract.sh <console目录>}"
SOL_FILE="src/main/resources/contracts/Evidence.sol"
OUT_DIR="src/main/java"
PACKAGE="com.itheima.qukuailian.bcos.contract"

if [ ! -f "$SOL_FILE" ]; then
  echo "未找到合约文件: $SOL_FILE（请在项目根目录执行）"
  exit 1
fi

cd "$CONSOLE_DIR"
java -cp "conf/:libs/*:apps/*" org.fisco.bcos.sdk.codegen.CodeGen \
  -s "$OLDPWD/$SOL_FILE" \
  -p "$PACKAGE" \
  -o "$OLDPWD/$OUT_DIR"

echo "生成完成，请检查 $OUT_DIR/$PACKAGE/Evidence.java"
