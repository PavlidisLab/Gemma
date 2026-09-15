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
package ubic.gemma.core.geoscrape;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.loader.expression.geo.model.GeoRecord;

import java.util.Locale;

/**
 * Brain + single-cell intersection matcher. Flags when:
 * <ol>
 *   <li>the record's library strategy looks like RNA-seq (or the title/sample
 *       details indicate single-cell tech: 10x, Smart-seq2, Drop-seq); AND</li>
 *   <li>{@link BrainKeywordMatcher} matches.</li>
 * </ol>
 *
 * <p>v2 will subsume this under the curation-agent pass; see {@link GeoRecordMatcher}.</p>
 */
@Component
public class SingleCellBrainMatcher implements GeoRecordMatcher {

    private final BrainKeywordMatcher brainMatcher;

    @Autowired
    public SingleCellBrainMatcher( BrainKeywordMatcher brainMatcher ) {
        this.brainMatcher = brainMatcher;
    }

    @Override
    public String name() {
        return "scbrain";
    }

    @Override
    public MatchResult evaluate( GeoRecord r ) {
        if ( r == null ) return MatchResult.miss();
        if ( !looksLikeSingleCell( r ) ) {
            return MatchResult.miss();
        }
        MatchResult brain = brainMatcher.evaluate( r );
        if ( !brain.isMatched() ) return MatchResult.miss();
        return MatchResult.hit( "single-cell + " + brain.getReason() );
    }

    private static boolean looksLikeSingleCell( GeoRecord r ) {
        String lib = r.getLibraryStrategy() == null ? "" : r.getLibraryStrategy().toLowerCase( Locale.ROOT );
        String details = r.getSampleDetails() == null ? "" : r.getSampleDetails().toLowerCase( Locale.ROOT );
        String title = r.getTitle() == null ? "" : r.getTitle().toLowerCase( Locale.ROOT );
        String summary = r.getSummary() == null ? "" : r.getSummary().toLowerCase( Locale.ROOT );
        String hay = lib + ' ' + details + ' ' + title + ' ' + summary;
        if ( hay.contains( "single cell" ) || hay.contains( "single-cell" )
                || hay.contains( "scrna-seq" ) || hay.contains( "scrnaseq" ) ) {
            return true;
        }
        if ( hay.contains( "10x" ) || hay.contains( "10× " ) || hay.contains( "smart-seq" )
                || hay.contains( "smartseq" ) || hay.contains( "drop-seq" ) || hay.contains( "dropseq" ) ) {
            return true;
        }
        return false;
    }
}
