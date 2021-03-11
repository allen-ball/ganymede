package ganymede.server.renderer;

import ball.annotation.ServiceProviderFor;
import ganymede.server.Renderer;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.spark.sql.Dataset;

/**
 * Spark {@link Dataset} {@link Renderer} service provider.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Renderer.class })
@ForType(Dataset.class)
@NoArgsConstructor @ToString
public class SparkDatasetRenderer extends AbstractThymeleafHTMLRenderer {
    @Override
    protected Map<String,Object> getMap(Object object) {
        return Map.<String,Object>of("dataset", object, "view", 50);
    }
}
