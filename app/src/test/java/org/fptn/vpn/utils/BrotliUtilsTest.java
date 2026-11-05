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

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.fptn.vpn.utils.token.TokenValidationUtils;
import org.fptn.vpn.utils.token.Token;
import org.fptn.vpn.vpnclient.exception.PVNClientException;
import org.junit.Test;

import java.io.IOException;

public class BrotliUtilsTest {
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    public void testDecodeBrotliString() throws IOException, PVNClientException {
        //given
        String compressed = "H0oLEVWiAEaLBENv7yR7HQ9cBNG3C5SFGsLjTvRSFjjtP1W5IDZWJ9ckQN3hXT9Yqobk8cb2gRIs1dBJEIW+Yuma//v554yGkTob6Qu8OMAJRFsFRYRFFg7zYzTgvn3j4YBesA8Mv4J6KQd9zizTv4+Csx+QRyZZl9A+VOTlN955DX5EXEl+KasynzQlREKiA6U0UtYSJf+FV95676d3/3rnUVGxl/vrw8f/PpyfXvn6t69+3z98vZ8F0tYhDX+OgMqAQZjA+33p59cTkTW8NAnF4P8YHKx/eyZvm1lpyi0tol51vWb0P/5S8AUx/w+tottWsNTk8ZJ6uwAa0uXZI9pLbvQawtpEKr1yambfAkQ3gxZoYAPEEi6uLkQJ3823mxmPdmUkW12VC/273/b67d2gtIfQ0EE3oNZ/6DllEzbYREPGC4vQXazlog75Kccq6TAgcbKLHII0M7rvqvLIS8RClV4U0cXL90ZSqBIbrkHlg7QJV2LmdtlGNkkWZp3QNgppPu6Q7sRaubfV4SpmFMRBujGTVeuiW0SiNW0LX6mstPZFVljRhJY1wvCUcGutnMrbh4KVLJhad/EtXcSyoZj2VGhWKaEDQeWByEnXjY1bMpf37j2M57i7yh7x3AiIAj4gGZFIwBGugqnSLftsPmjR2qgoZ+dhwUDpCLSCuim0gwO0wjNEc0uCzdFpwtdFR5nbdCairR2ViSG6kzQQA3BAaa5Kdi3dcXvoulB4VmN/CBULk2wBaVqEdotWBPKMcOUvYkvXHYk22qQrTFEJbR/lhQqTtQpt+54IBoVASsDoXNlJyrFsh8pNrFa9wewijyvlpLef676GaEvexwU2hBsePSFpo8F+bUelT54qo1EJKObDyhlpxfYwTGxmiUeE28dbL24PrjFrGTtLFuFsuj4I+TkLn3uWzxP5nV+fjP5h8kBmUAuwOhApuFHrwQimeiqFj7ofk+G2oZEsHUvaA9obMg7A2/NYIYXPA+xFYIJ6K430PpN+95sCxUUaQQKydp9YRLnRzOC4/cOvnr3r8znq7p/PYypon/j9+Ov98U9L4xEo4BrEiFWBhpn2iMpJnrs4kubsLRKdE077Aw";

        // when
        String decodedToken = BrotliUtils.decodeBrotliString(compressed);

        //then
        Token token = OBJECT_MAPPER.readValue(decodedToken, Token.class);
        TokenValidationUtils.validate(token);
        assertEquals("user242451984", token.getUsername());
        assertEquals("CLRVpUxT", token.getPassword());
    }
}
