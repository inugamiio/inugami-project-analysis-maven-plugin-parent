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
package io.inugami.maven.plugin.analysis.plugin.services.writer.neo4j;

import io.inugami.api.models.JsonBuilder;
import io.inugami.maven.plugin.analysis.api.actions.Neo4jValueEncoder;
import io.inugami.maven.plugin.analysis.api.utils.NodeUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class DefaultNeo4jEncoder implements Neo4jValueEncoder {

    public static final String DATE_ISO                   = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    // =========================================================================
    // API
    // =========================================================================
    @Override
    public String encode(final Object value) {
        String result = null;
        if (value != null) {
            result = processEncoding(value);
        }
        return result;
    }

    private String processEncoding(final Object value) {
        String result = null;
        if (value instanceof Boolean) {
            result = String.valueOf(value);
        }
        else if (isNumber(value)) {
            result = encodeNumber(value);
        }
        else if(isDate(value)){
            result = quotValue(encodeDate(value));
        }
        else {
            result = quotValue(NodeUtils.cleanLines(String.valueOf(value)));
        }
        return result;
    }



    // =========================================================================
    // NUMBER
    // =========================================================================
    private boolean isNumber(final Object value) {
        return value instanceof Short ||
                value instanceof Integer ||
                value instanceof Long ||
                value instanceof Double ||
                value instanceof BigInteger ||
                value instanceof BigDecimal;
    }

    private String encodeNumber(final Object value) {
        return String.valueOf(value);
    }


    // =========================================================================
    // DATE
    // =========================================================================
    private boolean isDate(final Object value) {
        return value instanceof Date ||
                value instanceof Calendar ||
                value instanceof LocalDate ||
                value instanceof LocalDateTime ||
                value instanceof ZonedDateTime ;
    }
    private String encodeDate(final Object value) {
        String result = null;
        try{
            if(value instanceof Date){
                result = new SimpleDateFormat(DATE_ISO).format((Date)value);
            }else if(value instanceof Calendar){
                result = new SimpleDateFormat(DATE_ISO).format(((Calendar)value).getTime());
            }else if(value instanceof LocalDate){
                result = ((LocalDate)value).format(DateTimeFormatter.ISO_DATE);
            }else if(value instanceof LocalDateTime){
                result = ((LocalDateTime)value).format(DateTimeFormatter.ISO_DATE);
            }else if(value instanceof ZonedDateTime){
                result = ((ZonedDateTime)value).format(DateTimeFormatter.ISO_DATE);
            }
        }catch (final Exception e){
        }

        return result;
    }

    // =========================================================================
    // OVERRIDES
    // =========================================================================
    private String quotValue(final String value) {
        return new JsonBuilder().valueQuot(value).toString();
    }

}
