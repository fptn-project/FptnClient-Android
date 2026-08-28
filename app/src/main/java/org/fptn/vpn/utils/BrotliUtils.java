/*
 * FPTN Android Client
 * Copyright (C) 2026  Skokov Stanislav, Enin Sergey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Website: https://fptn.org
 */

package org.fptn.vpn.utils;

import android.os.Build;

import org.brotli.dec.BrotliInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BrotliUtils {
    public static String decodeBrotliString(String base64CompressedData) throws IOException {
        // 1. Convert Base64 string back to compressed byte array
        byte[] compressedBytes = Build.VERSION.SDK_INT > 0 && Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                ? android.util.Base64.decode(base64CompressedData, android.util.Base64.DEFAULT)
                : Base64.getDecoder().decode(base64CompressedData);

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
