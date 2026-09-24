-- Repair Adobe Symbol-font text that was stored as Unicode Private Use Area codepoints.
--
-- A submitter types a Greek letter in Word as a Symbol-font Latin letter. Symbol is a font-specific
-- encoding, so software that maps it into Unicode naively adds 0xF000 per byte and lands in the PUA,
-- where meaning is font-specific BY DEFINITION -- no font can render it and a reader gets a tofu box.
-- GSE245831 stored "A" + U+F062 + " burden" for what should read "Ab(eta) burden".
--
-- Scope when written (uib, full-corpus scan of all 23,545 datasets, 2026-09-10): 105 datasets, 100
-- distinct accessions, 188 characters, confined to INVESTIGATION.NAME and .DESCRIPTION. Dataset-level
-- characteristics were clean on value, category, valueUri and categoryUri.
--
-- Keyed by CONTENT, not by the id list from that scan, on purpose:
--   * five accessions are platform splits sharing one description (GSE185340.1/.2 and four more), so
--     the same character sits in two rows -- a fix keyed by accession repairs one and leaves the twin;
--   * the scan is a snapshot, and anything imported between it and this migration is covered anyway;
--   * it is idempotent -- re-running changes nothing, because there is nothing left to match.
--
-- The replacements are GENERATED from /System/Library/Fonts/Symbol.ttf by composing its Macintosh
-- cmap (byte -> glyph name) with the reverse of its Unicode cmap (codepoint -> glyph name), and
-- cross-checked against the positions uib resolved independently from the Adobe AFM. The same table
-- drives SymbolFontPua, which stops GEO import producing these in the first place. Only the fifteen
-- positions this corpus actually contains are listed; the Java side carries all 185.
--
-- NOT repaired, deliberately:
--   * U+FFFD in GSE5298 -- a replacement character. The original byte is already gone and cannot be
--     recovered from the stored text; it needs the source record.
--   * U+F0D8 -> U+00AC is the font's answer, but in GSE199094 it sits between "system." and
--     " Clustering" and reads more like a stray paragraph mark than a negation. It is repaired to
--     the mapping the font gives rather than left as an unrenderable box; if the prose wants
--     something else, that is a content edit, not an encoding one.
-- H2 twin. A Flyway-built H2 holds no imported rows, so this is a no-op there; it exists
-- so the two migration trees stay in step.

-- U+F020 (space) -> U+0020 SPACE
UPDATE INVESTIGATION SET NAME = REPLACE(NAME, CHAR(61472), CHAR(32))
    WHERE NAME LIKE CONCAT('%', CHAR(61472), '%');
-- U+F020 (space) -> U+0020 SPACE
UPDATE INVESTIGATION SET DESCRIPTION = REPLACE(DESCRIPTION, CHAR(61472), CHAR(32))
    WHERE DESCRIPTION LIKE CONCAT('%', CHAR(61472), '%');
-- U+F02D (minus) -> U+2212 MINUS SIGN
UPDATE INVESTIGATION SET NAME = REPLACE(NAME, CHAR(61485), CHAR(8722))
    WHERE NAME LIKE CONCAT('%', CHAR(61485), '%');
-- U+F02D (minus) -> U+2212 MINUS SIGN
UPDATE INVESTIGATION SET DESCRIPTION = REPLACE(DESCRIPTION, CHAR(61485), CHAR(8722))
    WHERE DESCRIPTION LIKE CONCAT('%', CHAR(61485), '%');
-- U+F02E (period) -> U+002E FULL STOP
UPDATE INVESTIGATION SET NAME = REPLACE(NAME, CHAR(61486), CHAR(46))
    WHERE NAME LIKE CONCAT('%', CHAR(61486), '%');
-- U+F02E (period) -> U+002E FULL STOP
UPDATE INVESTIGATION SET DESCRIPTION = REPLACE(DESCRIPTION, CHAR(61486), CHAR(46))
    WHERE DESCRIPTION LIKE CONCAT('%', CHAR(61486), '%');
-- U+F02F (slash) -> U+002F SOLIDUS
UPDATE INVESTIGATION SET NAME = REPLACE(NAME, CHAR(61487), CHAR(47))
    WHERE NAME LIKE CONCAT('%', CHAR(61487), '%');
-- U+F02F (slash) -> U+002F SOLIDUS
UPDATE INVESTIGATION SET DESCRIPTION = REPLACE(DESCRIPTION, CHAR(61487), CHAR(47))
    WHERE DESCRIPTION LIKE CONCAT('%', CHAR(61487), '%');
-- U+F044 (Delta) -> U+0394 GREEK CAPITAL LETTER DELTA
UPDATE INVESTIGATION SET NAME = REPLACE(NAME, CHAR(61508), CHAR(916))
    WHERE NAME LIKE CONCAT('%', CHAR(61508), '%');
-- U+F044 (Delta) -> U+0394 GREEK CAPITAL LETTER DELTA
UPDATE INVESTIGATION SET DESCRIPTION = REPLACE(DESCRIPTION, CHAR(61508), CHAR(916))
    WHERE DESCRIPTION LIKE CONCAT('%', CHAR(61508), '%');
-- U+F061 (alpha) -> U+03B1 GREEK SMALL LETTER ALPHA
UPDATE INVESTIGATION SET NAME = REPLACE(NAME, CHAR(61537), CHAR(945))
    WHERE NAME LIKE CONCAT('%', CHAR(61537), '%');
-- U+F061 (alpha) -> U+03B1 GREEK SMALL LETTER ALPHA
UPDATE INVESTIGATION SET DESCRIPTION = REPLACE(DESCRIPTION, CHAR(61537), CHAR(945))
    WHERE DESCRIPTION LIKE CONCAT('%', CHAR(61537), '%');
