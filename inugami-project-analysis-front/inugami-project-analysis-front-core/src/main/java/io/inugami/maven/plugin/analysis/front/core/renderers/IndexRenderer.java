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
import io.inugami.maven.plugin.analysis.front.api.models.HtmlAttribute;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.inugami.maven.plugin.analysis.front.api.RenderingConstants.*;

@RequiredArgsConstructor
public class IndexRenderer {



    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private final       String contextPath;

    private static final List<FrontPluginSpi> PLUGINS = loadPlugins();


    // =========================================================================
    // API
    // =========================================================================
    private static List<FrontPluginSpi> loadPlugins() {
        List<FrontPluginSpi> result = new SpiLoader().loadSpiServicesByPriority(FrontPluginSpi.class);
        return result == null ? new ArrayList<>() : result;
    }

    public String render() {
        final JsonBuilder result = new JsonBuilder();
        result.write(openTag(HTML));
        renderHead(result);
        renderBody(result);
        result.write(closeTag(HTML));
        return result.toString();
    }


    // =========================================================================
    // PROTECTED
    // =========================================================================
    protected void renderHead(final JsonBuilder result) {
        result.write(DOCTYPE_HTML).line();
        result.write(openTag(HEAD));
        result.write(tag(TITLE, () -> buildPluginTitle()));
        result.write(autoClosableTag(LINK,
                                     HtmlAttribute.build("rel", "shortcut icon"),
                                     HtmlAttribute.build("type", "image/x-icon"),
                                     HtmlAttribute.build("type", buildPath(contextPath, buildPluginFavIcon()))
                                    ));

        result.write(autoClosableTag(BASE, HtmlAttribute.build("href", contextPath)));
        result.write(autoClosableTag(META,
                                     HtmlAttribute.build("name", "viewport"),
                                     HtmlAttribute.build("content", "width=device-width, initial-scale=1")));


        final List<String> scripts = PLUGINS.stream()
                                            .map(FrontPluginSpi::getJavaScriptLink)
                                            .filter(Objects::nonNull)
                                            .flatMap(List::stream)
                                            .collect(Collectors.toList());
        for (String script : scripts) {
            result.write(tag(SCRIPT, null, HtmlAttribute.build("src", buildPath(contextPath, script))));
        }

        final List<String> cssFiles = PLUGINS.stream()
                                             .map(FrontPluginSpi::getCssFiles)
                                             .filter(Objects::nonNull)
                                             .flatMap(List::stream)
                                             .collect(Collectors.toList());
        for (String css : cssFiles) {
            result.write(autoClosableTag(LINK,
                             HtmlAttribute.build("href", buildPath(contextPath, css)),
                             HtmlAttribute.build("rel", "stylesheet")));
        }

        result.write(tag(SCRIPT, this::renderMainScript, HtmlAttribute.build("type", "text/javascript")));

        result.write(closeTag(HEAD));
    }

    protected String renderMainScript(){
        final JsonBuilder js = new JsonBuilder().line();
        js.write(DECO).line();
        js.write("// GLOBALS VALUES").line();
        js.write(DECO).line();
        js.write("const CONTEXT_PATH=").valueQuot(contextPath+ PATH_SEP).addEndLine();
        final String resourcePath = contextPath+ PATH_SEP+"js"+PATH_SEP;
        js.write("const RESOURCES_PATH=").valueQuot(resourcePath).addEndLine();
        js.write("const APP_PATH=").valueQuot(resourcePath+"app").addEndLine();
        js.write("const APP_PATH=").valueQuot(resourcePath+"vendors").addEndLine();
        js.line();

        js.write(DECO).line();
        js.write("// SYSTEM JS CONFIG").line();
        js.write(DECO).line();
        js.openTuple().writeFunction("","global").openObject().line();

        js.write(renderSystemJsConfig()).line();
        js.write("if (global.filterSystemConfig)").openObject().line();
        js.tab().write("global.filterSystemConfig(config)").addEndLine();
        js.closeObject().line();

        js.write("System.config(config);").line();
        js.write("System.import(APP_PATH+'/app.boot.ts').catch(console.error.bind(console));").line();

        js.closeObject().closeTuple();
        js.openTuple().write("this").closeTuple().addEndLine();


        return js.toString();
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

        // path
        js.tab().write("paths").write(DDOT).openObject().line();
        js.tab().tab().addField("vendor").write("VENDOR_PATH + \"/\"").line();
        js.tab().closeObject().addSeparator().line();

        // bundles
        js.tab().write("bundles").write(DDOT).openObject().line();
        js.tab().closeObject().addSeparator().line();

        // map
        js.tab().write("map").write(DDOT).openObject().line();
        js.tab().closeObject().addSeparator().line();

        // packages
        js.tab().write("packages").write(DDOT).openObject().line();
        js.tab().closeObject().line();

        js.closeObject().addEndLine();
        return js.toString();
    }

