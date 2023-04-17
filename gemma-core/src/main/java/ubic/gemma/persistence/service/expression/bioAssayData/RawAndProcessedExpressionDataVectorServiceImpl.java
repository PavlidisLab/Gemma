package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
public class RawAndProcessedExpressionDataVectorServiceImpl extends AbstractDesignElementDataVectorService<DesignElementDataVector> implements RawAndProcessedExpressionDataVectorService {

    private final RawAndProcessedExpressionDataVectorDao mainDao;

    @Autowired
    private RawExpressionDataVectorService rawExpressionDataVectorService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

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

    @Override
    @Transactional(readOnly = true)
    public Collection<DesignElementDataVector> thaw( Collection<DesignElementDataVector> vectors ) {
        Collection<DesignElementDataVector> thawedVectors = new HashSet<>( vectors.size() );
        Collection<RawExpressionDataVector> rv = vectors.stream()
                .filter( v -> v instanceof RawExpressionDataVector )
                .map( v -> ( RawExpressionDataVector ) v )
                .collect( Collectors.toSet() );
        thawedVectors.addAll( rawExpressionDataVectorService.thaw( rv ) );
        Collection<ProcessedExpressionDataVector> pv = vectors.stream()
                .filter( v -> v instanceof ProcessedExpressionDataVector )
                .map( v -> ( ProcessedExpressionDataVector ) v )
                .collect( Collectors.toSet() );
        thawedVectors.addAll( processedExpressionDataVectorService.thaw( pv ) );
        return thawedVectors;
    }
}
