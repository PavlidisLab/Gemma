/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.analysis.sequence;

import org.springframework.lang.Nullable;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Used to convert BlatResult objects into PSL lines that can be displayed in the UCSC Genome Browser.
 * <p>
 * The output is a UCSC <a href='http://genome.ucsc.edu/goldenPath/help/customTrack.html'>custom track</a>:
 * a {@code browser position} line, a {@code track} line, and one PSL data line per alignment.
 * <p>
 * The host URL is supplied by the caller rather than read from {@code Settings} here, so that the
 * emitting layer decides which host it is speaking for (gemma-rest injects {@code ${gemma.hosturl}})
 * and so that the formatting is unit-testable without a configuration context.
 *
 * @author pavlidis
 */
public class BlatResult2Psl {

    private static final int EXTRA_WINDOW = 1000;

    /**
     * PSL is a tab-delimited format. This emitted space-delimited from 2007 until 2026-08-22;
     * hgTracks accepted it (UIB loaded both a human and a rat track in UCSC and each parsed as
     * {@code psl} with the right item count), because its custom-track reader splits on any
     * whitespace. Tabs are used anyway so the output stays valid for the tools that do require
     * them -- {@code pslToBigPsl}, the Table Browser, track hubs -- none of which were exercised.
     */
    private static final char SEP = '\t';

    /**
     * Order alignments best-first, by match count. Used to pick which alignment the {@code browser
     * position} line frames when a track carries several.
     */
    private static final Comparator<BlatResult> BEST_FIRST = Comparator.comparing(
            br -> br.getMatches() != null ? br.getMatches() : 0, Comparator.reverseOrder() );

    /**
     * See <a href='http://genome.ucsc.edu/goldenPath/help/customTrack.html'>golden path custom track help</a>.
     *
     * @param blatResult blat result
     * @return psl
     */
    private static String blatResult2Psl( BlatResult blatResult ) {
        StringBuilder buf = new StringBuilder();
        buf.append( blatResult.getMatches() ).append( SEP );
        buf.append( blatResult.getMismatches() ).append( SEP );
        buf.append( blatResult.getRepMatches() ).append( SEP );
        buf.append( blatResult.getNs() ).append( SEP );
        buf.append( blatResult.getQueryGapCount() ).append( SEP );
        buf.append( blatResult.getQueryGapBases() ).append( SEP );
        buf.append( blatResult.getTargetGapCount() ).append( SEP );
        buf.append( blatResult.getTargetGapBases() ).append( SEP );
        buf.append( blatResult.getStrand() ).append( SEP );
        buf.append( queryName( blatResult ) ).append( SEP );
        buf.append( blatResult.getQuerySequence().getLength() ).append( SEP );
        buf.append( blatResult.getQueryStart() ).append( SEP );
        buf.append( blatResult.getQueryEnd() ).append( SEP );
        buf.append( chromosomeName( blatResult ) ).append( SEP );
        if ( blatResult.getTargetChromosome() != null && blatResult.getTargetChromosome().getSequence() != null
                && blatResult.getTargetChromosome().getSequence().getLength() != null ) {
            buf.append( blatResult.getTargetChromosome().getSequence().getLength() ).append( SEP );
        } else if ( blatResult.getTargetSequence() != null && blatResult.getTargetSequence().getLength() != null ) {
            buf.append( blatResult.getTargetSequence().getLength() ).append( SEP );
        } else {
            buf.append( blatResult.getTargetEnd() + 1 ).append( SEP );// seems okay as long as more than the target end.
        }
        buf.append( blatResult.getTargetStart() ).append( SEP );
        buf.append( blatResult.getTargetEnd() ).append( SEP );
        buf.append( blatResult.getBlockCount() ).append( SEP );
        buf.append( blatResult.getBlockSizes() ).append( SEP );
        buf.append( blatResult.getQueryStarts() ).append( SEP );
        buf.append( blatResult.getTargetStarts() );
        buf.append( "\n" );
        return buf.toString();
    }

