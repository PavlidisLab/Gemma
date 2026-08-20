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

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the parts of Cellosaurus that state a RELATION, which is everything
 * {@code CellosaurusOntologyService} throws away.
 *
 * <p>That service builds a lexical index — names and synonyms — and discards the rest, which is
 * correct for what it does and is why cell-line provenance has been recorded as "blocked on CLO
 * asserting almost nothing". CLO asserts 345 donor diseases and 3 anatomic parts. Cellosaurus states
 * a disease for 81,041 of its lines and a derived-from site for 142,374, and it has been on disk the
 * whole time.</p>
 *
 * <p><b>Three facts per record, in three different shapes,</b> which is why this is a parser rather
 * than a field lookup:</p>
 *
 * <pre>
 * xref: NCIt:C4194 ! Invasive breast carcinoma of no special type   the donor's disease
 * comment: "... Derived from site: Metastatic; Pleural effusion; UBERON=UBERON_0000175."
 * xref: CLO:CLO_0007606                                             the alias into Gemma's vocabulary
 * </pre>
 *
 * <p>🛑 The site is inside a free-text {@code comment:} that also carries doubling times, HLA typing,
 * genome ancestry and anecdotes. It is read by locating {@code UBERON=} within a
 * {@code Derived from site:} clause rather than by parsing the comment, because the comment has no
 * grammar and pretending otherwise would break on the next release.</p>
 *
 * <p>🛑 {@code name:} has NO space after the colon while every other field does. Splitting on
 * {@code ": "} silently yields nothing for the one field that names the record.</p>
 *
 * <p><b>Only what makes a relation.</b> Species, cell-line type, donor sex and the
 * "Problematic cell line" misidentification flag are all read already, by
 * {@code CellosaurusOntologyService} into {@code LexicalTermMetadata}, and served on the term itself.
 * A relation's subject is that term, so anything copied here could only drift from what is already
 * shown.</p>
 */
class CellosaurusRelationReport {

    /** {@code xref: NCIt:C4194 ! Invasive breast carcinoma...} — identifier and label. */
    private static final Pattern NCIT = Pattern.compile( "^xref:\\s*NCIt:(\\S+)\\s*(?:!\\s*(.*))?$" );

    /** {@code xref: CLO:CLO_0007606} / {@code xref: EFO:EFO_0001203} — the alias into Gemma's space. */
    private static final Pattern CLO_OR_EFO = Pattern.compile( "^xref:\\s*(?:CLO|EFO):(\\S+)\\s*$" );

    /** {@code Derived from site: Metastatic; Pleural effusion; UBERON=UBERON_0000175.} */
    private static final Pattern SITE =
            Pattern.compile( "Derived from site:\\s*([^\\\"]*?)UBERON=(UBERON_\\d+)" );

    private static final Pattern TAXON = Pattern.compile( "^xref:\\s*NCBI_TaxID:(\\d+)" );

    /**
     * One cell line, reduced to the things that make a relation.
     */
    static class Entry {

        private String id;
        private String name;
        @Nullable
        private String diseaseCurie;
        @Nullable
        private String diseaseLabel;
        @Nullable
        private String siteUri;
        @Nullable
        private String siteDescription;
        @Nullable
        private Integer ncbiTaxonId;
        private final Set<String> aliasLocalNames = new LinkedHashSet<>();

        /** {@code CVCL_0031}. */
        String getId() {
            return id;
        }

        String getName() {
            return name;
        }

        /** {@code NCIt:C4194}, a foreign identifier that must be translated before it is stored. */
        @Nullable
        String getDiseaseCurie() {
            return diseaseCurie;
        }

        /** Cellosaurus's own label for the disease, used only to report what could not be translated. */
        @Nullable
        String getDiseaseLabel() {
            return diseaseLabel;
        }

        @Nullable
        String getSiteUri() {
            return siteUri;
        }

