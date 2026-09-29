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

import ubic.gemma.core.ontology.providers.MgiStrainOntologyService;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which MGI strains carry an allele, read from the strains' nomenclature.
 * <p>
 * MGI publishes no allele-to-strain table. {@code MGI_Strain.rpt} is three columns, and the {@code RRID:MGI:} column
 * of the disease report names genotypes: 0 of its 6,814 accessions are strains (measured 2026-09-14). But a strain's
 * nomenclature spells out the alleles it carries — {@code FVB.129(B6)-Smn1<tm5(Smn1/SMN2)Mrph>/J}. Measured
 * 2026-09-14: 2,239 of the 6,439 allele symbols in MGI's disease reports appear in at least one strain name, over
 * 5,582 strains, and 372 of those strains carry two or more.
 * <p>
 * An allele symbol is a gene part followed by a superscript in angle brackets. The gene part has no delimiter on its
 * left — a background runs straight into it with {@code -}, {@code .}, {@code ;} or {@code :}
 * ({@code B6.129S4-Il10<tm1Cgn>/J}) — and a gene symbol may itself contain one ({@code H2-Ab1}). So every suffix of
 * the text before {@code <} that starts after such a separator is indexed, and a lookup by the exact allele symbol
 * settles which of them was the gene. Transgene symbols ({@code Tg(...)}) carry no superscript and are never matched.
 */
final class MgiStrainAlleleIndex {

    record Strain( String uri, String name ) {
    }

    private static final Pattern ALLELE_IN_NAME = Pattern.compile( "([A-Za-z0-9()/,.;:\\-]+)(<[^<>]+>)" );

    private static final MgiStrainAlleleIndex EMPTY = new MgiStrainAlleleIndex( Collections.emptyMap() );

    private final Map<String, List<Strain>> byAlleleSymbol;

    private MgiStrainAlleleIndex( Map<String, List<Strain>> byAlleleSymbol ) {
        this.byAlleleSymbol = byAlleleSymbol;
    }

    static MgiStrainAlleleIndex empty() {
        return EMPTY;
    }

    /**
     * @param mgiStrainRpt {@code MGI_Strain.rpt}: {@code MGI:<id>} · nomenclature · strain type, tab-delimited
     */
    static MgiStrainAlleleIndex parse( InputStream mgiStrainRpt ) throws IOException {
        Map<String, List<Strain>> index = new HashMap<>();
        try ( BufferedReader r = new BufferedReader( new InputStreamReader( mgiStrainRpt, StandardCharsets.UTF_8 ) ) ) {
            String line;
            while ( ( line = r.readLine() ) != null ) {
                if ( line.isEmpty() || line.charAt( 0 ) == '#' ) {
                    continue;
                }
                String[] f = line.split( "\t", -1 );
                if ( f.length < 2 || !f[0].trim().startsWith( "MGI:" ) || f[1].isBlank() ) {
                    continue;
                }
                Strain strain = new Strain( MgiStrainOntologyService.URI_PREFIX + f[0].trim(), f[1].trim() );
                Map<String, Strain> candidates = new LinkedHashMap<>();
                Matcher m = ALLELE_IN_NAME.matcher( strain.name() );
                while ( m.find() ) {
                    String before = m.group( 1 );
                    String superscript = m.group( 2 );
                    candidates.put( before + superscript, strain );
                    for ( int i = 0; i < before.length(); i++ ) {
                        char c = before.charAt( i );
                        if ( ( c == '-' || c == '.' || c == ';' || c == ':' ) && i + 1 < before.length() ) {
                            candidates.put( before.substring( i + 1 ) + superscript, strain );
                        }
                    }
                }
                for ( String symbol : candidates.keySet() ) {
                    index.computeIfAbsent( symbol, k -> new ArrayList<>( 1 ) ).add( strain );
                }
            }
        }
        return new MgiStrainAlleleIndex( index );
    }

    /**
     * @return the strains whose nomenclature carries this exact allele symbol, in report order; empty if none
     */
    List<Strain> strainsCarrying( @Nullable String alleleSymbol ) {
        if ( alleleSymbol == null ) {
            return Collections.emptyList();
        }
        return byAlleleSymbol.getOrDefault( alleleSymbol, Collections.emptyList() );
    }

    boolean isEmpty() {
        return byAlleleSymbol.isEmpty();
    }
}
