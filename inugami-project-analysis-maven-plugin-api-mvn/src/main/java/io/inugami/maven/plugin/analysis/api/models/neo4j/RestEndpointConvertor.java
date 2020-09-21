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
package io.inugami.maven.plugin.analysis.api.models.neo4j;

import io.inugami.maven.plugin.analysis.api.models.rest.RestEndpoint;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.neo4j.driver.types.Node;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RestEndpointConvertor {

    public static RestEndpoint build(final Node node) {
        return node == null ? null : RestEndpoint.builder()
                                                 .uid(node.get("name").asString())
                                                 .uri(node.get("uri").asString())
                                                 .verb(node.get("verb").asString())
                                                 .nickname(node.get("nickname").asString())
                                                 .headers(node.get("header").asString())
                                                 .consume(node.get("accept").asString())
                                                 .produce(node.get("contentType").asString())
                                                 .body(node.get("requestPayload").asString())
                                                 .responseType(node.get("responsePayload").asString())
                                                 .description(node.get("description").asString())
                                                 .build();
    }


}
