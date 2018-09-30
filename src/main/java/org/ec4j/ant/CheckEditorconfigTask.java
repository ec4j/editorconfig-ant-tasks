/**
 * Copyright (c) 2018 EditorConfig Maven Plugin
 * project contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ec4j.ant;

import java.nio.charset.Charset;
import java.nio.file.Path;

import org.ec4j.maven.lint.api.Resource;
import org.ec4j.maven.lint.api.ViolationCollector;
import org.ec4j.maven.lint.api.ViolationHandler;

/**
 * Checks whether files are formatted according to rules defined in {@code .editorconfig} files. If fomat violations are
 * detected, either causes the build to fail (if {@link #failOnFormatViolation} is {@code true}) or just produces a
 * warning.
 *
 * @since 0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CheckEditorconfigTask extends AbstractEditorconfigTask {

    /**
     * Tells the mojo what to do in case formatting violations are found. if {@code true}, all violations will be
     * reported on the console as ERRORs and the build will fail. if {@code false}, all violations will be reported on
     * the console as WARNs and the build will proceed further.
     *
     * @since 0.0.1
     */
    private boolean failOnFormatViolation = true;

    /** {@inheritDoc} */
    @Override
    protected ViolationHandler createHandler() {
        return new ViolationCollector(failOnFormatViolation, "mvn editorconfig:format");
    }

    /** {@inheritDoc} */
    @Override
    protected Resource createResource(Path absFile, Path relFile, Charset encoding) {
        if (log.isTraceEnabled()) {
            log.trace("Creating a {} for path '{}' with encoding '{}'", Resource.class.getSimpleName(), relFile,
                    encoding);
        }
        return new Resource(absFile, relFile, encoding);
    }

}