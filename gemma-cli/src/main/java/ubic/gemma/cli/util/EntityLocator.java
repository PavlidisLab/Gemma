package ubic.gemma.cli.util;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;

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

    <T extends DataVector> QuantitationType locateQuantitationType( ExpressionExperiment ee, String qt, Collection<Class<? extends T>> vectorType );

    CellTypeAssignment locateCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, String cta );

    CellLevelCharacteristics locateCellLevelCharacteristics( ExpressionExperiment expressionExperiment, QuantitationType qt, String clcIdentifier );

    ExperimentalFactor locateExperimentalFactor( ExpressionExperiment expressionExperiment, String ctfName );

    BioAssay locateBioAssay( ExpressionExperiment ee, String sampleId );

    BioAssay locateBioAssay( ExpressionExperiment ee, QuantitationType quantitationType, String sampleId );

    DifferentialExpressionAnalysis locateDiffExAnalysis( ExpressionExperiment ee, String analysisIdentifier );
}
