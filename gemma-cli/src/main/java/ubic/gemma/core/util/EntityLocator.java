package ubic.gemma.core.util;

import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.DataVector;
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

    QuantitationType locateQuantitationType( ExpressionExperiment ee, String qt, Class<? extends DataVector> vectorType );

    QuantitationType locateQuantitationType( ExpressionExperiment ee, String qt, Collection<Class<? extends DataVector>> vectorType );

    CellTypeAssignment locateCellTypeAssignment( ExpressionExperiment expressionExperiment, QuantitationType qt, String cta );
}
