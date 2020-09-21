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
package io.inugami.maven.plugin.analysis.plugin.services.scan.classes;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.spi.SpiLoader;
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.actions.ProjectScanner;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ClassesScan implements ProjectScanner {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public List<JsonObject> scan(final ScanConext context) {
        final List<JsonObject> result = new ArrayList<>();

        final List<String>        foundClasses = scanProjectSource(
                context.getProject().getBuild().getOutputDirectory());
        final List<ClassAnalyzer> analyzers    = SpiLoader.INSTANCE.loadSpiServicesByPriority(ClassAnalyzer.class);
        for (final String className : foundClasses) {
            try {
                final Class<?> clazz = context.getClassLoader().loadClass(className);
                result.addAll(analyzeClass(clazz, analyzers, context));
            }
            catch (final ClassNotFoundException e) {
                if (log.isDebugEnabled()) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        return result;
    }


    private List<JsonObject> analyzeClass(final Class<?> clazz,
                                          final List<ClassAnalyzer> analyzers,
                                          final ScanConext context) {
        final List<JsonObject> result = new ArrayList<>();
        for (final ClassAnalyzer analyzer : analyzers) {
            if (analyzer.accept(clazz, context)) {
                try {
                    final List<JsonObject> resultSet = analyzer.analyze(clazz, context);
                    if (resultSet != null) {
                        result.addAll(resultSet);
                    }
                }
                catch (final Exception error) {
                    log.error(error.getMessage(), error);
                }

            }
        }
        return result;
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    private List<String> scanProjectSource(final String outputDirectory) {
        final List<String> result = new ArrayList<>();

        final File classesFolder = new File(outputDirectory);
        result.addAll(retrieveAllClasses(classesFolder, classesFolder.getAbsolutePath()));
        return result;
    }

    private List<String> retrieveAllClasses(final File classesFolder, final String baseFolder) {
        final List<String> classes = new ArrayList<>();
        if (classesFolder.exists() && classesFolder.isDirectory()) {
            for (final String fileName : classesFolder.list()) {
                final File file = new File(classesFolder.getAbsolutePath() + File.separator + fileName);

                if (file.isDirectory()) {
                    classes.addAll(retrieveAllClasses(file, baseFolder));
                }
                else if (fileName.endsWith(".class")) {
                    classes.add(buildClassName(file.getAbsolutePath(), baseFolder));
                }
            }
        }
        return classes;
    }

    private String buildClassName(final String absolutePath, final String baseFolder) {
        return absolutePath.substring(baseFolder.length() + 1)
                           .replaceAll(".class", "")
                           .replaceAll("[$]", ".")
                           .replaceAll(File.separator, ".");
    }


}
