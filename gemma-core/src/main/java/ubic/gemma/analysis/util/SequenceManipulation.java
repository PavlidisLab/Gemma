/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.analysis.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.Reporter;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;

/**
 * Convenient methods for manipulating BioSequences.
 * 
 * @author pavlidis
 * @version $Id$
 */
@SuppressWarnings("unchecked")
public class SequenceManipulation {
    protected static final Log log = LogFactory.getLog( SequenceManipulation.class );

    /**
     * Puts "chr" prefix on the chromosome name, if need be.
     * 
     * @param chromosome
     * @return
     */
    public static String blatFormatChromosomeName( String chromosome ) {
        String searchChrom = chromosome;
        if ( !searchChrom.startsWith( "chr" ) ) searchChrom = "chr" + searchChrom;
        return searchChrom;
    }

    /**
     * Convert a psl-formatted list (comma-delimited) to an int[].
     * 
     * @param blatLocations
     * @return
     */
    public static int[] blatLocationsToIntArray( String blatLocations ) {
        if ( blatLocations == null ) return null;
        String[] strings = blatLocations.split( "," );
        int[] result = new int[strings.length];
        for ( int i = 0; i < strings.length; i++ ) {
            result[i] = Integer.parseInt( strings[i] );
        }
        return result;
    }

    /**
     * Convert a CompositeSequence's immobilizedCharacteristics into a single sequence, using a simple merge-join
     * strategy.
     * 
     * @return
     */
    public static BioSequence collapse( CompositeSequence compositeSequence ) {
        Set copyOfProbes = copyCompositeSequenceReporters( compositeSequence );
        BioSequence collapsed = BioSequence.Factory.newInstance();
        collapsed.setSequence( "" );
        while ( !copyOfProbes.isEmpty() ) {
            Reporter next = findLeftMostProbe( copyOfProbes );
            int ol = SequenceManipulation.rightHandOverlap( collapsed, next.getImmobilizedCharacteristic() );
            String nextSeqStr = next.getImmobilizedCharacteristic().getSequence();
            collapsed.setSequence( collapsed.getSequence() + nextSeqStr.substring( ol ) );
            copyOfProbes.remove( next );
        }
        return collapsed;
    }

    /**
     * Removes "chr" prefix from the chromosome name, if it is there.
     * 
     * @param chromosome
     * @return
     */
    public static String deBlatFormatChromosomeName( String chromosome ) {
        String searchChrom = chromosome;
        if ( searchChrom.startsWith( "chr" ) ) searchChrom = searchChrom.substring( 3 );
        return searchChrom;
    }

    /**
     * Find where the center of a query location is in a gene. This is defined as the location of the center base of the
     * query sequence relative to the 3' end of the gene.
     * 
     * @param starts
     * @param sizes
     * @param gene
     * @return
     */
    public static int findCenter( String starts, String sizes ) {

        int[] startArray = blatLocationsToIntArray( starts );
        int[] sizesArray = blatLocationsToIntArray( sizes );

        return findCenterExonCenterBase( startArray, sizesArray );

    }

    /**
     * Given a physical location, find out how much of it overlaps with exons of a gene. This could involve more than
     * one exon.
     * <p>
     * 
     * @param chromosome
     * @param starts of the locations we are testing.
     * @param sizes of the locations we are testing.
     * @param strand to consider. If null, strandedness is ignored.
     * @param gene Gene we are testing
     * @return Number of bases which overlap with exons of the gene. A value of zero indicates that the location is
     *         entirely within an intron. If multiple GeneProducts are associated with this gene, the best (highest)
     *         overlap is reported).
     */
    public static int getGeneExonOverlaps( String chromosome, String starts, String sizes, String strand, Gene gene ) {
        if ( gene == null ) {
            log.warn( "Null gene" );
            return 0;
        }

        if ( gene.getPhysicalLocation().getChromosome() != null
                && !gene.getPhysicalLocation().getChromosome().getName().equals(
                        deBlatFormatChromosomeName( chromosome ) ) ) return 0;

        int bestOverlap = 0;
        for ( GeneProduct geneProduct : gene.getProducts() ) {
            int overlap = getGeneProductExonOverlap( starts, sizes, strand, geneProduct );
            if ( overlap > bestOverlap ) {
                bestOverlap = overlap;
            }
        }
        return bestOverlap;
    }

