package io.inugami.maven.plugin.analysis.api.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class SecurityUtilsTest {

    public static final String MY_SECRET_TOKEN_AES = "MySecretTokenAES";

    @Test
    void encodeAes_withAesValue_shouldDecode() {
        final String encoded = SecurityUtils.encodeAes("someValue", MY_SECRET_TOKEN_AES);
        final String result  = SecurityUtils.decodeAes(encoded, MY_SECRET_TOKEN_AES);
        assertThat(result).isEqualTo("someValue");

        assertThat(SecurityUtils.decodeAes("PrIKQQG0eF2j7FIeylOb0w==", MY_SECRET_TOKEN_AES)).isEqualTo("joefoobar");

    }

}