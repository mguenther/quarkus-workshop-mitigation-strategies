package workshop.quarkus.reactive;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;

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
                .onFailure().recoverWithUni(throwable -> Uni.createFrom().completionStage(message.nack(throwable)));
    }
}
