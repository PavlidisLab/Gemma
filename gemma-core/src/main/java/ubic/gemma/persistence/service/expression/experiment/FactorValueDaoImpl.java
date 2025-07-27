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
import org.springframework.util.Assert;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.service.AbstractNoopFilteringVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

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
    public FactorValue create( FactorValue factorValue ) {
        validate( factorValue );
        return super.create( factorValue );
    }

    @Override
    public FactorValue save( FactorValue entity ) {
        validate( entity );
        return super.save( entity );
    }

    @Override
    public void update( FactorValue entity ) {
        validate( entity );
        super.update( entity );
    }

    @Override
    public Collection<FactorValue> findByValue( String valuePrefix, int maxResults ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from FactorValue where value like :q" )
                .setParameter( "q", valuePrefix + "%" )
                .setMaxResults( maxResults )
                .list();
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
    public Map<Long, Integer> loadIdsWithNumberOfOldStyleCharacteristics( Set<Long> excludedIds ) {
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
                    .setParameterList( "ids", optimizeParameterList( excludedIds ) )
                    .list();
        }
        return result.stream().collect( Collectors.toMap( row -> ( Long ) row[0], row -> ( Integer ) row[1] ) );
    }

    @Override
    public void updateIgnoreAcl( FactorValue fv ) {
        update( fv );
    }

    @Override
    public void remove( @Nullable final FactorValue factorValue ) {
        if ( factorValue == null )
            return;

        // detach from the experimental factor
        factorValue.getExperimentalFactor().getFactorValues().remove( factorValue );

        // detach from any sample
        //noinspection unchecked
        List<BioMaterial> bms = this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct bm from BioMaterial bm "
                        + "join bm.factorValues fv where fv = :fv" )
                .setParameter( "fv", factorValue )
                .list();
        for ( BioMaterial bm : bms ) {
            bm.getFactorValues().remove( factorValue );
        }
        log.debug( String.format( "%s was detached from %d samples.", factorValue, bms.size() ) );

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

    private void validate( FactorValue factorValue ) {
        FactorType factorType;
        if ( Hibernate.isInitialized( factorValue.getExperimentalFactor() ) ) {
            factorType = factorValue.getExperimentalFactor().getType();
        } else {
            // if the EF is not initialized, just obtain the FactorType directly instead of loading the EF (which also
            // loads potentially many other FVs)
            factorType = requireNonNull( ( FactorType ) getSessionFactory().getCurrentSession()
                    .createQuery( "select ef.type from ExperimentalFactor ef where ef = :ef" )
                    .setParameter( "ef", factorValue.getExperimentalFactor() )
                    .uniqueResult() );
        }
        // validate categorical v.s. continuous factor values
        if ( factorType.equals( FactorType.CONTINUOUS ) ) {
            Assert.notNull( factorValue.getMeasurement(), "Continuous factor values must have a measurement: " + factorValue );
            Assert.isTrue( factorValue.getValue() == null || factorValue.getValue().equals( factorValue.getMeasurement().getValue() ),
                    "If provided, the value of the factor must match the measurement value." );
            Assert.isNull( factorValue.getIsBaseline(), "Continuous factor values cannot be (or not be) a baseline: " + factorValue );
        } else if ( factorType.equals( FactorType.CATEGORICAL ) ) {
            Assert.isNull( factorValue.getMeasurement(), "Categorical factor values must not have a measurement: " + factorValue );
        }
    }

    private void debug( List<?> results ) {
        StringBuilder sb = new StringBuilder();
        sb.append( "\nFactorValues found:\n" );
        for ( Object object : results ) {
            sb.append( object ).append( "\n" );
        }
        log.error( sb.toString() );
    }
}