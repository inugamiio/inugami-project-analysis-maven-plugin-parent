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
package io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models;

import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
public class ReleaseNoteResult {
    Set<String>         commit        = new LinkedHashSet<>();
    Set<Author>         authors       = new LinkedHashSet<>();
    List<Issue>         issues        = new ArrayList<>();
    List<MergeRequests> mergeRequests = new ArrayList<>();


    public ReleaseNoteResult addCommit(final String... commit) {
        for (final String item : commit) {
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
}
