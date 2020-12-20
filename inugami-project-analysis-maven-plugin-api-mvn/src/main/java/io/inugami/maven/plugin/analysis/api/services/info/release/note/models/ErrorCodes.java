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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Getter
public class ErrorCodes {

    List<ErrorCodeDTO> newErrorCodes;
    List<ErrorCodeDTO> errorCodes;
    List<ErrorCodeDTO> deletedErrorCodes;

    public ErrorCodes addNewErrorCodes(final List<ErrorCodeDTO> values) {
        if(values!=null && !values.isEmpty()){
            if (newErrorCodes == null) {
                newErrorCodes = new ArrayList<>();
            }
            newErrorCodes.addAll(values);
        }
        return this;
    }


    public ErrorCodes addErrorCodes(final List<ErrorCodeDTO> values) {
        if(values!=null && !values.isEmpty()){
            if (errorCodes == null) {
                errorCodes = new ArrayList<>();
            }
            errorCodes.addAll(values);
        }
        return this;
    }


    public ErrorCodes addDeletedErrorCodes(final List<ErrorCodeDTO> values) {
        if(values!=null && !values.isEmpty()){
            if (deletedErrorCodes == null) {
                deletedErrorCodes = new ArrayList<>();
            }
            deletedErrorCodes.addAll(values);
        }
        return this;
    }

}
