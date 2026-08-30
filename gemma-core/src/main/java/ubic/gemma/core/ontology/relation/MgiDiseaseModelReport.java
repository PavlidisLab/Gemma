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
 * Reads MGI's genotype-to-disease reports: which mutant genotypes MGI's curators say model which
 * diseases, and which they say do <b>not</b>.
 *
 * <p>Two files of one shape. {@code MGI_Geno_DiseaseDO.rpt} is the positive set and
 * {@code MGI_Geno_NotDiseaseDO.rpt} the negative one, and the negative file is the reason
 * {@link ubic.gemma.model.common.description.AnnotationRelationStatus} exists — a curated,
 * cited statement that a genotype does not model a disease is rare and cannot be recovered from
 * anything else.</p>
 *
 * <p><b>Ten tab-separated columns, no header.</b> Measured 2026-08-18:</p>
 *
 * <pre>
 * 1 allelic composition   Ednra&lt;tm1Ywa&gt;/Ednra&lt;tm1Ywa&gt;
 * 2 allele symbol         Ednra&lt;tm1Ywa&gt;          &lt;- the subject
 * 3 allele MGI id         MGI:1857473
 * 4 genetic background    129S/SvEv-Ednra&lt;tm1Ywa&gt;
 * 5 MP phenotype id       MP:0002127
 * 6 PubMed id             9449664
 * 7 gene MGI id           MGI:105923
 * 8 DO disease id         DOID:12583             &lt;- the object, before translation
 * 9 OMIM id               OMIM:192430
 * 10 genotype MGI id      MGI:2166570
 * </pre>
 *
 * <p>🛑 <b>One row per PHENOTYPE, not per relation.</b> The same genotype and disease repeat once for
 * every {@code MP:} term the genotype shows, so the file's 53,950 rows are 4,124 distinct
 * (allele, disease) pairs — a factor of thirteen. Reading rows as relations would inflate every count
 * built on them and make a well-phenotyped genotype look thirteen times better attested than a
 * sparsely-phenotyped one. Deduplication is not tidying here; it is the difference between counting
 * relations and counting MGI's phenotype annotations.</p>
 *
 * <p><b>Keyed on the ALLELE, not the gene.</b> The allele symbol is what distinguishes
 * {@code Sod1&lt;tm1Cje&gt;} from any other {@code Sod1} lesion, and rolling up to the gene is exactly
 * the flattening that makes MGI's gene-level report unusable for this — it answers {@code Sod1} with
 * Down syndrome and Parkinson disease rather than ALS. The gene id is read and kept for provenance and
 * is deliberately not the subject.</p>
 */
class MgiDiseaseModelReport {

    /**
     * One genotype-to-disease statement, after collapsing the phenotype rows behind it.
     */
    static class Entry {

        private final String alleleSymbol;
        private final String alleleId;
        private final String doid;
        private final String geneId;
        private final java.util.Set<String> citations = new java.util.LinkedHashSet<>();
        private int rows;

        Entry( String alleleSymbol, String alleleId, String doid, String geneId ) {
            this.alleleSymbol = alleleSymbol;
            this.alleleId = alleleId;
            this.doid = doid;
            this.geneId = geneId;
        }

        String getAlleleSymbol() {
            return alleleSymbol;
        }

        String getAlleleId() {
            return alleleId;
        }

        /** {@code DOID:12583}, a foreign identifier that must be translated before it is stored. */
        String getDoid() {
            return doid;
        }

        String getGeneId() {
            return geneId;
        }

        /**
         * Every PubMed id MGI cites for this statement, in file order; empty when it cites none.
         *
         * <p>🛑 Collected rather than taken once, from BOTH directions in which MGI supplies several:
         * a single cell can hold {@code 7600971|8631247}, and the phenotype rows that collapse into one
         * statement often cite different papers. Keeping the first would throw away most of the
         * evidence for exactly the best-studied genotypes — the ones with the most phenotype rows.</p>
         *
         * <p>94% of real pairs carry at least one, and their presence also decides the evidence code:
         * a cited statement is traceable to an author, an uncited one is an import whose own basis we
         * cannot see.</p>
         */
        java.util.Set<String> getCitations() {
            return citations;
        }

        /**
         * The citations as one quotable line for {@code ANNOTATION_RELATION.EVIDENCE}, or null when
         * there are none. Truncated to fit the column rather than overflowing it.
         */
        @javax.annotation.Nullable
        String getEvidence() {
            if ( citations.isEmpty() ) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for ( String c : citations ) {
                String next = ( sb.length() == 0 ? "" : ";" ) + "PMID:" + c;
                if ( sb.length() + next.length() > 255 ) {
                    break;
                }
                sb.append( next );
            }
            return sb.length() > 0 ? sb.toString() : null;
        }

        /** How many report rows collapsed into this entry — i.e. how many phenotypes MGI recorded. */
        int getRows() {
            return rows;
        }
    }

    /**
     * Parse one report into deduplicated entries, in file order.
     *
     * <p>Rows are skipped rather than failed on: a comment, a short line, or a row whose disease
     * column is not a {@code DOID:} are all things a report can legitimately contain, and one of them
     * must not cost the other 53,949.</p>
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
                if ( f.length < 10 ) {
                    continue;
                }
                String allele = StringUtils.trimToNull( f[1] );
                String alleleId = StringUtils.trimToNull( f[2] );
                String doCell = StringUtils.trimToNull( f[7] );
                if ( allele == null || doCell == null ) {
                    continue;
                }
                // 🛑 The DO cell is pipe-separated too, exactly like the citation cell below: MGI emits
                // `DOID:0110042|DOID:10652` for a genotype that models two diseases. Taking the cell
                // whole leaves a DOID nothing can translate -- `startsWith("DOID:")` is true of the
                // joined string, so it passed this guard, failed to resolve against MONDO, and was
                // tallied as untranslatable. Measured on the 2026-08-30 file: 5,723 of 53,950 rows have
                // a piped cell, and splitting recovers 734 (allele, disease) pairs, 282 of which are
                // alleles that had NO statement stored at all.
                for ( String part : doCell.split( "\\|" ) ) {
                    String doid = StringUtils.trimToNull( part );
                    if ( doid == null || !doid.startsWith( "DOID:" ) ) {
                        continue;
                    }
                    Entry e = byKey.computeIfAbsent( allele + '\t' + doid,
                            k -> new Entry( allele, alleleId, doid, StringUtils.trimToNull( f[6] ) ) );
                    e.rows++;
                    // one cell can hold several, pipe-separated -- MGI really does emit `7600971|8631247`
                    String cited = StringUtils.trimToNull( f[5] );
                    if ( cited != null ) {
                        for ( String id : cited.split( "\\|" ) ) {
                            String t = StringUtils.trimToNull( id );
                            if ( t != null ) {
                                e.citations.add( t );
                            }
                        }
                    }
                }
            }
        }
        return byKey.values();
    }
}
