package ubic.gemma.core.analysis.singleCell.aggregate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.audit.payload.SingleCellSubSetsCreatedPayload;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetReadService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.analysis.singleCell.CellLevelCharacteristicsMappingUtils.createMappingByFactorValueCharacteristics;
import static ubic.gemma.core.analysis.singleCell.CellLevelCharacteristicsMappingUtils.printMapping;
import static ubic.gemma.core.util.StringUtils.abbreviateWithSuffix;

@Service
@Slf4j
public class SingleCellExpressionExperimentSubSetServiceImpl implements SingleCellExpressionExperimentSubSetService {

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;

    /**
     * Read side is injected separately so the reuse lookup does not go through the heavier
     * {@link ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService} facade.
     */
    @Autowired
    private ExpressionExperimentSubSetReadService expressionExperimentSubSetReadService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private SingleCellExpressionExperimentSubSetAuditService subSetAuditService;


    @Transactional
    public List<ExpressionExperimentSubSet> createSubSetsByCellType( ExpressionExperiment ee, SingleCellExperimentSubSetsCreationConfig config ) {
        // the characteristics from the CTA have to be mapped with the statements from the factor values
        CellTypeAssignment cta = singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee )
                .orElseThrow( IllegalStateException::new );
        ExperimentalFactor cellTypeFactor = singleCellExpressionExperimentService.getCellTypeFactor( ee )
                .orElseThrow( () -> new IllegalStateException( ee + " does not have a cell type factor." ) );
        SingleCellDimension scd = singleCellExpressionExperimentService.getPreferredSingleCellDimensionWithoutCellIds( ee )
                .orElseThrow( () -> new IllegalStateException( ee + " does not have a preferred single-cell dimension." ) );
        Map<Characteristic, FactorValue> mappedCellTypeFactors = createMappingByFactorValueCharacteristics( cta, cellTypeFactor );
        return createSubSets( ee, scd, cta, cellTypeFactor, mappedCellTypeFactors, config );
    }

    @Override
    @Transactional
    public List<ExpressionExperimentSubSet> createSubSets( ExpressionExperiment ee, SingleCellDimension scd, CellLevelCharacteristics clc, ExperimentalFactor factor, Map<Characteristic, FactorValue> mappedCellTypeFactors,
            SingleCellExperimentSubSetsCreationConfig config ) {
        Set<FactorValue> unmappedFactorValues = new HashSet<>( factor.getFactorValues() );
        unmappedFactorValues.removeAll( mappedCellTypeFactors.values() );
        if ( !unmappedFactorValues.isEmpty() ) {
            if ( config.isIgnoreUnmatchedFactorValues() ) {
                log.warn( String.format( "Not all factor values in %s are mapped to cell types in %s, subsets for the following factor values will not be created:%n\t%s",
                        factor, clc, unmappedFactorValues.stream().map( FactorValue::toString ).collect( Collectors.joining( "\n\t" ) ) ) );
            } else {
                throw new IllegalStateException( String.format( "Not all factor values in %s are mapped to cell types in %s. Remove these factor values or set allowUnmappedFactorValues to true:%n\t%s",
                        factor, clc, unmappedFactorValues.stream().map( FactorValue::toString ).collect( Collectors.joining( "\n\t" ) ) ) );
            }
        }
        // only create pseudo-bulk assays for sources that actually contributed cells to the single-cell dimension
        Set<BioAssay> assaysInScd = new HashSet<>( scd.getBioAssays() );
        List<BioAssay> samplesToSubset = new ArrayList<>( assaysInScd.size() );
        List<BioAssay> samplesWithoutData = new ArrayList<>();
        for ( BioAssay sample : ee.getBioAssays() ) {
            if ( assaysInScd.contains( sample ) ) {
                samplesToSubset.add( sample );
            } else {
                samplesWithoutData.add( sample );
            }
        }
        if ( !samplesWithoutData.isEmpty() ) {
            log.warn( String.format( "%d sample(s) of %s are not present in %s and will be excluded from pseudo-bulk aggregation:%n\t%s",
                    samplesWithoutData.size(), ee, scd,
                    samplesWithoutData.stream().map( BioAssay::toString ).collect( Collectors.joining( "\n\t" ) ) ) );
        }
        // Index the subsets the experiment already has, keyed the way the factory names them, so a
        // second aggregation run reuses them instead of laying a complete second set beside the
        // first. Nothing here checked before, and production carries 47,143 subset rows under
        // 19,391 distinct names as a result. Where a name already has several copies, keep the
        // newest: measured on 2026-08-31, of the 6,491 duplicated (experiment, name) groups there
        // is none in which an older copy backs a live bio-assay dimension and the newest does not.
        Map<String, ExpressionExperimentSubSet> existingSubSetsByName = new HashMap<>();
        for ( ExpressionExperimentSubSet existing : expressionExperimentSubSetReadService.getSubSetsWithBioAssays( ee ) ) {
            existingSubSetsByName.merge( existing.getName(), existing, SingleCellExpressionExperimentSubSetServiceImpl::newest );
        }

        List<ExpressionExperimentSubSet> results = new ArrayList<>( clc.getCharacteristics().size() );
        List<ExpressionExperimentSubSet> reused = new ArrayList<>();
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
            ExpressionExperimentSubSet subset = ExpressionExperimentSubSet.Factory.newInstance( cellTypeName, ee );
            ExpressionExperimentSubSet existing = existingSubSetsByName.get( subset.getName() );
            if ( existing != null ) {
                // reuse the subset AND its pseudo-bulk assays: aggregation re-derives a dimension
                // from them with bioAssayDimensionService.findOrCreate(), which is the same shape
                // redoAggregate() already relies on
                assertReusable( existing, cellTypeName, samplesToSubset );
                reused.add( existing );
                results.add( existing );
                continue;
            }
            subset.setSourceExperiment( ee );
            subset.getCharacteristics().add( Characteristic.Factory.newInstance( characteristic ) );
            for ( BioAssay sample : samplesToSubset ) {
                subset.getBioAssays().add( createBioAssayForCellPopulation( sample, factorValue, characteristic, cellTypeName ) );
            }
            results.add( expressionExperimentSubSetService.create( subset ) );
        }
        String note = "Created " + ( results.size() - reused.size() ) + " and reused " + reused.size()
                + " aggregated single-cell subsets for " + factor;
        // Phase C bucket 2f: typed payload via the AuditedAspect. The audit row
        // is written by the @Audited annotation on
        // SingleCellExpressionExperimentSubSetAuditService#recordSubSetsCreated
        // — the co-bean hop is required because the Spring proxy can't
        // intercept self-invocations on this service (createSubSetsByCellType
        // calls createSubSets via this.).
        List<String> cellTypeLabels = new ArrayList<>( clc.getCharacteristics().size() );
        for ( Characteristic ct : clc.getCharacteristics() ) {
            cellTypeLabels.add( formatCellType( ct ) );
        }
        List<String> subsetLabels = results.stream().map( ExpressionExperimentSubSet::toString ).collect( Collectors.toList() );
        SingleCellSubSetsCreatedPayload payload = new SingleCellSubSetsCreatedPayload(
                cellTypeLabels,
                factor.toString(),
                printMapping( mappedCellTypeFactors ),
                subsetLabels );
        if ( log.isInfoEnabled() ) {
            log.info( note + "\nCell type assignment: " + String.join( ", ", cellTypeLabels )
                    + "\nCell type factor: " + factor
                    + "\nMapping of cell types to factor values:\n" + printMapping( mappedCellTypeFactors )
                    + "Subsets:\n\t" + String.join( "\n\t", subsetLabels ) );
        }
        subSetAuditService.recordSubSetsCreated( ee, note, payload );
        return results;
    }

    /**
     * Pick the later-created of two subsets that share a name, i.e. the one with the higher ID.
     */
    private static ExpressionExperimentSubSet newest( ExpressionExperimentSubSet a, ExpressionExperimentSubSet b ) {
        if ( a.getId() == null ) {
            return b;
        }
        if ( b.getId() == null ) {
            return a;
        }
        return b.getId() > a.getId() ? b : a;
    }

    /**
     * Refuse to reuse a subset whose pseudo-bulk assays do not correspond one-for-one to the samples
     * being aggregated.
     * <p>
     * Silently reusing a subset that covers a different set of samples would aggregate over the wrong
     * assays, which is worse than the duplicate row this reuse is here to prevent. The names are the
     * comparison because {@link #createBioAssayForCellPopulation} derives them deterministically from
     * the source sample name and the cell type.
     */
    private void assertReusable( ExpressionExperimentSubSet existing, String cellTypeName, List<BioAssay> samplesToSubset ) {
        Set<String> expected = new HashSet<>();
        for ( BioAssay sample : samplesToSubset ) {
            expected.add( cellPopulationAssayName( sample, cellTypeName ) );
        }
        Set<String> actual = existing.getBioAssays().stream()
                .map( BioAssay::getName )
                .collect( Collectors.toSet() );
        if ( !expected.equals( actual ) ) {
            Set<String> missing = new TreeSet<>( expected );
            missing.removeAll( actual );
            Set<String> unexpected = new TreeSet<>( actual );
            unexpected.removeAll( expected );
            throw new IllegalStateException( String.format(
                    "%s already has a subset named '%s', but its %d assay(s) do not correspond to the %d sample(s) being aggregated, so it cannot be reused.%nMissing: %s%nUnexpected: %s%nRe-aggregate the existing subsets instead, or remove them first.",
                    existing.getSourceExperiment(), existing.getName(), actual.size(), expected.size(),
                    missing.isEmpty() ? "(none)" : String.join( ", ", missing ),
                    unexpected.isEmpty() ? "(none)" : String.join( ", ", unexpected ) ) );
        }
    }

    private static String cellPopulationAssayName( BioAssay sample, String cellTypeName ) {
        return abbreviateWithSuffix( sample.getName(), " - " + cellTypeName, "…", BioAssay.MAX_NAME_LENGTH, true, StandardCharsets.UTF_8 );
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
        cellPopBa.setName( cellPopulationAssayName( sample, cellTypeName ) );
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
