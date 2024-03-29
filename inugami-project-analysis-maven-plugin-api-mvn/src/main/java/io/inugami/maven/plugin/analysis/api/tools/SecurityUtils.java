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
package io.inugami.maven.plugin.analysis.api.tools;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

@SuppressWarnings({"java:S5542", "java:S3329"})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class SecurityUtils {


    public static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    public static final String AES              = "AES";

    // =========================================================================
    // ENCODE
    // =========================================================================
    public static String encodeAes(final String value, final String secret) {
        if (secret == null) {
            return value;
        }
        byte[] encrypted = null;
        try {

            final byte[] raw      = secret.getBytes();
            final Key    skeySpec = new SecretKeySpec(raw, AES);
            final Cipher cipher   = Cipher.getInstance(CIPHER_ALGORITHM);
            final byte[] iv       = new byte[cipher.getBlockSize()];

            final IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivParams);
            encrypted = cipher.doFinal(value.getBytes());

        } catch (final Exception e) {
            throw new SecurityException(e.getMessage(), e);
        }
        return encrypted == null ? null : Base64.getEncoder().encodeToString(encrypted);
    }

    // =========================================================================
    // DECODE
    // =========================================================================
    public static String decodeAes(final Object value, final Object secret) {
        return decodeAes(value instanceof String ? (String) value : null,
                         secret instanceof String ? (String) secret : null);
    }

    public static String decodeAes(final String value, final String secret) {
        if (secret == null) {
            return value;
        }
        byte[] original = null;

        try {
            final byte[]          raw          = secret.getBytes();
            final Key             key          = new SecretKeySpec(raw, AES);
            final Cipher          cipher       = Cipher.getInstance(CIPHER_ALGORITHM);
            final byte[]          ivByte       = new byte[cipher.getBlockSize()];
            final IvParameterSpec ivParamsSpec = new IvParameterSpec(ivByte);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParamsSpec);
            original = cipher.doFinal(Base64.getDecoder().decode(value));
        } catch (final Exception e) {
            throw new SecurityException(e.getMessage(), e);
        }
        return original == null ? null : new String(original);
    }
}
