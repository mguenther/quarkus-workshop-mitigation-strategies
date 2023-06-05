package workshop.quarkus.reactive;

import net.mguenther.kafka.junit.KeyValue;

import java.util.Collections;
import java.util.List;

public class TestData<K, V> {

    private final List<KeyValue<K, V>> keyValues;

    private final String traceId;

    public TestData(final List<KeyValue<K, V>> listOfKeyValues, final String traceId) {
        this.keyValues = listOfKeyValues;
        this.traceId = traceId;
    }

    public List<KeyValue<K, V>> getKeyValues() {
        return Collections.unmodifiableList(keyValues);
    }

    public String getTraceId() {
        return traceId;
    }
}
