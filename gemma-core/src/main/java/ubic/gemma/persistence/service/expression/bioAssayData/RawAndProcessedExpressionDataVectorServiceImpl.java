package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawOrProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
public class RawAndProcessedExpressionDataVectorServiceImpl extends AbstractDesignElementDataVectorService<RawOrProcessedExpressionDataVector> implements RawAndProcessedExpressionDataVectorService {

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
    public Collection<RawOrProcessedExpressionDataVector> findAndThaw( BioAssayDimension bioAssayDimension ) {
        Collection<RawOrProcessedExpressionDataVector> thawedVectors = new HashSet<>();
        thawedVectors.addAll( rawExpressionDataVectorService.findAndThaw( bioAssayDimension ) );
        thawedVectors.addAll( processedExpressionDataVectorService.findAndThaw( bioAssayDimension ) );
        return thawedVectors;
    }

    @Override
    public Collection<RawOrProcessedExpressionDataVector> findAndThaw( Collection<QuantitationType> quantitationTypes ) {
        Collection<RawOrProcessedExpressionDataVector> thawedVectors = new HashSet<>();
        thawedVectors.addAll( rawExpressionDataVectorService.findAndThaw( quantitationTypes ) );
        thawedVectors.addAll( processedExpressionDataVectorService.findAndThaw( quantitationTypes ) );
        return thawedVectors;
    }

    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public Collection<RawOrProcessedExpressionDataVector> thaw( Collection<RawOrProcessedExpressionDataVector> vectors ) {
        Collection<RawOrProcessedExpressionDataVector> thawedVectors = new HashSet<>( vectors.size() );
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
