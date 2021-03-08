// Generated by delombok at Mon Mar 08 22:38:55 CET 2021
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

import io.inugami.api.models.data.basic.JsonObject;
import lombok.*;
import static io.inugami.maven.plugin.analysis.api.utils.Constants.UNDERSCORE;

public class ErrorCodeDTO implements Comparable<ErrorCodeDTO>, JsonObject {
    private static final long serialVersionUID = -5469719611823975510L;
    String errorCode;
    Integer statusCode;
    String message;
    String type;
    String artifact;

    @Override
    public int compareTo(final ErrorCodeDTO other) {
        final String currentHash = String.join(UNDERSCORE, String.valueOf(artifact), String.valueOf(errorCode));
        final String otherHash = String.join(UNDERSCORE, String.valueOf(other == null ? null : other.getArtifact()), String.valueOf(other == null ? null : other.getErrorCode()));
        return currentHash.compareTo(otherHash);
    }


    @java.lang.SuppressWarnings("all")
    public static class ErrorCodeDTOBuilder {
        @java.lang.SuppressWarnings("all")
        private String errorCode;
        @java.lang.SuppressWarnings("all")
        private Integer statusCode;
        @java.lang.SuppressWarnings("all")
        private String message;
        @java.lang.SuppressWarnings("all")
        private String type;
        @java.lang.SuppressWarnings("all")
        private String artifact;

        @java.lang.SuppressWarnings("all")
        ErrorCodeDTOBuilder() {
        }

        @java.lang.SuppressWarnings("all")
        public ErrorCodeDTO.ErrorCodeDTOBuilder errorCode(final String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public ErrorCodeDTO.ErrorCodeDTOBuilder statusCode(final Integer statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public ErrorCodeDTO.ErrorCodeDTOBuilder message(final String message) {
            this.message = message;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public ErrorCodeDTO.ErrorCodeDTOBuilder type(final String type) {
            this.type = type;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public ErrorCodeDTO.ErrorCodeDTOBuilder artifact(final String artifact) {
            this.artifact = artifact;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public ErrorCodeDTO build() {
            return new ErrorCodeDTO(this.errorCode, this.statusCode, this.message, this.type, this.artifact);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "ErrorCodeDTO.ErrorCodeDTOBuilder(errorCode=" + this.errorCode + ", statusCode=" + this.statusCode + ", message=" + this.message + ", type=" + this.type + ", artifact=" + this.artifact + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static ErrorCodeDTO.ErrorCodeDTOBuilder builder() {
        return new ErrorCodeDTO.ErrorCodeDTOBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public ErrorCodeDTO.ErrorCodeDTOBuilder toBuilder() {
        return new ErrorCodeDTO.ErrorCodeDTOBuilder().errorCode(this.errorCode).statusCode(this.statusCode).message(this.message).type(this.type).artifact(this.artifact);
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "ErrorCodeDTO(errorCode=" + this.getErrorCode() + ", statusCode=" + this.getStatusCode() + ", message=" + this.getMessage() + ", type=" + this.getType() + ", artifact=" + this.getArtifact() + ")";
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ErrorCodeDTO)) return false;
        final ErrorCodeDTO other = (ErrorCodeDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$errorCode = this.getErrorCode();
        final java.lang.Object other$errorCode = other.getErrorCode();
        if (this$errorCode == null ? other$errorCode != null : !this$errorCode.equals(other$errorCode)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ErrorCodeDTO;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $errorCode = this.getErrorCode();
        result = result * PRIME + ($errorCode == null ? 43 : $errorCode.hashCode());
        return result;
    }

    @java.lang.SuppressWarnings("all")
    public ErrorCodeDTO(final String errorCode, final Integer statusCode, final String message, final String type, final String artifact) {
        this.errorCode = errorCode;
        this.statusCode = statusCode;
        this.message = message;
        this.type = type;
        this.artifact = artifact;
    }

    @java.lang.SuppressWarnings("all")
    public ErrorCodeDTO() {
    }

    @java.lang.SuppressWarnings("all")
    public String getErrorCode() {
        return this.errorCode;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getStatusCode() {
        return this.statusCode;
    }

    @java.lang.SuppressWarnings("all")
    public String getMessage() {
        return this.message;
    }

    @java.lang.SuppressWarnings("all")
    public String getType() {
        return this.type;
    }

    @java.lang.SuppressWarnings("all")
    public String getArtifact() {
        return this.artifact;
    }
}
