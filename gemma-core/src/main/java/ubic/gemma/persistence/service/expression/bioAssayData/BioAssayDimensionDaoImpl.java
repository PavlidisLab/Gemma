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
package ubic.gemma.persistence.service.expression.bioAssayData;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.persistence.service.VoEnabledDao;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.bioAssayData.BioAssayDimension</code>.
 * </p>
 *
 * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimension
 */
@Repository
public class BioAssayDimensionDaoImpl extends VoEnabledDao<BioAssayDimension, BioAssayDimensionValueObject>
        implements BioAssayDimensionDao {

    @Autowired
    public BioAssayDimensionDaoImpl( SessionFactory sessionFactory ) {
        super( BioAssayDimension.class, sessionFactory );
    }

    @Override
    public BioAssayDimension find( BioAssayDimension bioAssayDimension ) {

        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( BioAssayDimension.class );
        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );

        if ( StringUtils.isNotBlank( bioAssayDimension.getName() ) ) {
            queryObject.add( Restrictions.eq( "name", bioAssayDimension.getName() ) );
        }

        if ( StringUtils.isNotBlank( bioAssayDimension.getDescription() ) ) {
            queryObject.add( Restrictions.eq( "description", bioAssayDimension.getDescription() ) );
        }

        queryObject.add( Restrictions.sizeEq( "bioAssays", bioAssayDimension.getBioAssays().size() ) );

        Collection<String> names = new HashSet<String>();
        assert bioAssayDimension.getBioAssays().size() > 0;
        for ( BioAssay bioAssay : bioAssayDimension.getBioAssays() ) {
            names.add( bioAssay.getName() );
        }
        queryObject.createCriteria( "bioAssays" ).add( Restrictions.in( "name", names ) );

        BioAssayDimension candidate = ( BioAssayDimension ) queryObject.uniqueResult();

        if ( candidate == null )
            return null;

        // Now check that the bioassays and order are exactly the same.
        Collection<BioAssay> desiredBioAssays = bioAssayDimension.getBioAssays();
        Collection<BioAssay> candidateBioAssays = candidate.getBioAssays();

        assert desiredBioAssays.size() == candidateBioAssays.size();

        Iterator<BioAssay> dit = desiredBioAssays.iterator();
        Iterator<BioAssay> cit = candidateBioAssays.iterator();

        while ( dit.hasNext() ) {
            BioAssay d = dit.next();
            BioAssay c = cit.next();
            if ( !c.equals( d ) )
                return null;
        }

        return candidate;

    }

    @Override
    public BioAssayDimension findOrCreate( BioAssayDimension bioAssayDimension ) {
        if ( bioAssayDimension == null || bioAssayDimension.getBioAssays() == null )
            throw new IllegalArgumentException();
        BioAssayDimension existingBioAssayDimension = find( bioAssayDimension );
        if ( existingBioAssayDimension != null ) {
            return existingBioAssayDimension;
        }
        if ( log.isDebugEnabled() )
            log.debug( "Creating new " + bioAssayDimension );
        return create( bioAssayDimension );
    }

    @Override
    public BioAssayDimension thaw( final BioAssayDimension bioAssayDimension ) {
        if ( bioAssayDimension == null ) return null;
        if ( bioAssayDimension.getId() == null ) return bioAssayDimension;

        this.getHibernateTemplate().execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.buildLockRequest( LockOptions.NONE ).lock( bioAssayDimension );
                Hibernate.initialize( bioAssayDimension );
                Hibernate.initialize( bioAssayDimension.getBioAssays() );

                for ( BioAssay ba : bioAssayDimension.getBioAssays() ) {
                    session.buildLockRequest( LockOptions.NONE ).lock( ba );
                    Hibernate.initialize( ba );
                    Hibernate.initialize( ba.getSampleUsed() );
                    Hibernate.initialize( ba.getArrayDesignUsed() );
                    BioMaterial bm = ba.getSampleUsed();
                    session.buildLockRequest( LockOptions.NONE ).lock( bm );
                    Hibernate.initialize( bm );
                    Hibernate.initialize( bm.getBioAssaysUsedIn() );
                    Hibernate.initialize( bm.getFactorValues() );

                }
                return null;
            }
        } );
        return bioAssayDimension;
    }

    @Override
    public BioAssayDimension thawLite( final BioAssayDimension bioAssayDimension ) {
        if ( bioAssayDimension == null )
            return null;
        if ( bioAssayDimension.getId() == null )
            return bioAssayDimension;

        this.getHibernateTemplate().execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.buildLockRequest( LockOptions.NONE ).lock( bioAssayDimension );
                Hibernate.initialize( bioAssayDimension );
                Hibernate.initialize( bioAssayDimension.getBioAssays() );
                return null;
            }
        } );
        return bioAssayDimension;
    }

    @Override
    public BioAssayDimensionValueObject loadValueObject( BioAssayDimension entity ) {
        return new BioAssayDimensionValueObject( entity );
    }

    @Override
    public Collection<BioAssayDimensionValueObject> loadValueObjects( Collection<BioAssayDimension> entities ) {
        Collection<BioAssayDimensionValueObject> vos = new LinkedHashSet<BioAssayDimensionValueObject>();
        for ( BioAssayDimension e : entities ) {
            vos.add( loadValueObject( e ) );
        }
        return vos;
    }
}