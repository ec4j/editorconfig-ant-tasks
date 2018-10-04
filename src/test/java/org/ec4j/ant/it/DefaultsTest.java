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
import java.nio.file.Path;

import org.apache.tools.ant.BuildException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultsTest extends AbstractAntTest {

    @Before
    public void before() throws IOException {
        before("defaults");
    }

    @Test
    public void checkDefaults() throws IOException {
        try {
            executeTarget("editorconfig.checkDefaults");
            Assert.fail("Expected " + BuildException.class.getName());
        } catch (BuildException e) {
            final String substr = "There are .editorconfig violations.";
            Assert.assertTrue("Should throw BuildException containing '" + substr + "'",
                    e.getMessage().contains("There are .editorconfig violations"));
        }

        assertLogText("Processing file '.editorconfig' using linter org.ec4j.maven.linters.TextLinter") //
                .assertLogText("Processing file 'build.xml' using linter org.ec4j.maven.linters.TextLinter") //
                .assertLogText("Processing file 'build.xml' using linter org.ec4j.maven.linters.XmlLinter") //
                .assertLogText(
                        "Processing file 'src/main/java/org/ec4j/maven/it/defaults/App.java' using linter org.ec4j.maven.linters.TextLinter"
                                .replace('/', File.separatorChar)) //
                .assertLogText(
                        "Processing file 'src/main/resources/trailing-whitespace.txt' using linter org.ec4j.maven.linters.TextLinter"
                                .replace('/', File.separatorChar)) //
                .assertLogText(
                        "src/main/resources/trailing-whitespace.txt@1,7: Delete 2 characters - violates trim_trailing_whitespace = true, reported by org.ec4j.maven.linters.TextLinter"
                                .replace('/', File.separatorChar)) //
                .assertLogText(
                        "Processing file 'src/main/resources/indent.xml' using linter org.ec4j.maven.linters.TextLinter"
                                .replace('/', File.separatorChar)) //
                .assertLogText(
                        "Processing file 'src/main/resources/indent.xml' using linter org.ec4j.maven.linters.XmlLinter"
                                .replace('/', File.separatorChar)) //
                .assertLogText(
                        "src/main/resources/indent.xml@23,5: Delete 1 character - violates indent_style = space, indent_size = 2, reported by org.ec4j.maven.linters.XmlLinter"
                                .replace('/', File.separatorChar)) //
                .assertLogText(
                        "src/main/resources/indent.xml@24,3: Delete 2 characters - violates indent_style = space, indent_size = 2, reported by org.ec4j.maven.linters.XmlLinter"
                                .replace('/', File.separatorChar)) //
                .assertLogText("Processing file 'README.adoc' using linter org.ec4j.maven.linters.TextLinter") //
                .assertLogText(
                        "README.adoc@2,1: Delete 2 characters - violates trim_trailing_whitespace = true, reported by org.ec4j.maven.linters.TextLinter") //
                .assertLogText("Checked 6 files") //
        ;
    }

    @Test
    public void format() throws Exception {

        executeTarget("editorconfig.formatDefaults");

        assertLogText("Processing file '.editorconfig' using linter org.ec4j.maven.linters.TextLinter") //
                .assertLogText("Processing file 'build.xml' using linter org.ec4j.maven.linters.TextLinter") //
                .assertLogText("Processing file 'build.xml' using linter org.ec4j.maven.linters.XmlLinter") //
                .assertLogText(
                        "Processing file 'src/main/java/org/ec4j/maven/it/defaults/App.java' using linter org.ec4j.maven.linters.TextLinter"
                                .replace('/', File.separatorChar)) //
                .assertLogText(
                        "Processing file 'src/main/resources/trailing-whitespace.txt' using linter org.ec4j.maven.linters.TextLinter"
                                .replace('/', File.separatorChar)) //
                .assertLogText(
                        "src/main/resources/trailing-whitespace.txt@1,7: Delete 2 characters - violates trim_trailing_whitespace = true, reported by org.ec4j.maven.linters.TextLinter"
                                .replace('/', File.separatorChar)) //
                .assertLogText(
                        "Processing file 'src/main/resources/indent.xml' using linter org.ec4j.maven.linters.TextLinter"
                                .replace('/', File.separatorChar)) //
                .assertLogText(
                        "Processing file 'src/main/resources/indent.xml' using linter org.ec4j.maven.linters.XmlLinter"
                                .replace('/', File.separatorChar)) //
                .assertLogText(
                        "src/main/resources/indent.xml@23,5: Delete 1 character - violates indent_style = space, indent_size = 2, reported by org.ec4j.maven.linters.XmlLinter"
                                .replace('/', File.separatorChar)) //
                .assertLogText(
                        "src/main/resources/indent.xml@24,3: Delete 2 characters - violates indent_style = space, indent_size = 2, reported by org.ec4j.maven.linters.XmlLinter"
                                .replace('/', File.separatorChar)) //
                .assertLogText("Processing file 'README.adoc' using linter org.ec4j.maven.linters.TextLinter") //
                .assertLogText(
                        "README.adoc@2,1: Delete 2 characters - violates trim_trailing_whitespace = true, reported by org.ec4j.maven.linters.TextLinter") // ;
                .assertLogText("Formatted 3 out of 6 files") //
        ;

        final Path actualBaseDir = antSupport.getProject().getBaseDir().toPath();
        assertFilesEqual(actualBaseDir, expectedDir, ".editorconfig");
        assertFilesEqual(actualBaseDir, expectedDir, "build.xml");
        assertFilesEqual(actualBaseDir, expectedDir, "README.adoc");
        assertFilesEqual(actualBaseDir, expectedDir, "src/main/java/org/ec4j/maven/it/defaults/App.java");
        assertFilesEqual(actualBaseDir, expectedDir, "src/main/resources/indent.xml");
        assertFilesEqual(actualBaseDir, expectedDir, "src/main/resources/trailing-whitespace.txt");

    }

}
