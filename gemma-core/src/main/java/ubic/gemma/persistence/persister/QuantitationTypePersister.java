package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeDao;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QuantitationTypePersister extends AbstractPersister<QuantitationType> implements CachingPersister<QuantitationType> {

    private final Map<Object, QuantitationType> quantitationTypeCache = new ConcurrentHashMap<>();

    @Autowired
    private QuantitationTypeDao quantitationTypeDao;

    @Autowired
    public QuantitationTypePersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public QuantitationType persist( QuantitationType qType ) {
        if ( qType == null )
            return null;
        if ( !this.isTransient( qType ) )
            return qType;

        /*
         * this cache is dangerous if run for multiple experiment loadings. For this reason we clear the cache
         * before persisting each experiment.
         */
        int key;
        if ( qType.getName() == null )
            throw new IllegalArgumentException( "QuantitationType must have a name" );
        key = qType.getName().hashCode();
        if ( qType.getDescription() != null )
            key += qType.getDescription().hashCode();

        if ( quantitationTypeCache.containsKey( key ) ) {
            return quantitationTypeCache.get( key );
        }

        /*
         * Note: we use 'create' here instead of 'findOrCreate' because we don't want quantitation types shared across
         * experiments.
         */
        QuantitationType qt = quantitationTypeDao.create( qType );
        quantitationTypeCache.put( key, qt );
        return qt;
    }

    /**
     * For clearing the cache.
     */
    @Override
    public void clearCache() {
        this.quantitationTypeCache.clear();
    }
}
