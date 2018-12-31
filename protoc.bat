@echo off
pushd %~dps0
set SRC_DIR=src\main\java\com\lttldrgn\portochat\proto
if exist %SRC_DIR% (
    rmdir /S /Q %SRC_DIR%
)
mkdir %SRC_DIR%
"%~dps0\third_party\protoc\protoc.exe" --java_out src\main\java portochat.proto
popd
