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
package io.inugami.maven.plugin.analysis.api.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

    public static final String PROJECT_BASE_DIR  = "project.basedir";
    public static final String PROJECT_BUILD_DIR = "project.build.directory";
    public static final String INTERACTIVE       = "interactive";
    public static final String PREVIOUS_VERSION  = "previousVersion";

    public static final String GAV_SEPARATOR  = ":";
    public static final String EMPTY          = " ";
    public static final String UNDERSCORE     = "_";
    public static final String LINE_DECO      = "-";
    public static final String ISSUES         = "Issues";
    public static final String MERGE_REQUESTS = "Merge request";
    public static final String NAME           = "name";
    public static final String EMAIL          = "email";
    public static final String AUTHORS        = "Authors";

    public static final String GROUP_ID    = "groupId";
    public static final String ARTIFACT_ID = "artifactId";
    public static final String VERSION     = "version";


}
