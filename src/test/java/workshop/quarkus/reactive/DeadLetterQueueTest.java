package workshop.quarkus.reactive;

import io.quarkus.test.junit.QuarkusTest;
import net.mguenther.kafka.junit.ExternalKafkaCluster;
import net.mguenther.kafka.junit.KeyValue;
import org.apache.kafka.common.header.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.mguenther.kafka.junit.ObserveKeyValues.on;
import static net.mguenther.kafka.junit.SendKeyValues.to;
import static workshop.quarkus.reactive.Generator.filterOnTraceId;
import static workshop.quarkus.reactive.Generator.generateTestData;

@QuarkusTest
class DeadLetterQueueTest {

    private ExternalKafkaCluster kafka;

    @BeforeEach
    void prepareTest() {
        kafka = ExternalKafkaCluster.at("localhost:9092");
    }

    @Test
    @DisplayName("failing messages should be moved to the dead letter queue")
    void failingMessagesShouldBeMovedToTheDeadLetterQueue() throws InterruptedException {

        var data = generateTestData(10);
        var expectedFailing = (data.getKeyValues().size() / 2);

        kafka.send(to("events", data.getKeyValues()));

        kafka.observe(on("dlq-events", expectedFailing)
                .observeFor(15, TimeUnit.SECONDS)
                .filterOnHeaders(filterOnTraceId(data.getTraceId())));
    }

    @Test
    @DisplayName("inspect and compare the original message and its corresponding failed message")
    void inspectOriginalAndFailedMessage() throws InterruptedException {

        var data = generateTestData(2);

        kafka.send(to("events", data.getKeyValues()));

        List<KeyValue<String, String>> originalMessages = kafka.observe(on("events", 2)
                .observeFor(15, TimeUnit.SECONDS)
                // this includes data like topic-partition, offset, ...
                .includeMetadata()
                .filterOnHeaders(filterOnTraceId(data.getTraceId())));

        KeyValue<String, String> failedMessage = kafka.observe(on("dlq-events", 1)
                        .observeFor(15, TimeUnit.SECONDS)
                        // this includes data like topic-partition, offset, ...
                        .includeMetadata()
                        .filterOnHeaders(filterOnTraceId(data.getTraceId())))
                .stream()
                .findFirst()
                .orElseThrow();

        // hint: inspect and compare headers
        failedMessage.getHeaders();
        // hint: inspect and compare metadata
        failedMessage.getMetadata();

        // use a break point here to inspect
        // leverage method 'get' to transform byte[]-based header values into a human-readable String
        System.out.println();
    }

    private String get(final Headers headers, final String key) {
        return new String(headers.lastHeader(key).value(), StandardCharsets.UTF_8);
    }
}
