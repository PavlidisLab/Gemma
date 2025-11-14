package ubic.gemma.core.analysis.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.singleCell.aggregate.SingleCellExpressionExperimentAggregateServiceImpl;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.rightPad;

/**
 * Utilities for creating, reading and writing mappings of {@link CellLevelCharacteristics} to {@link ExperimentalFactor}.
 * <p>
 * The most common use case for this is to map the cell type assigmnents from a {@link ubic.gemma.model.expression.bioAssayData.CellTypeAssignment}
 * to a cell type factor.
 *
 * @author poirigui
 */
@CommonsLog
public class CellLevelCharacteristicsMappingUtils {

    private static final CSVFormat CTA_MAPPING_FORMAT = CSVFormat.TDF.builder().setHeader( "cell_type_id", "factor_value_id" ).setCommentMarker( '#' ).build();

    /**
     * Map the cell types from a cell type assignment to factor values in a cell type factor.
     * <p>
     * There is a possibility that no factor value is found for a given cell type, in which case it is ignored.
     * <p>
     * TODO: this should be private, but we reuse the same logic for aggregating in {@link SingleCellExpressionExperimentAggregateServiceImpl}.
     *
     * @throws IllegalStateException if there is more than one factor value mapping a given cell type
     */
    public static Map<Characteristic, FactorValue> createMappingByFactorValueCharacteristics( CellLevelCharacteristics cta, ExperimentalFactor factor ) {
        Assert.isTrue( factor.getType() == FactorType.CATEGORICAL, "The factor must be categorical." );
        Map<Characteristic, FactorValue> mappedFactors = new HashMap<>();
        for ( Characteristic characteristic : cta.getCharacteristics() ) {
            Set<FactorValue> matchedFvs = factor.getFactorValues().stream()
                    .filter( fv -> fv.getCharacteristics().stream().anyMatch( s -> StatementUtils.hasSubject( s, characteristic ) ) )
                    .collect( Collectors.toSet() );
            if ( matchedFvs.isEmpty() ) {
                log.debug( characteristic + " matches no factor values in " + factor + ", ignoring..." );
                continue;
            } else if ( matchedFvs.size() > 1 ) {
                throw new IllegalStateException( characteristic + "matches more than one factor value in " + factor );
            }
            mappedFactors.put( characteristic, matchedFvs.iterator().next() );
        }
        return mappedFactors;
    }

    /**
     * Create a full mapping of cell types from a cell type assignment to factor values in a cell type factor.
     * <p>
     * In the case of a full mapping, every cell type is mapped to a (possibly empty) set of factor values.
     */
    public static Map<Characteristic, Set<FactorValue>> createFullMappingByFactorValueCharacteristics( CellLevelCharacteristics clc, ExperimentalFactor factor ) {
        Assert.isTrue( factor.getType() == FactorType.CATEGORICAL, "The factor must be categorical." );
        Map<Characteristic, Set<FactorValue>> mappedFactors = new HashMap<>();
        for ( Characteristic characteristic : clc.getCharacteristics() ) {
            mappedFactors.put( characteristic, factor.getFactorValues().stream()
                    .filter( fv -> fv.getCharacteristics().stream().anyMatch( s -> StatementUtils.hasSubject( s, characteristic ) ) )
                    .collect( Collectors.toSet() ) );
        }
        return mappedFactors;
    }

    /**
     * Infer the mapping of cell type assignments to factor values using a subset structure.
     * <p>
     * This method is resilient to changes in the {@link FactorValue} as it will look up the characteristics of the
     * subset via {@link ExpressionExperimentSubSet#getCharacteristics()}.
     */
    public static Map<Characteristic, FactorValue> createMappingBySubSetCharacteristics( CellLevelCharacteristics clc, ExperimentalFactor factor, Map<FactorValue, ExpressionExperimentSubSet> subsets ) {
        Assert.isTrue( factor.getType() == FactorType.CATEGORICAL, "The factor must be categorical." );
        Map<Characteristic, FactorValue> mappedFactors = new HashMap<>();
        for ( Characteristic characteristic : clc.getCharacteristics() ) {
            Set<Map.Entry<FactorValue, ExpressionExperimentSubSet>> matchedSubSets = subsets.entrySet().stream()
                    .filter( subset -> subset.getValue().getCharacteristics().stream().anyMatch( s -> CharacteristicUtils.equals( s.getValue(), s.getValueUri(), characteristic.getValue(), characteristic.getValueUri() ) ) )
                    .collect( Collectors.toSet() );
            if ( matchedSubSets.isEmpty() ) {
                log.debug( characteristic + " matches no subsets, ignoring..." );
                continue;
            } else if ( matchedSubSets.size() > 1 ) {
                throw new IllegalStateException( characteristic + " matches more than one subset." );
            }
            mappedFactors.put( characteristic, matchedSubSets.iterator().next().getKey() );
        }
        return mappedFactors;
    }

