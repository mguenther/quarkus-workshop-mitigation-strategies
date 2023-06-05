# Hints

**Spoiler Alert**

We encourage you to work on the assignment yourself or together with your peers. However, situations may present themselves to you where you're stuck on a specific assignment. Thus, this document contains a couple of hints that ought to guide you through a specific task of the lab assignment.

In any case, don't hesitate to talk to us if you're stuck on a given problem!

## Task 1.1

The channel configuration uses `ignore` as failure strategy, as indicated by the following configuration:

```properties
mp.messaging.incoming.events.failure-strategy=ignore
```

Hence, the application will continue to process events from the channel, but will not apply any means of compensation for events that it is not able to process.

## Task 1.2

The Reactive Messaging specification supports the following configuration parameters for dead letter queues.

* `dead-letter-queue.topic`: This is the topic used to write records to that the application failed to process. The default value is `dead-letter-topic-${channel}`, where `${channel}` refers to the name of the channel for which the DLQ is configured.
* `dead-letter-queue.key.serializer`: This refers to the serializer used to write the record key of the message to the DLQ. By default, Reactive Messaging deduces the key serializer from the key deserializer.
* `dead-letter-queue.value.serializer`: This refers to the serializer used to write the record value of the message to the DLQ. By default, Reactive Messaging deduces the value serializer from the value deserializer.
* `dead-letter-queue.producer-client-id`: This refers to the client ID used by the Kafka produces that writes messages to the DLQ. The default setting for this is `kafka-dead-letter-topic-producer-${clientId}`, where `${clientId}` is the value obtained from the client ID of the Kafka consumer.

## Task 1.3

Extend the `application.properties` by providing the correct values for the aforementioned (cf. task 1.2) configuration properties.

```properties
mp.messaging.incoming.events.failure-strategy=dead-letter-queue
mp.messaging.incoming.events.dead-letter-queue.topic=dlq-events
```

## Task 1.4

For every unprocessable message that goes into a dead letter queue, the SmallRye Apache Kafka Connectors adds a couple of headers to ease error tracing. If you take a look at the headers, you'll notice that the following key-value-pairs have been added.

* `dead-letter-exception-class-name`: Refers to the Java class of the exception.
* `dead-letter-reason`: Contains an error message. This is the value of `Throwable.getMessage()`.
* `dead-letter-topic`: Refers to the topic that the message originated from
* `dead-letter-partition`: Refers to the topic-partition that the message originated from
* `dead-letter-offset`: Refers to the original offset of the message

If `throwable.getCause() != null`, additional headers will be added to the message:

* `dead-letter-cause-class-name`: Refers to the Java class of the cause for the outer exception
* `dead-letter-cause`: Contains an error message.

The metadata itself differs of course, as the message written to the dead letter queue is assigned a new topic-partition within the DLQ and the underlying record is stored anew, which results in a new offset.

The message itself is not altered, so both key and value retain the original data.

## Task 1.5

Yes, this is possible. The method `message.nack(Throwable)` has the overloaded counterpart `message.nack(Throwable, Metadata)` that allows to change certain metadata. Changing the key of the record is possible, as the following example code suggests.

```java
message.nack(throwable, Metadata.of(
  OutgoingKafkaRecordMetadata.builder()
    .withKey("my-custom-key")
    .build()));
```

This can be useful if you have certain partitioning requirements for the DLQ that deviate from the ones that you employ on the original topic.

## Task 1.6

No, this is neither recommended nor possible.

## Task 1.7

Yes, this is possible by means of the same mechanism that we've seen earlier in task 1.5. You can add custom headers to the message that goes into the DLQ. This won't override the standard ones that we've seen during task 1.4; they will still be present for every message in the DLQ.

```java
message.nack(throwable, Metadata.of(
  OutgoingKafkaRecordMetadata.builder()
     .withHeaders(new RecordHeaders().add("warning", "Critical pressure loss!".getBytes(StandardCharsets.UTF_8)))
    .build()));
```

## Task 2

We'll discuss your design with your peers and talk about possible solutions to the problem.

## Task 3.1

There is no error with the code per se, but it can lead to undesired results due to the use of the `@Retry` annotation in conjunction with reactive messaging.

Observations:

* The method `process` is annotated with `@Incoming` and `@Outgoing`, meaning it's consuming from the `retries` channel and producing to the `retries-out` channel.
* The `@Retry` annotation means the method will be retried up to 5 times with a delay of 100 ms in between retries if an exception occurs.
* `IncomingKafkaRecord` as well as `OutgoingKafkaRecord` are used for message consumption and production, allowing access and manipulation of metadata like headers.
* The `IllegalArgumentException` is thrown 10% (estimation) of the times.

While there is no syntax error, the `@Retry` annotation might not behave as you would expect. The retry mechanism will kick in only if an exception is thrown during the **subscription** to the stream. In the case of a failure during the **processing** of a specific message (i.e., when `IllegalArgumentException` is thrown), the stream will be considered as failed, and no further processing will occur.

## Task 3.2

The `@Retry` annotation from MicroProfile Fault Tolerance and the retry mechanism from Mutiny both aim to provide retry capabilities, but they do it in slightly different contexts.

The `@Retry` annotation from MicroProfile Fault Tolerance is designed to work with the **traditional imperative programming model**. When a method marked with `@Retry` throws an exception, the mechanism catches the exception and tries to invoke the method again for a certain number of times.

On the other hand, Mutiny provides its own set of failure handling methods like `onFailure().retry().atMost(n)`, designed to work in a **reactive programming model**. If the creation of a `Uni` or `Multi` fails, these mechanisms are designed to catch the failure and retry.

Mixing both in the same reactive pipeline can lead to confusion and unexpected behaviors because they have different strategies on error handling. For instance, using `@Retry` in a method returning a `Uni` or `Multi` would only have an effect if the assembly of the `Uni` or the `Multi` pipeline throws an exception, not when an asynchronous failure is propagated through the pipeline.

So, in general, it is recommended to stick with one or the other based on the programming model you are working with. If you are working with reactive streams (i.e., methods returning `Uni` or `Multi`), using Mutiny's built-in failure recovery mechanisms would be the more suitable choice.

## Task 3.3

The following implementation integrates the retry mechanism from Mutiny properly into the reactive pipeline.

```java
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
```
