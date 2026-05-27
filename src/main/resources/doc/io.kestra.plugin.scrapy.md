# How to use the Scrapy plugin

Run Scrapy spiders and CLI commands inside a pre-built container from Kestra flows.

## Tasks

`CLI` runs one or more Scrapy commands — set `commands` (required list of Scrapy CLI commands such as `scrapy crawl <spider>` or `scrapy runspider <file>.py`). The default container image is `ghcr.io/kestra-io/scrapy`, which ships Scrapy plus scrapy-playwright, pandas, and pillow; override `containerImage` to pin a version or use a custom image. Use `beforeCommands` to install additional packages with `pip install` before the spider runs.

Use `namespaceFiles` to make spider code stored in a Kestra namespace available to the task, or `inputFiles` to pass files inline. Capture output with `outputFiles` — Scrapy supports `-o <file>.json`, `-o <file>.csv`, and other formats directly in the command. Apply [plugin defaults](https://kestra.io/docs/workflow-components/plugin-defaults) to share runner and image configuration across tasks.
