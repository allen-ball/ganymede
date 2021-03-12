package ganymede.notebook;

import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * {@link Notebook} Spring launcher.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@SpringBootApplication
@NoArgsConstructor @ToString
public class Notebook {

    /**
     * Static factory method to create an {@link NotebookContext}.  Also
     * initializes the Spring environment.
     *
     * @see SpringApplicationBuilder
     *
     * @return  An initialized {@link NotebookContext}.
     */
    public static NotebookContext newNotebookContext() {
        var __ = new NotebookContext();

        try {
            var type = Notebook.class;
            var profile = type.getSimpleName().toLowerCase();

            new SpringApplicationBuilder(type)
                .profiles(profile)
                .run();
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
        }

        return __;
    }
}
