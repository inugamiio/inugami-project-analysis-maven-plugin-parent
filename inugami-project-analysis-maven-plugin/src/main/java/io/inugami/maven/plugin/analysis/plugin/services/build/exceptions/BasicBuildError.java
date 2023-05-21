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
package io.inugami.maven.plugin.analysis.plugin.services.build.exceptions;

import io.inugami.api.exceptions.DefaultErrorCode;
import io.inugami.api.exceptions.ErrorCode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BasicBuildError {
    public static final ErrorCode PROPERTIES_RESOURCES_REQUIRED = DefaultErrorCode
            .builder()
            .errorCode("BASIC_BUILD_1")
            .message("resources configuration are required")
            .errorTypeConfiguration()
            .build();


    public static final ErrorCode FILE_OR_URL_REQUIRE = DefaultErrorCode
            .builder()
            .errorCode("BASIC_BUILD_2")
            .message("you need to define properties file path or properties url")
            .errorTypeConfiguration()
            .build();


    public static final ErrorCode FILE_NOT_EXISTS = DefaultErrorCode
            .builder()
            .errorCode("BASIC_BUILD_3")
            .message("file doesn't exists")
            .build();

    public static final ErrorCode URL_REQUIRE = DefaultErrorCode
            .builder()
            .errorCode("BASIC_BUILD_4")
            .message("url properties is required")
            .errorTypeConfiguration()
            .build();

    public static final ErrorCode TEMPLATE_FILE_NOT_EXISTS   = DefaultErrorCode
            .builder()
            .errorCode("BASIC_BUILD_5")
            .message("template file doesn't exists")
            .errorTypeConfiguration()
            .build();
    public static final ErrorCode TEMPLATE_FILE_NOT_READABLE = DefaultErrorCode
            .builder()
            .errorCode("BASIC_BUILD_5")
            .message("template file can't be read")
            .errorTypeConfiguration()
            .build();
}
