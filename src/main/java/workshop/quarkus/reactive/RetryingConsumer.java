package workshop.quarkus.reactive;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import io.smallrye.reactive.messaging.kafka.OutgoingKafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@ApplicationScoped
public class RetryingConsumer {

    @Incoming("retries")
    @Outgoing("retries-out")
    public Uni<OutgoingKafkaRecord<Object, String>> streamProcessor(IncomingKafkaRecord<Object, String> record) {
        return Uni.createFrom().item(record)
                .onItem().transform(Unchecked.function(m -> {
                    if (Math.random() <= 0.1) {
                        throw new IllegalArgumentException("Something terrible just happened");
                    }
                    final var traceId = header(m.getHeaders(), "trace");
                    return OutgoingKafkaRecord.from(record).withHeader("trace", traceId);
                }))
                .onItem().invoke(record::ack)
                .onFailure().retry().withBackOff(Duration.ofMillis(100)).atMost(5);
    }

    private String header(final Headers headers, final String key) {
        return new String(headers.lastHeader(key).value(), StandardCharsets.UTF_8);
    }
}
