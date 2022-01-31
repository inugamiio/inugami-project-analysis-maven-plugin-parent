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
package io.inugami.maven.plugin.analysis.front.core.renderers;

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.spi.SpiLoader;
import io.inugami.maven.plugin.analysis.front.api.FrontPluginSpi;
import io.inugami.maven.plugin.analysis.front.api.IndexHtmlLoadingContentSpi;
import io.inugami.maven.plugin.analysis.front.api.models.HtmlAttribute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.inugami.maven.plugin.analysis.front.api.RenderingConstants.*;
import static io.inugami.maven.plugin.analysis.front.api.utils.HtmlRenderingUtils.*;

@Slf4j
@RequiredArgsConstructor
public class IndexRenderer {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private final        String                     contextPath;
    private static final AtomicReference<String>    CACHE                      = new AtomicReference<>();
    private static final List<FrontPluginSpi>       PLUGINS                    = loadPlugins();
    private static final IndexHtmlLoadingContentSpi INDEX_HTML_LOADING_CONTENT = loadIndexHtmlLoaderRenderer();


    // =========================================================================
    // INIT
    // =========================================================================
    private static List<FrontPluginSpi> loadPlugins() {
        final List<FrontPluginSpi> result = new SpiLoader().loadSpiServicesByPriority(FrontPluginSpi.class);
        return result == null ? new ArrayList<>() : result;
    }

    private static IndexHtmlLoadingContentSpi loadIndexHtmlLoaderRenderer() {
        return new SpiLoader().loadSpiServiceByPriority(IndexHtmlLoadingContentSpi.class,
                                                        new DefaultIndexLoadingContent());
    }


    // =========================================================================
    // RENDERING
    // =========================================================================
    public synchronized String render() {
        String result = CACHE.get();
        if (result == null) {
            final JsonBuilder buffer = new JsonBuilder();
            buffer.write(openTag(HTML));
            renderHead(buffer);
            renderBody(buffer);
            buffer.write(closeTag(HTML));
            result = buffer.toString();
            CACHE.set(result);
        }
        return result;
    }


    // =========================================================================
    // PROTECTED
    // =========================================================================
    protected void renderHead(final JsonBuilder result) {
        result.write(DOCTYPE_HTML).line();
        result.write(openTag(HEAD));
        result.write(tag(TITLE, () -> INDEX_HTML_LOADING_CONTENT.buildPluginTitle(PLUGINS)));
        result.write(autoClosableTag(LINK,
                                     HtmlAttribute.build("rel", "shortcut icon"),
                                     HtmlAttribute.build("type", "image/x-icon"),
                                     HtmlAttribute.build("type", buildPath(contextPath, buildPluginFavIcon()))
                                    ));

        result.write(autoClosableTag(BASE, HtmlAttribute.build("href", ".")));
        result.write(autoClosableTag(META,
                                     HtmlAttribute.build("name", "viewport"),
                                     HtmlAttribute.build("content", "width=device-width, initial-scale=1")));

        final List<String> cssFiles = PLUGINS.stream()
                                             .map(FrontPluginSpi::getCssFiles)
                                             .filter(Objects::nonNull)
                                             .flatMap(List::stream)
                                             .collect(Collectors.toList());
        writeCss(result, Arrays.asList(
                "/css/fontawesome.all.min.css",
                "/vendors/bootstrap/dist/css/bootstrap.min.css",
                "/vendors/bootstrap/dist/css/bootstrap-grid.min.css"));
        writeCss(result, cssFiles);


        final List<String> scripts = PLUGINS.stream()
                                            .map(FrontPluginSpi::getJavaScriptLink)
                                            .filter(Objects::nonNull)
                                            .flatMap(List::stream)
                                            .collect(Collectors.toList());
        writeJavaScript(result, Arrays.asList(
                "/js/fontawesome.all.min.js",
                "/vendors/jquery/dist/jquery.slim.min.js",
                "/vendors/holder/holder.min.js",
                "/vendors/popper/popper.min.js",
                "/vendors/systemjs/dist/system.js",
                "/vendors/zone.js/bundles/zone.umd.min.js",
                "/vendors/bootstrap/dist/js/bootstrap.min.js"
                                             ));
        writeJavaScript(result, scripts);

        result.addLine();
        result.write(tag(SCRIPT, this::renderMainScript, HtmlAttribute.build("type", "text/javascript")));
        result.write(closeTag(HEAD));
    }

