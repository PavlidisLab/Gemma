package ubic.gemma.cli.util;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.Map;

/**
 * Locate various entities using identifiers supplied by the CLI.
 * @author poirigui
 */
public interface EntityLocator {

    Taxon locateTaxon( String identifier );

    ArrayDesign locateArrayDesign( String identifier );

    ExpressionExperiment locateExpressionExperiment( String identifier, boolean useReferencesIfPossible );

    Protocol locateProtocol( String protocolName );

    <T extends DataVector> QuantitationType locateQuantitationType( ExpressionExperiment ee, String qt, Class<? extends T> vectorType );

    <T extends DataVector> Map.Entry<Class<? extends T>, QuantitationType> locateQuantitationType( ExpressionExperiment ee, String qt, Collection<Class<? extends T>> vectorType );

    CellTypeAssignment locateCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, String cta );

    CellLevelCharacteristics locateCellLevelCharacteristics( ExpressionExperiment expressionExperiment, QuantitationType qt, String clcIdentifier );

    ExperimentalFactor locateExperimentalFactor( ExpressionExperiment expressionExperiment, String ctfName );

    /**
     * Locate an assay by its identifier.
     *
     * @param ee             dataset to lookup
     * @param assayId        assay identifier to lookup
     * @param includeSubSets whether to include assays that belong to subsets of the experiment. This is only relevant
     *                       for {@link ExpressionExperimentSubSet} that "own" their assays instead of sharing them with
     *                       the source experiment.
     */
    BioAssay locateBioAssay( ExpressionExperiment ee, String assayId, boolean includeSubSets );

    /**
     * Locate an assay by its identifier in a particular set of vectors.
     *
     * @param ee               dataset to lookup
     * @param quantitationType quantitation type for the vectors
     * @param sampleId         sample identifier to lookup
     */
    BioAssay locateBioAssay( ExpressionExperiment ee, QuantitationType quantitationType, String sampleId );

    /**
     *
     * @param ee             dataset to lookup
     * @param sampleId       sample identifier to lookup
     * @param includeSubSets whether to include samples associated to assays that belong to subsets of the experiment.
     *                       This is only relevant for {@link ExpressionExperimentSubSet}
     *                       that "own" their assays instead of sharing them with the source experiment.
     */
    BioMaterial locateSample( ExpressionExperiment ee, String sampleId, boolean includeSubSets );

    DifferentialExpressionAnalysis locateDiffExAnalysis( ExpressionExperiment ee, String analysisIdentifier );
}
