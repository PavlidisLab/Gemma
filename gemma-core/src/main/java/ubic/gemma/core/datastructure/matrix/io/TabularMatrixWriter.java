package ubic.gemma.core.datastructure.matrix.io;

import lombok.Setter;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.datastructure.matrix.DoubleSingleCellExpressionDataMatrix;
import ubic.gemma.core.datastructure.matrix.SingleCellExpressionDataMatrix;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ubic.gemma.core.datastructure.matrix.io.ExpressionDataWriterUtils.*;
import static ubic.gemma.core.util.TsvUtils.SUB_DELIMITER;
import static ubic.gemma.core.util.TsvUtils.format;
import static ubic.gemma.persistence.util.ByteArrayUtils.byteArrayToDoubles;

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
@Setter
public class TabularMatrixWriter implements SingleCellExpressionDataMatrixWriter {

    private final EntityUrlBuilder entityUrlBuilder;
    private final BuildInfo buildInfo;

    @Nullable
    private ScaleType scaleType;

    public TabularMatrixWriter( EntityUrlBuilder entityUrlBuilder, BuildInfo buildInfo ) {
        this.entityUrlBuilder = entityUrlBuilder;
        this.buildInfo = buildInfo;
    }

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
                int len = rowptr[i + 1] - rowptr[i];
                double[] vals = new double[len];
                int[] indices = new int[len];
                int w = 0;
                for ( int k = rowptr[i]; k < rowptr[i + 1]; k++ ) {
                    vals[w] = data[k];
                    indices[w] = colind[k];
                    w++;
                }
                writeDoubleVector( cs, cs2gene, matrix.getSingleCellDimension(), vals, indices, writer );
                written++;
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

    private int write( Iterator<SingleCellExpressionDataVector> it, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Writer writer ) throws IOException {
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

    private void writeHeader( ExpressionExperiment ee, QuantitationType qt, SingleCellDimension scd, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Writer pwriter ) throws IOException {
        String experimentUrl = ee.getId() != null ? entityUrlBuilder.fromHostUrl().entity( ee ).web().toUriString() : null;
        appendBaseHeader( ee, "Single-cell expression data", experimentUrl, buildInfo, pwriter );
        pwriter.append( "# Dataset: " ).append( format( ee ) ).append( "\n" );
        pwriter.append( "# Single-cell dimension: " ).append( format( scd ) ).append( "\n" );
        pwriter.append( "# Quantitation type: " ).append( format( qt ) ).append( "\n" );
        pwriter.append( "# Samples: " ).append( scd.getBioAssays().stream().map( TsvUtils::format ).collect( Collectors.joining( ", " ) ) ).append( "\n" );
        pwriter.append( "probe_id\tprobe_name" );
        if ( cs2gene != null ) {
            pwriter.write( "\tgene_id\tgene_name\tgene_ncbi_id\tgene_ensembl_id\tgene_official_symbol\tgene_official_name" );
        }
        for ( BioAssay ba : scd.getBioAssays() ) {
            Assert.notNull( ba.getName() );
            String sampleColumnPrefix = constructSampleName( ba.getSampleUsed(), ba ) + "_";
            pwriter.append( "\t" ).append( sampleColumnPrefix ).append( "cell_ids" )
                    .append( "\t" ).append( sampleColumnPrefix ).append( "values" );
        }
        pwriter.write( '\n' );
    }

    private void writeVector( SingleCellExpressionDataVector vector, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, Writer pwriter ) throws IOException {
        if ( vector.getQuantitationType().getRepresentation() != PrimitiveType.DOUBLE ) {
            throw new UnsupportedOperationException( "Writing single-cell vectors of " + vector.getQuantitationType().getRepresentation() + " is not supported." );
        }
        writeDoubleVector( vector.getDesignElement(), cs2gene, vector.getSingleCellDimension(), convertVector( byteArrayToDoubles( vector.getData() ), vector.getQuantitationType(), scaleType ), vector.getDataIndices(), pwriter );
    }

    private void writeDoubleVector( CompositeSequence cs, @Nullable Map<CompositeSequence, Set<Gene>> cs2gene, SingleCellDimension dimension, double[] vec, int[] indices, Writer pwriter ) throws IOException {
        pwriter.append( format( cs.getId() ) )
                .append( '\t' ).append( format( cs.getName() ) );
        if ( cs2gene != null ) {
            writeGene( cs2gene.get( cs ), pwriter );
        }
        int numSamples = dimension.getBioAssays().size();
        int start = 0;
        for ( int i = 0; i < numSamples; i++ ) {
            int sampleOffset = dimension.getBioAssaysOffset()[i];
            int nextSampleOffset = sampleOffset + dimension.getNumberOfCellsBySample( i );
            // check where the next sample begins, only search past this sample starting point
            int end = Arrays.binarySearch( indices, start, indices.length, nextSampleOffset );
            if ( end < 0 ) {
                end = -end - 1;
            }
            int nnz = end - start;
            if ( nnz == 0 ) {
                // no cells to write for gene & sample
                pwriter.append( "\t\t" );
            } else {
                String[] cellIds = new String[nnz];
                String[] vals = new String[nnz];
                int w = 0;
                for ( int j = start; j < end; j++ ) {
                    cellIds[w] = format( dimension.getCellIds().get( indices[j] ) );
                    vals[w] = format( vec[j] );
                    w++;
                }
                pwriter
                        .append( '\t' ).append( StringUtils.join( cellIds, SUB_DELIMITER ) )
                        .append( '\t' ).append( StringUtils.join( vals, SUB_DELIMITER ) );
            }
            start = end;
        }
        pwriter.append( '\n' );
    }

    private void writeGene( @Nullable Set<Gene> genes, Writer pwriter ) throws IOException {
        // id, probe_id, probe_name, gene_(id|name|ncbi_id|official_symbol|official_name)
        if ( genes == null || genes.isEmpty() ) {
            pwriter.write( "\t\t\t\t\t\t" );
            return;
        }
        List<Gene> sortedGenes = genes.stream().sorted( Comparator.comparing( Gene::getOfficialSymbol ) ).collect( Collectors.toList() );
        pwriter
                .append( '\t' ).append( formatGenesLongAttribute( sortedGenes, Gene::getId ) )
                .append( '\t' ).append( formatGenesAttribute( sortedGenes, Gene::getName ) )
                .append( '\t' ).append( formatGenesIntAttribute( sortedGenes, Gene::getNcbiGeneId ) )
                .append( '\t' ).append( formatGenesAttribute( sortedGenes, Gene::getEnsemblId ) )
                .append( '\t' ).append( formatGenesAttribute( sortedGenes, Gene::getOfficialSymbol ) )
                .append( '\t' ).append( formatGenesAttribute( sortedGenes, Gene::getOfficialName ) );
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
