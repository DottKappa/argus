package com.argus.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApiKeyProviderTest {

    @Test
    void testObfuscateAndDeobfuscate() {
        String originalKey = "4B75FE893A0D1234567890ABCDEF1234";
        String obfuscated = ApiKeyProvider.obfuscate(originalKey);
        
        assertNotEquals(originalKey, obfuscated);
        assertFalse(obfuscated.contains(originalKey));
        
        String recovered = ApiKeyProvider.deobfuscate(obfuscated);
        assertEquals(originalKey, recovered);
    }
}
