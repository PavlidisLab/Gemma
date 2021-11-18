package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.AbstractService;

import java.util.Collection;

public abstract class DesignElementDataVectorServiceImpl<T extends DesignElementDataVector> extends AbstractService<T>
        implements DesignElementDataVectorService<T> {

    private final DesignElementDataVectorDao<T> designElementDataVectorDao;

    protected DesignElementDataVectorServiceImpl( DesignElementDataVectorDao<T> mainDao ) {
        super( mainDao );
        this.designElementDataVectorDao = mainDao;
    }

    @Override
    @Transactional(readOnly = true)
    public void thawRawAndProcessed( Collection<DesignElementDataVector> vectors ) {
        this.designElementDataVectorDao.thawRawAndProcessed( vectors );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DesignElementDataVector> findRawAndProcessed( BioAssayDimension dim ) {
        return this.designElementDataVectorDao.findRawAndProcessed( dim );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DesignElementDataVector> findRawAndProcessed( QuantitationType qt ) {
        return this.designElementDataVectorDao.findRawAndProcessed( qt );
    }

    @Override
    @Transactional
    public void removeDataForCompositeSequence( CompositeSequence compositeSequence ) {
        this.designElementDataVectorDao.removeDataForCompositeSequence( compositeSequence );
    }

    @Override
    @Transactional
    public void removeDataForQuantitationType( QuantitationType quantitationType ) {
        this.designElementDataVectorDao.removeDataForQuantitationType( quantitationType );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<T> find( ArrayDesign arrayDesign, QuantitationType quantitationType ) {
        return this.designElementDataVectorDao.find( arrayDesign, quantitationType );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<T> find( BioAssayDimension bioAssayDimension ) {
        return this.designElementDataVectorDao.find( bioAssayDimension );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<T> find( Collection<QuantitationType> quantitationTypes ) {
        return this.designElementDataVectorDao.find( quantitationTypes );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<T> find( QuantitationType quantitationType ) {
        return this.designElementDataVectorDao.find( quantitationType );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<T> find( Collection<CompositeSequence> designElements, QuantitationType quantitationType ) {
        return this.designElementDataVectorDao.find( designElements, quantitationType );
    }

}
