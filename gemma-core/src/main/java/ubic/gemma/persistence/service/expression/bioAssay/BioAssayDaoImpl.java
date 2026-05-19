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
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractNoopFilteringVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;
import ubic.gemma.persistence.util.Cursor;
import ubic.gemma.persistence.util.CursorPage;
import ubic.gemma.persistence.util.Sort;

import org.springframework.lang.Nullable;
import java.util.*;

/**
 * @author pavlidis
 */
@Repository
public class BioAssayDaoImpl extends AbstractNoopFilteringVoEnabledDao<BioAssay, BioAssayValueObject> implements BioAssayDao {

    @Autowired
    public BioAssayDaoImpl( SessionFactory sessionFactory ) {
        super( BioAssay.class, sessionFactory );
    }

    @Override
    public BioAssay find( BioAssay bioAssay ) {
        return BusinessKey.find( this.getSessionFactory().getCurrentSession(), bioAssay );
    }

    @Nullable
    @Override
    public BioAssay findByShortName( String shortName ) {
        return findOneByProperty( "shortName", shortName );
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
                        "select b from BioAssay b join b.accession a where a.accession = :accession group by b" )
                .setParameter( "accession", accession ).list();
    }

    @Override
    public Collection<BioAssaySet> getBioAssaySets( BioAssay bioAssay ) {
        Collection<BioAssaySet> results = new HashSet<>();
        //noinspection unchecked
        results.addAll( getSessionFactory().getCurrentSession()
                .createQuery( "select bas from ExpressionExperiment bas join bas.bioAssays ba where ba = :ba group by bas" )
                .setParameter( "ba", bioAssay )
                .list() );
        //noinspection unchecked
        results.addAll( getSessionFactory().getCurrentSession()
                .createQuery( "select bas from ExpressionExperimentSubSet bas join bas.bioAssays ba where ba = :ba group by bas" )
                .setParameter( "ba", bioAssay )
                .list() );
        return results;
    }

    @Override
    public List<BioAssayValueObject> loadValueObjects( Collection<BioAssay> entities, @Nullable Map<ArrayDesign, ArrayDesignValueObject> ad2vo, @Nullable Map<BioAssay, BioAssay> assay2sourceAssayMap, boolean basic, boolean allFactorValues ) {
        List<BioAssayValueObject> vos = new LinkedList<>();
        for ( BioAssay e : entities ) {
            vos.add( new BioAssayValueObject( e, ad2vo, assay2sourceAssayMap != null ? assay2sourceAssayMap.get( e ) : null, basic, allFactorValues ) );
        }
        return vos;
    }

    @Override
    protected BioAssayValueObject doLoadValueObject( BioAssay entity ) {
        return new BioAssayValueObject( entity, null, null, false, false );
    }

    @Override
    public CursorPage<BioAssayValueObject> loadValueObjectsByCursorForExpressionExperiment(
            ExpressionExperiment ee, @Nullable Cursor cursor, int limit ) {
        if ( limit <= 0 ) {
            throw new IllegalArgumentException( "Cursor page limit must be > 0." );
        }
        // Cursors carry their sort spec so the client can't silently switch sorts between
        // pages — step 1b convention. The only sort we support here is ascending id (the
        // primary key, indexed and unique); see the doLoadValueObjectsByCursor restriction
        // in AbstractQueryFilteringVoEnabledDao step 1b.
        String expectedSortSpec = "+id";
        Sort sort = Sort.by( null, "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" );
        boolean backward = cursor != null && cursor.getDirection() == Cursor.Direction.BACKWARD;
        Long lastSeenId = null;
        if ( cursor != null ) {
            if ( !expectedSortSpec.equals( cursor.getSortSpec() ) ) {
                throw new IllegalArgumentException( "Cursor sort spec '" + cursor.getSortSpec()
                        + "' does not match the requested sort '" + expectedSortSpec + "'." );
            }
            Object[] key = cursor.getKeyTuple();
            if ( key.length != 1 ) {
                throw new IllegalArgumentException( "Cursor key tuple must have exactly 1 component for sort '"
                        + expectedSortSpec + "'; got " + key.length + "." );
            }
            try {
                lastSeenId = ( ( Number ) key[0] ).longValue();
            } catch ( ClassCastException e ) {
                throw new IllegalArgumentException( "Cursor key component must be numeric for sort '" + expectedSortSpec + "'.", e );
            }
        }
        // ASC forward → id > :lastId; ASC backward → id < :lastId ORDER BY id DESC (then reversed
        // for client-visible order). Mirrors AbstractQueryFilteringVoEnabledDao#doLoadValueObjectsByCursor.
        String comparator;
        String orderDirection;
        if ( cursor == null ) {
            comparator = "";
            orderDirection = "asc";
        } else if ( !backward ) {
            comparator = " and ba.id > :cursorId";
            orderDirection = "asc";
        } else {
            comparator = " and ba.id < :cursorId";
            orderDirection = "desc";
        }
        // Walk the EE→bioAssays association directly; BioAssayDao extends AbstractNoopFilteringVoEnabledDao
        // (no Filters→HQL compilation), so we can't reuse the generic getFilteringQuery() machinery here.
        String hql = "select ba from ExpressionExperiment ee join ee.bioAssays ba "
                + "where ee.id = :eeId" + comparator + " order by ba.id " + orderDirection;
        //noinspection unchecked
        Query<BioAssay> q = ( Query<BioAssay> ) getSessionFactory().getCurrentSession().createQuery( hql );
        q.setParameter( "eeId", ee.getId() );
        if ( lastSeenId != null ) {
            q.setParameter( "cursorId", lastSeenId );
        }
        q.setMaxResults( limit + 1 );
        List<BioAssay> rows = q.list();

        boolean hasMore = rows.size() > limit;
        if ( hasMore ) {
            rows = new ArrayList<>( rows.subList( 0, limit ) );
        }
        if ( backward ) {
            Collections.reverse( rows );
        }
        List<BioAssayValueObject> vos = new ArrayList<>( rows.size() );
        for ( BioAssay ba : rows ) {
            // basic + allFactorValues match the legacy /datasets/{dataset}/samples shape
            // (see DatasetArgService.getSamples).
            vos.add( new BioAssayValueObject( ba, null, null, true, true ) );
        }

        String nextCursor = null;
        String prevCursor = null;
        if ( !vos.isEmpty() ) {
            Long lastId = vos.get( vos.size() - 1 ).getId();
            Long firstId = vos.get( 0 ).getId();
            if ( backward || hasMore ) {
                nextCursor = new Cursor( expectedSortSpec, new Object[] { lastId }, Cursor.Direction.FORWARD ).encode();
            }
            if ( cursor != null ) {
                prevCursor = new Cursor( expectedSortSpec, new Object[] { firstId }, Cursor.Direction.BACKWARD ).encode();
            }
        }
        return new CursorPage<>( vos, sort, limit, nextCursor, prevCursor, null );
    }
}
