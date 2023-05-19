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
package io.inugami.maven.plugin.analysis.front.api;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@SuppressWarnings({"java:S1845"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RenderingConstants {
    public static final String APP_ROOT          = "app-root";
    public static final String HTML              = "html";
    public static final String BODY              = "body";
    public static final String HEAD              = "head";
    public static final String TAG_OPEN          = "<";
    public static final String TAG_CLOSE         = ">";
    public static final String TAG_OPEN_CLOSABLE = "</";
    public static final String SPACE             = " ";
    public static final String EQUALS            = "=";
    public static final String TAG_AUTO_CLOSABLE = "/>";
    public static final String DOCTYPE_HTML      = "<!DOCTYPE html>";
    public static final String DIV               = "div";
    public static final String IMG               = "img";
    public static final String H1                = "h1";
    public static final String H2                = "h2";
    public static final String TITLE             = "title";
    public static final String LINK              = "link";
    public static final String BASE              = "base";
    public static final String META              = "meta";
    public static final String SCRIPT            = "script";
    public static final String DECO              = "// =====================================================================";
    public static final String PATH_SEP          = "/";
    public static final String DDOT              = ":";
}
