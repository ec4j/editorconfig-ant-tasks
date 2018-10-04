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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;
import org.junit.Assert;

public class AbstractAntTest {

    public class CopyFileVisitor extends SimpleFileVisitor<Path> {
        private Path sourcePath = null;
        private final Path targetPath;

        public CopyFileVisitor(Path targetPath) {
            this.targetPath = targetPath;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            if (sourcePath == null) {
                sourcePath = dir;
            } else {
                Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.copy(file, targetPath.resolve(sourcePath.relativize(file)));
            return FileVisitResult.CONTINUE;
        }
    }

    /**
     * Deletes a file or directory recursively if it exists.
     *
     * @param directory the directory to delete
     * @throws IOException
     */
    public static void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        // directory iteration failed; propagate exception
                        throw exc;
                    }
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    // try to delete the file anyway, even if its attributes
                    // could not be read, since delete-only access is
                    // theoretically possible
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    protected final BuildFileTest antSupport = new BuildFileTest() {
    };

    protected Path expectedDir;
    protected void assertFilesEqual(Path actualBaseDir, Path expectedBaseDir, String relPath) throws IOException {
        final String contentActual = new String(Files.readAllBytes(actualBaseDir.resolve(relPath)),
                StandardCharsets.UTF_8);
        final String contentExpected = new String(Files.readAllBytes(expectedBaseDir.resolve(relPath)),
                StandardCharsets.UTF_8);
        Assert.assertEquals(relPath, contentExpected, contentActual);
    }

    protected AbstractAntTest assertLogText(String substring) {
        antSupport.assertDebuglogContaining(substring);
        return this;
    }

    protected void before(String projectName) throws IOException {
        expectedDir = Paths.get("src/test/projects/" + projectName + "-formatted").toAbsolutePath();
        final Path src = Paths.get("src/test/projects/" + projectName).toAbsolutePath();
        final Path target = Paths.get("target/test-projects/" + projectName).toAbsolutePath();
        deleteDirectory(target);
        Files.createDirectories(target);
        Files.walkFileTree(src, new CopyFileVisitor(target));
        antSupport.configureProject(target.resolve("build.xml").toString(), Project.MSG_VERBOSE);
        antSupport.getProject().setBaseDir(target.toFile());
    }

    protected void executeTarget(String target) throws IOException {
        try {
            antSupport.executeTarget(target);
        } finally {
            final Path projectDir = antSupport.getProject().getBaseDir().toPath();
            final Path logPath = projectDir.resolve(target + ".log");
            Files.write(logPath, antSupport.getFullLog().getBytes(StandardCharsets.UTF_8));
        }
    }

}
