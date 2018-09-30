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

import org.ec4j.maven.lint.api.EditableResource;
import org.ec4j.maven.lint.api.FormattingHandler;
import org.ec4j.maven.lint.api.Resource;
import org.ec4j.maven.lint.api.ViolationHandler;

/**
 * Formats a set of files so that they comply with rules defined in {@code .editorconfig} files.
 *
 * @since 0.0.1
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class FormatEditorconfigTask extends AbstractEditorconfigTask {

    /**
     * If {@code true}, a backup file will be created for every file that needs to be formatted just before the
     * formatted version is stored. If {@code false}, no backup is done and the files are formatted in place. See also
     * {@link #backupSuffix}.
     *
     * @since 0.0.1
     */
    private boolean backup = false;

    /**
     * A suffix to append to a file name to create its backup. See also {@link #backup}.
     *
     * @since 0.0.1
     */
    private String backupSuffix = ".bak";

    /** {@inheritDoc} */
    @Override
    protected ViolationHandler createHandler() {
        return new FormattingHandler(backup, backupSuffix);
    }

    /** {@inheritDoc} */
    @Override
    protected Resource createResource(Path absFile, Path relFile, Charset encoding) {
        return new EditableResource(absFile, relFile, encoding);
    }

}
