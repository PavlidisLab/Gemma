/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractNoopFilteringVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.experiment.FactorValue</code>.
 * </p>
 */
@Repository
public class FactorValueDaoImpl extends AbstractNoopFilteringVoEnabledDao<FactorValue, FactorValueValueObject>
        implements FactorValueDao {

    @Autowired
    public FactorValueDaoImpl( SessionFactory sessionFactory ) {
        super( FactorValue.class, sessionFactory );
    }

    @Override
    public Collection<FactorValue> findByValue( String valuePrefix ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "from FactorValue where value like :q" )
                .setParameter( "q", valuePrefix + "%" ).list();
    }

    @Override
    @Deprecated
    public FactorValue loadWithOldStyleCharacteristics( Long id, boolean readOnly ) {
        boolean previousReadOnly = getSessionFactory().getCurrentSession().isDefaultReadOnly();
        try {
            getSessionFactory().getCurrentSession().setDefaultReadOnly( readOnly );
            FactorValue fv = load( id );
            if ( fv != null ) {
                Hibernate.initialize( fv.getOldStyleCharacteristics() );
            }
            return fv;
        } finally {
            getSessionFactory().getCurrentSession().setDefaultReadOnly( previousReadOnly );
        }
    }

    @Override
    @Deprecated
    public Map<Long, Integer> loadAllExceptIds( Set<Long> excludedIds ) {
        List<Object[]> result;
        if ( excludedIds.isEmpty() ) {
            //noinspection unchecked
            result = ( List<Object[]> ) this.getSessionFactory().getCurrentSession()
                    .createQuery( "select fv.id, size(fv.oldStyleCharacteristics) from FactorValue fv group by fv order by id" )
                    .list();
        } else {
            //noinspection unchecked
            result = ( List<Object[]> ) this.getSessionFactory().getCurrentSession()
                    .createQuery( "select fv.id, size(fv.oldStyleCharacteristics) from FactorValue fv where fv.id not in :ids group by fv order by id" )
                    .setParameterList( "ids", excludedIds )
                    .list();
        }
        return result.stream().collect( Collectors.toMap( row -> ( Long ) row[0], row -> ( Integer ) row[1] ) );
    }

    @Override
    public void flushAndEvict( List<Long> batch ) {
        getSessionFactory().getCurrentSession().flush();
        load( batch ).forEach( getSessionFactory().getCurrentSession()::evict );
    }

    @Override
    public void remove( @Nullable final FactorValue factorValue ) {
        if ( factorValue == null )
            return;

        // detach from the experimental factor
        factorValue.getExperimentalFactor().getFactorValues().remove( factorValue );

        // load samples to evict from the cache (because we delete manually)
        //noinspection unchecked
        List<Long> bmIds = this.getSessionFactory().getCurrentSession()
                .createQuery( "select bm.id from BioMaterial bm "
                        + "join bm.factorValues fv where fv = :fv "
                        + "group by bm.id" )
                .setParameter( "fv", factorValue )
                .list();

        // detach the factor from any sample
        int deleted = this.getSessionFactory().getCurrentSession()
                .createSQLQuery( "delete from BIO_MATERIAL_FACTOR_VALUES where FACTOR_VALUES_FK = :fvId" )
                .setParameter( "fvId", factorValue.getId() )
                .executeUpdate();

        // evict the collections from the cache
        for ( Long bmId : bmIds ) {
            this.getSessionFactory().getCache()
                    .evictCollection( "ubic.gemma.model.expression.biomaterial.BioMaterial.factorValues", bmId );
        }

        AbstractDao.log.debug( String.format( "%s was detached from %d samples.", factorValue, deleted ) );

        super.remove( factorValue );
    }

    @Override
    public FactorValue find( FactorValue factorValue ) {
        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( FactorValue.class );

        BusinessKey.checkKey( factorValue );

        BusinessKey.createQueryObject( queryObject, factorValue );

        return ( FactorValue ) queryObject.uniqueResult();
    }

    @Override
    protected FactorValueValueObject doLoadValueObject( FactorValue entity ) {
        return new FactorValueValueObject( entity );
    }

    private void debug( List<?> results ) {
        StringBuilder sb = new StringBuilder();
        sb.append( "\nFactorValues found:\n" );
        for ( Object object : results ) {
            sb.append( object ).append( "\n" );
        }
        AbstractDao.log.error( sb.toString() );
    }
}