package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

@Service
public class RawAndProcessedExpressionDataVectorServiceImpl extends AbstractDesignElementDataVectorService<DesignElementDataVector> implements RawAndProcessedExpressionDataVectorService {

    private final RawAndProcessedExpressionDataVectorDao mainDao;

    @Autowired
    public RawAndProcessedExpressionDataVectorServiceImpl( RawAndProcessedExpressionDataVectorDao mainDao ) {
        super( mainDao );
        this.mainDao = mainDao;
    }

    @Override
    @Transactional
    public int removeByCompositeSequence( CompositeSequence cs ) {
        return mainDao.removeByCompositeSequence( cs );
    }
}