-- U+F062 (beta) -> U+03B2 GREEK SMALL LETTER BETA
UPDATE INVESTIGATION SET NAME = REPLACE(NAME, CHAR(61538), CHAR(946))
    WHERE NAME LIKE CONCAT('%', CHAR(61538), '%');
-- U+F062 (beta) -> U+03B2 GREEK SMALL LETTER BETA
UPDATE INVESTIGATION SET DESCRIPTION = REPLACE(DESCRIPTION, CHAR(61538), CHAR(946))
    WHERE DESCRIPTION LIKE CONCAT('%', CHAR(61538), '%');
-- U+F065 (epsilon) -> U+03B5 GREEK SMALL LETTER EPSILON
UPDATE INVESTIGATION SET NAME = REPLACE(NAME, CHAR(61541), CHAR(949))
    WHERE NAME LIKE CONCAT('%', CHAR(61541), '%');
-- U+F065 (epsilon) -> U+03B5 GREEK SMALL LETTER EPSILON
UPDATE INVESTIGATION SET DESCRIPTION = REPLACE(DESCRIPTION, CHAR(61541), CHAR(949))
    WHERE DESCRIPTION LIKE CONCAT('%', CHAR(61541), '%');
-- U+F067 (gamma) -> U+03B3 GREEK SMALL LETTER GAMMA
UPDATE INVESTIGATION SET NAME = REPLACE(NAME, CHAR(61543), CHAR(947))
    WHERE NAME LIKE CONCAT('%', CHAR(61543), '%');
-- U+F067 (gamma) -> U+03B3 GREEK SMALL LETTER GAMMA
UPDATE INVESTIGATION SET DESCRIPTION = REPLACE(DESCRIPTION, CHAR(61543), CHAR(947))
    WHERE DESCRIPTION LIKE CONCAT('%', CHAR(61543), '%');
-- U+F06B (kappa) -> U+03BA GREEK SMALL LETTER KAPPA
UPDATE INVESTIGATION SET NAME = REPLACE(NAME, CHAR(61547), CHAR(954))
    WHERE NAME LIKE CONCAT('%', CHAR(61547), '%');
-- U+F06B (kappa) -> U+03BA GREEK SMALL LETTER KAPPA
UPDATE INVESTIGATION SET DESCRIPTION = REPLACE(DESCRIPTION, CHAR(61547), CHAR(954))
    WHERE DESCRIPTION LIKE CONCAT('%', CHAR(61547), '%');
-- U+F073 (sigma) -> U+03C3 GREEK SMALL LETTER SIGMA
UPDATE INVESTIGATION SET NAME = REPLACE(NAME, CHAR(61555), CHAR(963))
    WHERE NAME LIKE CONCAT('%', CHAR(61555), '%');
-- U+F073 (sigma) -> U+03C3 GREEK SMALL LETTER SIGMA
UPDATE INVESTIGATION SET DESCRIPTION = REPLACE(DESCRIPTION, CHAR(61555), CHAR(963))
    WHERE DESCRIPTION LIKE CONCAT('%', CHAR(61555), '%');
-- U+F077 (omega) -> U+03C9 GREEK SMALL LETTER OMEGA
UPDATE INVESTIGATION SET NAME = REPLACE(NAME, CHAR(61559), CHAR(969))
    WHERE NAME LIKE CONCAT('%', CHAR(61559), '%');
-- U+F077 (omega) -> U+03C9 GREEK SMALL LETTER OMEGA
UPDATE INVESTIGATION SET DESCRIPTION = REPLACE(DESCRIPTION, CHAR(61559), CHAR(969))
    WHERE DESCRIPTION LIKE CONCAT('%', CHAR(61559), '%');
-- U+F0B1 (plusminus) -> U+00B1 PLUS-MINUS SIGN
UPDATE INVESTIGATION SET NAME = REPLACE(NAME, CHAR(61617), CHAR(177))
    WHERE NAME LIKE CONCAT('%', CHAR(61617), '%');
-- U+F0B1 (plusminus) -> U+00B1 PLUS-MINUS SIGN
UPDATE INVESTIGATION SET DESCRIPTION = REPLACE(DESCRIPTION, CHAR(61617), CHAR(177))
    WHERE DESCRIPTION LIKE CONCAT('%', CHAR(61617), '%');
-- U+F0BE (arrowhorizex) -> U+23AF HORIZONTAL LINE EXTENSION
UPDATE INVESTIGATION SET NAME = REPLACE(NAME, CHAR(61630), CHAR(9135))
    WHERE NAME LIKE CONCAT('%', CHAR(61630), '%');
-- U+F0BE (arrowhorizex) -> U+23AF HORIZONTAL LINE EXTENSION
UPDATE INVESTIGATION SET DESCRIPTION = REPLACE(DESCRIPTION, CHAR(61630), CHAR(9135))
    WHERE DESCRIPTION LIKE CONCAT('%', CHAR(61630), '%');
-- U+F0D8 (logicalnot) -> U+00AC NOT SIGN
UPDATE INVESTIGATION SET NAME = REPLACE(NAME, CHAR(61656), CHAR(172))
    WHERE NAME LIKE CONCAT('%', CHAR(61656), '%');
-- U+F0D8 (logicalnot) -> U+00AC NOT SIGN
UPDATE INVESTIGATION SET DESCRIPTION = REPLACE(DESCRIPTION, CHAR(61656), CHAR(172))
    WHERE DESCRIPTION LIKE CONCAT('%', CHAR(61656), '%');
