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
package io.inugami.maven.plugin.analysis.plugin.services.scan.git;

import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.*;

@SuppressWarnings({"java:S5998", "java:S5361", "java:S5852","java:S5852","java:S6353","java:S1210"})
@Getter
public class TagRefName implements Comparable<TagRefName> {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final Pattern VERSION_REGEX = Pattern.compile("^([a-zA-Z-_.]*)((?:[0-9]+[._-]{0,1})+)(.*)$");
    public static final  int     MAX_TAG_NAME  = 256;
    private final        String  tagName;
    private              String  tagVersion;
    private              int     tagMajorVersion;
    private              int     tagMinorVersion;
    private              int     tagPatchVersion;
    private              String  tagProjectTagFlag;

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================
    public TagRefName(final String tagName) {
        this.tagName = tagName;
        if (tagName != null && tagName.length() < MAX_TAG_NAME) {
            final Matcher matcher = VERSION_REGEX.matcher(tagName);
            if (matcher.matches()) {
                tagVersion = cleanVersion(matcher.group(2)) + matcher.group(3);
                tagMajorVersion = extractMajorVersion(tagVersion);
                tagMinorVersion = extractMinorVersion(tagVersion);
                tagPatchVersion = extractPatchVersion(tagVersion);
                tagProjectTagFlag = extractTag(tagVersion);
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TagRefName{");
        sb.append(tagName);
        sb.append('}');
        return sb.toString();
    }

    private static String cleanVersion(final String version) {
        return version.replaceAll("_", ".").replaceAll("-", ".");
    }

    @Override
    public int compareTo(final TagRefName other) {
        int result = 0;
        if (other == null) {
            result = 1;
        } else if (tagVersion == null && other.getTagVersion() != null) {
            result = -1;
        } else if (tagVersion == null && other.getTagVersion() == null) {
            result = 0;
        } else {
            result = tagVersion == null ? -1 : tagVersion.compareTo(other.getTagVersion());
        }
        return result;
    }

    public boolean isPreviousOrSame(final int major, final int minor, final int patch) {
        return !isSuperior(major, minor, patch);
    }

    private boolean isSuperior(final int major, final int minor, final int patch) {
        return tagMajorVersion > major || tagMinorVersion > minor || tagPatchVersion > patch;
    }
}
