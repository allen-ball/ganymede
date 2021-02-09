package galyleo.kernel;

import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Galyleo Jupyter {@link Kernel} {@link RestController}.
 *
 * {@injected.fields}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@RestController
@RequestMapping(value = { "/kernel/v1/" },
                consumes = APPLICATION_JSON_VALUE,
                produces = APPLICATION_JSON_VALUE)
@NoArgsConstructor @ToString @Log4j2
public class KernelRestController {
    @Autowired
    private Kernel kernel = null;
}
