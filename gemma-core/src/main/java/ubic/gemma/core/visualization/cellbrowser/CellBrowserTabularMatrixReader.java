package ubic.gemma.core.visualization.cellbrowser;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.util.Assert;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.sparse.CompRowMatrixUtils;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author poirigui
 */
@CommonsLog
public class CellBrowserTabularMatrixReader {

    private static final CSVFormat CELL_BROWSER_MATRIX_FORMAT = CSVFormat.TDF.builder()
            .setHeader()
            .setSkipHeaderRecord( true )
            .get();

    @Setter
    private boolean ignoreUnmatchedCellIds = true;

    @Setter
    private boolean ignoreUnmatchedDesignElements = true;

    /**
     *
     * @param reader
     * @param quantitationType
     * @param designElementsMap
     * @return
     * @throws IOException
     */
    public SingleCellExpressionDataDoubleMatrix readMatrix( Reader reader, QuantitationType quantitationType,
            Map<String, CompositeSequence> designElementsMap, Map<String, BioAssay> cellIdToBioAssayMap ) throws IOException {
        Assert.isTrue( quantitationType.getRepresentation() == PrimitiveType.DOUBLE );
        try ( CSVParser parser = CELL_BROWSER_MATRIX_FORMAT.parse( reader ) ) {
            List<String> cellIds = parser.getHeaderNames();
            cellIds = cellIds.subList( 1, cellIds.size() );

            // sort cell IDs by assay
            ArrayList<String> sortedCellIds = new ArrayList<>( cellIds );
            sortedCellIds.sort( Comparator.comparing( cellIdToBioAssayMap::get, Comparator.nullsLast( Comparator.comparing( BioAssay::getName ) ) ) );

            // index of a cell in the sorted output
            Map<String, Integer> cellIdIndex = ListUtils.indexOfElements( sortedCellIds );

            Set<String> unmatchedCellIds = new HashSet<>();
            List<BioAssay> bioAssays = new ArrayList<>();
            IntArrayList bioAssaysOffset = new IntArrayList();
            BioAssay currentBa = null;
            for ( int i = 0; i < sortedCellIds.size(); i++ ) {
                String cellId = sortedCellIds.get( i );
                BioAssay ba = cellIdToBioAssayMap.get( cellId );
                if ( ba == null ) {
                    unmatchedCellIds.add( cellId );
                    continue;
                }
                if ( currentBa != ba ) {
                    bioAssays.add( ba );
                    bioAssaysOffset.add( i );
                    currentBa = ba;
                }
            }

            if ( !unmatchedCellIds.isEmpty() ) {
                String m = String.format( "No assay found for %d cell IDs. Here's a few examples: %s.", unmatchedCellIds.size(),
                        unmatchedCellIds.stream().limit( 10 ).collect( Collectors.joining( ", " ) ) );
                if ( ignoreUnmatchedCellIds ) {
                    log.warn( m );
                } else {
                    throw new IllegalArgumentException( m );
                }
            }

            List<CompositeSequence> designElements = new ArrayList<>();

            IntArrayList rowptr = new IntArrayList( cellIds.size() + 1 );
            // assume 30,000 genes and 95% sparsity
            int estimatedEntries = Math.round( 0.05f * cellIds.size() * 30000 );
            IntArrayList colind = new IntArrayList( estimatedEntries );
            DoubleArrayList data = new DoubleArrayList( estimatedEntries );

            rowptr.add( 0 );
            Set<String> unmatchedGenes = new HashSet<>();
            for ( CSVRecord record : parser ) {
                int nnz = 0;
                String gene = record.get( "gene" );
                CompositeSequence designElement = designElementsMap.get( gene );
                if ( designElement == null ) {
                    if ( ignoreUnmatchedDesignElements ) {
                        unmatchedGenes.add( gene );
                        continue;
                    } else {
                        throw new IllegalArgumentException( "No design element found for gene '" + gene + "'." );
                    }
                }
                designElements.add( designElement );
                for ( String cellId : sortedCellIds ) {
                    double value = Double.parseDouble( record.get( cellId ) );
                    if ( value != 0 ) {
                        colind.add( cellIdIndex.get( cellId ) );
                        data.add( value );
                        nnz++;
                    }
                }
                rowptr.add( nnz );
            }

            if ( !unmatchedGenes.isEmpty() ) {
                String m = String.format( "No design elements for %d gene identifiers. Here's a few examples: %s.", unmatchedGenes.size(),
                        unmatchedGenes.stream().limit( 10 ).collect( Collectors.joining( ", " ) ) );
                if ( ignoreUnmatchedDesignElements ) {
                    log.warn( m );
                } else {
                    throw new IllegalArgumentException( m );
                }
            }

            ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
            SingleCellDimension dimension = new SingleCellDimension();
            dimension.setCellIds( cellIds );
            dimension.setNumberOfCellIds( cellIds.size() );
            dimension.setBioAssays( bioAssays );
            bioAssaysOffset.trimToSize();
            dimension.setBioAssaysOffset( bioAssaysOffset.elements() );
            data.trimToSize();
            CompRowMatrix mat = CompRowMatrixUtils.newCompRowMatrix( designElements.size(), cellIds.size(),
                    rowptr.elements(), colind.elements(), data.elements() );

            return new SingleCellExpressionDataDoubleMatrix( ee, quantitationType, dimension, designElements, mat );
        }
    }
}
