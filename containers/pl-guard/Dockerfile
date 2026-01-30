# pl-guard/Dockerfile
FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

# System deps (minimal)
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
 && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt \ 
    --extra-index-url https://download.pytorch.org/whl/cpu

COPY app.py .
EXPOSE 8000

CMD ["bash", "-lc", "uvicorn app:app --host 0.0.0.0 --port 8000 --workers ${WORKERS:-1} --log-level ${LOG_LEVEL:-info}"]
