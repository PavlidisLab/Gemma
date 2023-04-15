package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;

@Service
public class RawAndProcessedExpressionDataVectorServiceImpl extends AbstractDesignElementDataVectorService<DesignElementDataVector> implements RawAndProcessedExpressionDataVectorService {

    @Autowired
    public RawAndProcessedExpressionDataVectorServiceImpl( RawAndProcessedExpressionDataVectorDao mainDao ) {
        super( mainDao );
    }
}