    private void writeCss(final JsonBuilder result, final List<String> cssFiles) {
        for (final String css : cssFiles) {
            result.write(autoClosableTag(LINK,
                                         HtmlAttribute.build("href", buildPath(contextPath, css)),
                                         HtmlAttribute.build("rel", "stylesheet")));
        }
    }

    private void writeJavaScript(final JsonBuilder result, final List<String> scripts) {
        for (final String script : scripts) {
            result.write(tag(SCRIPT, null, HtmlAttribute.build("src", buildPath(contextPath, script))));
        }
    }

    protected String renderMainScript() {
        final JsonBuilder js = new JsonBuilder().line();
        js.write(DECO).line();
        js.write("// GLOBALS VALUES").line();
        js.write(DECO).line();
        js.write("const CONTEXT_PATH=").valueQuot(contextPath).addEndLine();
        js.write("document['CONTEXT_PATH']=CONTEXT_PATH").addEndLine();
        final String resourcePath = contextPath + PATH_SEP;
        js.write("const RESOURCES_PATH=").valueQuot(contextPath + "/js/").addEndLine();
        js.write("const APP_PATH=").valueQuot(resourcePath + "app").addEndLine();
        js.write("const VENDOR_PATH=").valueQuot(resourcePath + "vendors/").addEndLine();
        js.line();

        js.write(writeMessageProperties());
        js.line();

        js.write(DECO).line();
        js.write("// SYSTEM JS CONFIG").line();
        js.write(DECO).line();
        js.openTuple().writeFunction("", "global").openObject().line();

        js.write(renderSystemJsConfig()).line();
        js.write("if (global.filterSystemConfig)").openObject().line();
        js.tab().write("global.filterSystemConfig(config)").addEndLine();
        js.closeObject().line();

        js.write("System.config(config);").line();
        js.write("var app = System.import('/analysis/app/main.ts')").line();
        js.tab().write(".catch(console.error.bind(console));").line();
        js.tab().write("})(this);").line();
        js.line();
        return js.toString();
    }