    /**
     * Creates text that can be displayed directly as a track in UCSC, using their hgTracks program.
     * <a href='http://genome.ucsc.edu/goldenPath/help/customTrack.html'>golden path custom track help</a>.
     *
     * @param blatResult blat result
     * @param hostUrl    the Gemma host the track is attributed to, written into the leading comment line
     * @return psl track
     */
    public static String blatResult2PslTrack( BlatResult blatResult, String hostUrl ) {
        return blatResults2PslTrack( java.util.Collections.singleton( blatResult ), hostUrl, null );
    }

    /**
     * Creates a single custom track carrying every supplied alignment.
     * <p>
     * A probe generally has more than one BLAT alignment, and they are only useful side by side, so
     * they share one track. UCSC takes a single {@code browser position}, which frames the
     * best-scoring alignment; alignments elsewhere in the genome are still in the track, they are
     * just not what the browser opens on.
     *
     * @param blatResults the alignments to emit; must not be empty
     * @param hostUrl     the Gemma host the track is attributed to, written into the leading comment line
     * @param trackName   name for the track, or {@code null} to use the query sequence name of the
     *                    best-scoring alignment
     * @return psl track
     */
    public static String blatResults2PslTrack( Collection<BlatResult> blatResults, String hostUrl,
            @Nullable String trackName ) {
        if ( blatResults.isEmpty() ) {
            throw new IllegalArgumentException( "Cannot build a PSL track from zero alignments." );
        }

        List<BlatResult> ordered = new ArrayList<>( blatResults );
        ordered.sort( BEST_FIRST );
        BlatResult best = ordered.get( 0 );

        String name = trackName != null ? trackName : queryName( best );

        StringBuilder buf = new StringBuilder();
        buf.append( "## Generated by Gemma (" ).append( hostUrl ).append( ")\n" );
        buf.append( "browser position " ).append( chromosomeName( best ) ).append( ":" )
                .append( Math.max( 1, best.getTargetStart() - EXTRA_WINDOW ) ).append( "-" )
                .append( best.getTargetEnd() + EXTRA_WINDOW ).append( "\n" );
        buf.append( "track name=\"" ).append( name ).append( "\"" )
                .append( " description=\"Gemma BLAT alignment\" visibility=2 useScore=1\n" );
        for ( BlatResult blatResult : ordered ) {
            buf.append( blatResult2Psl( blatResult ) );
        }
        buf.append( "\n" );
        return buf.toString();
    }

    /**
     * The visible item label of the track, as UCSC renders it.
     * <p>
     * Gemma names the collapsed Affymetrix BioSequence {@code <probe>_collapsed}, and UCSC shows
     * the PSL query name as the item label -- so a visitor who clicked {@code 1007_s_at} would read
     * {@code 1007_s_at_collapsed}, which names an internal shape rather than the probe. The suffix
     * is dropped for display. Only one trailing occurrence goes, so a probe genuinely named
     * {@code foo_collapsed} still reads {@code foo_collapsed}.
     */
    private static String queryName( BlatResult blatResult ) {
        String name = blatResult.getQuerySequence().getName();
        if ( name != null && name.endsWith( SequenceManipulation.COLLAPSED_NAME_SUFFIX ) ) {
            return name.substring( 0, name.length() - SequenceManipulation.COLLAPSED_NAME_SUFFIX.length() );
        }
        return name;
    }

    /**
     * UCSC names its sequences {@code chrN}; Gemma stores the bare {@code N}. The chromosome is
     * mandatory for a positioned alignment -- an alignment without one cannot be placed in the
     * browser at all, so this refuses rather than emitting a line that would silently land on the
     * wrong sequence.
     */
    private static String chromosomeName( BlatResult blatResult ) {
        if ( blatResult.getTargetChromosome() == null || blatResult.getTargetChromosome().getName() == null ) {
            throw new IllegalArgumentException(
                    "BLAT result " + blatResult.getId() + " has no target chromosome, so it cannot be placed in the genome browser." );
        }
        return "chr" + blatResult.getTargetChromosome().getName();
    }
}
