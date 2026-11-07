#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/../src"
mkdir -p vendor
if [ ! -d vendor/whisper ]; then git clone --depth 1 --branch v1.7.6 https://github.com/ggml-org/whisper.cpp.git vendor/whisper; fi
if [ ! -d vendor/llama ]; then git clone --depth 1 --branch b5046 https://github.com/ggml-org/llama.cpp.git vendor/llama; fi
# The two engines are separate CMake projects to isolate incompatible ggml revisions.
test "$(git -C vendor/whisper rev-parse HEAD)" = "a8d002cfd879315632a579e73f0148d06959de36" || { echo "Unexpected whisper revision" >&2; exit 1; }
test "$(git -C vendor/llama rev-parse HEAD)" = "74d4f5b041ad837153b0e90fc864b8290e01d8d5" || { echo "Unexpected llama revision" >&2; exit 1; }

if [ ! -f vendor/sherpa/jniLibs/arm64-v8a/libsherpa-onnx-c-api.so ]; then
 mkdir -p vendor/sherpa/include/sherpa-onnx/c-api
 curl --http1.1 -fL --retry 3 https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.12.14/sherpa-onnx-v1.12.14-android.tar.bz2 -o vendor/sherpa/android.tar.bz2
 tar xjf vendor/sherpa/android.tar.bz2 -C vendor/sherpa ./jniLibs/arm64-v8a
 curl -fL https://raw.githubusercontent.com/k2-fsa/sherpa-onnx/v1.12.14/sherpa-onnx/c-api/c-api.h -o vendor/sherpa/include/sherpa-onnx/c-api/c-api.h
fi