        /** {@code Metastatic; Pleural effusion} — kept as the evidence line for the site relation. */
        @Nullable
        String getSiteDescription() {
            return siteDescription;
        }

        @Nullable
        Integer getNcbiTaxonId() {
            return ncbiTaxonId;
        }

        /**
         * CLO and EFO local names this record cross-references — the alias bridge, and the reason a
         * Cellosaurus fact can reach a dataset annotated in CLO. MCF-7 has four.
         */
        Set<String> getAliasLocalNames() {
            return aliasLocalNames;
        }

        boolean hasRelation() {
            return diseaseCurie != null || siteUri != null;
        }
    }

    /**
     * Stream the file, handing each record with at least one relation to {@code sink}.
     *
     * <p>Streamed rather than collected: the artifact is 118 MB and 168,970 records, and only a
     * minority of what it holds is wanted. Building the whole list first would cost the producer's
     * memory for no benefit it can use.</p>
     */
    static void parse( InputStream is, Consumer<Entry> sink ) throws IOException {
        try ( BufferedReader r = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) ) ) {
            Entry current = null;
            String line;
            while ( ( line = r.readLine() ) != null ) {
                if ( line.startsWith( "id: CVCL_" ) ) {
                    emit( current, sink );
                    current = new Entry();
                    current.id = line.substring( "id: ".length() ).trim();
                    continue;
                }
                if ( current == null ) {
                    continue;
                }
                // 🛑 no space after the colon on this one field, unlike every other
                if ( line.startsWith( "name:" ) ) {
                    current.name = StringUtils.trimToNull( line.substring( "name:".length() ) );
                    continue;
                }
                if ( line.startsWith( "xref:" ) ) {
                    readXref( current, line );
                    continue;
                }
                if ( line.startsWith( "comment:" ) ) {
                    readComment( current, line );
                }
            }
            emit( current, sink );
        }
    }

    private static void readXref( Entry e, String line ) {
        Matcher m = NCIT.matcher( line );
        if ( m.matches() ) {
            // first wins: a record with several NCIt terms is stating one diagnosis in several
            // granularities, and picking among them is picking a disease
            if ( e.diseaseCurie == null ) {
                e.diseaseCurie = "NCIT:" + m.group( 1 );
                e.diseaseLabel = StringUtils.trimToNull( m.group( 2 ) );
            }
            return;
        }
        m = CLO_OR_EFO.matcher( line );
        if ( m.matches() ) {
            e.aliasLocalNames.add( m.group( 1 ) );
            return;
        }
        m = TAXON.matcher( line );
        if ( m.find() && e.ncbiTaxonId == null ) {
            try {
                e.ncbiTaxonId = Integer.valueOf( m.group( 1 ) );
            } catch ( NumberFormatException ignored ) {
                // a taxon id that is not a number is not a taxon
            }
        }
    }

    private static void readComment( Entry e, String line ) {
        // 🛑 The "Problematic cell line" flag is deliberately NOT read here. Cellosaurus's 1,391
        // misidentified lines matter, and they already have a home: the lexical service parses the
        // flag into LexicalTermMetadata and /annotations/term serves it on the cell line itself. A
        // relation's subject IS that term, so a second copy here could only drift from the first.
        Matcher m = SITE.matcher( line );
        if ( m.find() && e.siteUri == null ) {
            e.siteUri = "http://purl.obolibrary.org/obo/" + m.group( 2 );
            String description = m.group( 1 ).replaceAll( "[;,\\s]+$", "" ).trim();
            e.siteDescription = StringUtils.trimToNull( description );
        }
    }

    private static void emit( @Nullable Entry e, Consumer<Entry> sink ) {
        if ( e != null && e.name != null && e.hasRelation() ) {
            sink.accept( e );
        }
    }

    /** Convenience for tests and small inputs. */
    static List<Entry> parseAll( InputStream is ) throws IOException {
        List<Entry> out = new ArrayList<>();
        parse( is, out::add );
        return out;
    }
}
