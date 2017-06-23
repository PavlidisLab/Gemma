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
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.service.VoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.experiment.FactorValue</code>.
 * </p>
 */
@Repository
public class FactorValueDaoImpl extends VoEnabledDao<FactorValue, FactorValueValueObject> implements FactorValueDao {

    @Autowired
    public FactorValueDaoImpl( SessionFactory sessionFactory ) {
        super( FactorValue.class, sessionFactory );
    }

    @Override
    public FactorValue find( FactorValue factorValue ) {
        try {
            Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( FactorValue.class );

            BusinessKey.checkKey( factorValue );

            BusinessKey.createQueryObject( queryObject, factorValue );

            java.util.List<?> results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    this.debug( results );
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            results.size() + " instances of '" + FactorValue.class.getName()
                                    + "' was found when executing query for " + factorValue );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( FactorValue ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public Collection<FactorValue> findByValue( String valuePrefix ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "from FactorValue where value like :q" )
                .setParameter( "q", valuePrefix + "%" ).list();
    }

    @Override
    public FactorValue findOrCreate( FactorValue factorValue ) {
        FactorValue existingFactorValue = this.find( factorValue );
        if ( existingFactorValue != null ) {
            return existingFactorValue;
        }
        if ( log.isDebugEnabled() )
            log.debug( "Creating new factorValue" );
        return create( factorValue );
    }

    @Override
    public void remove( final FactorValue factorValue ) {
        if ( factorValue == null )
            return;

        //noinspection unchecked
        Collection<BioMaterial> bms = this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct bm from BioMaterial as bm join bm.factorValues fv where fv = :fv" )
                .setParameter( "fv", factorValue ).list();

        log.info( "Disassociating " + factorValue + " from " + bms.size() + " biomaterials" );
        for ( BioMaterial bioMaterial : bms ) {
            log.info( "Processing " + bioMaterial ); // temporary, debugging.
            if ( bioMaterial.getFactorValues().remove( factorValue ) ) {
                this.getSessionFactory().getCurrentSession().update( bioMaterial );
            } else {
                log.warn( "Unexpectedly the factor value was not actually associated with " + bioMaterial );
            }
        }

        List<?> efs = this.getHibernateTemplate()
                .findByNamedParam( "select ef from ExperimentalFactor ef join ef.factorValues fv where fv = :fv", "fv",
                        factorValue );

        ExperimentalFactor ef = ( ExperimentalFactor ) efs.iterator().next();
        ef.getFactorValues().remove( factorValue );
        this.getSessionFactory().getCurrentSession().update( ef );

        // will get the dreaded 'already in session' error if we don't do this.
        this.getSessionFactory().getCurrentSession().flush();
        this.getSessionFactory().getCurrentSession().clear();

        this.getSessionFactory().getCurrentSession().delete( factorValue );

    }

    private void debug( List<?> results ) {
        StringBuilder sb = new StringBuilder();
        sb.append( "\nFactorValues found:\n" );
        for ( Object object : results ) {
            sb.append( object ).append( "\n" );
        }
        log.error( sb.toString() );
    }

    @Override
    public FactorValueValueObject loadValueObject( FactorValue entity ) {
        return new FactorValueValueObject( entity );
    }

    @Override
    public Collection<FactorValueValueObject> loadValueObjects( Collection<FactorValue> entities ) {
        Collection<FactorValueValueObject> vos = new LinkedHashSet<>();
        for ( FactorValue fv : entities ) {
            vos.add( loadValueObject( fv ) );
        }
        return vos;
    }
}