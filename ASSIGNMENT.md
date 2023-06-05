# Lab Assignment

Your task is to extend a simple Kafka-based messaging component to support dead letter queues as well as retries as a mitigation strategy for messages that you are not able to process.

## Task #1: Dead Letter Queue

Have a look at class `DeadLetterAwareConsumer`. This class consumes messages from topic `events`. These messages are quite simple: They don't have a key, but only a `String`-based value which represents a number. For every even message offset the `DeadLetterAwareConsumer` raises an exception to generate an error.

1. Check the configuration of the `events` channel. What do you expect happens in case of an error given this configuration?

2. Familiarize yourself with the configuration parameters of a dead letter queue for a channel. What properties are available? What are their defaults? (cf. [https://quarkus.io/guides/kafka](https://quarkus.io/guides/kafka))

3. Implement a dead letter queue for channel `events`. The name of the dead letter queue should be `dead-letter-topic-events`. After you've successfully implemented the DLQ for this channel, the provided test cases in `DeadLetterQueueTest` should run successfully.

4. Compare the original message with its corresponding message in the dead letter queue. What can you observe? (you can use test method `inspectOriginalAndFailedMessage` in test class `DeadLetterQueueTest`).

5. Is it possible to alter the key of the message that goes into the dead letter queue? Why?

6. Is it possible to alter the value of the message that goes into the dead letter queue? Why?

7. Is it possible to alter the headers of the message that goes into the dead letter queue?

## Task #2: Sequenced Dead Letter Queue

We've talked a bit about use cases that require to thread or sequence messages that go into a DLQ. How would you implement such a sequenced DLQ? If it helps, start simple and restrict the number of application instances to 1. What do you need to consider if you want to implement the sequenced DLQ in a truly distributed setting with multiple application instances per service? 

This won't be a coding exercise, but rather a design discussion.

## Task #3: Retries

Retries are a way to deal with transient errors. They are best applied for messages that can be processed in isolation, and thus, don't have any dependencies on their order of consumption. Have a look at class `RetryingConsumer`. This class consumes messages from topic `retries`. The message structure is the same as in task 1. For every message received, the `RetryingConsumer` flips a coin to decide whether to process it correctly.

1. Have a look at the following code. What can you tell about the implementation? What is your expectation for the behavior of this `RetryingConsumer` at runtime?

```java
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
```

2. Discuss the differences between the `@Retry` annotation from MicroProfile Fault Tolerance and the retry mechanism from Mutiny.

3. How would you re-write that piece of code in a reactive way?

## That's it! You've done great!

You have completed all assignments. If you have any further questions or need clarification, please don't hesitate to reach out to us. We're here to help.