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
package io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IssueTrackerCommons {


    // =========================================================================
    // ENUM
    // =========================================================================
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    public static enum TicketType {
        MERGE_REQUEST("MergeRequest", "pr_"),
        ISSUE("Issue", "issue_"),
        ISSUE_LABEL("IssueLabel", "issue_label_");

        private final String nodeType;
        private final String nodePrefix;
    }

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String HAS_ISSUE_LINK      = "HAS_ISSUE_LINK";
    public static final String TICKET_HAVE_VERSION = "TICKET_HAVE_VERSION";
    public static final String HAVE_TICKET         = "HAVE_TICKET";
    public static final String HAS_LABEL           = "HAS_LABEL";
    public static final String FIELD_URL           = "url";
    public static final String FIELD_DESCRIPTION   = "description";
    public static final String FIELD_CREATED_AT    = "created_at";
    public static final String FIELD_MERGED_AT     = "merged_at";
    public static final String FIELD_CLOSED_AT     = "closed_at";
    public static final String FIELD_BASE_SHA      = "base_sha";
    public static final String FIELD_HEAD_SHA      = "head_sha";
    public static final String PR_URL              = "inugami.maven.plugin.analysis.issue.tracker.pr.url";
    public static final String FIELD_IID           = "iid";
    public static final String UUID                = "id";
}
