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
package ubic.gemma.core.analysis.sequence;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.core.loader.expression.arrayDesign.Reporter;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;

import java.util.Collection;
import java.util.HashSet;

/**
 * Convenient methods for manipulating BioSequences and PhysicalLocations
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Possible external use
public class SequenceManipulation {
    private static final Log log = LogFactory.getLog( SequenceManipulation.class );

    /**
     * Puts "chr" prefix on the chromosome name, if need be.
     */
    public static String blatFormatChromosomeName( String chromosome ) {
        String searchChromosome = chromosome;
        if ( !searchChromosome.startsWith( "chr" ) )
            searchChromosome = "chr" + searchChromosome;
        return searchChromosome;
    }

    /**
     * Remove a 3' polyA or 5' polyT tail. The entire tail is removed.
     *
     * @param thresholdLength to trigger removal.
     */
    @SuppressWarnings("Annotator") // Annotator not working properly with the string concat
    public static String stripPolyAorT( String sequence, int thresholdLength ) {
        sequence = sequence.replaceAll( "A{" + thresholdLength + ",}$", "" );
        return sequence.replaceAll( "^T{" + thresholdLength + ",}", "" );
    }

    /**
     * Convert a psl-formatted list (comma-delimited) to an int[].
     */
    public static int[] blatLocationsToIntArray( String blatLocations ) {
        if ( blatLocations == null )
            return null;
        String[] strings = blatLocations.split( "," );
        int[] result = new int[strings.length];
        for ( int i = 0; i < strings.length; i++ ) {
            try {
                result[i] = Integer.parseInt( strings[i] );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException(
                        "Could not parse integer blat location " + blatLocations + ", from " + strings[i], e );
            }
        }
        return result;
    }

    /**
     * Convert a CompositeSequence's immobilizedCharacteristics into a single sequence, using a simple merge-join
     * strategy.
     *
     * @return BioSequence. Not all fields are filled in and must be set by the caller.
     */
    public static BioSequence collapse( Collection<Reporter> sequences ) {
        Collection<Reporter> copyOfSequences = copyReporters( sequences );
        BioSequence collapsed = BioSequence.Factory.newInstance();
        collapsed.setSequence( "" );
        if ( log.isDebugEnabled() )
            log.debug( "Collapsing " + sequences.size() + " sequences" );
        while ( !copyOfSequences.isEmpty() ) {
            Reporter next = findLeftMostProbe( copyOfSequences );
            int ol = SequenceManipulation.rightHandOverlap( collapsed, next.getImmobilizedCharacteristic() );
            String nextSeqStr = next.getImmobilizedCharacteristic().getSequence();
            collapsed.setSequence( collapsed.getSequence() + nextSeqStr.substring( ol ) );
            if ( log.isDebugEnabled() ) {
                log.debug( "New Seq to add: " + nextSeqStr + " Overlap=" + ol + " Result=" + collapsed.getSequence() );
            }
            copyOfSequences.remove( next );
        }
        collapsed.setIsCircular( false );
        collapsed.setIsApproximateLength( false );
        collapsed.setLength( ( long ) collapsed.getSequence().length() );
        collapsed.setDescription( "Collapsed from " + sequences.size() + " reporter sequences" );
        return collapsed;
    }

    /**
     * Removes "chr" prefix from the chromosome name, if it is there.
     */
    public static String deBlatFormatChromosomeName( String chromosome ) {
        String searchChromosome = chromosome;
        if ( searchChromosome.startsWith( "chr" ) )
            searchChromosome = searchChromosome.substring( 3 );
        return searchChromosome;
    }

    /**
     * Find where the center of a query location is in a gene. This is defined as the location of the center base of the
     * query sequence relative to the 3' end of the gene.
     */
    public static int findCenter( String starts, String sizes ) {

        int[] startArray = blatLocationsToIntArray( starts );
        int[] sizesArray = blatLocationsToIntArray( sizes );

        return findCenterExonCenterBase( startArray, sizesArray );

    }

    /**
     * Given a gene, find out how much of it overlaps with exons provided as starts and sizes. This could involve more
     * than one exon.
     *
     * @param chromosome, as "chrX" or "X".
     * @param starts      of the locations we are testing.
     * @param sizes       of the locations we are testing.
     * @param strand      to consider. If null, strand is ignored.
     * @param gene        Gene we are testing
     * @return Number of bases which overlap with exons of the gene. A value of zero indicates that the location is
     * entirely within an intron. If multiple GeneProducts are associated with this gene, the best (highest)
     * overlap is reported).
     */
    public static int getGeneExonOverlaps( String chromosome, String starts, String sizes, String strand, Gene gene ) {
        if ( gene == null ) {
            log.warn( "Null gene" );
            return 0;
        }

        if ( gene.getPhysicalLocation().getChromosome() != null && !gene.getPhysicalLocation().getChromosome().getName()
                .equals( deBlatFormatChromosomeName( chromosome ) ) )
            return 0;

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
     * Compute the overlap of a physical location with a transcript (gene product). This assumes that the chromosome is
     * already matched.
     *
     * @param starts      of the locations we are testing (in the target, so on the same coordinates as the geneProduct
     *                    location is scored)
     * @param sizes       of the locations we are testing.
     * @param strand      the strand to look on. If null, strand is ignored.
     * @param geneProduct GeneProduct we are testing. If strand of PhysicalLocation is null, we ignore strand.
     * @return Total number of bases which overlap with exons of the transcript. A value of zero indicates that the
     * location is entirely within an intron, or the strand is wrong.
     */
    public static int getGeneProductExonOverlap( String starts, String sizes, String strand, GeneProduct geneProduct ) {

        if ( starts == null || sizes == null || geneProduct == null )
            throw new IllegalArgumentException( "Null data" );

        // If strand is null we don't bother looking at it; if the strands don't match we return 0
        PhysicalLocation gpPhysicalLocation = geneProduct.getPhysicalLocation();
        if ( strand != null && gpPhysicalLocation != null && gpPhysicalLocation.getStrand() != null
                && !gpPhysicalLocation.getStrand().equals( strand ) ) {
            return 0;
        }

        // These are transient instances
        Collection<PhysicalLocation> exons = geneProduct.getExons();

        int[] startArray = blatLocationsToIntArray( starts );
        int[] sizesArray = blatLocationsToIntArray( sizes );

        if ( exons.size() == 0 ) {
            /*
             * simply use the gene product location itself. This was the case if we have ProbeAlignedRegion...otherwise
             * it's not expected
             */
            log.warn( "No exons for " + geneProduct );
            return 0;

        }

        // this was happening when data was truncated by the database!
        assert startArray.length == sizesArray.length :
                startArray.length + " starts and " + sizesArray.length + " sizes (expected equal numbers)";

        int totalOverlap = 0;
        int totalLength = 0;
        for ( int i = 0; i < sizesArray.length; i++ ) {
            int start = startArray[i];
            int end = start + sizesArray[i];
            for ( PhysicalLocation exonLocation : exons ) {
                int exonStart = exonLocation.getNucleotide().intValue();
                int exonEnd = exonLocation.getNucleotide().intValue() + exonLocation.getNucleotideLength();
                totalOverlap += PhysicalLocation.computeOverlap( start, end, exonStart, exonEnd );
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
     * @return The index of the end of the overlap. If zero, there is no overlap. In other words, this is the amount
     * that needs to be trimmed off the compare sequence if we are going to join it on to the target without
     * generating redundancy.
     */
    @SuppressWarnings("WeakerAccess") // Possible external use
    public static int rightHandOverlap( BioSequence target, BioSequence query ) {

        if ( target == null || query == null )
            throw new IllegalArgumentException( "Null parameters" );

        String targetString = target.getSequence();
        String queryString = query.getSequence();

        if ( targetString == null )
            throw new IllegalArgumentException( "Target sequence was empty" );
        if ( queryString == null )
            throw new IllegalArgumentException( "Query sequence was empty" );

        // match the end of the target with the beginning of the query. We start with the whole thing
        for ( int i = 0; i < targetString.length(); i++ ) {
            String targetSub = targetString.substring( i );

            if ( queryString.indexOf( targetSub ) == 0 ) {
                return targetSub.length();
            }
        }

        return 0;
    }

    public static String reverseComplement( String sequence ) {
        if ( StringUtils.isBlank( sequence ) ) {
            return sequence;
        }

        StringBuilder buf = new StringBuilder();
        for ( int i = sequence.length() - 1; i >= 0; i-- ) {
            buf.append( complement( sequence.charAt( i ) ) );
        }
        return buf.toString();
    }

    /**
     * See for example http://www.bio-soft.net/sms/iupac.html
     */
    private static char complement( char baseLetter ) {

        switch ( baseLetter ) {
            // basics
            case 'A':
                return 'T';
            case 'T':
                return 'A';
            case 'G':
                return 'C';
            case 'C':
                return 'G';
            case 'U':
                return 'A';

            // purine/pyrimidine
            case 'R':
                return 'Y';
            case 'Y':
                return 'R';

            // complementary pairs.
            case 'S':
                return 'W';
            case 'W':
                return 'S';

            // non-complementary pairs
            case 'K':
                return 'M';
            case 'M':
                return 'K';

            // choice of 3
            case 'B':
                return 'D';
            case 'D':
                return 'B';

            case 'H':
                return 'V';
            case 'V':
                return 'H';

            // special
            case 'N':
                return 'N';
            case '-':
                return '-';
            case ' ':
                return ' ';
            default:
                break;
        }

        throw new IllegalArgumentException( "Don't know complement to " + baseLetter );

    }

    /**
     * @param sizes Blat-formatted string of sizes (comma-delimited)
     */
    public static int totalSize( String sizes ) {
        return totalSize( blatLocationsToIntArray( sizes ) );
    }

    /**
     * Compute the overlap between two physical locations. If both do not have a length the overlap is zero unless they
     * point to exactly the same nucleotide location, in which case the overlap is 1.
     */
    @SuppressWarnings("WeakerAccess") // Possible external use
    public static int computeOverlap( PhysicalLocation a, PhysicalLocation b ) {
        if ( !a.getChromosome().equals( b.getChromosome() ) ) {
            return 0;
        }

        if ( a.getNucleotideLength() == null && b.getNucleotideLength() == null ) {
            if ( a.getNucleotide().equals( b.getNucleotide() ) ) {
                return 1;
            }
            return 0;
        }

        return PhysicalLocation
                .computeOverlap( a.getNucleotide(), a.getNucleotide() + a.getNucleotideLength(), b.getNucleotide(),
                        b.getNucleotide() + b.getNucleotideLength() );

    }

    private static Collection<Reporter> copyReporters( Collection<Reporter> reporters ) {
        Collection<Reporter> copyReporters = new HashSet<>();
        for ( Reporter next : reporters ) {
            assert next.getStartInBioChar() != null : "Null startInBioChar";
            assert next.getImmobilizedCharacteristic() != null : "Null immobilized characteristic";

            Reporter copy = Reporter.Factory.newInstance();
            copy.setImmobilizedCharacteristic( next.getImmobilizedCharacteristic() );
            copy.setName( next.getName() );
            copy.setStartInBioChar( next.getStartInBioChar() );
            copyReporters.add( copy );
        }
        assert copyReporters.size() == reporters.size() : "Sequences did not copy properly";
        return copyReporters;
    }

    /**
     * Find the index of the aligned base in the center exon that is the center of the query.
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
     */
    private static Reporter findLeftMostProbe( Collection<Reporter> copyOfProbes ) {
        Long minLocation = ( long ) Integer.MAX_VALUE;
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

    private static int middleBase( int totalSize ) {
        return ( int ) Math.floor( totalSize / 2.0 );
    }

    private static int totalSize( int[] sizesArray ) {
        int totalSize = 0;
        for ( int aSizesArray : sizesArray ) {
            totalSize += aSizesArray;
        }
        return totalSize;
    }

}
