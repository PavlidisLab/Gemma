package ubic.gemma.core.loader.util.mapper;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Strategy for mapping sample name to {@link BioAssay} that relies on overlapping cell IDs.
 * @author poirigui
 */
@CommonsLog
public class CellIdOverlapBioAssayMapper extends MapBasedEntityMapper<BioAssay> implements BioAssayMapper {

    public CellIdOverlapBioAssayMapper( Map<BioAssay, Set<String>> bioAssayToCellIds, Map<String, Set<String>> sampleNameToCellIds ) {
        super( "Cell ID overlap", createSampleMapping( bioAssayToCellIds, sampleNameToCellIds ) );
    }

    private static Map<String, BioAssay> createSampleMapping( Map<BioAssay, Set<String>> bioAssayToCellIds, Map<String, Set<String>> sampleNameToCellIds ) {
        List<BioAssay> bioAssays = new ArrayList<>( bioAssayToCellIds.keySet() );
        List<String> sampleNames = new ArrayList<>( sampleNameToCellIds.keySet() );

        double[][] overlap = new double[sampleNameToCellIds.size()][bioAssayToCellIds.size()];
        Map<String, BioAssay> sampleNameMapping = new HashMap<>();
        for ( int i = 0; i < sampleNames.size(); i++ ) {
            String sampleName = sampleNames.get( i );
            for ( int j = 0; j < bioAssays.size(); j++ ) {
                BioAssay ba = bioAssays.get( j );
                overlap[i][j] = jaccard( sampleNameToCellIds.get( sampleName ), bioAssayToCellIds.get( ba ) );
            }
        }

        // print the correspondence matrix
        StringBuilder overlapMatrix = new StringBuilder();
        overlapMatrix.append( "Sample Name\t" ).append( bioAssays.stream().map( BioAssay::getName ).collect( Collectors.joining( "\t" ) ) )
                .append( "\n" );
        for ( int i = 0; i < sampleNames.size(); i++ ) {
            overlapMatrix
                    .append( sampleNames.get( i ) ).append( "\t" )
                    .append( Arrays.stream( overlap[i] ).mapToObj( f -> String.format( "%.2f", f ) ).collect( Collectors.joining( "\t" ) ) )
                    .append( "\n" );
        }
        log.info( "Cell ID overlap matrix using Jaccard similarity metric:\n" + overlapMatrix );

        // resolve the best mappings from the overlap matrix
        for ( int i = 0; i < sampleNames.size(); i++ ) {
            String sampleName = sampleNames.get( i );
            double max = 0;
            int maxJ = -1;
            int maxCount = 0;
            for ( int j = 0; j < bioAssays.size(); j++ ) {
                if ( overlap[i][j] > max ) {
                    max = overlap[i][j];
                    maxJ = j;
                    maxCount = 1;
                } else if ( overlap[i][j] == max ) {
                    maxCount++;
                }
            }
            if ( maxJ == -1 ) {
                // this will be treated as an unmatched sample
                log.warn( "No cell ID overlap found for sample " + sampleName + "." );
                continue;
            }
            if ( maxCount > 1 ) {
                List<BioAssay> ambiguousAssays = new ArrayList<>();
                for ( int j = 0; j < bioAssays.size(); j++ ) {
                    if ( overlap[i][j] == max ) {
                        ambiguousAssays.add( bioAssays.get( j ) );
                    }
                }
                log.warn( String.format( "Overlap for %s was not unique: %d assays have %.2f%% matching cell IDs:\n\t%s",
                        sampleName, maxCount, 100 * max, ambiguousAssays.stream().map( String::valueOf ).collect( Collectors.joining( "\n\t" ) ) ) );
                continue;
            }
            sampleNameMapping.put( sampleName, bioAssays.get( maxJ ) );
        }

        // warn for BA that have more than one sample name associated to?
        // those could be fine (i.e. authors used various identifiers for the s
        Map<BioAssay, Set<String>> nnn = sampleNameMapping.entrySet().stream()
                .collect( Collectors.groupingBy( Map.Entry::getValue, Collectors.mapping( Map.Entry::getKey, Collectors.toSet() ) ) );
        nnn.forEach( ( ba, v ) -> {
            if ( v.size() > 1 ) {
                log.warn( "More than one sample name associated to " + ba + ": " + v );
            }
        } );

        log.info( "Final sample association:\n\t" + sampleNameMapping.entrySet().stream()
                .map( e -> e.getKey() + " → " + e.getValue().getName() )
                .collect( Collectors.joining( "\n\t" ) ) );

        return sampleNameMapping;
    }

    /**
     * Efficiently compute the Jaccard index of two sets.
     */
    private static double jaccard( Set<String> a, Set<String> b ) {
        int intersectionSize = 0;
        for ( String elem : a ) {
            if ( b.contains( elem ) ) {
                intersectionSize++;
            }
        }
        return ( double ) intersectionSize / ( double ) ( a.size() + b.size() - intersectionSize );
    }
}
