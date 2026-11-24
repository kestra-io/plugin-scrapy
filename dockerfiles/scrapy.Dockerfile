FROM python:3.11-slim

LABEL org.opencontainers.image.source="https://github.com/kestra-io/plugin-scrapy"
LABEL org.opencontainers.image.description="Scrapy runtime"

ENV PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN apt-get update && apt-get upgrade -y && apt-get install -y --no-install-recommends \
      gcc build-essential libxml2-dev libxslt-dev ca-certificates curl \
 && rm -rf /var/lib/apt/lists/*

# Unprivileged user (fixed UID/GID 1000) with a writable HOME. The container itself starts as root
# and kestra-entrypoint.sh drops to this user: Kestra creates the task working directory as root.
RUN groupadd --gid 1000 kestra \
 && useradd --uid 1000 --gid kestra --create-home --home-dir /home/kestra --shell /bin/sh kestra

# User-owned virtualenv so that `pip install` in beforeCommands keeps working without root.
ENV VIRTUAL_ENV=/opt/venv \
    PATH=/opt/venv/bin:$PATH \
    PLAYWRIGHT_BROWSERS_PATH=/home/kestra/.cache/ms-playwright

# The base image ships its own pip/setuptools/wheel in /usr/local, which the venv upgrade below never touches.
RUN /usr/local/bin/python -m pip install --no-cache-dir --upgrade pip "setuptools>=84.1.0" wheel

RUN python -m venv /opt/venv \
 && pip install --no-cache-dir --upgrade pip setuptools wheel uv \
 && uv pip install \
    "scrapy>=2.12" \
    itemadapter \
    scrapy-playwright \
    pillow \
    pandas \
 && chown -R kestra:kestra /opt/venv

COPY --chmod=0755 dockerfiles/kestra-entrypoint.sh /usr/local/bin/kestra-entrypoint.sh

WORKDIR /home/kestra

# SEC-0091: the container starts as root on purpose so kestra-entrypoint.sh can chown the root:root
# working dir created by the Kestra runner, then drops to UID 1000 with setpriv before running user commands.
# nosemgrep: dockerfile.security.missing-user-entrypoint.missing-user-entrypoint
ENTRYPOINT ["/bin/sh", "-c"]
# nosemgrep: dockerfile.security.missing-user.missing-user
CMD ["python --version"]
