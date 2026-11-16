package io.kestra.plugin.scrapy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.executions.LogEntry;
import io.kestra.core.models.property.Property;
import io.kestra.core.queues.QueueFactoryInterface;
import io.kestra.core.queues.QueueInterface;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import reactor.core.publisher.Flux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KestraTest
public class CLITest {

    @Inject
    RunContextFactory runContextFactory;

    @Inject
    @Named(QueueFactoryInterface.WORKERTASKLOG_NAMED)
    private QueueInterface<LogEntry> logQueue;

    @Test
    void scrapyVersion() throws Exception {
        List<LogEntry> logs = new CopyOnWriteArrayList<>();
        Flux<LogEntry> receive = TestsUtils.receive(logQueue, l -> logs.add(l.getLeft()));

        var task = CLI.builder()
            .id("scrapy-version-" + UUID.randomUUID())
            .type(CLI.class.getName())
            .commands(
                Property.ofValue(
                    List.of(
                        "scrapy version",
                        "echo 'Scrapy version check completed!'"
                    )
                )
            )
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, task, Map.of());
        ScriptOutput run = task.run(runContext);

        assertThat(run.getExitCode(), is(0));

        TestsUtils.awaitLog(logs, log -> log.getMessage() != null && log.getMessage().contains("Scrapy version check completed!"));
        receive.blockLast();
        assertThat(logs.stream().anyMatch(log -> log.getMessage() != null && log.getMessage().contains("Scrapy version check completed!")), is(true));
    }

    @Test
    void runspider() throws Exception {
        List<LogEntry> logs = new CopyOnWriteArrayList<>();
        Flux<LogEntry> receive = TestsUtils.receive(logQueue, l -> logs.add(l.getLeft()));

        var task = CLI.builder()
            .id("scrapy-runspider-" + UUID.randomUUID())
            .type(CLI.class.getName())
            .commands(Property.ofValue(List.of("scrapy runspider quotes_spider.py -o quotes.jsonl -s CLOSESPIDER_PAGECOUNT=1 -s LOG_LEVEL=INFO")))
            .inputFiles(Map.of("quotes_spider.py", """
                import scrapy

                class QuotesSpider(scrapy.Spider):
                    name = "quotes"
                    start_urls = ["https://quotes.toscrape.com"]

                    def parse(self, response):
                        for quote in response.css("div.quote"):
                            yield {
                                "text": quote.css("span.text::text").get(),
                                "author": quote.css("small.author::text").get(),
                            }
                """))
            .outputFiles(Property.ofValue(List.of("quotes.jsonl")))
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, task, Map.of());
        ScriptOutput run = task.run(runContext);

        assertThat(run.getExitCode(), is(0));
        assertThat(run.getOutputFiles().containsKey("quotes.jsonl"), is(true));

        receive.blockLast();
    }

    @Test
    void scrapyHelp() throws Exception {
        List<LogEntry> logs = new CopyOnWriteArrayList<>();
        Flux<LogEntry> receive = TestsUtils.receive(logQueue, l -> logs.add(l.getLeft()));

        var task = CLI.builder()
            .id("scrapy-help-" + UUID.randomUUID())
            .type(CLI.class.getName())
            .commands(
                Property.ofValue(
                    List.of(
                        "scrapy --help",
                        "echo 'Scrapy help command completed!'"
                    )
                )
            )
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, task, Map.of());
        ScriptOutput run = task.run(runContext);

        assertThat(run.getExitCode(), is(0));

        TestsUtils.awaitLog(logs, log -> log.getMessage() != null && log.getMessage().contains("Scrapy help command completed!"));
        receive.blockLast();
        assertThat(logs.stream().anyMatch(log -> log.getMessage() != null && log.getMessage().contains("Scrapy help command completed!")), is(true));
    }

    @Test
    void defaultImageDropsRootThroughEntrypoint() throws Exception {
        assertThat(entryPoint(CLI.builder(), null, null), is(CLI.DEFAULT_IMAGE_ENTRYPOINT));
    }

    @Test
    void defaultImageEntrypointFallsBackToTheCommandWhenScriptIsMissing() {
        assertThat(CLI.DEFAULT_IMAGE_ENTRYPOINT, is(List.of(
            "/bin/sh",
            "-c",
            "if [ -x /usr/local/bin/kestra-entrypoint.sh ]; then exec /usr/local/bin/kestra-entrypoint.sh \"$@\"; fi; exec \"$@\"",
            "kestra-entrypoint"
        )));
    }

    @Test
    void defaultImageWithTagOrDigestDropsRootThroughEntrypoint() throws Exception {
        assertThat(entryPoint(CLI.builder().containerImage(Property.ofValue("ghcr.io/kestra-io/scrapy:1.2.3")), null, null), is(CLI.DEFAULT_IMAGE_ENTRYPOINT));
        assertThat(entryPoint(CLI.builder().containerImage(Property.ofValue("ghcr.io/kestra-io/scrapy@sha256:abc")), null, null), is(CLI.DEFAULT_IMAGE_ENTRYPOINT));
    }

    @Test
    void customImageKeepsEmptyEntrypoint() throws Exception {
        assertThat(entryPoint(CLI.builder().containerImage(Property.ofValue("my-registry:5000/scrapy-custom:1")), null, null), is(List.of()));
        assertThat(entryPoint(CLI.builder().containerImage(Property.ofValue("ghcr.io/kestra-io/scrapy-other")), null, null), is(List.of()));
    }

    @Test
    void userDefinedEntrypointIsKept() throws Exception {
        assertThat(entryPoint(CLI.builder(), List.of("/custom.sh"), null), is(List.of("/custom.sh")));
    }

    @Test
    void explicitUserKeepsEmptyEntrypoint() throws Exception {
        assertThat(entryPoint(CLI.builder(), null, "root"), is(List.of()));
    }

    private List<String> entryPoint(CLI.CLIBuilder<?, ?> builder, List<String> entryPoint, String user) throws Exception {
        var task = builder
            .id("scrapy-defaults-" + UUID.randomUUID())
            .type(CLI.class.getName())
            .commands(Property.ofValue(List.of("scrapy version")))
            .build();
        var runContext = TestsUtils.mockRunContext(runContextFactory, task, Map.of());
        var options = DockerOptions.builder().entryPoint(entryPoint).user(user).build();

        return task.injectDefaults(runContext, options).getEntryPoint();
    }
}
