package pwp.client.utils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public final class Tuple<K, V> {
    private final K a;
    private final V b;

    public static <K, V> Tuple<K, V> of(K a, V b) {
        return new Tuple<>(a, b);
    }
}
