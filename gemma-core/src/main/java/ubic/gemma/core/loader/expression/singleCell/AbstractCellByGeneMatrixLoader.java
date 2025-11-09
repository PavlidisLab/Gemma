package ubic.gemma.core.loader.expression.singleCell;

import cern.colt.list.IntArrayList;
import lombok.Setter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.sequencing.SequencingMetadata;
import ubic.gemma.core.loader.util.mapper.EntityMapper;
import ubic.gemma.core.util.FileUtils;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeUtils;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static ubic.gemma.persistence.util.ByteArrayUtils.objectListToBytes;

/**
 * Load cell by gene matrix formats.
 */
@Setter
public abstract class AbstractCellByGeneMatrixLoader implements CellByGeneMatrixLoader {

    protected final Log log = LogFactory.getLog( this.getClass() );

    private boolean ignoreUnmatchedDesignElements = true;

    /**
     * Format to use for parsing tabular files.
     */
    private CSVFormat format = DEFAULT_FORMAT;

    /**
     * Read the transpose of the files.
     * <p>
     * The default is to assume that rows are genes and columns are cells.
     */
    private boolean transpose;

    /**
     * Charset to use to parse tabular files.
     */
    private Charset charset = StandardCharsets.UTF_8;

    protected boolean isTranspose() {
        return transpose;
    }

    protected CSVParser parseMatrix( Path file ) throws IOException {
        if ( file.getFileName().toString().endsWith( ".gz" ) ) {
            return format.parse( new InputStreamReader( FileUtils.openCompressedFile( file ), charset ) );
        } else {
            return format.parse( Files.newBufferedReader( file, charset ) );
        }
    }

    protected void readCellIds( Path file, List<String> cellIds ) throws IOException {
        try ( CSVParser parser = parseMatrix( file ) ) {
            if ( transpose ) {
                for ( CSVRecord record : parser ) {
                    cellIds.add( record.get( 0 ) );
                }
            } else {
                cellIds.addAll( parser.getHeaderNames().subList( 1, parser.getHeaderNames().size() ) );
            }
        }
    }

    protected void readGenes( Path file, Collection<String> geneIds ) throws IOException {
        try ( CSVParser parser = parseMatrix( file ) ) {
            if ( transpose ) {
                geneIds.addAll( parser.getHeaderNames().subList( 1, parser.getHeaderNames().size() ) );
            } else {
                log.warn( "Reading genes from a cell by gene matrix is very inefficient, consider using streamGenes() instead." );
                for ( CSVRecord record : parser ) {
                    geneIds.add( record.get( 0 ) );
                }
            }
        }
    }

    protected Stream<String> streamGenes( Path file ) throws IOException {
        if ( transpose ) {
            try ( CSVParser parser = parseMatrix( file ) ) {
                List<String> cellIds = new ArrayList<>();
                for ( CSVRecord record : parser ) {
                    cellIds.add( record.get( 0 ) );
                }
                return cellIds.stream();
            }
        } else {
            return parseMatrix( file ).stream().map( record -> record.get( 0 ) );
        }
    }

    /**
     * Detect the quantitation type from data.
     */
    protected QuantitationType detectQt( Path path ) {
        // TODO
        return QuantitationType.Factory.newInstance();
    }

    @Nullable
    protected SingleCellExpressionDataVector readVector( SingleCellDimension dimension,
            QuantitationType quantitationType, EntityMapper.StatefulEntityMapper<CompositeSequence> mapper,
            CSVRecord record, Collection<String> unresolveGeneIds ) {
        Assert.isTrue( !transpose, "Cannot read a single-cell vector from a row record for a transposed matrix." );
        SingleCellExpressionDataVector vector = new SingleCellExpressionDataVector();
        String geneId = record.get( 0 );
        Optional<CompositeSequence> designElement = mapper.matchOne( geneId );
        if ( designElement.isPresent() ) {
            vector.setDesignElement( designElement.get() );
        } else if ( ignoreUnmatchedDesignElements ) {
            unresolveGeneIds.add( geneId );
            return null;
        } else {
            throw new IllegalArgumentException( "Could not resolve a design element for '" + geneId + "', use ignoreUnmatchedDesignElements to ignore unmapped gene identifiers." );
        }
        List<Object> data = new ArrayList<>();
        IntArrayList dataIndices = new IntArrayList();
        List<String> cellIds = dimension.getCellIds();
        for ( int i = 0; i < cellIds.size(); i++ ) {
            String cellId = cellIds.get( i );
            Object val = parseValue( record.get( cellId ), quantitationType );
            if ( QuantitationTypeUtils.isDefaultValue( val, quantitationType ) ) {
                continue;
            }
            data.add( val );
            dataIndices.add( i );
        }
        vector.setData( objectListToBytes( data ) );
        vector.setDataIndices( dataIndices.elements() );
        return vector;
    }

    /**
     * Parse a value with awareness of the quantitation type.
     */
    private Object parseValue( String s, QuantitationType quantitationType ) {
        switch ( quantitationType.getRepresentation() ) {
            case DOUBLE:
                return TsvUtils.parseDouble( s );
            case FLOAT:
                return TsvUtils.parseFloat( s );
            case LONG:
                return TsvUtils.parseLong( s );
            case INT:
                return TsvUtils.parseInt( s );
            default:
                throw new UnsupportedOperationException( "nope." );
        }
    }

    @Override
    public Set<ExperimentalFactor> getFactors( Collection<BioAssay> samples, @Nullable Map<BioMaterial, Set<FactorValue>> factorValueAssignments ) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<BioMaterial, Set<Characteristic>> getSamplesCharacteristics( Collection<BioAssay> samples ) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<CellTypeAssignment> getCellTypeAssignments( SingleCellDimension dimension ) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<CellLevelCharacteristics> getOtherCellLevelCharacteristics( SingleCellDimension dimension ) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<BioAssay, SequencingMetadata> getSequencingMetadata( SingleCellDimension dimension ) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<BioAssay, SequencingMetadata> getSequencingMetadata( Collection<BioAssay> samples ) throws IOException {
        throw new UnsupportedOperationException();
    }
}
