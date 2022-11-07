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
package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.webmethod;

import lombok.*;

import java.util.List;
import java.util.Set;

@Builder(toBuilder = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Getter
@AllArgsConstructor
public class WebServiceInfoDTO {

    @EqualsAndHashCode.Include
    private final String name;

    @EqualsAndHashCode.Include
    private final String targetNamespace;

    private final String serviceName;

    private final String portName;

    private final String wsdlLocation;

    private final String endpointInterface;

    private final boolean soap;

    private final List<String> roles;

    private final String rootContext;

    private final String authMethod;

    private final String urlPattern;

    private final String  virtualHost;
    private final String  transportGuarantee;
    private final boolean secureWSDLAccess;
    private final String  realmName;

    private final String consume;
    private final String produce;
    private final String encoding;
    private final String description;

    private final Set<WebServiceHeaderDTO> headers;

}
