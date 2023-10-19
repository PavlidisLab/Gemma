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
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractNoopFilteringVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

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
    public void remove( @Nullable final FactorValue factorValue ) {
        if ( factorValue == null )
            return;

        //noinspection unchecked
        Collection<BioMaterial> bms = this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct bm from BioMaterial as bm join bm.factorValues fv where fv = :fv" )
                .setParameter( "fv", factorValue ).list();

        AbstractDao.log.info( "Disassociating " + factorValue + " from " + bms.size() + " biomaterials" );
        for ( BioMaterial bioMaterial : bms ) {
            AbstractDao.log.info( "Processing " + bioMaterial ); // temporary, debugging.
            if ( bioMaterial.getFactorValues().remove( factorValue ) ) {
                this.getSessionFactory().getCurrentSession().update( bioMaterial );
            } else {
                AbstractDao.log.warn( "Unexpectedly the factor value was not actually associated with " + bioMaterial );
            }
        }

        // detach from all associated experimental factors
        //noinspection unchecked
        List<ExperimentalFactor> efs = this.getSessionFactory().getCurrentSession()
                .createQuery( "select ef from ExperimentalFactor ef join ef.factorValues fv where fv = :fv" )
                .setParameter( "fv", factorValue )
                .list();
        AbstractDao.log.info( "Disassociating " + factorValue + " from " + efs.size() + " experimental factors" );
        for ( ExperimentalFactor ef : efs ) {
            ef.getFactorValues().remove( factorValue );
            this.getSessionFactory().getCurrentSession().update( ef );
        }

        // remove any attached statements since those are not mapped in the collection
        int removedStatements = getSessionFactory().getCurrentSession()
                .createSQLQuery( "delete from CHARACTERISTIC where class = 'Statement' and FACTOR_VALUE_FK = :fvId" )
                .setParameter( "fvId", factorValue.getId() )
                .executeUpdate();
        log.info( String.format( "Removed %d statements from %s", removedStatements, factorValue ) );

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