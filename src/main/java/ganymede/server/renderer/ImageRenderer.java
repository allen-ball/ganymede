package ganymede.server.renderer;
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
/* import ball.annotation.ServiceProviderFor; */
import com.fasterxml.jackson.databind.node.ObjectNode;
import ganymede.server.Renderer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Abstract image {@link Renderer} base class.  See {@link ImageIO}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
/* @ServiceProviderFor({ Renderer.class }) */
@ForType(byte[].class)
@NoArgsConstructor @ToString
public abstract class ImageRenderer implements AnnotatedRenderer {
    @Override
    public void renderTo(ObjectNode bundle, Object object) {
        var bytes = (byte[]) object;

        try (var in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var reader = ImageIO.getImageReaders(in).next();

            reader.setInput(in);

            var mimeType = reader.getOriginatingProvider().getMIMETypes()[0];

            if (! bundle.with(DATA).has(mimeType)) {
                bundle.with(DATA)
                    .put(mimeType, BASE64_ENCODER.encodeToString(bytes));

                var metadata = bundle.with(METADATA).with(mimeType);

                metadata.put("height", reader.getHeight(0));
                metadata.put("width", reader.getWidth(0));
            }
        } catch (IOException exception) {
            exception.printStackTrace(System.err);
        }
    }
}
