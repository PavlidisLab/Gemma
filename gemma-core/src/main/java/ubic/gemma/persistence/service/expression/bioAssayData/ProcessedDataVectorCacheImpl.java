package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.cache.CacheUtils;

import javax.annotation.Nullable;
import java.util.Collection;

import static java.util.Collections.unmodifiableCollection;

@Component
class ProcessedDataVectorCacheImpl implements ProcessedDataVectorCache {

    private static final String VECTOR_CACHE_NAME = "ProcessedExpressionDataVectorCache";

    private final Cache cache;

    @Value("${gemma.cache." + VECTOR_CACHE_NAME + ".enabled}")
    private boolean enabled;

    @Autowired
    public ProcessedDataVectorCacheImpl( CacheManager cacheManager ) {
        this.cache = CacheUtils.getCache( cacheManager, VECTOR_CACHE_NAME );
    }

    @Nullable
    @Override
    public Collection<DoubleVectorValueObject> get( ExpressionExperiment ee ) {
        Assert.notNull( ee.getId() );
        if ( !enabled ) {
            return null;
        }
        Cache.ValueWrapper val = cache.get( ee.getId() );
        //noinspection unchecked
        return val != null ? ( Collection<DoubleVectorValueObject> ) val.get() : null;
    }

    @Override
    public void put( ExpressionExperiment ee, Collection<DoubleVectorValueObject> vectors ) {
        Assert.notNull( ee.getId() );
        if ( !enabled ) {
            return;
        }
        cache.put( ee.getId(), unmodifiableCollection( vectors ) );
    }

    @Override
    public void evict( ExpressionExperiment ee ) {
        Assert.notNull( ee.getId() );
        if ( !enabled ) {
            return;
        }
        cache.evict( ee.getId() );
    }

    @Override
    public void clear() {
        if ( !enabled ) {
            return;
        }
        cache.clear();
    }
}