    protected void renderBody(final JsonBuilder result) {
        result.write(openTag(BODY, HtmlAttribute.build("class", "inugami-project-analysis " + buildPluginsClasses())));
        result.write(openTag(APP_COMPONENT));

        result.write(openTag(DIV, HtmlAttribute.build("class", "loading")));
        result.write(openTag(DIV, HtmlAttribute.build("class", "info")));

        result.write(tag(H1, () -> buildPluginTitle()));
        result.write(tag(H2, () -> "loading ..."));
        result.write(openTag(DIV, HtmlAttribute.build("class", "icon-loading")));
        result.write(autoClosableTag(IMG, HtmlAttribute.build("src",
                                                              buildPath(contextPath, buildPluginLoadingImage()))));
        result.write(closeTag(DIV));

        result.write(closeTag(DIV));
        result.write(closeTag(DIV));
        result.write(closeTag(APP_COMPONENT));
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

        return String.join(SPACE, values);
    }

    private String buildPluginTitle() {
        return PLUGINS.stream()
                      .map(FrontPluginSpi::getTitle)
                      .filter(Objects::nonNull)
                      .findFirst()
                      .orElse("Inugami project analysis");
    }

    private String buildPluginLoadingImage() {
        return PLUGINS.stream()
                      .map(FrontPluginSpi::getLoadingImage)
                      .filter(Objects::nonNull)
                      .findFirst()
                      .orElse("/images/loading.gif");
    }

    private String buildPluginFavIcon() {
        return PLUGINS.stream()
                      .map(FrontPluginSpi::getFavIcon)
                      .filter(Objects::nonNull)
                      .findFirst()
                      .orElse("/favicon.ico");
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    public static String openTag(final String tagName,
                                 final HtmlAttribute... attributes) {

        final JsonBuilder result = new JsonBuilder();
        if (tagName == null) {
            return result.toString();
        }
        result.write(TAG_OPEN).append(tagName);

        if (attributes.length > 0) {
            for (HtmlAttribute attribute : attributes) {
                result.write(SPACE);
                result.write(attribute.getName());
                result.write(EQUALS);
                result.valueQuot(attribute.getValue());
            }
        }
        result.write(TAG_CLOSE);
        result.line();
        return result.toString();
    }

    public static String tag(final String tagName,
                             final Supplier<String> appender,
                             final HtmlAttribute... attributes) {
        final JsonBuilder result = new JsonBuilder();
        if (result == null || tagName == null) {
            return result.toString();
        }
        result.write(TAG_OPEN).append(tagName);

        if (attributes.length > 0) {
            for (HtmlAttribute attribute : attributes) {
                result.write(SPACE);
                result.write(attribute.getName());
                result.write(EQUALS);
                result.valueQuot(attribute.getValue());
            }
        }
        result.write(TAG_CLOSE);
        if (appender != null) {
            result.write(appender.get());
        }

        return result.write(TAG_OPEN_CLOSABLE)
                     .write(tagName)
                     .write(TAG_CLOSE)
                     .line()
                     .toString();
    }

    public static String autoClosableTag(final String tagName,
                                         final HtmlAttribute... attributes) {
        final JsonBuilder result = new JsonBuilder();
        if (tagName == null) {
            return result.toString();
        }
        result.write(TAG_OPEN).append(tagName);

        if (attributes.length > 0) {
            for (HtmlAttribute attribute : attributes) {
                result.write(SPACE);
                result.write(attribute.getName());
                result.write(EQUALS);
                result.valueQuot(attribute.getValue());
            }
        }
        result.write(TAG_AUTO_CLOSABLE);
        result.line();
        return result.toString();
    }

    public static String closeTag(final String tagName) {
        return new JsonBuilder()
                .write(TAG_OPEN_CLOSABLE)
                .write(tagName)
                .write(TAG_CLOSE)
                .line()
                .toString();
    }

    private String buildPath(final String contextPath, final String resources) {
        return contextPath + (resources.startsWith(PATH_SEP) ? resources : PATH_SEP + resources);
    }
}
