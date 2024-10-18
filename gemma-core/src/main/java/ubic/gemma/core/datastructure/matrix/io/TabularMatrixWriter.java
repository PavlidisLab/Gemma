package ubic.gemma.core.datastructure.matrix.io;

import no.uib.cipr.matrix.sparse.CompRowMatrix;
import org.springframework.util.Assert;
import ubic.gemma.core.datastructure.matrix.DoubleSingleCellExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.util.ByteArrayUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ubic.gemma.core.datastructure.matrix.io.TsvUtils.*;

/**
 * Write a set of single-cell vectors to a simple tabular format.
 * <p>
 * The following column are written to disk:
 * <p>
 * - probe_id
 * - probe_name
 * - gene_(id|name|ncbi_id|official_symbol|official_name) if a cs2gene mapping is provided
 * - sample_id
 * - sample_name
 * - cell_id
 * - value
 * @author poirigui
 */
public class TabularMatrixWriter implements SingleCellExpressionDataMatrixWriter {

    @Override
    public int write( SingleCellExpressionDataMatrix<?> matrix, Writer writer ) throws IOException {
        return write( matrix, null, writer );
    }

    public int write( SingleCellExpressionDataMatrix<?> matrix, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Writer writer ) throws IOException {
        Assert.isInstanceOf( DoubleSingleCellExpressionDataMatrix.class, matrix,
                "Only single-cell matrices of doubles are supported." );
        CompRowMatrix mat = ( ( DoubleSingleCellExpressionDataMatrix ) matrix ).getMatrix();
        int[] rowptr = mat.getRowPointers();
        int[] colind = mat.getColumnIndices();
        double[] data = mat.getData();
        int written = 0;
        try ( PrintWriter pwriter = new PrintWriter( writer ) ) {
            writeHeader( matrix.getExpressionExperiment(), matrix.getQuantitationType(), matrix.getSingleCellDimension(), cs2gene, pwriter );
            for ( int i = 0; i < rowptr.length - 1; i++ ) {
                CompositeSequence cs = matrix.getDesignElements().get( i );
                for ( int k = rowptr[i]; k < rowptr[i + 1]; k++ ) {
                    int j = colind[k];
                    double val = data[k];
                    BioAssay ba = matrix.getSingleCellDimension().getBioAssay( j );
                    String cellId = matrix.getSingleCellDimension().getCellIds().get( j );
                    writeRow( cs, ba, cellId, format( val ), cs2gene, pwriter );
                    written++;
                }
            }
        }
        return written;
    }

    public int write( Collection<SingleCellExpressionDataVector> vectors, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Writer writer ) throws IOException {
        return write( vectors.iterator(), cs2gene, writer );
    }

    public int write( Stream<SingleCellExpressionDataVector> vectors, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Writer writer ) throws IOException {
        return write( vectors.iterator(), cs2gene, writer );
    }

    private int write( Iterator<SingleCellExpressionDataVector> it, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Writer writer ) {
        int written = 0;
        try ( PrintWriter pwriter = new PrintWriter( writer ) ) {
            SingleCellExpressionDataVector firstVec = it.next();
            writeHeader( firstVec.getExpressionExperiment(), firstVec.getQuantitationType(), firstVec.getSingleCellDimension(), cs2gene, pwriter );
            writeVector( firstVec, cs2gene, pwriter );
            written++;
            while ( it.hasNext() ) {
                writeVector( it.next(), cs2gene, pwriter );
                written++;
            }
        }
        return written;
    }

    private void writeHeader( ExpressionExperiment ee, QuantitationType qt, SingleCellDimension scd, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, PrintWriter pwriter ) {
        for ( String line : GEMMA_CITATION_NOTICE ) {
            pwriter.printf( "# %s\n", line );
        }
        pwriter.printf( "# Single-cell expression data for " + ee + "\n" );
        pwriter.printf( "# Single-cell dimension: " + scd + "\n" );
        pwriter.printf( "# Quantitation type: " + qt + "\n" );
        pwriter.printf( "probe_id\tprobe_name" );
        if ( cs2gene != null ) {
            pwriter.printf( "\tgene_id\tgene_name\tgene_ncbi_id\tgene_ensembl_id\tgene_official_symbol\tgene_official_name" );
        }
        pwriter.printf( "\tsample_id\tsample_name\tcell_id\tvalue\n" );
    }

