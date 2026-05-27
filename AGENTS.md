# Kestra Scrapy Plugin

## What

- Provides a single CLI task under `io.kestra.plugin.scrapy` that runs Scrapy spiders and CLI commands.
- Ships a pre-built Docker image (`ghcr.io/kestra-io/scrapy`) built from `dockerfiles/scrapy.Dockerfile`.

## Why

- Teams that run Scrapy spiders from Kestra otherwise reach for a generic `python.Commands` task with manual `pip install scrapy`, or build a bespoke image per project. Both routes lose plugin catalog discoverability, image consistency, and clear documentation.
- A typed task with the same ergonomics as other Kestra script plugins: `commands`, `beforeCommands`, `inputFiles`, `outputFiles`, `namespaceFiles`.
- Chain spider runs with downstream SQL transforms, notifications, or file uploads in one flow without rebuilding the environment each time.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `scrapy`

### Key Plugin Classes

- `io.kestra.plugin.scrapy.CLI` (extends `AbstractExecScript`, implements `RunnableTask<ScriptOutput>`, `NamespaceFilesInterface`, `InputFilesInterface`, `OutputFilesInterface`)

### Project Structure

```
plugin-scrapy/
├── dockerfiles/
│   └── scrapy.Dockerfile
├── src/main/java/io/kestra/plugin/scrapy/
├── src/test/java/io/kestra/plugin/scrapy/
├── build.gradle
└── README.md
```

## Local rules

- Base the wording on the implemented packages and classes, not on template README text.
- One CLI task wrapping a pre-built Docker image. No Java library dependencies beyond the standard Kestra plugin scaffold.

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
- https://docs.scrapy.org/en/latest/
