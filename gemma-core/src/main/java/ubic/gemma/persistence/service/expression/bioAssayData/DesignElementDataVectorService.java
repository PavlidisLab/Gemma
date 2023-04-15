package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.BaseService;

import javax.annotation.CheckReturnValue;
import java.util.Collection;

@SuppressWarnings("unused")
        // Possible external use
interface DesignElementDataVectorService<T extends DesignElementDataVector> extends BaseService<T> {

    /**
     * Thaws the given vectors.
     *
     * @param designElementDataVectors the vectors to thaw.
     */
    @CheckReturnValue
    Collection<T> thaw( Collection<T> designElementDataVectors );

    /**
     * Find specific type (raw or processed, depending on the service) of vectors that meet the given criteria.
     *
     * @param bioAssayDimension the BA dimension
     * @return the found vectors of type {@link T}
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    Collection<T> find( BioAssayDimension bioAssayDimension );

    /**
     * Find specific type ({@link T}) of vectors that meet the given criteria.
     *
     * @param quantitationTypes the QTs
     * @return the found vectors of type {@link T}
     */
    @Secured({ "GROUP_ADMIN" })
    Collection<T> find( Collection<QuantitationType> quantitationTypes );

    /**
     * Find specific type ({@link T}) of vectors that meet the given criteria.
     *
     * @param quantitationType the QT
     * @return the found vectors of type {@link T}
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    Collection<T> find( QuantitationType quantitationType );

    @Override
    @Secured({ "GROUP_USER" })
    void update( T dedv );
}
