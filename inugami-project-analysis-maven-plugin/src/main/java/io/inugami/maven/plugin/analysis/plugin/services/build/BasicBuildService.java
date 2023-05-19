/* --------------------------------------------------------------------
 *  Inugami
 * --------------------------------------------------------------------
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.inugami.maven.plugin.analysis.plugin.services.build;

import io.inugami.api.exceptions.FatalException;
import io.inugami.commons.files.FilesUtils;
import io.inugami.maven.plugin.analysis.api.models.FileResources;
import io.inugami.maven.plugin.analysis.api.models.PropertiesResources;
import io.inugami.maven.plugin.analysis.api.models.Resource;
import io.inugami.maven.plugin.analysis.plugin.services.MavenArtifactResolver;
import io.inugami.maven.plugin.analysis.plugin.services.rendering.TemplateRenderer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.aether.artifact.Artifact;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.inugami.api.exceptions.Asserts.assertTrue;
import static io.inugami.maven.plugin.analysis.plugin.services.build.exceptions.BasicBuildError.TEMPLATE_FILE_NOT_EXISTS;
import static io.inugami.maven.plugin.analysis.plugin.services.build.exceptions.BasicBuildError.TEMPLATE_FILE_NOT_READABLE;

@SuppressWarnings({"java:S2095"})
@Slf4j
public class BasicBuildService {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private final static PropertiesLoader PROPERTIES_LOADER  = new PropertiesLoader();
    private final static TemplateRenderer TEMPLATE_RENDERING = new TemplateRenderer();
    private final static List<String>     TEXT_FILES         = Arrays.asList("application/json",
                                                                             "application/x-javascript",
                                                                             "application/x-sh",
                                                                             "application/xml",
                                                                             "image/svg+xml",
                                                                             "application/xhtml+xml");

    private final static List<String> TEXT_FILES_EXTS = Arrays.asList("txt",
                                                                      "bat",
                                                                      "sh",
                                                                      "adoc",
                                                                      "xml",
                                                                      "html",
                                                                      "xhtml",
                                                                      "svg",
                                                                      "ts",
                                                                      "js",
                                                                      "json",
                                                                      "yml",
                                                                      "yaml",
                                                                      "properties",
                                                                      "conf",
                                                                      "css",
                                                                      "scss",
                                                                      "less",
                                                                      "sql",
                                                                      "cql",
                                                                      "csv");
    public static final  String       MACOSX_DELETED  = "__MACOSX";


    // =========================================================================
    // delete
    // =========================================================================
    public void delete(final List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        for (final String path : paths) {
            delete(path);
        }
    }

    private void delete(final String path) {
        final File file = new File(path).getAbsoluteFile();

        if (isNotAllowDeleteFile(file)) {
            log.error("not allow to delete file : {}", file.getAbsoluteFile());
        }

        final File parent = file.getParentFile();


        final Pattern fileNameRegex = Pattern.compile(file.getName());
        for (final String fileName : parent.list()) {
            if (fileNameRegex.matcher(fileName).matches()) {
                deleteFile(new File(String.join(File.separator, parent.getAbsolutePath(), fileName)));
            }
        }
    }


    private void deleteFile(final File file) {
        if (file.exists()) {
            if (file.isFile()) {

                if (file.delete()) {
                    log.info("file {} deleted", file.getAbsoluteFile());
                } else {
                    log.error("file {} not deleted", file.getAbsoluteFile());
                }
            } else {
                try {
                    FileUtils.deleteDirectory(file);
                    log.info("folder {} deleted", file.getAbsoluteFile());
                } catch (final IOException e) {
                    log.error(e.getMessage(), e);
                    log.info("folder {} not deleted", file.getAbsoluteFile());
                }
            }

        }
    }


    protected boolean isNotAllowDeleteFile(final File file) {
        boolean result = false;
        if (file == null) {
            return true;
        }
        final File   parent = file.getAbsoluteFile().getParentFile();
        final String path   = file.getAbsolutePath();


        if ("/".equals(path) || path.trim().isEmpty() || windowsRootPath(path)) {
            result = true;
        } else if (!parent.exists()) {
            log.error("parent path doesn't exists : {}", file.getParent());
            result = true;
        }


        return result;
    }

    protected boolean windowsRootPath(final String path) {
        if (path.length() > 3) {
            return false;
        }
        final String[] parts = path.split(":");
        return parts.length == 2 && (parts[1].equals("\\") || parts[1].equals("/"));
    }

    // =========================================================================
    // mkdirs
    // =========================================================================
    public void mkdirs(final List<String> paths) {
        if (paths != null) {
            for (final String path : paths) {
                mkdir(path);
            }
        }
    }

    private void mkdir(final String path) {
        if (path == null) {
            return;
        }
        final File file = new File(path);
        if (file.exists()) {
            log.warn("{} already exists", path);
            return;
        }

        if (file.mkdirs()) {
            log.info("{} created", path);
        } else {
            log.error("{} not created", path);
        }
    }


    // =========================================================================
    // copyResources
    // =========================================================================
    public void copyResources(final List<Resource> resources,
                              final Map<String, String> properties,
                              final boolean filtering,
                              final boolean mavenFiltering,
                              final MavenArtifactResolver artifactResolver,
                              final List<String> extensions) throws IOException {
        if (resources == null || resources.isEmpty()) {
            return;
        }
        for (final Resource resource : resources) {
            copyResources(resource, properties, filtering, mavenFiltering, artifactResolver, extensions);
        }
    }

    private void copyResources(final Resource resource,
                               final Map<String, String> properties,
                               final boolean filtering,
                               final boolean mavenFiltering,
                               final MavenArtifactResolver artifactResolver,
                               final List<String> extensions) throws IOException {
        if (resource.getTarget() == null) {
            return;
        }
        if (resource.getGav() == null && resource.getPath() == null) {
            return;
        }

        String resourcePath = resource.getPath();

        if (resourcePath == null && resource.getGav() != null) {
            resourcePath = resolveGavFilePath(resource.getGav(), artifactResolver);
        }
        if (resourcePath == null) {
            log.error("can't resolve input resource : {}", resource);
            return;
        }
        copyfile(resourcePath, resource.getTarget(), resource.getProperties(), properties, filtering,
                 mavenFiltering, extensions);

    }

    private String resolveGavFilePath(final String gav, final MavenArtifactResolver artifactResolver) {
        final Artifact artifact = artifactResolver.resolve(gav);
        return artifact == null ? null : artifact.getFile().getAbsolutePath();
    }

    private void copyfile(final String inputFile,
                          final String target,
                          final Map<String, String> resourceProperties,
                          final Map<String, String> properties,
                          final boolean filtering,
                          final boolean mavenFiltering,
                          final List<String> extensions) throws IOException {
        final File file       = new File(inputFile);
        final File targetFile = new File(target);

        if (!file.exists()) {
            log.error("file doesn't exists : {}", file.getAbsoluteFile());
            return;
        }
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }


        processCopy(file, targetFile, filtering, properties, resourceProperties, mavenFiltering, extensions);
    }

    private void processCopy(final File file,
                             final File target,
                             final boolean filtering,
                             final Map<String, String> properties,
                             final Map<String, String> resourceProperties,
                             final boolean mavenFiltering,
                             final List<String> extensions) throws IOException {

        if (file.isFile()) {
            if (target.exists()) {
                target.delete();
            }
            if (!target.getAbsoluteFile().getParentFile().exists()) {
                target.getAbsoluteFile().getParentFile().mkdirs();
            }
            if (filtering && !isTextFile(file, extensions)) {
                final String content = FilesUtils.readContent(file);
                final String fileContent = TEMPLATE_RENDERING.render(file.getAbsolutePath(), content, properties,
                                                                     resourceProperties, mavenFiltering);
                FilesUtils.write(fileContent, target);
            } else {
                FilesUtils.copy(file, target.getParentFile());
            }
        } else {
            final String[] childrenFile = file.list();

            for (final String childFile : childrenFile) {
                final File child       = new File(file.getAbsolutePath() + File.separator + childFile);
                final File childTarget = new File(target.getAbsolutePath() + File.separator + childFile);
                processCopy(child, childTarget, filtering, properties, resourceProperties, mavenFiltering, extensions);
            }
        }

    }

    // =========================================================================
    // unpack
    // =========================================================================
    public void unpack(final List<Resource> resources,
                       final Map<String, String> properties,
                       final boolean filtering,
                       final boolean mavenFiltering,
                       final MavenArtifactResolver artifactResolver,
                       final List<String> textFiles) throws IOException {

        if (resources == null || resources.isEmpty()) {
            return;
        }

        for (final Resource resource : resources) {
            unpackResource(resource, properties, filtering, mavenFiltering, artifactResolver, textFiles);
        }
    }

    private void unpackResource(final Resource resource,
                                final Map<String, String> properties,
                                final boolean filtering,
                                final boolean mavenFiltering,
                                final MavenArtifactResolver artifactResolver,
                                final List<String> textFiles) throws IOException {
        if (resource.getTarget() == null) {
            return;
        }
        if (resource.getGav() == null && resource.getPath() == null) {
            return;
        }

        String archive = resource.getPath();
        if (archive == null && resource.getGav() != null) {
            archive = resolveGavFilePath(resource.getGav(), artifactResolver);
        }

        if (archive == null) {
            log.error("can't unpack resource : {}", resource);
        } else {
            unpackArchive(archive, resource.getTarget(), resource.getProperties(), properties, filtering,
                          mavenFiltering, textFiles);
        }
    }

    private void unpackArchive(final String archive,
                               final String target,
                               final Map<String, String> properties,
                               final Map<String, String> mavenProperties,
                               final boolean filtering,
                               final boolean mavenFiltering,
                               final List<String> textFiles) throws IOException {

        final File targetFile = new File(target).getAbsoluteFile();
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        final FileInputStream fileZipStream = openFileInputStream(new File(archive));
        final ZipInputStream  zip           = new ZipInputStream(fileZipStream);
        try {
            ZipEntry entry;
            do {
                entry = zip.getNextEntry();
                if (entry != null) {
                    if (!targetFile.getAbsolutePath().contains(MACOSX_DELETED)) {
                        unzipFile(targetFile, zip, entry, properties, mavenProperties, filtering, mavenFiltering,
                                  textFiles);
                    }
                }
            } while (entry != null);

        } catch (final IOException e) {
            log.error(e.getMessage());
            throw e;
        } finally {
            close(() -> zip.closeEntry());
            close(() -> zip.close());
            close(() -> fileZipStream.close());
        }
    }

    private void unzipFile(final File target,
                           final ZipInputStream zip,
                           final ZipEntry entry,
                           final Map<String, String> properties,
                           final Map<String, String> mavenProperties,
                           final boolean filtering,
                           final boolean mavenFiltering,
                           final List<String> textFiles)
            throws FileNotFoundException, IOException {
        final byte[] buffer   = new byte[1024];
        final String fileName = entry.getName();
        final File   newFile  = buildFileEntry(target, fileName);

        log.info("unzip : {}", newFile.getAbsolutePath());


        if (entry.isDirectory()) {
            newFile.mkdirs();
        } else {
            final boolean textFile = isTextFile(newFile, textFiles);

            if (filtering && textFile) {
                final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                int                         len;
                while ((len = zip.read(buffer)) > 0) {
                    byteStream.write(buffer, 0, len);
                }
                final String content = new String(byteStream.toByteArray(), StandardCharsets.UTF_8);
                final String realContent = TEMPLATE_RENDERING.render(newFile.getAbsolutePath(), content,
                                                                     mavenProperties, properties, mavenFiltering);
                FilesUtils.write(realContent, newFile);
            } else {
                try (final FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zip.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            }

        }

        close(() -> zip.closeEntry());
        close(() -> zip.closeEntry());
    }

    protected boolean isTextFile(final File newFile, final List<String> textFiles) {
        boolean result    = false;
        String  extension = null;
        if (newFile.getName().contains(".")) {
            final String[] nameParts = newFile.getName().split("[.]");
            extension = nameParts.length <= 1 ? null : nameParts[nameParts.length - 1];
        }

        if (extension != null) {
            result = matchTextFile(extension);
        }

        if (!result && textFiles != null) {
            for (final String textFile : textFiles) {
                final Pattern regex = Pattern.compile(textFile);
                result = regex.matcher(newFile.getName()).matches();
                if (result) {
                    break;
                }
            }
        }
        return result;
    }

    private boolean matchTextFile(final String extension) {
        return TEXT_FILES_EXTS.contains(extension.toLowerCase());
    }

    private File buildFileEntry(final File target, final String fileName) {
        // @formatter:off
        final String path = new StringBuilder(target.getAbsolutePath()).append(File.separator).append(fileName)
                                                                       .toString();
        // @formatter:on
        final File result = new File(path);

        final File parent = result.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        return result;
    }

    // =========================================================================
    // writeFiles
    // =========================================================================
    public void writeFiles(final List<FileResources> fileResources, final Map<String, String> properties,
                           final boolean applyMavenProperties) {
        if (fileResources != null && !fileResources.isEmpty()) {
            for (final FileResources fileResource : fileResources) {
                writeFile(fileResource, properties, applyMavenProperties);
            }
        }
    }

    public void writeFile(final FileResources fileResource, final Map<String, String> properties,
                          final boolean applyMavenProperties) {
        if (fileResource.getTemplate() == null && fileResource.getTemplatePath() == null) {
            log.error("can't write file without template");
            return;
        }

        String templateId = null;
        String content    = fileResource.getTemplate();
        if (content == null && fileResource.getTemplatePath() != null) {
            final File file = new File(fileResource.getTemplatePath());
            assertTrue(TEMPLATE_FILE_NOT_EXISTS.addDetail(file.getAbsolutePath()), file.exists());
            assertTrue(TEMPLATE_FILE_NOT_READABLE.addDetail(file.getAbsolutePath()), file.canRead());
            try {
                content = FilesUtils.readContent(file);
                templateId = file.getAbsolutePath();
            } catch (final IOException e) {
                throw new FatalException(TEMPLATE_FILE_NOT_READABLE.addDetail(file.getAbsolutePath()));
            }
        }

        final String fileContent = TEMPLATE_RENDERING.render(templateId, content, properties,
                                                             fileResource.getProperties(),
                                                             applyMavenProperties);

        if (fileContent == null) {
            log.error("can't write null content");
        } else {
            final File target = new File(fileResource.getTarget()).getAbsoluteFile();
            if (!target.getParentFile().exists()) {
                if (target.getParentFile().mkdirs()) {
                    log.error("folder created : {}", target.getParentFile());
                } else {
                    log.error("can't create folder {}", target.getParentFile());
                }
            }
            FilesUtils.write(fileContent, target);
        }

    }

    // =========================================================================
    // loadProperties
    // =========================================================================
    public void loadProperties(final List<PropertiesResources> resources,
                               final Map<String, String> globalProperties,
                               final Properties properties) {

        if (properties != null) {
            if (globalProperties != null) {
                for (final Map.Entry<String, String> entry : globalProperties.entrySet()) {
                    log.debug("include properties : {}={}", entry.getKey(), entry.getValue());
                    properties.put(entry.getKey(), entry.getValue());
                }
            }

            if (resources != null && !resources.isEmpty()) {
                for (final PropertiesResources propertiesResource : resources) {
                    loadProperties(propertiesResource, properties);
                }
            }
        }
    }

    private void loadProperties(final PropertiesResources propertiesResources,
                                final Properties properties) {
        final Map<String, String> loadedProperties = PROPERTIES_LOADER.loadProperties(propertiesResources);

        if (loadedProperties != null) {
            for (final Map.Entry<String, String> entry : loadedProperties.entrySet()) {
                log.debug("include loaded properties : {}={}", entry.getKey(), entry.getValue());
                properties.put(entry.getKey(), entry.getValue());
            }
        }
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    private FileInputStream openFileInputStream(final File tomcatZip) throws IOException {
        try {
            return new FileInputStream(tomcatZip);
        } catch (final FileNotFoundException e) {
            throw e;
        }
    }

    private void close(final AutoCloseable closable) {
        try {
            closable.close();
        } catch (final Exception e) {
            log.error(e.getMessage());
        }

    }


}
