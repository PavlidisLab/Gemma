package ubic.gemma.core.analysis.singleCell.aggregate;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.eventType.SingleCellSubSetsCreatedEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.analysis.singleCell.aggregate.CellLevelCharacteristicsMappingUtils.createMappingByFactorValueCharacteristics;
import static ubic.gemma.core.analysis.singleCell.aggregate.CellLevelCharacteristicsMappingUtils.printMapping;
import static ubic.gemma.core.util.StringUtils.abbreviateWithSuffix;

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


    @Transactional
    public List<ExpressionExperimentSubSet> splitByCellType( ExpressionExperiment ee, SplitConfig config ) {
        // the characteristics from the CTA have to be mapped with the statements from the factor values
        CellTypeAssignment cta = singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee )
                .orElseThrow( IllegalStateException::new );
        ExperimentalFactor cellTypeFactor = singleCellExpressionExperimentService.getCellTypeFactor( ee )
                .orElseThrow( () -> new IllegalStateException( ee + " does not have a cell type factor." ) );
        Map<Characteristic, FactorValue> mappedCellTypeFactors = createMappingByFactorValueCharacteristics( cta, cellTypeFactor );
        return split( ee, cta, cellTypeFactor, mappedCellTypeFactors, config );
    }

    @Override
    @Transactional
    public List<ExpressionExperimentSubSet> split( ExpressionExperiment ee, CellLevelCharacteristics clc, ExperimentalFactor factor, Map<Characteristic, FactorValue> mappedCellTypeFactors,
            SplitConfig config ) {
        Set<FactorValue> unmappedFactorValues = new HashSet<>( factor.getFactorValues() );
        unmappedFactorValues.removeAll( mappedCellTypeFactors.values() );
        if ( !unmappedFactorValues.isEmpty() ) {
            if ( config.isIgnoreUnmatchedFactorValues() ) {
                log.warn( String.format( "Not all factor values in %s are mapped to cell types in %s, subsets for the following factor values will not be created:\n\t%s",
                        factor, clc, unmappedFactorValues.stream().map( FactorValue::toString ).collect( Collectors.joining( "\n\t" ) ) ) );
            } else {
                throw new IllegalStateException( String.format( "Not all factor values in %s are mapped to cell types in %s. Remove these factor values or set allowUnmappedFactorValues to true:\n\t%s",
                        factor, clc, unmappedFactorValues.stream().map( FactorValue::toString ).collect( Collectors.joining( "\n\t" ) ) ) );
            }
        }
        List<ExpressionExperimentSubSet> results = new ArrayList<>( clc.getCharacteristics().size() );
        // create sample by cell type populations
        for ( Characteristic characteristic : clc.getCharacteristics() ) {
            FactorValue factorValue = mappedCellTypeFactors.get( characteristic );
            if ( factorValue == null ) {
                if ( config.isIgnoreUnmatchedCharacteristics() ) {
                    log.warn( "No factor value found for " + characteristic + " in " + factor + ", no subset will be created." );
                    continue;
                } else {
                    throw new IllegalStateException( "No factor value found for " + characteristic + " in " + factor + "." );
                }
            }
            String cellTypeName = characteristic.getValue();
            ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
            subset.setName( abbreviateWithSuffix( ee.getName(), " - " + cellTypeName, "…", ExpressionExperiment.MAX_NAME_LENGTH, true, StandardCharsets.UTF_8 ) );
            subset.setSourceExperiment( ee );
            subset.getCharacteristics().add( Characteristic.Factory.newInstance( characteristic ) );
            for ( BioAssay sample : ee.getBioAssays() ) {
                subset.getBioAssays().add( createBioAssayForCellPopulation( sample, factorValue, characteristic, cellTypeName ) );
            }
            results.add( expressionExperimentSubSetService.create( subset ) );
        }
        String note = "Created " + results.size() + " aggregated single-cell subsets for " + factor;
        StringBuilder details = new StringBuilder();
        details.append( "Cell type assignment: " );
        boolean first = true;
        for ( Characteristic ct : clc.getCharacteristics() ) {
            if ( !first ) {
                details.append( ", " );
            }
            first = false;
            details.append( formatCellType( ct ) );
        }
        details.append( "\n" );
        details.append( "Cell type factor: " ).append( factor ).append( "\n" );
        details.append( "Mapping of cell types to factor values:\n" );
        details.append( printMapping( mappedCellTypeFactors ) );
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

    private BioAssay createBioAssayForCellPopulation( BioAssay sample, FactorValue cellTypeFactorValue, Characteristic cellType, String cellTypeName ) {
        BioAssay cellPopBa = new BioAssay();
        cellPopBa.setName( abbreviateWithSuffix( sample.getName(), " - " + cellTypeName, "…", BioAssay.MAX_NAME_LENGTH, true, StandardCharsets.UTF_8 ) );
        cellPopBa.setArrayDesignUsed( sample.getArrayDesignUsed() );
        // we can't fill that yet, because we don't deal with expression data, but the rest of the sequencing
        // information can be copied
        // is is filled afterwards when we aggregate in SingleCellExpressionExperimentAggregatorServiceImpl#updateSequenceReadCounts(BioAssayDimension, double[])
        // cellPopBa.setSequenceReadCount( sample.getSequenceReadCount() );
        cellPopBa.setSequenceReadLength( sample.getSequenceReadLength() );
        cellPopBa.setSequencePairedReads( sample.getSequencePairedReads() );
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
        bm.setName( abbreviateWithSuffix( sourceBioMaterial.getName(), " - " + cellTypeName, "…", BioMaterial.MAX_NAME_LENGTH, true, StandardCharsets.UTF_8 ) );
        bm.setSourceTaxon( sourceBioMaterial.getSourceTaxon() );
        bm.setSourceBioMaterial( sourceBioMaterial );
        bm.getCharacteristics().add( Characteristic.Factory.newInstance( cellType ) );
        bm.getFactorValues().add( cellTypeFactor );
        return bioMaterialService.create( bm );
    }
}
