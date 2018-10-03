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
package org.ec4j.ant.it;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

public class EncodingTest extends AbstractAntTest {

    @Before
    public void before() throws IOException {
        before("encoding");
    }

    @Test
    public void encoding() throws Exception {

        executeTarget("editorconfig.checkEncoding");

                assertLogText("Processing file '.editorconfig' using linter org.ec4j.maven.linters.TextLinter") //
                .assertLogText("Creating a Resource for path '.editorconfig' with encoding 'ISO-8859-2'") //
                .assertLogText(
                        "Processing file 'src/main/resources/simplelogger.properties' using linter org.ec4j.maven.linters.TextLinter"
                                .replace('/', File.separatorChar)) //
                .assertLogText(
                        "Creating a Resource for path 'src/main/resources/simplelogger.properties' with encoding 'ISO-8859-1'"
                                .replace('/', File.separatorChar)) //
                .assertLogText("Checked 3 files") //
        ;

    }

}
