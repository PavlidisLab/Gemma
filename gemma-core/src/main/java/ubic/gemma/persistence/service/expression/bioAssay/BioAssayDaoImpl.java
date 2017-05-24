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
import org.hibernate.jdbc.Work;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.BusinessKey;
import ubic.gemma.persistence.util.EntityUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * @author pavlidis
 */
@Repository
public class BioAssayDaoImpl extends AbstractDao<BioAssay> implements BioAssayDao {

    /* ********************************
     * Constructors
     * ********************************/

    public BioAssayDaoImpl() {
        super( BioAssay.class );
    }

    @Autowired
    public BioAssayDaoImpl( SessionFactory sessionFactory ) {
        super( BioAssay.class );
        setSessionFactory( sessionFactory );
    }

    /* ********************************
     * Public methods
     * ********************************/

    /**
     * @see BioAssayDao#countAll()
     */
    @Override
    public Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new RuntimeException( "Error performing 'BioAssayDao.countAll()' --> " + th, th );
        }
    }

    @Override
    public BioAssay find( BioAssay bioAssay ) {
        try {
            Criteria queryObject = BusinessKey
                    .createQueryObject( super.getSessionFactory().getCurrentSession(), bioAssay );

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
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * @see BioAssayDao#findBioAssayDimensions(BioAssay)
     */
    @Override
    public Collection<BioAssayDimension> findBioAssayDimensions( BioAssay bioAssay ) {
        //noinspection unchecked
        return this.getSession().createQuery(
                "select bad from BioAssayDimensionImpl bad inner join bad.bioAssays as ba where :bioAssay in ba " )
                .setParameter( "bioAssay", bioAssay ).list();
    }

    @Override
    public Collection<BioAssay> findByAccession( String accession ) {
        if ( StringUtils.isBlank( accession ) )
            return new HashSet<>();

        //noinspection unchecked
        return this.getSession().createQuery(
                "select distinct b from BioAssay b inner join b.accession a where a.accession = :accession" )
                .setParameter( "accession", accession ).list();
    }

    @Override
    public BioAssay findOrCreate( BioAssay bioAssay ) {
        BioAssay newBioAssay = find( bioAssay );
        if ( newBioAssay != null ) {
            if ( log.isDebugEnabled() )
                log.debug( "Found existing bioAssay: " + newBioAssay );
            return newBioAssay;
        }
        if ( log.isDebugEnabled() )
            log.debug( "Creating new bioAssay: " + bioAssay );
        return create( bioAssay );
    }

    @Override
    public Collection<BioAssay> thaw( Collection<BioAssay> bioAssays ) {
        if ( bioAssays.isEmpty() )
            return bioAssays;
        List<?> thawedBioassays = this.getHibernateTemplate().findByNamedParam(
                "select distinct b from BioAssay b left join fetch b.arrayDesignUsed"
                        + " left join fetch b.derivedDataFiles join fetch b.sampleUsed bm"
                        + " left join bm.factorValues left join bm.bioAssaysUsedIn left join fetch "
                        + " b.auditTrail at left join fetch at.events where b.id in (:ids) ", "ids",
                EntityUtils.getIds( bioAssays ) );
        //noinspection unchecked
        return ( Collection<BioAssay> ) thawedBioassays;
    }

    /**
     * @see BioAssayDao#thaw(BioAssay)
     */
    @Override
    public void thaw( final BioAssay bioAssay ) {
        try {
            this.handleThaw( bioAssay );
        } catch ( Throwable th ) {
            throw new RuntimeException( "Error performing 'BioAssayDao.thaw(BioAssay bioAssay)' --> " + th, th );
        }
    }

    @Override
    public Collection<BioAssay> loadValueObjects( Collection<Long> ids ) {
        return null;
    }

    /* ********************************
     * Protected methods
     * ********************************/

    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from BioAssay";
        try {
            org.hibernate.Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( query );
            queryObject.setCacheable( true );
            return ( ( Long ) queryObject.iterate().next() ).intValue();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /* ********************************
     * Private methods
     * ********************************/

    private void handleThaw( final BioAssay bioAssay ) throws Exception {

        this.getSession().doWork( new Work() {
            @Override
            public void execute( Connection connection ) throws SQLException {
                getSession().buildLockRequest( LockOptions.NONE ).lock( bioAssay );
                Hibernate.initialize( bioAssay.getArrayDesignUsed() );
                Hibernate.initialize( bioAssay.getDerivedDataFiles() );
                BioMaterial bm = bioAssay.getSampleUsed();
                getSession().buildLockRequest( LockOptions.NONE ).lock( bm );
                Hibernate.initialize( bm );
                Hibernate.initialize( bm.getBioAssaysUsedIn() );
                Hibernate.initialize( bm.getFactorValues() );
                getSession().evict( bm );
                getSession().evict( bioAssay );
            }
        } );
    }

}