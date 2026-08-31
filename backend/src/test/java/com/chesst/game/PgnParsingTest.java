package com.chesst.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PgnParsingTest {

    @Test
    void pgnToSans_stripsHeadersAndMoveNumbers() {
        String pgn = "[Event \"Test\"]\n[White \"A\"]\n[Black \"B\"]\n[Result \"1-0\"]\n\n1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 1-0";
        String sans = GameService.pgnToSans(pgn);
        assertEquals("e4 e5 Nf3 Nc6 Bb5 a6", sans);
    }

    @Test
    void pgnToSans_handlesEmpty() {
        assertEquals("", GameService.pgnToSans(null));
        assertEquals("", GameService.pgnToSans(""));
    }
}
