package com.chesst.integration;

public record ExternalGameImport(
        String sourceGameId,
        String white,
        String black,
        String result,
        String event,
        String site,
        String date,
        String eco,
        String openingName,
        String pgn
) {
    public static ExternalGameImport from(ExternalGameDto d) {
        return new ExternalGameImport(
                d.sourceGameId(), d.white(), d.black(), d.result(),
                d.event(), d.site(), d.date(), d.eco(), d.openingName(), d.pgn()
        );
    }
}
