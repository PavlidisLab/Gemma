package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;

@Service
public class BioAssaySetServiceImpl implements BioAssaySetService {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;

    @Override
    public Class<? extends BioAssaySet> getElementClass() {
        return BioAssaySet.class;
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public BioAssaySet find( BioAssaySet entity ) {
        if ( entity instanceof ExpressionExperiment ) {
            return expressionExperimentService.find( ( ExpressionExperiment ) entity );
        } else if ( entity instanceof ExpressionExperimentSubSet ) {
            return expressionExperimentSubSetService.find( ( ExpressionExperimentSubSet ) entity );
        } else {
            throw new IllegalArgumentException( String.format( "Unsupported BioAssaySet subtype %s.", entity.getClass().getName() ) );
        }
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public BioAssaySet findOrFail( BioAssaySet entity ) {
        if ( entity instanceof ExpressionExperiment ) {
            return expressionExperimentService.findOrFail( ( ExpressionExperiment ) entity );
        } else if ( entity instanceof ExpressionExperimentSubSet ) {
            return expressionExperimentSubSetService.findOrFail( ( ExpressionExperimentSubSet ) entity );
        } else {
            throw new IllegalArgumentException( String.format( "Unsupported BioAssaySet subtype %s.", entity.getClass().getName() ) );
        }
    }

    @Override
    public Collection<BioAssaySet> load( Collection<Long> ids ) {
        throw new UnsupportedOperationException( "Cannot load BioAssaySet by ID, use a specific service instead." );
    }

    @Nullable
    @Override
    public BioAssaySet load( Long id ) {
        throw new UnsupportedOperationException( "Cannot load BioAssaySet by ID, use a specific service instead." );
    }

    @Nonnull
    @Override
    public BioAssaySet loadOrFail( Long id ) throws NullPointerException {
        throw new UnsupportedOperationException( "Cannot load BioAssaySet by ID, use a specific service instead." );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssaySet> loadAll() {
        ArrayList<BioAssaySet> results = new ArrayList<>();
        results.addAll( expressionExperimentService.loadAll() );
        results.addAll( expressionExperimentSubSetService.loadAll() );
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return expressionExperimentService.countAll() + expressionExperimentSubSetService.countAll();
    }

    @Override
    @Transactional
    public void remove( Collection<? extends BioAssaySet> entities ) {
        for ( BioAssaySet bas : entities ) {
            remove( bas );
        }
    }

    @Override
    @Transactional
    public void remove( BioAssaySet entity ) {
        if ( entity instanceof ExpressionExperiment ) {
            expressionExperimentService.remove( ( ExpressionExperiment ) entity );
        } else if ( entity instanceof ExpressionExperimentSubSet ) {
            expressionExperimentSubSetService.remove( ( ExpressionExperimentSubSet ) entity );
        } else {
            throw new IllegalArgumentException( String.format( "Unsupported BioAssaySet subtype %s.", entity.getClass().getName() ) );
        }
    }
}
