package org.fptn.vpn.utils;

import org.brotli.dec.BrotliInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BrotliUtils {
    public static String decodeBrotliString(String base64CompressedData) throws IOException {
        // 1. Convert Base64 string back to compressed byte array
        byte[] compressedBytes = Base64.getDecoder().decode(base64CompressedData);

        // 2. Use BrotliInputStream to decompress
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
             BrotliInputStream bis = new BrotliInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = bis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }

            // 3. Convert decompressed bytes back to UTF-8 String
            return baos.toString(StandardCharsets.UTF_8.name());
        }
    }
}
