package ganymede.kernel.client;
/*-
 * ##########################################################################
 * Ganymede
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
import com.fasterxml.jackson.databind.JsonNode;
import ganymede.kernel.client.api.DefaultApi;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * Ganymede {@link ganymede.kernel.Kernel} REST client.
 *
 * <p>{@link ApiClient} implements:</p>
 * {@include /ganymede-rest-protocol.yml}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ToString @Log4j2
public class KernelRestClient extends ApiClient {

    /**
     * The name of the {@link System} property containing the
     * {@link ganymede.kernel.Kernel}'s REST server port.
     */
    public static final String PORT_PROPERTY = "kernel.port";

    {
        setPort(Integer.decode(System.getProperty(PORT_PROPERTY)));
        setRequestInterceptor(t -> t.header("Accept", "application/json"));
    }

    /**
     * See {@link DefaultApi#kernelId()}.
     */
    public UUID kernelId() throws Exception {
        return new DefaultApi(this).kernelId();
    }

    /**
     * See {@link DefaultApi#getExecuteRequest()}.
     */
    public JsonNode getExecuteRequest() throws Exception {
        return new DefaultApi(this).getExecuteRequest();
    }

    /**
     * See {@link DefaultApi#display(JsonNode)}.
     *
     * @param   bundle          The MIME bundle {@link JsonNode}.
     */
    public void display(JsonNode bundle) throws Exception {
        new DefaultApi(this).display(bundle);
    }

    /**
     * See {@link DefaultApi#print(JsonNode)}.
     *
     * @param   bundle          The MIME bundle {@link JsonNode}.
     */
    public void print(JsonNode bundle) throws Exception {
        new DefaultApi(this).print(bundle);
    }

    /**
     * See {@link DefaultApi#classpath()}.
     */
    public List<String> classpath() throws Exception {
        return new DefaultApi(this).classpath();
    }

    /**
     * See {@link DefaultApi#imports()}.
     */
    public List<String> imports() throws Exception {
        return new DefaultApi(this).imports();
    }

    /**
     * See {@link DefaultApi#variables()}.
     */
    public Map<String,String> variables() throws Exception {
        return new DefaultApi(this).variables();
    }
}