    /**
     * Create a mapping of cell type assignments to factor values from a file.
     */
    public static Map<Characteristic, FactorValue> readMappingFromFile( CellLevelCharacteristics clc, ExperimentalFactor factor, Path cellTypeMappingFile ) throws IOException {
        Assert.isTrue( factor.getType() == FactorType.CATEGORICAL, "The factor must be categorical." );
        Map<Long, Characteristic> cById = IdentifiableUtils.getIdMap( clc.getCharacteristics() );
        Map<Long, FactorValue> fvById = IdentifiableUtils.getIdMap( factor.getFactorValues() );
        Map<Characteristic, FactorValue> mappedCellTypeFactors = new HashMap<>();
        try ( CSVParser parser = CSVParser.parse( cellTypeMappingFile, StandardCharsets.UTF_8, CTA_MAPPING_FORMAT ) ) {
            for ( CSVRecord record : parser ) {
                Long key = Long.parseLong( StringUtils.strip( record.get( "cell_type_id" ) ) );
                Characteristic value = cById.get( key );
                if ( value == null ) {
                    throw new IllegalStateException( "There is no cell type with ID " + key + " in the cell type assignment." );
                }
                Long fvId = Long.parseLong( StringUtils.strip( record.get( "factor_value_id" ) ) );
                FactorValue fv = fvById.get( fvId );
                if ( fv == null ) {
                    throw new IllegalStateException( "There is no factor value with ID " + fvId + " in the cell type factor." );
                }
                if ( mappedCellTypeFactors.put( value, fv ) != null ) {
                    throw new IllegalStateException( "More than one factor value is associated to " + value + "." );
                }
            }
        }
        return mappedCellTypeFactors;
    }

    /**
     * Create a mapping of cell type assignments to factor values from a file.
     */
    public static void writeMapping( CellLevelCharacteristics cta, ExperimentalFactor factor, Map<Characteristic, FactorValue> cta2f, Writer dest ) throws IOException {
        try ( CSVPrinter printer = new CSVPrinter( dest, CTA_MAPPING_FORMAT ) ) {
            for ( Map.Entry<Characteristic, FactorValue> entry : cta2f.entrySet() ) {
                printer.printComment( "Cell type: " + formatCellType( entry.getKey() ) );
                printer.printComment( "Factor value: " + FactorValueUtils.getSummaryString( entry.getValue() ) );
                printer.printRecord( entry.getKey().getId(), entry.getValue().getId() );
            }
            HashSet<Characteristic> unmappedCellTypes = new HashSet<>( cta.getCharacteristics() );
            unmappedCellTypes.removeAll( cta2f.keySet() );
            for ( Characteristic cellType : unmappedCellTypes ) {
                printer.printComment( "Cell type: " + formatCellType( cellType ) );
                printer.printComment( cellType.getId() + "\t<unmapped>" );
            }
            HashSet<FactorValue> unmappedFactorValues = new HashSet<>( factor.getFactorValues() );
            unmappedFactorValues.removeAll( cta2f.values() );
            for ( FactorValue fv : unmappedFactorValues ) {
                printer.printComment( "Factor value: " + FactorValueUtils.getSummaryString( fv ) );
                printer.printComment( "<unmapped>\t" + fv.getId() );
            }
        }
    }

    public static String printMapping( Map<Characteristic, FactorValue> mappedCellTypeFactors ) {
        StringBuilder sb = new StringBuilder();
        try {
            printMapping( mappedCellTypeFactors, sb );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        return sb.toString();
    }

    public static void printMapping( Map<Characteristic, FactorValue> mappedCellTypeFactors, Appendable details ) throws IOException {
        int longestCellType = mappedCellTypeFactors.keySet().stream()
                .mapToInt( ct -> formatCellType( ct ).length() )
                .max()
                .orElse( 0 );
        for ( Map.Entry<Characteristic, FactorValue> entry : mappedCellTypeFactors.entrySet() ) {
            Characteristic k = entry.getKey();
            FactorValue v = entry.getValue();
            details.append( "\t" ).append( rightPad( formatCellType( k ), longestCellType ) ).append( " → " ).append( FactorValueUtils.getSummaryString( v ) ).append( "\n" );
        }
    }

    private static String formatCellType( Characteristic ct ) {
        if ( ct.getValueUri() != null ) {
            return "[" + ct.getValue() + "]" + " (" + ct.getValueUri() + ")";
        } else {
            return ct.getValue();
        }
    }
}
