package workshop.quarkus.reactive;

import io.quarkus.test.junit.QuarkusTest;
import net.mguenther.kafka.junit.ExternalKafkaCluster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static net.mguenther.kafka.junit.ObserveKeyValues.on;
import static net.mguenther.kafka.junit.SendKeyValues.to;
import static workshop.quarkus.reactive.Generator.filterOnTraceId;
import static workshop.quarkus.reactive.Generator.generateTestData;

@QuarkusTest
class RetryTest {

    private ExternalKafkaCluster kafka;

    @BeforeEach
    void prepareTest() {
        kafka = ExternalKafkaCluster.at("localhost:9092");
    }

    @Test
    @DisplayName("the outgoing topic should contain at least 95% of messages")
    void theOutgoingTopicShouldContainAtLeast95PercentOfMessages() throws InterruptedException {

        var data = generateTestData(250);
        var max = data.getKeyValues().size();
        // The RetryingConsumer discards 10% of messages, so if we retries work as intended,
        // we're expecting more than 90% of messages in the outgoing topic. For this test,
        // we'll assume that the retry mechanism is able to process more than 95% of incoming
        // messages.
        var expected = max - (int) (((double) max / 100.0) * 5.0);

        kafka.send(to("retries", data.getKeyValues()));

        kafka.observe(on("retries-out", expected)
                .observeFor(30, TimeUnit.SECONDS)
                .filterOnHeaders(filterOnTraceId(data.getTraceId())));
    }
}
