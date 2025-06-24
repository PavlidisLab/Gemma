package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.persistence.service.AbstractService;

import java.util.Collection;

public abstract class AbstractBulkExpressionDataVectorService<T extends BulkExpressionDataVector> extends AbstractService<T>
        implements BulkExpressionDataVectorService<T> {

    private final DesignElementDataVectorDao<T> designElementDataVectorDao;

    protected AbstractBulkExpressionDataVectorService( DesignElementDataVectorDao<T> mainDao ) {
        super( mainDao );
        this.designElementDataVectorDao = mainDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<T> find( BioAssayDimension bioAssayDimension ) {
        return this.designElementDataVectorDao.find( bioAssayDimension );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<T> findAndThaw( BioAssayDimension bioAssayDimension ) {
        Collection<T> vectors = this.designElementDataVectorDao.find( bioAssayDimension );
        this.designElementDataVectorDao.thaw( vectors );
        return vectors;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<T> find( Collection<QuantitationType> quantitationTypes ) {
        return this.designElementDataVectorDao.find( quantitationTypes );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<T> findAndThaw( Collection<QuantitationType> quantitationTypes ) {
        Collection<T> vectors = this.designElementDataVectorDao.find( quantitationTypes );
        this.designElementDataVectorDao.thaw( vectors );
        return vectors;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<T> find( QuantitationType quantitationType ) {
        return this.designElementDataVectorDao.find( quantitationType );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<T> findAndThaw( QuantitationType qt ) {
        Collection<T> vectors = designElementDataVectorDao.find( qt );
        designElementDataVectorDao.thaw( vectors );
        return vectors;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<T> thaw( Collection<T> vectors ) {
        vectors = ensureInSession( vectors );
        designElementDataVectorDao.thaw( vectors );
        return vectors;
    }
}
