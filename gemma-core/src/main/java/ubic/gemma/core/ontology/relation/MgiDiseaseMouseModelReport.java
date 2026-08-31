/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.ontology.relation;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads MGI's {@code MGI_DiseaseMouseModel.rpt}: the disease-centric view of the same curation
 * {@link MgiDiseaseModelReport} reads genotype-first.
 *
 * <p><b>Neither report subsumes the other, and this is the bigger one.</b> Measured 2026-08-30 against
 * the same day's {@code MGI_Geno_DiseaseDO.rpt}: 6,299 asserted (allele, DOID) pairs here against
 * 4,516 there, and 3,331 of these appear in neither genotype report — 2,770 of the 5,542 alleles named
 * here have no statement in the genotype reports at all. The union is 8,086 pairs. Concretely, of the
 * 29 MGI identifiers TGEMO cross-references — the classes Gemma's corpus is actually annotated with —
 * the genotype report names 3 and this one names 16, so APP/PS1, 5xFAD, PS19, R6/1, R6/2 and Ts65Dn
 * had no MGI relation of any kind before this file was read.</p>
 *
 * <p><b>Thirteen tab-separated columns behind a 21-line {@code #} header.</b> Measured 2026-08-30 on
 * the 15,471 data rows:</p>
 *
 * <pre>
 * 1  DO term name                    17-beta hydroxysteroid dehydrogenase 3 deficiency
 * 2  DO term id                      DOID:0112248            &lt;- the object, before translation
 * 3  NOT model                       empty, or `NOT`         &lt;- the status
 * 4  allele pairs                    Hsd17b3&lt;tm1.2Mpo&gt;/Hsd17b3&lt;tm1.2Mpo&gt;
 * 5  strain background               involves: 129S6/SvEvTac * C57BL/6N
 * 6  allele symbol                   Hsd17b3&lt;tm1.2Mpo&gt;       &lt;- the subject
 * 7  allele MGI id                   MGI:6508689             &lt;- what TGEMO cross-references
 * 8  total allele references         1
 * 9  repository id from allele       JAX:031107
 * 10 RRID                            RRID:MGI:6508693
 * 11 marker symbol                   Hsd17b3
 * 12 marker MGI id                   MGI:107177
 * 13 repository id from gene         MMRRC:069136-MU|...
 * </pre>
 *
 * <p>🛑 <b>No PubMed column.</b> Column 8 is a COUNT of the allele's references, not the references
 * themselves, and there is no way to recover which paper supports a given (allele, disease) pair from
 * this file. Every entry therefore carries no citation and is stored
 * {@link ubic.gemma.model.association.GOEvidenceCode#IIA}. Do not read column 8 as evidence: a count
 * of papers about an allele says nothing about who, if anyone, said it models this disease.</p>
 *
 * <p>🛑 <b>7,006 of the 15,471 rows name no model at all</b> — the report lists every disease with a
 * one-to-one mouse ortholog whether or not a model exists, so the allele columns are simply empty.
 * They are skipped; a row with no subject is not a relation.</p>
 *
 * <p><b>Unlike the genotype report, the disease cell is not piped</b> — 0 of 15,471 rows, measured. It
 * is split anyway, because the sibling parser was silently dropping 734 pairs to exactly that
 * assumption until 2026-08-30 and splitting a cell with no separator costs nothing.</p>
 *
 * @see MgiDiseaseModelReport for the genotype-first reports and the statement shape shared with them
 */
class MgiDiseaseMouseModelReport {

    /** The column count the header block documents; a shorter line is not this report's shape. */
    private static final int COLUMNS = 13;

    /**
     * One (allele, disease) statement, carrying which side of MGI's {@code NOT} column it came from.
     *
     * <p>Extends the genotype report's entry so the producer builds a relation from either report
     * through one path — the two files are different views of one statement, and a second
     * {@code build()} would be a second place for the subject URI, the predicate or the taxon to drift.
     * The inherited citation set is always empty here; see the class note on column 8.</p>
     */
    static class Entry extends MgiDiseaseModelReport.Entry {

        private final boolean notModel;

        Entry( String alleleSymbol, String alleleId, String doid, String geneId, boolean notModel ) {
            super( alleleSymbol, alleleId, doid, geneId );
            this.notModel = notModel;
        }

        /**
         * True when MGI's curators recorded that this genotype does <b>not</b> sufficiently model the
         * disease — 338 rows, 236 distinct pairs, measured 2026-08-30.
         */
        boolean isNotModel() {
            return notModel;
        }
    }

    /**
     * Parse the report into deduplicated entries, in file order.
     *
     * <p>🛑 <b>The {@code NOT} flag is part of the key</b>, so a pair MGI states both ways stays two
     * entries rather than collapsing to whichever the file happened to print first. It really does
     * state both: 60 pairs appear asserted on one allele-pair/background combination and {@code NOT}
     * on another, measured 2026-08-30. Which one is stored is a precedence question the producer
     * answers; a parser that silently picked one would hide it.</p>
     *
     * <p>Rows are skipped rather than failed on, as in the sibling parser: a header comment, a short
     * line, a disease with no model, or a cell that is not a {@code DOID:} are all things this report
     * legitimately contains.</p>
     */
    static Collection<Entry> parse( InputStream is ) throws IOException {
        Map<String, Entry> byKey = new LinkedHashMap<>();
        try ( BufferedReader r = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) ) ) {
            String line;
            while ( ( line = r.readLine() ) != null ) {
                if ( line.isEmpty() || line.charAt( 0 ) == '#' ) {
                    continue;
                }
                String[] f = line.split( "\t", -1 );
                if ( f.length < COLUMNS ) {
                    continue;
                }
                String doCell = StringUtils.trimToNull( f[1] );
                String allele = StringUtils.trimToNull( f[5] );
                if ( allele == null || doCell == null ) {
                    continue;
                }
                String alleleId = StringUtils.trimToNull( f[6] );
                String geneId = StringUtils.trimToNull( f[11] );
                // MGI documents exactly one value here, `NOT`; anything non-empty is read as the
                // curators having said no rather than as a value to guess at.
                boolean notModel = StringUtils.trimToNull( f[2] ) != null;
                for ( String part : doCell.split( "\\|" ) ) {
                    String doid = StringUtils.trimToNull( part );
                    if ( doid == null || !doid.startsWith( "DOID:" ) ) {
                        continue;
                    }
                    byKey.computeIfAbsent( allele + '\t' + doid + '\t' + notModel,
                            k -> new Entry( allele, alleleId, doid, geneId, notModel ) ).addRow();
                }
            }
        }
        return byKey.values();
    }
}
