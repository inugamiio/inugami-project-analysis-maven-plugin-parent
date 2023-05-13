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
package io.inugami.maven.plugin.analysis.api.services.info.release.note.models;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Setter
@Getter
public class ReleaseNoteResult {
    GavInfo                   gav;
    ProjectDependenciesGraph  projectDependenciesGraph;
    Set<Map<String, Object>>  commit        = new LinkedHashSet<>();
    Set<Author>               authors       = new LinkedHashSet<>();
    List<Issue>               issues        = new ArrayList<>();
    List<MergeRequests>       mergeRequests = new ArrayList<>();
    Map<String, Differential> differentials = new LinkedHashMap<>();

    Map<String, Object> extractedInformation = new LinkedHashMap<>();
    Map<String, Object> customFields         = new LinkedHashMap<>();


    public ReleaseNoteResult addCommit(final Map<String, Object>... commit) {
        for (final Map<String, Object> item : commit) {
            if (item != null) {
                this.commit.add(item);
            }
        }
        return this;
    }

    public ReleaseNoteResult addAuthor(final Author value) {
        if (value != null) {
            authors.add(value);
        }
        return this;
    }

    public ReleaseNoteResult addIssue(final Issue value) {
        if (value != null) {
            issues.add(value);
        }
        return this;
    }

    public ReleaseNoteResult addMergeRequest(final MergeRequests value) {
        if (value != null) {
            mergeRequests.add(value);
        }
        return this;
    }

    public ReleaseNoteResult addDifferential(final String name, final Differential differential) {
        if (name != null && differential != null) {
            differentials.put(name, differential);
        }
        return this;
    }

    public ReleaseNoteResult addCustomField(final String name, final Object value) {
        if (name != null && value != null) {
            customFields.put(name, value);
        }
        return this;
    }

    public void addExtractedInformation(final String key, final Object value) {
        if (key != null && value != null) {
            extractedInformation.put(key, value);
        }
    }
}
