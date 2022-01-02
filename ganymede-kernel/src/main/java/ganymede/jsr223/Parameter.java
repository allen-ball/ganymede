package ganymede.jsr223;
/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2021, 2022 Allen D. Ball
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
import ball.annotation.processing.AnnotatedProcessor;
import ball.annotation.processing.For;
import ball.annotation.processing.TargetMustExtend;
import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.annotation.processing.Processor;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link AnnotatedScriptEngineFactory#getParameters()} source
 * {@link java.lang.annotation.Annotation}.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@Documented
@Retention(RUNTIME)
@Repeatable(Parameters.class)
@Target({ TYPE })
@TargetMustExtend(AnnotatedScriptEngineFactory.class)
public @interface Parameter {
    String name() default "";
    String value() default "";

    /**
     * {@link Processor} implementation.
     */
    @ServiceProviderFor({ Processor.class })
    @For({ Parameter.class })
    @NoArgsConstructor @ToString
    public static class ProcessorImpl extends AnnotatedProcessor {
    }
}
