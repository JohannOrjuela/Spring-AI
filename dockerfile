expose NGC_API_KEY=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
expose LOCAL_NIM_CACHE=~/.cache/nim
mkdir -p "$LOCAL_NIM_CACHE"
chmod -R 777 $LOCAL_NIM_CACHE
docker run -it --rm \
    --gpus all \
    --shm-size=16GB \
    -e NGC_API_KEY \
    -v "$LOCAL_NIM_CACHE:/opt/nim/.cache" \
    -p 8000:8000 \
    nvcr.io/nim/openai/gpt-oss-120b:latest

