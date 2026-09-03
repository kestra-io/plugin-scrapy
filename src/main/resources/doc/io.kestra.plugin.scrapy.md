# How to use the Scrapy plugin

Run Scrapy spiders and CLI commands inside a pre-built container from Kestra flows.

## Tasks

`CLI` runs one or more Scrapy commands — set `commands` (required list of Scrapy CLI commands such as `scrapy crawl <spider>` or `scrapy runspider <file>.py`). The default container image is `ghcr.io/kestra-io/scrapy`, which ships Scrapy plus scrapy-playwright, pandas, and pillow; override `containerImage` to pin a version or use a custom image. Use `beforeCommands` to install additional packages with `pip install` before the spider runs.

Use `namespaceFiles` to make spider code stored in a Kestra namespace available to the task, or `inputFiles` to pass files inline. Capture output with `outputFiles` — Scrapy supports `-o <file>.json`, `-o <file>.csv`, and other formats directly in the command. Set runner and image configuration on each task.

## Notes

- Scrapy output formats (`-o file.json`, `-o file.csv`) write to the task working directory; declare them in `outputFiles` to make them available downstream.
- Use `beforeCommands` for extra dependencies, for example `pip install scrapy-splash`.
- Pin `containerImage` to a tagged image for reproducible runs.
