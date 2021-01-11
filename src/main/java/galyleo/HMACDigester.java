package galyleo;

import java.math.BigInteger;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.Data;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * HMAC message digester.  See discussion in
 * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#the-wire-protocol target=newtab The Wire Protocol}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Data
public class HMACDigester {
    private final Mac mac;

    /**
     * Sole constructor.
     *
     * @param   scheme          See
     *                          {@link Connection.Properties Connection.Properties.signatureScheme}.
     * @param   key             See
     *                          {@link Connection.Properties Connection.Properties.key}.
     */
    public HMACDigester(String scheme, String key) {
        Mac mac = null;

        if (key != null && (! key.isEmpty())) {
            try {
                mac = Mac.getInstance(scheme.replaceAll("-", ""));
                mac.init(new SecretKeySpec(key.getBytes(US_ASCII), scheme));
            } catch (Exception exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }

        this.mac = mac;
    }

    /**
     * Method to calculate a digest for message parts.  See {@link
     * Mac#update(byte[])} and {@link Mac#doFinal()}.
     *
     * @param   parts           The {@code byte[]} parts of the message to
     *                          digest.
     *
     * @return  The digest {@link String}.
     */
    public String digest(byte[]... parts) {
        String digest = "";

        if (mac != null) {
            synchronized (mac) {
                Stream.of(parts).forEach(mac::update);

                var bytes = mac.doFinal();

                digest = new BigInteger(1, bytes).toString(16);
            }
        }

        return digest;
    }

    /**
     * Method to verify a digest for message parts.
     *
     * @param   digest          The digest to verify.
     * @param   parts           The {@code byte[]} parts of the message to
     *                          digest.
     *
     * @return  {@code true} if the argument digest matches the one
     *          calculated; {@code false} otherwise.
     */
    public boolean verify(String digest, byte[]... parts) {
        return digest.equals(digest(parts));
    }
}
