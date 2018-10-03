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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.ec4j.ant.AntLintLogger.Level;
import org.ec4j.core.Cache.Caches;
import org.ec4j.core.Resource.Resources;
import org.ec4j.core.ResourceProperties;
import org.ec4j.core.ResourcePropertiesService;
import org.ec4j.core.model.PropertyType;
import org.ec4j.maven.lint.api.Constants;
import org.ec4j.maven.lint.api.FormatException;
import org.ec4j.maven.lint.api.Linter;
import org.ec4j.maven.lint.api.LinterRegistry;
import org.ec4j.maven.lint.api.Logger;
import org.ec4j.maven.lint.api.Resource;
import org.ec4j.maven.lint.api.ViolationHandler;

/**
 * A base for {@link CheckEditorconfigTask} and {@link FormatEditorconfigTask}
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public abstract class AbstractEditorconfigTask extends Task {

    /**
     * If set to {@code true}, the class path will be scanned for implementations of {@link Linter} and all
     * {@link Linter}s found will be added to {@link #linters} with their default includes and excludes.
     *
     * @since 0.0.1
     */
    protected boolean addLintersFromClassPath = true;

    /** The result of {@code basedir.toPath()} */
    protected Path basedirPath;

    /** The result of {@code Charset.forName(encoding)} */
    protected Charset charset;

    /**
     * The default encoding of files selected by {@link #includes} and {@link #excludes}. This value can be overriden by
     * a {@code charset} property of an {@code .editorconfig} file.
     *
     * @since 0.0.1
     */
    protected String encoding;

    /**
     * If {@code true} the default exclude patterns (that exclude binary files and other non-source code files, see
     * {@link Constants#DEFAULT_EXCLUDES}) will be prepended to the list of {@link #excludes}. Otherwise, no defaults
     * will be prepended to {@link #excludes}.
     *
     * @since 0.0.1
     */
    protected boolean excludeNonSourceFiles;
    /**
     * If {@code true} the plugin execution will fail with an error in case no single {@code .editorconfig} property
     * matches any file of the current Maven project - this usually means that there is no {@code .editorconfig} file in
     * the whole source tree. If {@code false}, only a warning is produced in such a situation.
     *
     * @since 0.0.1
     */
    protected boolean failOnNoMatchingProperties = true;

    /**
     * File patterns to include/exclude from the set of files to process. See also {@link #excludeNonSourceFiles} and {@link #excludeSubmodules}.
     *
     * @since 0.0.1
     */
    protected List<FileSet> filesets = new ArrayList<>();

    /**
     * Set the includes and excludes for the individual {@link Linter}s
     *
     * @since 0.0.1
     */
    protected List<LinterConfig> linters = new ArrayList<>();

    protected Logger log = new AntLintLogger(this, Level.INFO);

    protected Level logLevel = Level.INFO;

    public AbstractEditorconfigTask() {
        super();
    }

    public void addFileset(FileSet fileset) {
        filesets.add(fileset);
    }

    protected LinterRegistry buildLinterRegistry() {

        final LinterRegistry.Builder linterRegistryBuilder = LinterRegistry.builder().log(log);

        if (addLintersFromClassPath) {
            linterRegistryBuilder.scan(getClass().getClassLoader());
        }

        if (linters != null && !linters.isEmpty()) {
            for (LinterConfig linter : linters) {
                if (linter.isEnabled()) {
                    linterRegistryBuilder.entry(linter.getId(), linter.getClassName(), this.getClass().getClassLoader(),
                            linter.getIncludes(), linter.getExcludes(), linter.isUseDefaultIncludesAndExcludes());
                } else {
                    linterRegistryBuilder.removeEntry(linter.getId());
                }
            }
        }

        return linterRegistryBuilder.build();

    }

    protected abstract ViolationHandler createHandler();

    public List<LinterConfig> createLinters() {
        return this.linters = new ArrayList<>();
    }

    /**
     * @param absFile  the {@link Path} to create a {@link Resource} for. Must be absolute.
     * @param relFile  the {@link Path} to create a {@link Resource} for. Must be relative to {@link #basedirPath}.
     * @param encoding
     * @return
     */
    protected abstract Resource createResource(Path absFile, Path relFile, Charset encoding);

    /**
     * Called by Maven for executing the Mojo.
     *
     */
    @Override
    public void execute() {

        if (this.encoding == null || this.encoding.isEmpty()) {
            this.charset = Charset.defaultCharset();
            log.warn("Using current platform's default encoding {} to read .editorconfig files. You do not want this. Set either 'project.build.sourceEncoding' or 'editorconfig.encoding' property.",
                    charset);
        } else {
            this.charset = Charset.forName(this.encoding);
        }
        this.basedirPath = getProject().getBaseDir().toPath();

        LinterRegistry linterRegistry = buildLinterRegistry();
        final List<Path> includedFiles = scanIncludedFiles();

        try {
            final ViolationHandler handler = createHandler();

            final ResourcePropertiesService resourcePropertiesService = ResourcePropertiesService.builder() //
                    .cache(Caches.permanent()) //
                    .build();
            handler.startFiles();
            boolean propertyMatched = false;
            for (Path file : includedFiles) {
                // file relative to basedir
                final Path absFile = basedirPath.resolve(file);
                log.info("Processing file '{}'", file);
                final ResourceProperties editorConfigProperties = resourcePropertiesService
                        .queryProperties(Resources.ofPath(absFile, charset));
                if (!editorConfigProperties.getProperties().isEmpty()) {
                    propertyMatched = true;
                    final Charset useEncoding = Charset
                            .forName(editorConfigProperties.getValue(PropertyType.charset, charset.name(), true));
                    final Resource resource = createResource(absFile, file, useEncoding);
                    final List<Linter> filteredLinters = linterRegistry.filter(file);
                    ViolationHandler.ReturnState state = ViolationHandler.ReturnState.RECHECK;
                    while (state != ViolationHandler.ReturnState.FINISHED) {
                        for (Linter linter : filteredLinters) {
                            log.trace("Processing file '{}' using linter {}", file, linter.getClass().getName());
                            handler.startFile(resource);
                            linter.process(resource, editorConfigProperties, handler);
                        }
                        state = handler.endFile();
                    }
                }
            }
            handler.endFiles();
            if (!propertyMatched) {
                if (failOnNoMatchingProperties) {
                    throw new BuildException(String.format("No .editorconfig properties applicable for files under '{}'", basedirPath), getLocation());
                } else {
                    log.warn("No .editorconfig properties applicable for files under '{}'", basedirPath);
                }
            }
        } catch (IOException e) {
            throw new BuildException(e.getMessage(), e);
        } catch (FormatException e) {
            throw new BuildException("\n\n" + e.getMessage() + "\n\n", e, getLocation());
        }

    }

    /**
     * A {@link DirectoryScanner} boiler plate.
     *
     * @return A {@link String} array of included files
     */
    private List<Path> scanIncludedFiles() {
        final List<Path> result = new ArrayList<>();
        if (filesets.isEmpty()) {
            final FileSet fs = new FileSet();
            fs.setDir(getProject().getBaseDir());
            filesets.add(fs);
        }
        final Path basedirPath = getProject().getBaseDir().toPath();
        for (FileSet fs : filesets) {
            if (excludeNonSourceFiles) {
                fs.appendExcludes(Constants.DEFAULT_EXCLUDES.toArray(new String[0]));
            }
            final DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            final Path dir = fs.getDir().toPath();
            for (String includedFile : ds.getIncludedFiles()) {
                final Path absPath = dir.resolve(includedFile);
                result.add(basedirPath.relativize(absPath));
            }
        }
        return result;
    }

    public void setAddLintersFromClassPath(boolean addLintersFromClassPath) {
        this.addLintersFromClassPath = addLintersFromClassPath;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setExcludeNonSourceFiles(boolean excludeNonSourceFiles) {
        this.excludeNonSourceFiles = excludeNonSourceFiles;
    }

    public void setFailOnNoMatchingProperties(boolean failOnNoMatchingProperties) {
        this.failOnNoMatchingProperties = failOnNoMatchingProperties;
    }

    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
        this.log = new AntLintLogger(this, logLevel);
    }
}
