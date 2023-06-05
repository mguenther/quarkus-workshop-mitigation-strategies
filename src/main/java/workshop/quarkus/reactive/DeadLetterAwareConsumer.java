package workshop.quarkus.reactive;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Metadata;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class DeadLetterAwareConsumer {

    @Incoming("events")
    public Uni<Void> process(final IncomingKafkaRecord<String, String> message) {
        return Uni.createFrom().item(message)
                .onItem()
                .transform(Unchecked.function(m -> {
                    if (m.getOffset() % 2 == 0) {
                        throw new IllegalArgumentException("Something went terribly wrong!");
                    }
                    return m;
                }))
                .onItem().invoke(message::ack)
                .onItem().ignore().andContinueWithNull()
                .onFailure().recoverWithUni(throwable -> Uni.createFrom().completionStage(message.nack(new IllegalArgumentException("I'm wrapping the cause of the error"), Metadata.of(
                        OutgoingKafkaRecordMetadata.builder()
                                .withKey("my-custom-key")
                                .withHeaders(new RecordHeaders().add("warning", "Critical pressure loss!".getBytes(StandardCharsets.UTF_8)))
                                .build()))));
    }
}
