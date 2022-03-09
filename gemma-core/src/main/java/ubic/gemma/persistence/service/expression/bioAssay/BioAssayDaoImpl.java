/*
 * The Gemma project.
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.persistence.service.expression.bioAssay;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.jdbc.Work;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;
import ubic.gemma.persistence.util.EntityUtils;

import java.sql.Connection;
import java.util.*;

/**
 * @author pavlidis
 */
@Repository
public class BioAssayDaoImpl extends AbstractVoEnabledDao<BioAssay, BioAssayValueObject> implements BioAssayDao {

    @Autowired
    public BioAssayDaoImpl( SessionFactory sessionFactory ) {
        super( BioAssay.class, sessionFactory );
    }

    @Override
    public Collection<BioAssay> create( final Collection<BioAssay> entities ) {
        this.getSessionFactory().getCurrentSession().doWork( new Work() {
            @Override
            public void execute( Connection connection ) {
                for ( BioAssay entity : entities ) {
                    BioAssayDaoImpl.this.create( entity );
                }
            }
        } );
        return entities;
    }

    @Override
    public void update( final Collection<BioAssay> entities ) {
        this.getSessionFactory().getCurrentSession().doWork( new Work() {
            @Override
            public void execute( Connection connection ) {
                for ( BioAssay entity : entities ) {
                    BioAssayDaoImpl.this.update( entity );
                }
            }
        } );
    }

    @Override
    public BioAssay find( BioAssay bioAssay ) {
        try {
            Criteria queryObject = BusinessKey
                    .createQueryObject( this.getSessionFactory().getCurrentSession(), bioAssay );

            List<?> results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + BioAssay.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( BioAssay ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw getHibernateTemplate().convertHibernateAccessException( ex );
        }
    }

    @Override
    public BioAssay findOrCreate( BioAssay bioAssay ) {
        BioAssay newBioAssay = this.find( bioAssay );
        if ( newBioAssay != null ) {
            if ( AbstractDao.log.isDebugEnabled() )
                AbstractDao.log.debug( "Found existing bioAssay: " + newBioAssay );
            return newBioAssay;
        }
        if ( AbstractDao.log.isDebugEnabled() )
            AbstractDao.log.debug( "Creating new bioAssay: " + bioAssay );
        return this.create( bioAssay );
    }

    @Override
    public Collection<BioAssayDimension> findBioAssayDimensions( BioAssay bioAssay ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select bad from BioAssayDimension bad inner join bad.bioAssays as ba where :bioAssay in ba " )
                .setParameter( "bioAssay", bioAssay ).list();
    }

    @Override
    public Collection<BioAssay> findByAccession( String accession ) {
        if ( StringUtils.isBlank( accession ) )
            return new HashSet<>();

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct b from BioAssay b inner join b.accession a where a.accession = :accession" )
                .setParameter( "accession", accession ).list();
    }

    @Override
    public void thaw( final BioAssay bioAssay ) {
        try {
            this.getSessionFactory().getCurrentSession().doWork( new Work() {
                @Override
                public void execute( Connection connection ) {
                    Session session = getSessionFactory().getCurrentSession();
                    session.buildLockRequest( LockOptions.NONE ).lock( bioAssay );
                    Hibernate.initialize( bioAssay.getArrayDesignUsed() );
                    Hibernate.initialize( bioAssay.getOriginalPlatform() );
                    BioMaterial bm = bioAssay.getSampleUsed();
                    session.buildLockRequest( LockOptions.NONE ).lock( bm );
                    Hibernate.initialize( bm );
                    Hibernate.initialize( bm.getBioAssaysUsedIn() );
                    Hibernate.initialize( bm.getFactorValues() );
                    session.evict( bm );
                    session.evict( bioAssay );
                }
            } );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'BioAssayDao.thawRawAndProcessed(BioAssay bioAssay)' --> " + th, th );
        }
    }

    @Override
    public Collection<BioAssay> thaw( Collection<BioAssay> bioAssays ) {
        if ( bioAssays.isEmpty() )
            return bioAssays;
        List<?> thawedBioassays = this.getHibernateTemplate().findByNamedParam(
                "select distinct b from BioAssay b left join fetch b.arrayDesignUsed"
                        + " left join fetch b.sampleUsed bm"
                        + " left join bm.factorValues left join bm.bioAssaysUsedIn where b.id in (:ids) ",
                "ids",
                EntityUtils.getIds( bioAssays ) );
        //noinspection unchecked
        return ( Collection<BioAssay> ) thawedBioassays;
    }

    /**
     * Method that allows specification of FactorValueBasicValueObject in the bioMaterialVOs
     *
     * @param entities the bio assays to convert into a VO
     * @param basic true to use FactorValueBasicValueObject, false to use classic FactorValueValueObject
     * @return a collection of bioAssay value objects
     */
    @Override
    //TODO remove when FactorValueValueObject usage is phased out
    public List<BioAssayValueObject> loadValueObjects( Collection<BioAssay> entities, boolean basic ) {
        List<BioAssayValueObject> vos = new LinkedList<>();
        for ( BioAssay e : entities ) {
            vos.add( new BioAssayValueObject( e, basic ) );
        }
        return vos;
    }

    @Override
    public BioAssayValueObject loadValueObject( BioAssay entity ) {
        return new BioAssayValueObject( entity, false );
    }

}