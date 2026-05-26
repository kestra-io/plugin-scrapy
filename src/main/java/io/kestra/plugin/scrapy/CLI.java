package io.kestra.plugin.scrapy;

import java.util.List;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.InputFilesInterface;
import io.kestra.core.models.tasks.NamespaceFilesInterface;
import io.kestra.core.models.tasks.OutputFilesInterface;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.runners.TargetOS;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.scripts.exec.AbstractExecScript;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Run Scrapy spiders and CLI commands.",
    description = """
        Run a spider from a Scrapy project with `scrapy crawl`, or a standalone spider file with `scrapy runspider`, and capture the output as Kestra files."""
)
@Plugin(
    examples = {
        @Example(
            title = "Run a standalone spider file and capture the output as a Kestra file.",
            full = true,
            code = """
                id: scrapy_quotes
                namespace: company.team

                tasks:
                  - id: crawl
                    type: io.kestra.plugin.scrapy.CLI
                    inputFiles:
                      quotes_spider.py: |
                        import scrapy

                        class QuotesSpider(scrapy.Spider):
                            name = "quotes"
                            start_urls = ["https://quotes.toscrape.com"]
                            custom_settings = {"CLOSESPIDER_PAGECOUNT": 1}

                            def parse(self, response):
                                for quote in response.css("div.quote"):
                                    yield {
                                        "text": quote.css("span.text::text").get(),
                                        "author": quote.css("small.author::text").get(),
                                    }
                    commands:
                      - scrapy runspider quotes_spider.py -o quotes.jsonl
                    outputFiles:
                      - quotes.jsonl
                """
        ),
        @Example(
            title = "Crawl a full Scrapy project shipped inline as input files.",
            full = true,
            code = """
                id: scrapy_project_crawl
                namespace: company.team

                tasks:
                  - id: crawl
                    type: io.kestra.plugin.scrapy.CLI
                    inputFiles:
                      scrapy.cfg: |
                        [settings]
                        default = myproject.settings
                      myproject/__init__.py: ""
                      myproject/settings.py: |
                        BOT_NAME = "myproject"
                        SPIDER_MODULES = ["myproject.spiders"]
                        NEWSPIDER_MODULE = "myproject.spiders"
                        CLOSESPIDER_ITEMCOUNT = 20
                      myproject/spiders/__init__.py: ""
                      myproject/spiders/products.py: |
                        import scrapy

                        class ProductsSpider(scrapy.Spider):
                            name = "products"
                            start_urls = ["https://books.toscrape.com"]

                            def parse(self, response):
                                for b in response.css("article.product_pod"):
                                    yield {
                                        "title": b.css("h3 a::attr(title)").get(),
                                        "price": b.css("p.price_color::text").get(),
                                    }
                    commands:
                      - scrapy list
                      - scrapy crawl products -o output.json
                    outputFiles:
                      - output.json
                """
        ),
        @Example(
            title = "Scheduled daily crawl that captures the output as a Kestra file.",
            full = true,
            code = """
                id: scrapy_books_daily
                namespace: company.team

                triggers:
                  - id: daily
                    type: io.kestra.plugin.core.trigger.Schedule
                    cron: "0 6 * * *"

                tasks:
                  - id: crawl
                    type: io.kestra.plugin.scrapy.CLI
                    inputFiles:
                      spider.py: |
                        import scrapy

                        class BooksSpider(scrapy.Spider):
                            name = "books"
                            start_urls = ["https://books.toscrape.com"]
                            custom_settings = {"CLOSESPIDER_ITEMCOUNT": 20}

                            def parse(self, response):
                                for b in response.css("article.product_pod"):
                                    yield {
                                        "title": b.css("h3 a::attr(title)").get(),
                                        "price": b.css("p.price_color::text").get(),
                                    }
                    commands:
                      - scrapy runspider spider.py -o books.csv
                    outputFiles:
                      - books.csv

                  - id: create_table
                    type: io.kestra.plugin.jdbc.postgresql.Query
                    url: jdbc:postgresql://localhost:5432/postgres
                    username: postgres
                    password: "{{ secret('POSTGRES_PASSWORD') }}"
                    sql: CREATE TABLE IF NOT EXISTS raw_books (title TEXT, price TEXT);

                  - id: load_to_db
                    type: io.kestra.plugin.jdbc.postgresql.CopyIn
                    url: jdbc:postgresql://localhost:5432/postgres
                    username: postgres
                    password: "{{ secret('POSTGRES_PASSWORD') }}"
                    from: "{{ outputs.crawl.outputFiles['books.csv'] }}"
                    table: raw_books
                    format: CSV
                    header: true
                """
        )
    }
)
public class CLI extends AbstractExecScript implements RunnableTask<ScriptOutput>, NamespaceFilesInterface, InputFilesInterface, OutputFilesInterface {
    private static final String DEFAULT_IMAGE = "ghcr.io/kestra-io/scrapy";

    @Schema(
        title = "The Scrapy commands to run.",
        description = """
            For example `scrapy crawl <spider>` or `scrapy runspider <file>.py`."""
    )
    @NotNull
    @PluginProperty(group = "main")
    protected Property<List<String>> commands;

    @Schema(
        title = "The container image to run Scrapy in.",
        description = """
            Defaults to `ghcr.io/kestra-io/scrapy`, pre-installed with Scrapy and common extras. Override to pin a version or use a custom image."""
    )
    @Builder.Default
    @PluginProperty(group = "execution")
    protected Property<String> containerImage = Property.ofValue(DEFAULT_IMAGE);

    @Override
    protected DockerOptions injectDefaults(RunContext runContext, DockerOptions original) throws IllegalVariableEvaluationException {
        var builder = original.toBuilder();
        if (original.getImage() == null) {
            builder.image(runContext.render(this.getContainerImage()).as(String.class).orElse(null));
        }
        if (original.getEntryPoint() == null || original.getEntryPoint().isEmpty()) {
            builder.entryPoint(List.of());
        }
        return builder.build();
    }

    @Override
    public ScriptOutput run(RunContext runContext) throws Exception {
        TargetOS os = runContext.render(this.targetOS).as(TargetOS.class).orElse(null);

        return this.commands(runContext)
            .withInterpreter(this.interpreter)
            .withBeforeCommands(beforeCommands)
            .withBeforeCommandsWithOptions(true)
            .withCommands(this.commands)
            .withTargetOS(os)
            .run();
    }
}
