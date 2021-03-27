package ganymede.server;
/*-
 * ##########################################################################
 * Ganymede
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2021 Allen D. Ball
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ##########################################################################
 */
import java.math.BigInteger;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.zeromq.ZMQ;

/**
 * HMAC message digester.  See discussion in
 * {@link.uri https://jupyter-client.readthedocs.io/en/latest/messaging.html#the-wire-protocol target=newtab The Wire Protocol}.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@Data @Log4j2
public class HMACDigester {
    private final Mac mac;

    /**
     * Sole constructor.
     *
     * @param   scheme          The signature scheme.
     * @param   key             See key.
     */
    public HMACDigester(String scheme, String key) {
        Mac mac = null;

        if (key != null && (! key.isEmpty())) {
            try {
                mac = Mac.getInstance(scheme.replaceAll("-", ""));
                mac.init(new SecretKeySpec(key.getBytes(ZMQ.CHARSET), scheme));
            } catch (Exception exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }

        this.mac = mac;
    }

    /**
     * Method to calculate a digest for message frames.  See
     * {@link Mac#update(byte[])} and {@link Mac#doFinal()}.
     *
     * @param   frames          The {@code byte[]} frames of the message to
     *                          digest.
     *
     * @return  The digest {@link String}.
     */
    public String digest(byte[]... frames) {
        String digest = "";

        if (mac != null) {
            synchronized (mac) {
                Stream.of(frames).forEach(mac::update);

                var bytes = mac.doFinal();

                digest = new BigInteger(1, bytes).toString(16);
            }

            var length = 2 * mac.getMacLength();

            while (digest.length() < length) {
                digest = "0" + digest;
            }
        }

        return digest;
    }

    /**
     * Method to verify a digest for message frames.
     *
     * @param   digest          The digest to verify.
     * @param   frames          The {@code byte[]} frames of the message to
     *                          digest.
     *
     * @return  {@code true} if the argument digest matches the one
     *          calculated; {@code false} otherwise.
     */
    public boolean verify(String digest, byte[]... frames) {
        return digest.equals(digest(frames));
    }
}
