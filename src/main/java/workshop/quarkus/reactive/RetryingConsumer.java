package workshop.quarkus.reactive;

import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import io.smallrye.reactive.messaging.kafka.OutgoingKafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class RetryingConsumer {

    @Incoming("retries")
    @Outgoing("retries-out")
    @Retry(delay = 100, maxRetries = 5)
    public OutgoingKafkaRecord<Object, String> process(IncomingKafkaRecord<Object, String> record) {

        final var traceId = header(record.getHeaders(), "trace");

        if (Math.random() <= 0.1) {
            throw new IllegalArgumentException("Something terrible just happened");
        }

        return OutgoingKafkaRecord.from(record).withHeader("trace", traceId);
    }

    private String header(final Headers headers, final String key) {
        return new String(headers.lastHeader(key).value(), StandardCharsets.UTF_8);
    }
}
