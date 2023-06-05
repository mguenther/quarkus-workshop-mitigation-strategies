package workshop.quarkus.reactive;

import net.mguenther.kafka.junit.KeyValue;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Generator {

    public static TestData<String, String> generateTestData(final int howMany) {
        return generateTestData(howMany, generateTraceId());
    }

    public static TestData<String, String> generateTestData(final int howMany, final String traceId) {
        final List<KeyValue<String, String>> keyValues = Stream.iterate(0, k -> k + 1)
                .limit(howMany)
                .map(String::valueOf)
                .map(stringValue -> {
                    final var kv = new KeyValue<String, String>(null, stringValue);
                    kv.addHeader("trace", traceId, StandardCharsets.UTF_8);
                    return kv;
                })
                .collect(Collectors.toList());
        return new TestData<>(keyValues, traceId);
    }

    public static String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    public static Predicate<Headers> filterOnTraceId(final String traceId) {
        return headers -> new String(headers.lastHeader("trace").value()).equals(traceId);
    }
}
