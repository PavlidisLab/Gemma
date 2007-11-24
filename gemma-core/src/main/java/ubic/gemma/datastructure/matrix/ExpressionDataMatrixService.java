/**
 * 
 */
package ubic.gemma.datastructure.matrix;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * Methods for retrieving data matrices using the database.
 * 
 * @spring.bean id="expressionDataMatrixService"
 * @spring.property name="vectorService" ref="designElementDataVectorService"
 * @spring.property name="eeService" ref="expressionExperimentService"
 * @spring.property name="quantitationTypeService" ref="quantitationTypeService"
 * @author Paul
 * @version $Id$
 */
public class ExpressionDataMatrixService {

    private static Log log = LogFactory.getLog( ExpressionDataMatrixService.class.getName() );

    QuantitationTypeService quantitationTypeService;
    ExpressionExperimentService eeService;
    DesignElementDataVectorService vectorService;

    /**
     * Given a (possibly un-thawed) ee, return the preferered data matrix.
     * 
     * @param ee
     * @param maskMissing This will only be done if the experiment uses two-color technology
     * @return
     */
    @SuppressWarnings("unchecked")
    public ExpressionDataDoubleMatrix getPreferredDataMatrix( ExpressionExperiment ee, boolean maskMissing ) {
        eeService.thawLite( ee );

        Collection<QuantitationType> qts = ExpressionDataMatrixBuilder.getUsefulQuantitationTypes( ee );

        Collection<ArrayDesign> adsUsed = eeService.getArrayDesignsUsed( ee );
        boolean isTwoColor = !adsUsed.iterator().next().getTechnologyType().equals( TechnologyType.ONECOLOR )
                && maskMissing;
        Collection<QuantitationType> qtc = new HashSet<QuantitationType>();
        for ( QuantitationType quantitationType : qts ) {
            if ( quantitationType.getIsPreferred() ) {
                qtc.add( quantitationType );
            } else if ( isTwoColor && quantitationType.getType().equals( StandardQuantitationType.PRESENTABSENT ) ) {
                qtc.add( quantitationType );
            }
        }

        if ( qts.size() == 0 ) throw new IllegalArgumentException( "No usable quantitation types in " + ee );

        log.info( "Loading vectors..." );

        Collection<DesignElementDataVector> dataVectors = eeService.getDesignElementDataVectors( ee, qtc );
        vectorService.thaw( dataVectors );

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( dataVectors );

        ExpressionDataDoubleMatrix preferredData = builder.getPreferredData();
        if ( maskMissing && isTwoColor ) {
            for ( ArrayDesign ad : adsUsed ) {
                builder.maskMissingValues( preferredData, ad );
            }
        }
        return preferredData;
    }

    public void setQuantitationTypeService( QuantitationTypeService quantitationTypeService ) {
        this.quantitationTypeService = quantitationTypeService;
    }

    public void setEeService( ExpressionExperimentService eeService ) {
        this.eeService = eeService;
    }

    public void setVectorService( DesignElementDataVectorService vectorService ) {
        this.vectorService = vectorService;
    }
}
