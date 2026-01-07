package ubic.gemma.model.expression.bioAssay;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialUtils;

import java.text.DateFormat;
import java.util.*;
import java.util.stream.Collectors;

@CommonsLog
public class BioAssayUtils {

    /**
     * This is only the date part of the ISO 8601 standard.
     */
    private static final DateFormat DATE_FORMAT = new StdDateFormat();

    /**
     * Create a mapping between assays and their source assays from a given {@link BioAssaySet}.
     * <p>
     * If an assay has more than one corresponding source assay or no source assay at all, it is ignored and logged as a
     * warning.
     */
    public static Map<BioAssay, BioAssay> createBioAssayToSourceBioAssayMap( BioAssaySet sourceAssaySet, Collection<BioAssay> bas ) {
        Map<BioAssay, BioAssay> assay2sourceAssayMap = new HashMap<>();
        for ( BioAssay ba : bas ) {
            if ( ba.getSampleUsed().getSourceBioMaterial() == null ) {
                log.warn( ba + " does not have a source assay in " + sourceAssaySet + "." );
                continue;
            }

            // collect all BMs in the hierarchy
            Set<BioMaterial> sourceBms = new HashSet<>();
            BioMaterialUtils.visitBioMaterials( ba.getSampleUsed(), sourceBms::add );

            // only retain assays that are in the source assay set
            Set<BioAssay> sourceAssays = sourceBms.stream()
                    .flatMap( bm -> bm.getBioAssaysUsedIn().stream() )
                    .filter( sourceAssaySet.getBioAssays()::contains )
                    .collect( Collectors.toSet() );

            if ( sourceAssays.size() == 1 ) {
                assay2sourceAssayMap.put( ba, sourceAssays.iterator().next() );
            } else if ( sourceAssays.isEmpty() ) {
                log.warn( ba + " does not have a source assay in " + sourceAssaySet + "." );
            } else {
                log.warn( ba + " has more than one source assay in " + sourceAssaySet + "." );
            }
        }
        return assay2sourceAssayMap;
    }

    /**
     * Create a mapping of biomaterial to characteristics for each category.
     * <p>
     * Unlike {@link BioMaterial}s, assays do not hold a collection of characteristics and instead have a few fields
     * that are converted to characteristics.
     *
     * @see BioMaterialUtils#createCharacteristicMap(Collection)
     * @see #getCharacteristics(BioAssay)
     */
    public static Map<Category, Map<BioAssay, Collection<Characteristic>>> createCharacteristicMap( Collection<BioAssay> assays ) {
        Map<Category, Map<BioAssay, Collection<Characteristic>>> map = new HashMap<>();
        for ( BioAssay assay : assays ) {
            for ( Characteristic characteristic : getCharacteristics( assay ) ) {
                map.computeIfAbsent( CharacteristicUtils.getCategory( characteristic ), k -> new HashMap<>() )
                        .computeIfAbsent( assay, k -> new HashSet<>( assays.size() ) )
                        .add( characteristic );
            }
        }
        return map;
    }

    /**
     * Extract various {@link BioAssay} metadata as a set of characteristics.
     */
    private static Set<Characteristic> getCharacteristics( BioAssay bioAssay ) {
        HashSet<Characteristic> result = new HashSet<>();
        if ( bioAssay.getProcessingDate() != null ) {
            result.add( Characteristic.Factory.newInstance( BioAssayCategories.PROCESSING_DATE, DATE_FORMAT.format( bioAssay.getProcessingDate() ), null ) );
        }
        result.add( Characteristic.Factory.newInstance( BioAssayCategories.IS_OUTLIER, String.valueOf( bioAssay.getIsOutlier() ), null ) );
        if ( bioAssay.getSequencePairedReads() != null ) {
            result.add( Characteristic.Factory.newInstance( BioAssayCategories.SEQUENCE_PAIRED_READS, String.valueOf( bioAssay.getSequencePairedReads() ), null ) );
        }
        if ( bioAssay.getSequenceReadLength() != null ) {
            result.add( Characteristic.Factory.newInstance( BioAssayCategories.SEQUENCE_READ_LENGTH, String.valueOf( bioAssay.getSequenceReadLength() ), null ) );
        }
        if ( bioAssay.getSequenceReadCount() != null ) {
            result.add( Characteristic.Factory.newInstance( BioAssayCategories.SEQUENCE_READ_COUNT, String.valueOf( bioAssay.getSequenceReadCount() ), null ) );
        }
        if ( bioAssay.getNumberOfCells() != null ) {
            result.add( Characteristic.Factory.newInstance( BioAssayCategories.NUMBER_OF_CELLS, String.valueOf( bioAssay.getNumberOfCells() ), null ) );
        }
        if ( bioAssay.getNumberOfDesignElements() != null ) {
            result.add( Characteristic.Factory.newInstance( BioAssayCategories.NUMBER_OF_DESIGN_ELEMENTS, String.valueOf( bioAssay.getNumberOfDesignElements() ), null ) );
        }
        if ( bioAssay.getNumberOfCellsByDesignElements() != null ) {
            result.add( Characteristic.Factory.newInstance( BioAssayCategories.NUMBER_OF_CELLS_BY_DESIGN_ELEMENTS, String.valueOf( bioAssay.getNumberOfCellsByDesignElements() ), null ) );
        }
        return result;
    }
}
