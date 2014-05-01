/*
 * The gemma-core project
 * 
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.analysis.sequence;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultValueObject;

/**
 * @author paul
 * @version $Id$
 */
public class ProbeMapUtils {

    private static Logger log = LoggerFactory.getLogger( ProbeMapUtils.class );

    /**
     * Prune a set of results that have the same coordinates and query. See bug 4037
     * 
     * @param blatResults
     */
    public static void removeDuplicates( Collection<BlatResult> blatResults ) {
        int init = blatResults.size();
        Set<Integer> hashes = new HashSet<>();
        for ( Iterator<BlatResult> it = blatResults.iterator(); it.hasNext(); ) {
            BlatResult br = it.next();

            Integer hash = hashBlatResult( br );

            if ( hashes.contains( hash ) ) {
                it.remove();
            }

            hashes.add( hash );

        }

        if ( blatResults.size() < init && log.isDebugEnabled() ) {
            log.debug( "Pruned " + ( init - blatResults.size() ) + "/" + init + " duplicates" );
        }

    }

    /**
     * Compute a hash for the result based only on the characteristics of the alignment (that is, not the Id)
     * 
     * @param br
     * @return
     */
    public static Integer hashBlatResult( BlatResult br ) {
        int result = 1;
        int prime = 31;
        result = prime * result + ( ( br.getQuerySequence() == null ) ? 0 : br.getQuerySequence().hashCode() );
        result = prime * result + ( ( br.getTargetChromosome() == null ) ? 0 : br.getTargetChromosome().hashCode() );

        result = prime * result + ( ( br.getBlockCount() == null ) ? 0 : br.getBlockCount().hashCode() );
        result = prime * result + ( ( br.getBlockSizes() == null ) ? 0 : br.getBlockSizes().hashCode() );
        result = prime * result + ( ( br.getMatches() == null ) ? 0 : br.getMatches().hashCode() );
        result = prime * result + ( ( br.getMismatches() == null ) ? 0 : br.getMismatches().hashCode() );
        result = prime * result + ( ( br.getNs() == null ) ? 0 : br.getNs().hashCode() );
        result = prime * result + ( ( br.getQueryEnd() == null ) ? 0 : br.getQueryEnd().hashCode() );
        result = prime * result + ( ( br.getQueryGapBases() == null ) ? 0 : br.getQueryGapBases().hashCode() );
        result = prime * result + ( ( br.getQueryGapCount() == null ) ? 0 : br.getQueryGapCount().hashCode() );
        result = prime * result + ( ( br.getQueryStart() == null ) ? 0 : br.getQueryStart().hashCode() );
        result = prime * result + ( ( br.getQueryStarts() == null ) ? 0 : br.getQueryStarts().hashCode() );
        result = prime * result + ( ( br.getRepMatches() == null ) ? 0 : br.getRepMatches().hashCode() );
        result = prime * result + ( ( br.getStrand() == null ) ? 0 : br.getStrand().hashCode() );
        result = prime * result + ( ( br.getTargetEnd() == null ) ? 0 : br.getTargetEnd().hashCode() );
        result = prime * result + ( ( br.getTargetGapBases() == null ) ? 0 : br.getTargetGapBases().hashCode() );
        result = prime * result + ( ( br.getTargetGapCount() == null ) ? 0 : br.getTargetGapCount().hashCode() );
        result = prime * result + ( ( br.getTargetStart() == null ) ? 0 : br.getTargetStart().hashCode() );
        result = prime * result + ( ( br.getTargetStarts() == null ) ? 0 : br.getTargetStarts().hashCode() );
        return result;
    }

    /**
     * Compute a hash for the result based only on the characteristics of the alignment (that is, not the Id)
     * 
     * @param br
     * @return
     */
    public static Integer hashBlatResult( BlatResultValueObject br ) {
        int result = 1;
        int prime = 31;
        result = prime * result + ( ( br.getQuerySequence() == null ) ? 0 : br.getQuerySequence().hashCode() );
        result = prime * result
                + ( ( br.getTargetChromosomeName() == null ) ? 0 : br.getTargetChromosomeName().hashCode() );

        result = prime * result + ( ( br.getBlockCount() == null ) ? 0 : br.getBlockCount().hashCode() );
        result = prime * result + ( ( br.getBlockSizes() == null ) ? 0 : br.getBlockSizes().hashCode() );
        result = prime * result + ( ( br.getMatches() == null ) ? 0 : br.getMatches().hashCode() );
        result = prime * result + ( ( br.getMismatches() == null ) ? 0 : br.getMismatches().hashCode() );
        result = prime * result + ( ( br.getNs() == null ) ? 0 : br.getNs().hashCode() );
        result = prime * result + ( ( br.getQueryEnd() == null ) ? 0 : br.getQueryEnd().hashCode() );
        result = prime * result + ( ( br.getQueryGapBases() == null ) ? 0 : br.getQueryGapBases().hashCode() );
        result = prime * result + ( ( br.getQueryGapCount() == null ) ? 0 : br.getQueryGapCount().hashCode() );
        result = prime * result + ( ( br.getQueryStart() == null ) ? 0 : br.getQueryStart().hashCode() );
        result = prime * result + ( ( br.getQueryStarts() == null ) ? 0 : br.getQueryStarts().hashCode() );
        result = prime * result + ( ( br.getRepMatches() == null ) ? 0 : br.getRepMatches().hashCode() );
        result = prime * result + ( ( br.getStrand() == null ) ? 0 : br.getStrand().hashCode() );
        result = prime * result + ( ( br.getTargetEnd() == null ) ? 0 : br.getTargetEnd().hashCode() );
        result = prime * result + ( ( br.getTargetGapBases() == null ) ? 0 : br.getTargetGapBases().hashCode() );
        result = prime * result + ( ( br.getTargetGapCount() == null ) ? 0 : br.getTargetGapCount().hashCode() );
        result = prime * result + ( ( br.getTargetStart() == null ) ? 0 : br.getTargetStart().hashCode() );
        result = prime * result + ( ( br.getTargetStarts() == null ) ? 0 : br.getTargetStarts().hashCode() );
        return result;

    }

}
