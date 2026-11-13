FROM python:3.11-slim

LABEL org.opencontainers.image.source="https://github.com/kestra-io/plugin-scrapy"
LABEL org.opencontainers.image.description="Scrapy runtime"

ENV PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN apt-get update && apt-get install -y --no-install-recommends \
      gcc build-essential libxml2-dev libxslt-dev ca-certificates curl \
 && rm -rf /var/lib/apt/lists/*

# Run as a non-root user (fixed UID/GID 1000) with a writable HOME.
RUN groupadd --gid 1000 kestra \
 && useradd --uid 1000 --gid kestra --create-home --home-dir /home/kestra --shell /bin/sh kestra

# User-owned virtualenv so that `pip install` in beforeCommands keeps working without root.
ENV VIRTUAL_ENV=/opt/venv \
    PATH=/opt/venv/bin:$PATH \
    PLAYWRIGHT_BROWSERS_PATH=/home/kestra/.cache/ms-playwright

RUN python -m venv /opt/venv \
 && pip install uv \
 && uv pip install \
    "scrapy>=2.12" \
    itemadapter \
    scrapy-playwright \
    pillow \
    pandas \
 && chown -R kestra:kestra /opt/venv

USER kestra
WORKDIR /home/kestra

ENTRYPOINT ["/bin/sh", "-c"]
CMD ["python --version"]
