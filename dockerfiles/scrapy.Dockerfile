FROM python:3.11-slim

LABEL org.opencontainers.image.source="https://github.com/kestra-io/plugin-scrapy"
LABEL org.opencontainers.image.description="Scrapy runtime"

ENV PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN apt-get update && apt-get install -y --no-install-recommends \
      gcc build-essential libxml2-dev libxslt-dev ca-certificates curl \
 && rm -rf /var/lib/apt/lists/*

RUN pip install uv && uv pip install --system \
    "scrapy>=2.12" \
    itemadapter \
    scrapy-playwright \
    pillow \
    pandas

ENTRYPOINT ["/bin/sh", "-c"]
CMD ["python --version"]
