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
import io.inugami.api.exceptions.services.ConnectorException;
import io.inugami.api.spi.SpiLoader;
import io.inugami.commons.connectors.HttpBasicConnector;
import io.inugami.commons.connectors.HttpConnectorResult;
import io.inugami.commons.connectors.HttpRequest;
import io.inugami.commons.files.FilesUtils;
import io.inugami.maven.plugin.analysis.api.convertors.PropertiesConvertorSpi;
import io.inugami.maven.plugin.analysis.api.models.PropertiesResources;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.inugami.api.exceptions.Asserts.*;
import static io.inugami.maven.plugin.analysis.plugin.services.build.exceptions.BasicBuildError.*;

@Slf4j
public class PropertiesLoader {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final List<PropertiesConvertorSpi> CONVERTORS    = SpiLoader.getInstance().loadSpiServicesByPriority(
            PropertiesConvertorSpi.class);
    public static final  String                       AUTHORIZATION = "Authorization";

    // =========================================================================
    // LOAD PROPERTIES
    // =========================================================================
    public Map<String, String> loadProperties(final PropertiesResources propertiesResources) {
        assertNotNull(PROPERTIES_RESOURCES_REQUIRED, propertiesResources);
        assertFalse(FILE_OR_URL_REQUIRE,
                    propertiesResources.getPropertiesPath() == null && propertiesResources.getPropertiesUrl() == null);

        Map<String, String> result = propertiesResources.getPropertiesPath() == null
                ? loadUrlProperties(propertiesResources)
                : loadFileProperties(propertiesResources);

        if (propertiesResources.getProperties() != null) {
            if (result == null) {
                result = new LinkedHashMap<>();
            }
            result.putAll(propertiesResources.getProperties());
        }
        return result;
    }


    // =========================================================================
    // LOAD FILE
    // =========================================================================
    private Map<String, String> loadFileProperties(final PropertiesResources propertiesResources) {
        final File file = new File(propertiesResources.getPropertiesPath());

        assertTrue(
                FILE_NOT_EXISTS.addDetail("can't load properties, file {0} doesn't exists", file.getAbsoluteFile()),
                file.exists());

        String content = null;
        try {
            final Charset charset = propertiesResources.getEncoding() == null
                    ? StandardCharsets.UTF_8
                    : Charset.forName(propertiesResources.getEncoding());
            content = new String(FilesUtils.readBytes(file), charset);
        } catch (final IOException e) {
            throw new FatalException(e.getMessage(), e);
        }
        return convertToMap(content, propertiesResources.getType());
    }

    // =========================================================================
    // LOAD URL
    // =========================================================================
    private Map<String, String> loadUrlProperties(final PropertiesResources propertiesResources) {
        assertNotEmpty(URL_REQUIRE, propertiesResources.getPropertiesUrl());

        final ResponseContentType content = callExternalApi(propertiesResources);

        PropertiesConvertorSpi convertor = null;

        if (content.getContent() == null) {
            log.error("no response data from : {}", propertiesResources.getPropertiesUrl());
        } else {
            convertor = resolveConvertor(content.getContentType());
        }


        if (convertor == null) {
            log.error("can't resolve properties convertor from contentTye : {} sent by {}", content.getContentType(),
                      propertiesResources.getPropertiesUrl());
        }


        return convertor == null ? null : convertor.convert(content.getContent());
    }

    private ResponseContentType callExternalApi(final PropertiesResources propertiesResources) {
        final HttpBasicConnector http = new HttpBasicConnector();

        final Map<String, String> headers = new LinkedHashMap<>();
        if (propertiesResources.getPropertiesUrlAuthorization() != null) {
            headers.put(AUTHORIZATION, propertiesResources.getPropertiesUrlAuthorization());
        }

        HttpConnectorResult response = null;
        try {
            response = http.get(HttpRequest.builder()
                                           .url(propertiesResources.getPropertiesUrl())
                                           .headers(headers)
                                           .build());
        } catch (final ConnectorException e) {
            log.error(e.getMessage(), e);
        } finally {
            http.close();
        }

        if (response == null || response.getStatusCode() >= 400) {
            throw new FatalException(String.format("[HTTP-%s] error on calling %s : %s",
                                                   response == null ? 500 : response.getStatusCode(),
                                                   propertiesResources.getPropertiesUrl(),
                                                   response == null ? "undefined error" : response.getMessage()));
        }

        return ResponseContentType.builder()
                                  .content(new String(response.getData(), response.getCharset()))
                                  .contentType(response.getContentType())
                                  .build();
    }


    @Builder
    @AllArgsConstructor
    @Getter
    @ToString
    private static class ResponseContentType {
        private final String content;
        private final String contentType;
    }

    // =========================================================================
    // CONVERTOR
    // =========================================================================
    private Map<String, String> convertToMap(final String content, final String type) {
        final PropertiesConvertorSpi convertor = resolveConvertor(type);
        return convertor == null ? null : convertor.convert(content);
    }

    private PropertiesConvertorSpi resolveConvertor(final String type) {
        PropertiesConvertorSpi result = null;
        for (final PropertiesConvertorSpi convertor : CONVERTORS) {
            if (convertor.accept(type)) {
                result = convertor;
                break;
            }
        }
        return result;
    }


}
