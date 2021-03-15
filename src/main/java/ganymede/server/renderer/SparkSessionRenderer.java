package ganymede.server.renderer;

import ball.annotation.ServiceProviderFor;
import ganymede.server.Renderer;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.spark.sql.SparkSession;

/**
 * {@link SparkSession} {@link Renderer} service provider.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ Renderer.class })
@ForType(SparkSession.class)
@NoArgsConstructor @ToString
public class SparkSessionRenderer extends AbstractThymeleafHTMLRenderer {
    @Override
    protected Map<String,Object> getMap(Object object) {
        return Map.<String,Object>of("session", object);
    }
}
