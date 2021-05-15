package ganymede.server.renderer;
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
import ball.annotation.ServiceProviderFor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.server.Renderer;
import java.io.ByteArrayOutputStream;
import lombok.NoArgsConstructor;
import lombok.ToString;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.string.DataFramePrinter;

import static ganymede.server.Server.OBJECT_MAPPER;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.springframework.util.MimeTypeUtils.TEXT_HTML_VALUE;
import static tech.tablesaw.api.Table.defaultWriterRegistry;

/**
 * {@link.uri https://github.com/jtablesaw/tablesaw target=newtab Tablesaw}
 * {@link Table} {@link Renderer} service provider.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@ServiceProviderFor({ Renderer.class })
@ForClassName("tech.tablesaw.api.Table")
@NoArgsConstructor @ToString
public class TablesawTableRenderer implements Renderer {
    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        var table = (Table) object;
/*
        if (! bundle.with(DATA).has(APPLICATION_JSON_VALUE)) {
            try {
                if (hasWriterForExtension("json")) {
                    var string = table.write().toString("json");
                    var node = OBJECT_MAPPER.readTree(string);

                    bundle.with(DATA)
                        .set(APPLICATION_JSON_VALUE, node);
                    bundle.with(METADATA)
                        .with(APPLICATION_JSON_VALUE)
                        .put("expanded", true);
                }
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }
        }
*/
        if (! bundle.with(DATA).has(TEXT_HTML_VALUE)) {
            try {
                if (hasWriterForExtension("html")) {
                    bundle.with(DATA)
                        .put(TEXT_HTML_VALUE, table.write().toString("html"));
                }
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }
        }

        if (table != null) {
            var string = "";

            try (var out = new ByteArrayOutputStream()) {
                new DataFramePrinter(Integer.MAX_VALUE, out).print(table);

                string = new String(out.toByteArray());
            } catch (Exception exception) {
                exception.printStackTrace(System.err);
            }

            MAP.renderTo(bundle, string);
        }
    }

    private boolean hasWriterForExtension(String extension) {
        return defaultWriterRegistry.getWriterForExtension(extension) != null;
    }
}
