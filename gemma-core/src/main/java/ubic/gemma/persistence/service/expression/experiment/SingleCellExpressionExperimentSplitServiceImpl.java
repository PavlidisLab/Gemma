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
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static ubic.gemma.persistence.service.expression.experiment.SingleCellUtils.mapCellTypeAssignmentToCellTypeFactor;

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
    public List<ExpressionExperimentSubSet> splitByCellType( ExpressionExperiment ee ) {
        CellTypeAssignment cta = singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee )
                .orElseThrow( () -> new IllegalStateException( ee + " does not have a preferred cell type assignment." ) );
        return splitByCellType( ee, cta );
    }

    @Override
    @Transactional
    public List<ExpressionExperimentSubSet> splitByCellType( ExpressionExperiment ee, CellTypeAssignment cta ) {
        // the characteristics from the CTA have to be mapped with the statements from the factor values
        ExperimentalFactor cellTypeFactor = singleCellExpressionExperimentService.getCellTypeFactor( ee )
                .orElseThrow( () -> new IllegalStateException( ee + " does not have a cell type factor." ) );
        return splitByCellType( ee, cta, cellTypeFactor );
    }

    private List<ExpressionExperimentSubSet> splitByCellType( ExpressionExperiment ee, CellTypeAssignment cta, ExperimentalFactor cellTypeFactor ) {
        Map<Characteristic, FactorValue> mappedCellTypeFactors = mapCellTypeAssignmentToCellTypeFactor( cta, cellTypeFactor );
        List<ExpressionExperimentSubSet> results = new ArrayList<>( cta.getCellTypes().size() );
        // create sample by cell type populations
        for ( Characteristic cellType : cta.getCellTypes() ) {
            String cellTypeName = cellType.getValue();
            ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
            subset.setName( ee.getName() + " - " + cellTypeName );
            subset.setSourceExperiment( ee );
            subset.getCharacteristics().add( Characteristic.Factory.newInstance( cellType ) );
            for ( BioAssay sample : ee.getBioAssays() ) {
                subset.getBioAssays().add( createBioAssayForCellPopulation( sample, mappedCellTypeFactors, cellType, cellTypeName ) );
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

    private String formatCellType( Characteristic ct ) {
        if ( ct.getValueUri() != null ) {
            return "[" + ct.getValue() + "]" + " (" + ct.getValueUri() + ")";
        } else {
            return ct.getValue();
        }
    }

    private BioAssay createBioAssayForCellPopulation( BioAssay sample, Map<Characteristic, FactorValue> mappedCellTypeFactors, Characteristic cellType, String cellTypeName ) {
        BioAssay cellPopBa = new BioAssay();
        cellPopBa.setName( sample.getName() + " - " + cellTypeName );
        cellPopBa.setArrayDesignUsed( sample.getArrayDesignUsed() );
        BioMaterial cellPopBm = createBioMaterialForCellPopulation( sample.getSampleUsed(), mappedCellTypeFactors.get( cellType ), cellType, cellTypeName );
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