    /**
     * Compute the overlap of a physical location with a transcript (gene product).
     * 
     * @param starts of the locations we are testing.
     * @param sizes of the locations we are testing.
     * @param strand the strand to look on. If null, strand is ignored.
     * @param geneProduct GeneProduct we are testing. If strand of PhysicalLocation is null, we ignore strand.
     * @return Total number of bases which overlap with exons of the transcript. A value of zero indicates that the
     *         location is entirely within an intron.
     */
    public static int getGeneProductExonOverlap( String starts, String sizes, String strand, GeneProduct geneProduct ) {

        if ( starts == null || sizes == null || geneProduct == null )
            throw new IllegalArgumentException( "Null data" );

        if ( strand != null && geneProduct.getPhysicalLocation().getStrand() != null
                && geneProduct.getPhysicalLocation().getStrand() != strand ) {
            return 0;
        }

        Collection<PhysicalLocation> exons = geneProduct.getExons();

        int[] startArray = blatLocationsToIntArray( starts );
        int[] sizesArray = blatLocationsToIntArray( sizes );

        assert startArray.length == sizesArray.length;

        int totalOverlap = 0;
        int totalLength = 0;
        for ( int i = 0; i < sizesArray.length; i++ ) {
            int start = startArray[i];
            int end = start + sizesArray[i];
            for ( PhysicalLocation exonLocation : exons ) {
                int exonStart = exonLocation.getNucleotide().intValue();
                int exonEnd = exonLocation.getNucleotide().intValue() + exonLocation.getNucleotideLength().intValue();
                totalOverlap += computeOverlap( start, end, exonStart, exonEnd );
            }
            totalLength += end - start;
        }

        if ( totalOverlap > totalLength )
            log.warn( "More overlap than length of sequence, trimming because " + totalOverlap + " > " + totalLength );

        return Math.min( totalOverlap, totalLength );
    }

    /**
     * Compute just any overlap the compare sequence has with the target on the right side.
     * 
     * @param query
     * @param target
     * @return The index of the end of the overlap. If zero, there is no overlap. In other words, this is the amount
     *         that needs to be trimmed off the compare sequence if we are going to join it on to the target without
     *         generating redundancy.
     */
    public static int rightHandOverlap( BioSequence target, BioSequence query ) {

        if ( target == null || query == null ) throw new IllegalArgumentException( "Null parameters" );

        String targetString = target.getSequence();
        String queryString = query.getSequence();

        if ( targetString == null ) throw new IllegalArgumentException( "Target sequence was empty" );
        if ( queryString == null ) throw new IllegalArgumentException( "Query sequence was empty" );

        // match the end of the target with the beginning of the query. We start with the whole thing
        for ( int i = 0; i < targetString.length(); i++ ) {
            String targetSub = targetString.substring( i );

            if ( queryString.indexOf( targetSub ) == 0 ) {
                return targetSub.length();
            }
        }

        return 0;
    }

    /**
     * @param sizes Blat-formatted string of sizes (comma-delimited)
     * @return
     */
    public static int totalSize( String sizes ) {
        return totalSize( blatLocationsToIntArray( sizes ) );
    }

