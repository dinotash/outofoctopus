package com.outofoctopus.encryption

import static com.google.common.truth.Truth.assertThat

import com.google.inject.Guice

class EncryptionClientTest extends GroovyTestCase {
    private static final String TEST_KEY = "test"
    private static final String OTHER_KEY = "octopus-users"
    private static final String PLAIN_TEXT = "How much is that Gutenberg Bible in the window?"

    // Get a different response from encryption each time, but all can be decoded with the right key
    private static final String EXPECTED_CIPHER = "CiQAR3Mpnd/diGRuyrZMTTdULzoMc+y4XnjiyXZQyag8L4nf878SWABUdOiQHF8jJzCZpYH+W74Podw8dgIOupabLXPwGH9F4dWfgAngq22Kg7F3xU3PPGTNIci32AfFi163icOoakR8nP3beU96KexHK6VToBZa6C/ljNBjOtU="
    private static final String EXPECTED_CIPHER_2 = "CiQAR3MpndIcuHElnPg6G5NvkFeAbQWsqv0CbzrnGGVF3klPrDASWABUdOiQt7dzvcJNUlLzR/4hb4Co2YwPFjbzTTWImjw7j1fHp2iNQAw6Le67A4Wn+8kkgWpMlprUgC5bNrMgbfVtdPti6spiLtuiHScPdiUPoNxGp5msaAk"

    private static EncryptionClient encryptionClient

    void setUp() {
        encryptionClient = Guice.createInjector(new EncryptionModule()).getInstance(EncryptionClient.class)
    }

    void testEncryptDecrypt() throws Exception {
        String cipher = encryptionClient.encrypt(TEST_KEY, PLAIN_TEXT)
        assertThat(encryptionClient.decrypt(TEST_KEY, cipher)).isEqualTo(PLAIN_TEXT)
    }

    void testDecrypt() throws Exception {
        assertThat(encryptionClient.decrypt(TEST_KEY, EXPECTED_CIPHER)).isEqualTo(PLAIN_TEXT)
        assertThat(encryptionClient.decrypt(TEST_KEY, EXPECTED_CIPHER_2)).isEqualTo(PLAIN_TEXT)
    }

    void testErrorWithWrongKey() throws Exception {
        try {
            String cipher = encryptionClient.encrypt(TEST_KEY, PLAIN_TEXT)
            encryptionClient.decrypt(OTHER_KEY, cipher)
        } catch (IllegalArgumentException e) {
            // threw expected error
        }
    }
}