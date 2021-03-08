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
package io.inugami.maven.plugin.analysis.api.scan.issue.tracker;

import com.fasterxml.jackson.databind.JsonNode;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;

import java.io.Serializable;
import java.util.LinkedHashMap;

public interface JiraCustomFieldsAppender {
    void append(String issueId,
                JsonNode json,
                LinkedHashMap<String, Serializable> issueProperties,
                ScanNeo4jResult neo4jResult);
}
