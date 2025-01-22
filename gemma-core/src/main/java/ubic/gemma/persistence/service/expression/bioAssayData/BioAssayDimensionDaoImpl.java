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
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static ubic.gemma.persistence.util.QueryUtils.optimizeIdentifiableParameterList;

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

    /**
     * Find a BioAssayDimension with the exact same list of BioAssays, name and description.
     */
    @Override
    public BioAssayDimension find( BioAssayDimension bioAssayDimension ) {
        BioAssayDimension found = super.find( bioAssayDimension );
        if ( found != null ) {
            return found;
        }

        Collection<Long> bioAssayIds = new HashSet<>();
        for ( BioAssay bioAssay : bioAssayDimension.getBioAssays() ) {
            Assert.notNull( bioAssay.getId(), "Cannot find a BioAssayDimension with a non-persistent BioAssay." );
            bioAssayIds.add( bioAssay.getId() );
        }

        Criteria queryObject = this.getSessionFactory().getCurrentSession()
                .createCriteria( BioAssayDimension.class );

        if ( StringUtils.isNotBlank( bioAssayDimension.getName() ) ) {
            queryObject.add( Restrictions.eq( "name", bioAssayDimension.getName() ) );
        }

        if ( StringUtils.isNotBlank( bioAssayDimension.getDescription() ) ) {
            queryObject.add( Restrictions.eq( "description", bioAssayDimension.getDescription() ) );
        }

        // same size and set of IDs, this is not guaranteeing the order though
        queryObject.add( Restrictions.sizeEq( "bioAssays", bioAssayDimension.getBioAssays().size() ) );
        if ( !bioAssayIds.isEmpty() ) {
            queryObject.createCriteria( "bioAssays" )
                    .add( Restrictions.in( "id", bioAssayIds ) )
                    .setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY );
        }

        //noinspection unchecked
        List<BioAssayDimension> candidates = ( List<BioAssayDimension> ) queryObject.list();

        // Now check that the bioassays and order are exactly the same.
        for ( BioAssayDimension candidate : candidates ) {
            if ( candidate.getBioAssays().equals( bioAssayDimension.getBioAssays() ) ) {
                return candidate;
            }
        }

        return null;
    }

    @Override
    public Collection<BioAssayDimension> findByBioAssayContainsAll( Collection<BioAssay> bioAssays ) {
        if ( bioAssays.isEmpty() ) {
            return Collections.emptySet();
        }
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select bad from BioAssayDimension bad join bad.bioAssays ba where ba in :bas group by bad having count(ba) = :numBas" )
                .setParameterList( "bas", optimizeIdentifiableParameterList( bioAssays ) )
                .setParameter( "numBas", bioAssays.stream().distinct().count() )
                .list();
    }

    @Override
    protected BioAssayDimensionValueObject doLoadValueObject( BioAssayDimension entity ) {
        return new BioAssayDimensionValueObject( entity );
    }
}