    private String writeMessageProperties() {
        final JsonBuilder                      result     = new JsonBuilder();
        final Map<String, Map<String, String>> properties = loadPluginsMessageProperties();

        result.write("document['MESSAGES'] =").openObject().line();
        final Iterator<Map.Entry<String, Map<String, String>>> iterator = properties.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, Map<String, String>> messages = iterator.next();
            result.tab().addField(messages.getKey()).openObject().line();

            final Iterator<Map.Entry<String, String>> entryIterator = messages.getValue().entrySet().iterator();
            while (entryIterator.hasNext()) {
                final Map.Entry<String, String> entry = entryIterator.next();
                result.tab().tab().addField(entry.getKey()).valueQuot(entry.getValue());
                if (entryIterator.hasNext()) {
                    result.addSeparator().line();
                }
                else {
                    result.line();
                }
            }
            result.tab().closeObject();
            if (iterator.hasNext()) {
                result.addSeparator().line();
            }
            else {
                result.line();
            }
        }
        result.closeObject().addEndLine();
        return result.toString();
    }


    private String renderSystemJsConfig() {
        final JsonBuilder js = new JsonBuilder().line();
        js.write("SystemJS.typescriptOptions = ").openObject().line();
        js.tab().addField("target").valueQuot("es5").addSeparator().line();
        js.tab().addField("module").valueQuot("system").addSeparator().line();
        js.tab().addField("moduleResolution").valueQuot("node").addSeparator().line();
        js.tab().addField("sourceMap").write(true).addSeparator().line();
        js.tab().addField("emitDecoratorMetadata").write(true).addSeparator().line();
        js.tab().addField("experimentalDecorators").write(true).addSeparator().line();
        js.tab().addField("noImplicitAny").write(true).addSeparator().line();
        js.tab().addField("suppressImplicitAnyIndexErrors").write(true).line();
        js.closeObject().addEndLine().line();

        // config
        js.write("var config =").openObject().line();
        js.tab().write("transpiler").write(DDOT).valueQuot("ts").addSeparator().line();

        // meta
        js.tab().write("meta").write(DDOT).openObject().line();
        js.tab().tab().write("typescript").write(DDOT).openObject().line();
        js.tab().tab().tab().write("exports").write(DDOT).valueQuot("ts").line();
        js.tab().tab().closeObject().line();
        js.tab().closeObject().addSeparator().line();

        // bundles
        js.tab().write("bundles").write(DDOT).openObject().line();
        js.tab().tab().addField(contextPath + "/vendors/rxjs-system-bundle/Rx.system.min.js").openList().line();
        final Iterator<String> bundleIterator = List.of("rxjs",
                                                        "rxjs/*",
                                                        "rxjs/operator/*",
                                                        "rxjs/operators/*",
                                                        "rxjs/observable/*",
                                                        "rxjs/scheduler/*",
                                                        "rxjs/symbol/*",
                                                        "rxjs/add/operator/*",
                                                        "rxjs/add/observable/*",
                                                        "rxjs/util/*").iterator();
        while (bundleIterator.hasNext()) {
            js.tab().tab().tab().valueQuot(bundleIterator.next());
            if (bundleIterator.hasNext()) {
                js.addSeparator().line();
            }
            else {
                js.line();
            }
        }
        js.tab().tab().closeList().line();
        js.tab().closeObject().addSeparator().line();

        // map
        js.tab().write("map").write(DDOT).openObject().line();
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("app", "APP_PATH");
        map.put("@angular/core", "VENDOR_PATH + '@angular/core/bundles/core.umd.min.js'");
        map.put("@angular/compiler", "VENDOR_PATH + '@angular/compiler/bundles/compiler.umd.min.js'");
        map.put("@angular/common", "VENDOR_PATH + '@angular/common/bundles/common.umd.min.js'");
        map.put("@angular/platform-browser",
                "VENDOR_PATH + '@angular/platform-browser/bundles/platform-browser.umd.min.js'");
        map.put("@angular/platform-browser-dynamic",
                "VENDOR_PATH + '@angular/platform-browser-dynamic/bundles/platform-browser-dynamic.umd.min.js'");
        map.put("@angular/common/http", "VENDOR_PATH + '@angular/common/bundles/common-http.umd.min.js'");
        map.put("@angular/forms", "VENDOR_PATH + '@angular/forms/bundles/forms.umd.min.js'");
        map.put("@angular/platform-browser/animations",
                "VENDOR_PATH + '@angular/platform-browser/bundles/platform-browser-animations.umd.min.js'");
        map.put("@angular/animations/browser",
                "VENDOR_PATH + '@angular/platform-browser/bundles/platform-browser.umd.min.js'");
        map.put("@angular/animations", "VENDOR_PATH + '@angular/animations/bundles/animations.umd.min.js'");
        map.put("@angular/router", "VENDOR_PATH + '@angular/router/bundles/router.umd.min.js'");
        map.put("ts", "VENDOR_PATH + 'plugin-typescript/lib/plugin.js'");
        map.put("typescript", "VENDOR_PATH + 'typescript/lib/typescript.min.js'");
        map.put("d3", "VENDOR_PATH + 'd3js/d3.min.js'");

        for (final FrontPluginSpi plugin : PLUGINS) {
            if (plugin.getVendorModulesMap() != null) {
                map.putAll(plugin.getVendorModulesMap());
            }
        }

        final Iterator<Map.Entry<String, String>> mapIterator = map.entrySet().iterator();
        while (mapIterator.hasNext()) {
            final Map.Entry<String, String> mapEntry = mapIterator.next();
            js.tab().tab().tab().addField(mapEntry.getKey()).write(mapEntry.getValue());
            if (mapIterator.hasNext()) {
                js.addSeparator().line();
            }
            else {
                js.line();
            }
        }
        js.tab().closeObject().addSeparator().line();

        // packages
        js.tab().write("packages").write(DDOT).openObject().line();
        js.tab().tab().write("\"app\"                    : { defaultExtension: 'ts' }").line();
        js.tab().closeObject().line();
        js.closeObject().addEndLine();
        return js.toString();
    }

    protected void renderBody(final JsonBuilder result) {
        result.write(openTag(BODY, HtmlAttribute.build("class", buildPluginsClasses())));
        result.write(openTag(APP_ROOT));
        result.write(INDEX_HTML_LOADING_CONTENT.getLoadingContent(contextPath, PLUGINS));
        result.write(closeTag(APP_ROOT));
        result.write(closeTag(BODY));
    }

    // =========================================================================
    // PLUGINS TOOLS
    // =========================================================================
    private String buildPluginsClasses() {
        final List<String> values = PLUGINS.stream()
                                           .map(FrontPluginSpi::getRootClass)
                                           .filter(Objects::nonNull)
                                           .collect(Collectors.toList());

        values.add("loading");
        return String.join(SPACE, values);
    }


    private String buildPluginFavIcon() {
        return PLUGINS.stream()
                      .map(FrontPluginSpi::getFavIcon)
                      .filter(Objects::nonNull)
                      .findFirst()
                      .orElse("/favicon.ico");
    }

    private Map<String, Map<String, String>> loadPluginsMessageProperties() {
        final Map<String, Map<String, String>> buffer = new LinkedHashMap<>();
        for (final FrontPluginSpi plugin : PLUGINS) {
            final Map<String, Map<String, String>> properties = loadPluginProperties(plugin.getMessageProperties());

            for (final Map.Entry<String, Map<String, String>> entry : properties.entrySet()) {
                final Map<String, String> localResult = buffer.get(entry.getKey());
                if (localResult == null) {
                    buffer.put(entry.getKey(), entry.getValue());
                }
                else {
                    localResult.putAll(entry.getValue());
                }
            }
        }

        return orderProperties(buffer);
    }


    private Map<String, Map<String, String>> loadPluginProperties(final List<String> files) {
        final Map<String, Map<String, String>> result = new LinkedHashMap<>();

        for (final String propertiesFile : files) {
            try (final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propertiesFile)) {
                final String     local      = extractLocal(propertiesFile);
                final Properties properties = new Properties();
                properties.load(inputStream);

                final Map<String, String> subResult = new LinkedHashMap<>();
                for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        subResult.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                    }
                }

                final Map<String, String> localResult = result.get(local);
                if (localResult == null) {
                    result.put(local, subResult);
                }
                else {
                    localResult.putAll(subResult);
                }
            }
            catch (final Exception error) {
                log.error(error.getMessage(), error);
            }

        }
        return result;
    }

    private String extractLocal(final String propertiesFile) {
        String result = "en";
        if (propertiesFile.contains("_") && propertiesFile.contains(".properties")) {
            result = propertiesFile.substring(propertiesFile.lastIndexOf("_") + 1, propertiesFile.lastIndexOf("."));
        }
        return result;
    }

    private Map<String, Map<String, String>> orderProperties(final Map<String, Map<String, String>> buffer) {
        final Map<String, Map<String, String>> result = new LinkedHashMap<>();

        final List<String> keys = new ArrayList<>(buffer.keySet());
        Collections.sort(keys);

        for (final String key : keys) {
            final Map<String, String> properties   = buffer.get(key);
            final Map<String, String> subResult    = new LinkedHashMap<>();
            final List<String>        propertyKeys = new ArrayList<>(properties.keySet());
            Collections.sort(propertyKeys);

            for (final String propertyKey : propertyKeys) {
                subResult.put(propertyKey, properties.get(propertyKey));
            }

            result.put(key, subResult);
        }
        return result;
    }
}
