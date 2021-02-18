package galyleo.shell;

import galyleo.util.ServiceProviderMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import lombok.NoArgsConstructor;

/**
 * {@link Magic} {@link Map}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor
public class MagicMap extends TreeMap<String,Magic> {
    private static final long serialVersionUID = 194836601918125315L;

    /** @serial */
    private final ServiceProviderMap<Magic> map =
        new ServiceProviderMap<>(Magic.class);

    { reload(); }

    /**
     * Reload the underlying {@link ServiceProviderMap} and add the
     * corresponding entries.
     *
     * @return  {@link.this}
     */
    public MagicMap reload() {
        map.reload().values().stream()
            .flatMap(v -> Stream.of(v.getMagicNames()).map(k -> Map.entry(k, v)))
            .forEach(t -> putIfAbsent(t.getKey(), t.getValue()));

        return this;
    }
}
