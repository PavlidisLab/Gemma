package ubic.gemma.persistence.service.expression.experiment;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.eventType.SingleCellSubSetsCreatedEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@CommonsLog
public class SingleCellExpressionExperimentSplitServiceImpl implements SingleCellExpressionExperimentSplitService {

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Override
    @Transactional
    public List<ExpressionExperimentSubSet> splitByCellType( ExpressionExperiment ee, CellTypeAssignment cta, boolean allowUnmappedFactorValues ) {
        // the characteristics from the CTA have to be mapped with the statements from the factor values
        ExperimentalFactor cellTypeFactor = singleCellExpressionExperimentService.getCellTypeFactor( ee )
                .orElseThrow( () -> new IllegalStateException( ee + " does not have a cell type factor." ) );
        Map<Characteristic, FactorValue> mappedCellTypeFactors = mapCellTypeAssignmentToCellTypeFactor( cta, cellTypeFactor );
        List<ExpressionExperimentSubSet> results = new ArrayList<>( cta.getCellTypes().size() );
        // create sample by cell type populations
        for ( Characteristic cellType : cta.getCellTypes() ) {
            FactorValue factorValue = mappedCellTypeFactors.get( cellType );
            if ( factorValue == null ) {
                if ( allowUnmappedFactorValues ) {
                    log.warn( "No factor value found for " + cellType + " in " + cellTypeFactor + ", no subset will be created." );
                    continue;
                } else {
                    throw new IllegalStateException( "No factor value found for " + cellType + " in " + cellTypeFactor + "." );
                }
            }
            String cellTypeName = cellType.getValue();
            ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
            subset.setName( ee.getName() + " - " + cellTypeName );
            subset.setSourceExperiment( ee );
            subset.getCharacteristics().add( Characteristic.Factory.newInstance( cellType ) );
            for ( BioAssay sample : ee.getBioAssays() ) {
                subset.getBioAssays().add( createBioAssayForCellPopulation( sample, factorValue, cellType, cellTypeName ) );
            }
            results.add( expressionExperimentSubSetService.create( subset ) );
        }
        String note = "Created " + results.size() + " aggregated single-cell subsets for " + cellTypeFactor;
        StringBuilder details = new StringBuilder();
        details.append( "Cell type assignment: " );
        boolean first = true;
        for ( Characteristic ct : cta.getCellTypes() ) {
            if ( !first ) {
                details.append( ", " );
            }
            first = false;
            details.append( formatCellType( ct ) );
        }
        details.append( "\n" );
        details.append( "Cell type factor: " ).append( cellTypeFactor ).append( "\n" );
        details.append( "Mapping of cell types to factor values:\n" );
        int longestCellType = cta.getCellTypes().stream()
                .mapToInt( ct -> formatCellType( ct ).length() )
                .max()
                .orElse( 0 );
        mappedCellTypeFactors.forEach( ( k, v ) -> details.append( "\t" ).append( StringUtils.rightPad( formatCellType( k ), longestCellType ) ).append( " → " ).append( v ).append( "\n" ) );
        details.append( "Subsets:" );
        for ( ExpressionExperimentSubSet subset : results ) {
            details.append( "\n" ).append( "\t" ).append( subset );
        }
        log.info( note + "\n" + details );
        auditTrailService.addUpdateEvent( ee, SingleCellSubSetsCreatedEvent.class, note, details.toString() );
        return results;
    }

    /**
     * Map the cell types from a cell type assignment to factor values in a cell type factor.
     * <p>
     * There is a possibility that no factor value is found for a given cell type, in which case it is ignored.
     * <p>
     * TODO: this should be private, but we reuse the same logic for aggregating in {@link SingleCellExpressionExperimentAggregatorServiceImpl}.
     * @throws IllegalStateException if there is more than one factor value mapping a given cell type
     */
    static Map<Characteristic, FactorValue> mapCellTypeAssignmentToCellTypeFactor( CellTypeAssignment cta, ExperimentalFactor cellTypeFactor ) {
        Map<Characteristic, FactorValue> mappedCellTypeFactors = new HashMap<>();
        for ( Characteristic cellType : cta.getCellTypes() ) {
            Set<FactorValue> matchedFvs = cellTypeFactor.getFactorValues().stream()
                    .filter( fv -> fv.getCharacteristics().stream().anyMatch( s -> StatementUtils.hasSubject( s, cellType ) ) )
                    .collect( Collectors.toSet() );
            if ( matchedFvs.isEmpty() ) {
                log.debug( cellType + " matches no factor values in " + cellTypeFactor + ", ignoring..." );
                continue;
            } else if ( matchedFvs.size() > 1 ) {
                throw new IllegalStateException( cellType + "matches more than one factor values in " + cellTypeFactor );
            }
            mappedCellTypeFactors.put( cellType, matchedFvs.iterator().next() );
        }
        return mappedCellTypeFactors;
    }

    private String formatCellType( Characteristic ct ) {
        if ( ct.getValueUri() != null ) {
            return "[" + ct.getValue() + "]" + " (" + ct.getValueUri() + ")";
        } else {
            return ct.getValue();
        }
    }

    private BioAssay createBioAssayForCellPopulation( BioAssay sample, FactorValue cellTypeFactorValue, Characteristic cellType, String cellTypeName ) {
        BioAssay cellPopBa = new BioAssay();
        cellPopBa.setName( sample.getName() + " - " + cellTypeName );
        cellPopBa.setArrayDesignUsed( sample.getArrayDesignUsed() );
        BioMaterial cellPopBm = createBioMaterialForCellPopulation( sample.getSampleUsed(), cellTypeFactorValue, cellType, cellTypeName );
        cellPopBa.setSampleUsed( cellPopBm );
        cellPopBm.setBioAssaysUsedIn( Collections.singleton( cellPopBa ) );
        // FIXME: an ExpressionExperimentSubSet does not properly "own" its BAs because it's typically meant to
        //        subset existing BAs from an EE, thus we have to create the BAs one-by-one instead of relying
        //        on cascading behavior
        return bioAssayService.create( cellPopBa );
    }

    private BioMaterial createBioMaterialForCellPopulation( BioMaterial sourceBioMaterial, FactorValue cellTypeFactor, Characteristic cellType, String cellTypeName ) {
        BioMaterial bm = new BioMaterial();
        bm.setName( sourceBioMaterial.getName() + " - " + cellTypeName );
        bm.setSourceTaxon( sourceBioMaterial.getSourceTaxon() );
        bm.setSourceBioMaterial( sourceBioMaterial );
        bm.getCharacteristics().add( Characteristic.Factory.newInstance( cellType ) );
        bm.getFactorValues().add( cellTypeFactor );
        return bioMaterialService.create( bm );
    }
}
