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
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    @Transactional(readOnly = true)
    public Collection<BioAssaySet> load( Collection<Long> ids ) {
        Collection<BioAssaySet> result = new ArrayList<>();
        result.addAll( expressionExperimentService.load( ids ) );
        result.addAll( expressionExperimentSubSetService.load( ids ) );
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssaySet> loadOrFail( Collection<Long> ids ) throws NullPointerException {
        return loadOrFail( ids, NullPointerException::new );
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> Collection<BioAssaySet> loadOrFail( Collection<Long> ids, Function<String, T> exceptionSupplier ) throws T {
        ids = new HashSet<>( ids );
        Collection<BioAssaySet> result = new ArrayList<>();
        result.addAll( expressionExperimentService.load( ids ) );
        result.addAll( expressionExperimentSubSetService.load( ids ) );
        if ( result.size() < ids.size() ) {
            for ( BioAssaySet o : result ) {
                ids.remove( o.getId() );
            }
            throw exceptionSupplier.apply( String.format( "No BioAssaySet with IDs %s found.",
                    ids.stream().sorted().map( String::valueOf ).collect( Collectors.joining( ", " ) ) ) );
        }
        return result;
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    public BioAssaySet load( Long id ) {
        BioAssaySet bas;
        bas = expressionExperimentService.load( id );
        if ( bas != null ) {
            return bas;
        }
        return expressionExperimentSubSetService.load( id );
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public BioAssaySet loadOrFail( Long id ) throws NullPointerException {
        BioAssaySet bas;
        bas = expressionExperimentService.load( id );
        if ( bas != null ) {
            return bas;
        }
        return expressionExperimentSubSetService.loadOrFail( id );
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> BioAssaySet loadOrFail( Long id, Supplier<T> exceptionSupplier ) throws T {
        BioAssaySet bas;
        bas = expressionExperimentService.load( id );
        if ( bas != null ) {
            return bas;
        }
        return expressionExperimentSubSetService.loadOrFail( id, exceptionSupplier );
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> BioAssaySet loadOrFail( Long id, Function<String, T> exceptionSupplier ) throws T {
        BioAssaySet bas;
        bas = expressionExperimentService.load( id );
        if ( bas != null ) {
            return bas;
        }
        return expressionExperimentSubSetService.loadOrFail( id, exceptionSupplier );
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> BioAssaySet loadOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T {
        BioAssaySet bas;
        bas = expressionExperimentService.load( id );
        if ( bas != null ) {
            return bas;
        }
        return expressionExperimentSubSetService.loadOrFail( id, exceptionSupplier, message );
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
    @Transactional(readOnly = true)
    public Stream<BioAssaySet> streamAll() {
        return Stream.concat( expressionExperimentService.streamAll(), expressionExperimentSubSetService.streamAll() );
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<BioAssaySet> streamAll( boolean createNewSession ) {
        return Stream.concat( expressionExperimentService.streamAll( createNewSession ), expressionExperimentSubSetService.streamAll( createNewSession ) );
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
