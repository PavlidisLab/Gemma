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
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;

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
public class BioAssayDimensionDaoImpl extends AbstractVoEnabledDao<BioAssayDimension, BioAssayDimensionValueObject>
        implements BioAssayDimensionDao {

    @Autowired
    public BioAssayDimensionDaoImpl( SessionFactory sessionFactory ) {
        super( BioAssayDimension.class, sessionFactory );
    }

    @Override
    public BioAssayDimension findOrCreate( BioAssayDimension bioAssayDimension ) {
        if ( bioAssayDimension == null || bioAssayDimension.getBioAssays() == null )
            throw new IllegalArgumentException();
        BioAssayDimension existingBioAssayDimension = this.find( bioAssayDimension );
        if ( existingBioAssayDimension != null ) {
            return existingBioAssayDimension;
        }
        if ( AbstractDao.log.isDebugEnabled() )
            AbstractDao.log.debug( "Creating new " + bioAssayDimension );
        return this.create( bioAssayDimension );
    }

    @Override
    public BioAssayDimension find( BioAssayDimension bioAssayDimension ) {

        if ( bioAssayDimension.getBioAssays().isEmpty() ) {
            throw new IllegalArgumentException( "BioAssayDimension had no BioAssays" );
        }

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

        Collection<String> names = new HashSet<>();
        for ( BioAssay bioAssay : bioAssayDimension.getBioAssays() ) {
            names.add( bioAssay.getName() );
        }
        queryObject.createCriteria( "bioAssays" ).add( Restrictions.in( "name", names ) );
        queryObject.setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY );
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
    @Transactional
    public BioAssayDimension thawLite( final BioAssayDimension bioAssayDimension ) {
        if ( bioAssayDimension == null )
            return null;
        if ( bioAssayDimension.getId() == null )
            return bioAssayDimension;
        Session session = getSessionFactory().getCurrentSession();
        session.buildLockRequest( LockOptions.NONE ).lock( bioAssayDimension );
        Hibernate.initialize( bioAssayDimension );
        Hibernate.initialize( bioAssayDimension.getBioAssays() );
        return bioAssayDimension;
    }

    @Override
    @Transactional
    public BioAssayDimension thaw( final BioAssayDimension bioAssayDimension ) {
        if ( bioAssayDimension == null )
            return null;
        if ( bioAssayDimension.getId() == null )
            return bioAssayDimension;

        Session session = getSessionFactory().getCurrentSession();
        session.buildLockRequest( LockOptions.NONE ).lock( bioAssayDimension );
        Hibernate.initialize( bioAssayDimension );
        Hibernate.initialize( bioAssayDimension.getBioAssays() );

        for ( BioAssay ba : bioAssayDimension.getBioAssays() ) {
            if ( ba != null ) {
                session.buildLockRequest( LockOptions.NONE ).lock( ba );
                Hibernate.initialize( ba );
                Hibernate.initialize( ba.getSampleUsed() );
                Hibernate.initialize( ba.getArrayDesignUsed() );
                Hibernate.initialize( ba.getOriginalPlatform() );
                BioMaterial bm = ba.getSampleUsed();
                session.buildLockRequest( LockOptions.NONE ).lock( bm );
                Hibernate.initialize( bm );
                Hibernate.initialize( bm.getBioAssaysUsedIn() );
                Hibernate.initialize( bm.getFactorValues() );
            }
        }

        return bioAssayDimension;
    }

    @Override
    public BioAssayDimensionValueObject loadValueObject( BioAssayDimension entity ) {
        return new BioAssayDimensionValueObject( entity );
    }

}