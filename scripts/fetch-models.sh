#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p src/app/src/main/assets/models
check_sha() {
 if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | cut -d ' ' -f 1
 else shasum -a 256 "$1" | cut -d ' ' -f 1; fi
}
fetch() {
 name="$1"; url="$2"; sha="$3"
 path="src/app/src/main/assets/models/$name"
 if [ -f "$path" ] && [ "$(check_sha "$path")" = "$sha" ]; then echo "Verified $name"; return; fi
 if [ "${CHECK_ONLY:-0}" = 1 ]; then echo "Missing or corrupt embedded model: $name" >&2; exit 1; fi
 curl --http1.1 -fL --retry 3 --retry-all-errors -C - "$url" -o "$path.part"
 test "$(check_sha "$path.part")" = "$sha" || { echo "SHA256 mismatch: $name" >&2; exit 1; }
 mv "$path.part" "$path"
}
fetch 'asr.bin' 'https://huggingface.co/ggerganov/whisper.cpp/resolve/5359861c739e955e79d9a303bcbc70fb988958b1/ggml-small-q5_1.bin' 'ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb'
fetch 'llm.gguf' 'https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/9217f5db79a29953eb74d5343926648285ec7e67/qwen2.5-0.5b-instruct-q4_k_m.gguf' '74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db'
fetch 'embedding.onnx' 'https://github.com/k2-fsa/sherpa-onnx/releases/download/speaker-recongition-models/nemo_en_titanet_small.onnx' 'ad4a1802485d8b34c722d2a9d04249662f2ece5d28a7a039063ca22f515a789e'
if [ ! -f src/app/src/main/assets/models/segmentation.onnx ]; then
 if [ "${CHECK_ONLY:-0}" = 1 ]; then echo 'Missing segmentation model' >&2; exit 1; fi
 mkdir -p work-models
 curl --http1.1 -fL --retry 3 'https://github.com/k2-fsa/sherpa-onnx/releases/download/speaker-segmentation-models/sherpa-onnx-pyannote-segmentation-3-0.tar.bz2' -o work-models/segmentation.tar.bz2
 tar xjf work-models/segmentation.tar.bz2 -C work-models
 cp work-models/sherpa-onnx-pyannote-segmentation-3-0/model.onnx src/app/src/main/assets/models/segmentation.onnx
fi
test "$(check_sha src/app/src/main/assets/models/segmentation.onnx)" = '220ad67ca923bef2fa91f2390c786097bf305bceb5e261d4af67b38e938e1079'