    private void writeVector( SingleCellExpressionDataVector vector, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, PrintWriter pwriter ) {
        switch ( vector.getQuantitationType().getRepresentation() ) {
            case DOUBLE:
                writeDoubleVector( vector, cs2gene, pwriter );
                break;
            case INT:
                writeIntVector( vector, cs2gene, pwriter );
                break;
            default:
                throw new UnsupportedOperationException( "Writing single-cell vectors of " + vector.getQuantitationType().getRepresentation() + " is not supported." );
        }
    }

    private void writeDoubleVector( SingleCellExpressionDataVector vector, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, PrintWriter pwriter ) {
        double[] vec = ByteArrayUtils.byteArrayToDoubles( vector.getData() );
        List<String> cellIds = vector.getSingleCellDimension().getCellIds();
        int[] indices = vector.getDataIndices();
        for ( int j = 0; j < indices.length; j++ ) {
            int i = indices[j];
            double v = vec[j];
            BioAssay ba = vector.getSingleCellDimension().getBioAssay( i );
            String cellId = cellIds.get( i );
            writeRow( vector.getDesignElement(), ba, cellId, format( v ), cs2gene, pwriter );
        }
    }

    private void writeIntVector( SingleCellExpressionDataVector vector, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, PrintWriter pwriter ) {
        CompositeSequence cs = vector.getDesignElement();
        int[] intVec = ByteArrayUtils.byteArrayToInts( vector.getData() );
        List<String> cellIds = vector.getSingleCellDimension().getCellIds();
        int[] indices = vector.getDataIndices();
        for ( int j = 0; j < indices.length; j++ ) {
            int i = indices[j];
            int v = intVec[j];
            BioAssay ba = vector.getSingleCellDimension().getBioAssay( i );
            String cellId = cellIds.get( i );
            writeRow( cs, ba, cellId, format( v ), cs2gene, pwriter );
        }
    }

    private void writeRow( CompositeSequence cs, BioAssay ba, String cellId, String formattedVal, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, PrintWriter pwriter ) {
        pwriter.printf( "%d\t%s", cs.getId(), format( cs.getName() ) );
        if ( cs2gene != null ) {
            writeGene( cs2gene.get( cs ), pwriter );
        }
        pwriter.printf( "%s\t%s\t%s\t%s\n", format( ba.getId() ), format( ba.getName() ), format( cellId ), formattedVal );
    }

    private void writeGene( @Nullable Set<Gene> genes, PrintWriter pwriter ) {
        // id, probe_id, probe_name, gene_(id|name|ncbi_id|official_symbol|official_name)
        if ( genes == null || genes.isEmpty() ) {
            pwriter.print( "\t\t\t\t" );
            return;
        }
        List<Gene> sortedGenes = genes.stream().sorted( Comparator.comparing( Gene::getOfficialSymbol ) ).collect( Collectors.toList() );
        pwriter.printf( "%s\t%s\t%s\t%s\t%s\t%s",
                formatGenesLongAttribute( sortedGenes, Gene::getId ),
                formatGenesAttribute( sortedGenes, Gene::getName ),
                formatGenesIntAttribute( sortedGenes, Gene::getNcbiGeneId ),
                formatGenesAttribute( sortedGenes, Gene::getEnsemblId ),
                formatGenesAttribute( sortedGenes, Gene::getOfficialSymbol ),
                formatGenesAttribute( sortedGenes, Gene::getOfficialName ) );
    }

    private String formatGenesLongAttribute( List<Gene> genes, Function<Gene, Long> func ) {
        return genes.stream().map( func ).map( TsvUtils::format ).collect( Collectors.joining( String.valueOf( SUB_DELIMITER ) ) );
    }

    private String formatGenesIntAttribute( List<Gene> genes, Function<Gene, Integer> func ) {
        return genes.stream().map( func ).map( TsvUtils::format ).collect( Collectors.joining( String.valueOf( SUB_DELIMITER ) ) );
    }

    private String formatGenesAttribute( List<Gene> genes, Function<Gene, String> func ) {
        return genes.stream().map( func ).map( TsvUtils::format ).collect( Collectors.joining( String.valueOf( SUB_DELIMITER ) ) );
    }
}
