package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.BaseService;

import java.util.Collection;

@SuppressWarnings("unused")
        // Possible external use
interface DesignElementDataVectorService<T extends DesignElementDataVector> extends BaseService<T> {

    /**
     * Thaws all data vectors in the given collection
     *
     * @param vectors the vectors to be thawed
     */
    @Secured({ "GROUP_ADMIN" })
    void thawRawAndProcessed( Collection<DesignElementDataVector> vectors );

    /**
     * Finds all vectors for the given BA Dimension
     *
     * @param dim the BA dimension to limit the vector search to
     * @return the found data vectors
     */
    @Secured({ "GROUP_ADMIN" })
    Collection<DesignElementDataVector> findRawAndProcessed( BioAssayDimension dim );

    /**
     * Finds all vectors for the given quantitation type
     *
     * @param qt the quantitation type to limit the vector search to
     * @return the found data vectors
     */
    @Secured({ "GROUP_ADMIN" })
    Collection<DesignElementDataVector> findRawAndProcessed( QuantitationType qt );

    /**
     * Removes specific type ({@link T}) of vectors for the given CS.
     *
     * @param compositeSequence the sequence to remove the data for.
     */
    @Secured({ "GROUP_ADMIN" })
    void removeDataForCompositeSequence( CompositeSequence compositeSequence );

    /**
     * Removes specific type ({@link T}) of vectors for the given QT.
     *
     * @param quantitationType the QT to remove the data for.
     */
    @Secured({ "GROUP_ADMIN" })
    void removeDataForQuantitationType( QuantitationType quantitationType );

    /**
     * Find specific type ({@link T}) of vectors that meet the given criteria.
     *
     * @param arrayDesign      the AD
     * @param quantitationType the QT
     * @return the found vectors of type {@link T}.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    Collection<T> find( ArrayDesign arrayDesign, QuantitationType quantitationType );

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

    /**
     * Find specific type ({@link T}) of vectors that meet the given criteria.
     *
     * @param designElements   design elements
     * @param quantitationType the QT
     * @return the found vectors of type {@link T}
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    Collection<T> find( Collection<CompositeSequence> designElements, QuantitationType quantitationType );

    @Override
    @Secured({ "GROUP_USER" })
    void update( T dedv );

}
