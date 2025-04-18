package ubic.gemma.core.loader.expression.singleCell.metadata;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.measurement.MeasurementKind;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssayData.CellLevelMeasurements;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static java.util.Objects.requireNonNull;

@CommonsLog
public class GenericCellLevelMeasurementsMetadataParser implements CellLevelFeaturesMetadataParser<CellLevelMeasurements> {

    private static final CSVFormat TSV_FORMAT = CSVFormat.TDF.builder()
            .setHeader()
            // .setHeader( "sample_id", "cell_id", "value", "type", "unit", "kind", "representation" )
            .setSkipHeaderRecord( true )
            .setCommentMarker( '#' )
            .get();

    private final SingleCellDimension dimension;

    public GenericCellLevelMeasurementsMetadataParser( SingleCellDimension dimension, BioAssayMapper bioAssayToSampleNameMapper ) {
        this.dimension = dimension;
    }

    @Override
    public void setUseCellIdsIfSampleNameIsMissing( boolean useCellIdsIfSampleNameIsMissing ) {

    }

    @Override
    public void setInferSamplesFromCellIdsOverlap( boolean inferSamplesFromCellIdsOverlap ) {

    }

    @Override
    public void setIgnoreUnmatchedSamples( boolean ignoreUnmatchedSamples ) {

    }

    @Override
    public void setIgnoreUnmatchedCellIds( boolean ignoreUnmatchedCellIds ) {

    }

    public Set<CellLevelMeasurements> parse( Path metadataFile ) throws IOException {
        // maps category ID to measurements
        Map<String, CellLevelMeasurements> measurementsMap = new HashMap<>();
        Map<String, Unit> unitCache = new HashMap<>();
        Map<CellLevelMeasurements, Object> dataCache = new HashMap<>();
        try ( CSVParser parser = TSV_FORMAT.parse( Files.newBufferedReader( metadataFile, StandardCharsets.UTF_8 ) ) ) {
            for ( CSVRecord record : parser ) {
                String sampleId = record.get( "sample_id" );
                String cellId = record.get( "cell_id" );
                int cellIndex = getCellIndex( sampleId, cellId );
                String categoryId = getCategoryId( record );
                CellLevelMeasurements measurements = measurementsMap
                        .computeIfAbsent( categoryId, k -> createCellLevelMeasurements( record, unitCache ) );
                Object data = dataCache.computeIfAbsent( measurements, this::createData );
                String value = StringUtils.stripToNull( record.get( "value" ) );
                switch ( measurements.getRepresentation() ) {
                    case DOUBLE:
                        ( ( double[] ) data )[cellIndex] = Double.parseDouble( value );
                        break;
                    case FLOAT:
                        ( ( float[] ) data )[cellIndex] = Float.parseFloat( value );
                        break;
                    case LONG:
                        ( ( long[] ) data )[cellIndex] = Long.parseLong( value );
                        break;
                    case INT:
                        ( ( int[] ) data )[cellIndex] = Integer.parseInt( value );
                        break;
                    case BOOLEAN:
                        ( ( boolean[] ) data )[cellIndex] = Boolean.parseBoolean( value );
                        break;
                    case BITSET:
                        ( ( BitSet ) data ).set( cellIndex, Boolean.parseBoolean( value ) );
                        break;
                    default:
                        throw new UnsupportedOperationException( "Unsupported representation " + measurements.getRepresentation() + " for cell-level measurements." );
                }
            }
        }

        // populate the data fields
        for ( CellLevelMeasurements measurements : measurementsMap.values() ) {
            switch ( measurements.getRepresentation() ) {
                case DOUBLE:
                    measurements.setDataAsDoubles( ( double[] ) dataCache.get( measurements ) );
                    break;
                case FLOAT:
                    measurements.setDataAsFloats( ( float[] ) dataCache.get( measurements ) );
                    break;
                case LONG:
                    measurements.setDataAsLongs( ( long[] ) dataCache.get( measurements ) );
                    break;
                case INT:
                    measurements.setDataAsInts( ( int[] ) dataCache.get( measurements ) );
                    break;
                case BOOLEAN:
                    measurements.setDataAsBooleans( ( boolean[] ) dataCache.get( measurements ) );
                    break;
                case BITSET:
                    measurements.setDataAsBitSet( ( BitSet ) dataCache.get( measurements ) );
                    break;
                default:
                    throw new UnsupportedOperationException( "Unsupported representation " + measurements.getRepresentation() + " for cell-level measurements." );
            }
        }

        return new HashSet<>( measurementsMap.values() );
    }

    private CellLevelMeasurements createCellLevelMeasurements( CSVRecord record, Map<String, Unit> unitCache ) {
        String category = StringUtils.stripToNull( record.get( "category" ) );
        String categoryUri = StringUtils.stripToNull( record.get( "category_uri" ) );
        CellLevelMeasurements measurements = CellLevelMeasurements.Factory.newInstance( new Category( category, categoryUri ) );
        String unit = StringUtils.stripToNull( record.get( "unit" ) );
        if ( unit != null ) {
            measurements.setUnit( unitCache.computeIfAbsent( unit, Unit.Factory::newInstance ) );
        }
        String type;
        if ( ( type = StringUtils.stripToNull( record.get( "type" ) ) ) != null ) {
            measurements.setType( MeasurementType.valueOf( type.toUpperCase() ) );
        } else {
            measurements.setType( MeasurementType.ABSOLUTE );
        }
        String kind;
        if ( ( kind = StringUtils.stripToNull( record.get( "kind" ) ) ) != null ) {
            try {
                measurements.setKindCV( MeasurementKind.valueOf( kind.toUpperCase() ) );
            } catch ( IllegalArgumentException e ) {
                log.warn( "Unrecognized measurement kind: " + kind + ". Defaulting to OTHER." );
                measurements.setKindCV( MeasurementKind.OTHER );
                measurements.setOtherKind( kind );
            }
        }
        String repr = requireNonNull( StringUtils.stripToNull( record.get( "representation" ) ),
                "The representation column must be provided." );
        measurements.setRepresentation( PrimitiveType.valueOf( repr.toUpperCase() ) );
        return measurements;
    }

    private Object createData( CellLevelMeasurements k ) {
        switch ( k.getRepresentation() ) {
            case DOUBLE:
                double[] d = new double[dimension.getNumberOfCells()];
                Arrays.fill( d, Double.NaN );
                return d;
            case FLOAT:
                float[] f = new float[dimension.getNumberOfCells()];
                Arrays.fill( f, Float.NaN );
                return f;
            case LONG:
                return new long[dimension.getNumberOfCells()];
            case INT:
                return new int[dimension.getNumberOfCells()];
            case BOOLEAN:
                return new boolean[dimension.getNumberOfCells()];
            case BITSET:
                return new BitSet( dimension.getNumberOfCells() );
            default:
                // TODO: support variable-length representation
                throw new UnsupportedOperationException( "Unsupported representation: " + k.getRepresentation() + "." );
        }
    }

    private String getCategoryId( CSVRecord record ) {
        String cid;
        if ( ( cid = StringUtils.stripToNull( record.get( "category_id" ) ) ) != null ) {
            return cid;
        }
        if ( ( cid = StringUtils.stripToNull( record.get( "category_uri" ) ) ) != null ) {
            return cid;
        }
        if ( ( cid = StringUtils.stripToNull( record.get( "category" ) ) ) != null ) {
            return cid;
        }
        throw new IllegalStateException( "No category ID found in record: " + record );
    }

    private int getCellIndex( String sampleId, String cellId ) {
        return -1;
    }
}
