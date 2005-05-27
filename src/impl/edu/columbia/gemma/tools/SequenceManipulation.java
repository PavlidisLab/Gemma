/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.tools;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.Reporter;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.PhysicalLocation;
import edu.columbia.gemma.genome.biosequence.BioSequence;
import edu.columbia.gemma.genome.gene.GeneProduct;

/**
 * Convenient methods for manipulating BioSequences.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SequenceManipulation {
    protected static final Log log = LogFactory.getLog( SequenceManipulation.class );

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
     * Given a physical location, find out how much of it overlaps with exons of a gene. This could involve more than
     * one exon.
     * <p>
     * 
     * @param chromosome
     * @param starts of the locations we are testing.
     * @param sizes of the locations we are testing.
     * @param gene Gene we are testing
     * @return Number of bases which overlap with exons of the gene. A value of zero indicates that the location is
     *         entirely within an intron.
     *         <p>
     *         FIXME this should take a PhysicalLocation as input.
     *         <p>
     *         FIXME this should check the chromosome (commented out for now as a convenience)
     *         <p>
     *         FIXME This only checks for one gene product per gene.
     */
    public static int getGeneExonOverlaps( String chromosome, String starts, String sizes, Gene gene ) {
        if ( gene == null ) return 0;
        // if ( !gene.getPhysicalLocation().getChromosome().toString().equals( chromosome ) ) return 0;
        Collection products = gene.getProducts();
        for ( Iterator iter = products.iterator(); iter.hasNext(); ) {
            GeneProduct geneProduct = ( GeneProduct ) iter.next();
            return getGeneProductExonOverlap( starts, sizes, null, geneProduct );
        }
        return 0;
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
                && geneProduct.getPhysicalLocation().getStrand() != strand ) return 0;

        Collection exons = geneProduct.getExons();

        int[] startArray = blatLocationsToIntArray( starts );
        int[] sizesArray = blatLocationsToIntArray( sizes );

        assert startArray.length == sizesArray.length;

        int totalOverlap = 0;
        int totalLength = 0;
        for ( int i = 0; i < sizesArray.length; i++ ) {
            int start = startArray[i];
            int end = start + sizesArray[i];
            for ( Iterator iterator = exons.iterator(); iterator.hasNext(); ) {
                PhysicalLocation exonLocation = ( PhysicalLocation ) iterator.next();
                int exonStart = exonLocation.getNucleotide().intValue();
                int exonEnd = exonLocation.getNucleotide().intValue() + exonLocation.getNucleotideLength().intValue();
                totalOverlap += computeOverlap( start, end, exonStart, exonEnd );
            }
            totalLength += end - start;
        }

        if ( totalOverlap > totalLength )
            log.warn( "More overlap than length of sequence, trimming because " + totalOverlap + " > " + totalLength );
        totalOverlap = Math.min( totalOverlap, totalLength );

        return totalOverlap;
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
     * From a set of Reporters, find the one with the left-most location.
     * 
     * @param copyOfProbes
     * @return
     */
    private static Reporter findLeftMostProbe( Set copyOfProbes ) {
        int minLocation = Integer.MAX_VALUE;
        Reporter next = null;
        for ( Iterator iter = copyOfProbes.iterator(); iter.hasNext(); ) {
            Reporter probe = ( Reporter ) iter.next();

            int loc = probe.getStartInBioChar();
            if ( loc <= minLocation ) {
                minLocation = loc;
                next = probe;
            }
        }
        return next;
    }

    /**
     * @param compositeSequence
     * @return Set of Reporters for this compositesequence.
     */
    private static Set copyCompositeSequenceReporters( CompositeSequence compositeSequence ) {
        Set copyOfProbes = new HashSet();
        if ( compositeSequence == null ) throw new IllegalArgumentException( "CompositeSequence cannot be null" );
        assert compositeSequence.getReporters() != null : "Null reporters for composite sequence";
        for ( Iterator iter = compositeSequence.getReporters().iterator(); iter.hasNext(); ) {
            Reporter next = ( Reporter ) iter.next();

            Reporter copy = Reporter.Factory.newInstance();
            copy.setImmobilizedCharacteristic( next.getImmobilizedCharacteristic() );
            copy.setStartInBioChar( next.getStartInBioChar() );
            copyOfProbes.add( copy );
        }
        return copyOfProbes;
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