    /**
     * 1. exon ---------------<br>
     * &nbsp; &nbsp; &nbsp;input&nbsp;&nbsp;&nbsp; -------<br>
     * 2. exon &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;-----------<br>
     * &nbsp; &nbsp; &nbsp;input -----------<br>
     * 3. exon ---------------<br>
     * &nbsp; &nbsp; &nbsp; input &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;---------<br>
     * 4. exon&nbsp;&nbsp;&nbsp;&nbsp; -------<br>
     * &nbsp; &nbsp; &nbsp;input ---------------- <br>
     * 
     * @param start
     * @param end
     * @param exonStart
     * @param exonEnd
     * @return
     */
    private static int computeOverlap( int start, int end, int exonStart, int exonEnd ) {
        if ( exonStart > exonEnd ) throw new IllegalArgumentException( "Exon start must be before end" );
        if ( start > end ) throw new IllegalArgumentException( "Start must be before end" );

        log.debug( "Comparing query length " + ( end - start ) + ", location: " + start + "-->" + end
                + " to exon length " + ( exonEnd - exonStart ) + ", location: " + exonStart + "--->" + exonEnd );

        int overlap = 0;
        if ( exonEnd < start || end < exonStart ) {
            log.debug( "Exon doesn't overlap" );
            overlap = 0;
        } else if ( start <= exonStart ) {
            if ( end < exonEnd ) {
                overlap = end - exonStart; // overhang on the left
            } else {
                overlap = exonEnd - exonStart; // includes entire exon
            }
        } else if ( end < exonEnd ) { // entirely contained within exon.
            overlap = end - start; // length of our test sequence.
        } else {
            overlap = exonEnd - start; // overhang on the right
        }

        assert overlap >= 0 : "Negative overlap";
        assert ( double ) overlap / ( double ) ( end - start ) <= 1.0 : "Overlap longer than sequence";
        log.debug( "Overlap=" + overlap );
        return overlap;
    }

    /**
     * @param compositeSequence
     * @return Set of Reporters for this compositesequence.
     */
    private static Set<Reporter> copyCompositeSequenceReporters( CompositeSequence compositeSequence ) {
        Set<Reporter> copyOfProbes = new HashSet<Reporter>();
        if ( compositeSequence == null ) throw new IllegalArgumentException( "CompositeSequence cannot be null" );
        assert compositeSequence.getComponentReporters() != null : "Null reporters for composite sequence";
        for ( Reporter next : compositeSequence.getComponentReporters() ) {
            Reporter copy = Reporter.Factory.newInstance();
            copy.setImmobilizedCharacteristic( next.getImmobilizedCharacteristic() );
            copy.setStartInBioChar( next.getStartInBioChar() );
            copyOfProbes.add( copy );
        }
        return copyOfProbes;
    }

    /**
     * Find the index of the aligned base in the center exon that is the center of the query.
     * 
     * @param centerExon
     * @param startArray
     * @param sizesArray
     * @return
     */
    private static int findCenterExonCenterBase( int[] startArray, int[] sizesArray ) {
        int middle = middleBase( totalSize( sizesArray ) );
        int totalSize = 0;
        for ( int i = 0; i < sizesArray.length; i++ ) {
            totalSize += sizesArray[i];
            if ( totalSize >= middle ) {
                totalSize -= sizesArray[i];
                int diff = middle - totalSize;
                return startArray[i] + diff;
            }
        }
        assert false : "Failed to find center!";
        throw new IllegalStateException( "Should not be here!" );
    }

    /**
     * From a set of Reporters, find the one with the left-most location.
     * 
     * @param copyOfProbes
     * @return
     */
    private static Reporter findLeftMostProbe( Set<Reporter> copyOfProbes ) {
        Long minLocation = new Long( Integer.MAX_VALUE );
        Reporter next = null;
        for ( Reporter probe : copyOfProbes ) {
            Long loc = probe.getStartInBioChar();
            if ( loc <= minLocation ) {
                minLocation = loc;
                next = probe;
            }
        }
        return next;
    }

    /**
     * @param totalSize
     * @return
     */
    private static int middleBase( int totalSize ) {
        int middle = ( int ) Math.floor( totalSize / 2.0 );
        return middle;
    }

    /**
     * @param sizesArray
     * @return
     */
    private static int totalSize( int[] sizesArray ) {
        int totalSize = 0;
        for ( int i = 0; i < sizesArray.length; i++ ) {
            totalSize += sizesArray[i];
        }
        return totalSize;
    }

}
