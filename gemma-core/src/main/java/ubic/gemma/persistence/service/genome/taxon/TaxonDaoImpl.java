/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.genome.taxon;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.*;
import org.hibernate.jdbc.Work;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledDao;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.util.*;

import java.sql.Connection;
import java.util.*;

/**
 * @author pavlidis
 * @see Taxon
 */
@Repository
public class TaxonDaoImpl extends AbstractFilteringVoEnabledDao<Taxon, TaxonValueObject> implements TaxonDao {

    @Autowired
    public TaxonDaoImpl( SessionFactory sessionFactory ) {
        super( Taxon.class, sessionFactory );
    }

    @Override
    public Taxon findOrCreate( Taxon taxon ) {
        Taxon existingTaxon = this.find( taxon );
        if ( existingTaxon != null ) {
            if ( AbstractDao.log.isDebugEnabled() )
                AbstractDao.log.debug( "Found existing taxon: " + taxon );
            return existingTaxon;
        }

        if ( StringUtils.isBlank( taxon.getCommonName() ) && StringUtils.isBlank( taxon.getScientificName() ) ) {
            throw new IllegalArgumentException( "Cannot create a taxon without names: " + taxon );
        }

        AbstractDao.log.warn( "Creating new taxon: " + taxon );
        return super.create( taxon );
    }

    @Override
    public Taxon find( Taxon taxon ) {

        BusinessKey.checkValidKey( taxon );

        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( Taxon.class )
                .setReadOnly( true );
        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );
        BusinessKey.addRestrictions( queryObject, taxon );

        List<?> results = queryObject.list();
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of '" + taxon.getClass().getName()
                                + "' was found when executing query" );
            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return ( Taxon ) result;

    }

    @Override
    public Taxon findByCommonName( final String commonName ) {
        if ( StringUtils.isBlank( commonName ) )
            throw new IllegalArgumentException( "commonName cannot be empty" );
        return this.findOneByStringProperty( "commonName", commonName );
    }

    @Override
    public Taxon findByScientificName( final String scientificName ) {
        if ( StringUtils.isBlank( scientificName ) )
            throw new IllegalArgumentException( "scientificName cannot be empty" );
        return this.findOneByStringProperty( "scientificName", scientificName );
    }

    @Override
    public Collection<Taxon> findTaxonUsedInEvidence() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct taxon from Gene as g join g.phenotypeAssociations as evidence join g.taxon as taxon" )
                .list();
    }

    @Override
    public void thaw( final Taxon taxon ) {
        this.getSessionFactory().getCurrentSession().doWork( new Work() {
            @Override
            public void execute( Connection connection ) {
                TaxonDaoImpl.this.getSession().buildLockRequest( LockOptions.NONE ).lock( taxon );
                Hibernate.initialize( taxon.getExternalDatabase() );
                TaxonDaoImpl.this.getSession().evict( taxon );
            }
        } );
    }

    @Override
    public Taxon findByNcbiId( Long ncbiId ) {
        return ( Taxon ) this.findByProperty( "ncbiId", ncbiId );
    }

    @Override
    public TaxonValueObject loadValueObject( Taxon entity ) {
        this.thaw( entity );
        return new TaxonValueObject( entity );
    }

    @Override
    protected TaxonValueObject processLoadValueObjectsQueryResult( Object result ) {
        Object[] row = ( Object[] ) result;
        TaxonValueObject vo = new TaxonValueObject( ( Taxon ) row[1] );
        if ( row[2] != null ) {
            vo.setExternalDatabase( new ExternalDatabaseValueObject( ( ExternalDatabase ) row[2] ) );
        }
        return vo;
    }

    @Override
    protected Query getLoadValueObjectsQuery( List<ObjectFilter[]> filters, Sort sort ) {

        //noinspection JpaQlInspection // the constants for aliases is messing with the inspector
        String queryString = "select " + getObjectAlias() + ".id as id, " // 0
                + getObjectAlias() + ", " // 1
                + "ED " // 2
                + "from Taxon as " + getObjectAlias() + " " // taxon
                + "left join " + getObjectAlias() + ".externalDatabase as ED " // external db
                + "where " + getObjectAlias() + ".id is not null "; // needed to use formRestrictionCause()

        queryString += ObjectFilterQueryUtils.formRestrictionClause( filters );
        queryString += "group by " + getObjectAlias() + ".id ";
        queryString += ObjectFilterQueryUtils.formOrderByProperty( sort.getOrderBy(), sort.getDirection() );

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        ObjectFilterQueryUtils.addRestrictionParameters( query, filters );

        return query;
    }

    @Override
    protected Query getCountValueObjectsQuery( List<ObjectFilter[]> filters ) {
        //noinspection JpaQlInspection // the constants for aliases is messing with the inspector
        String queryString = "select count(distinct " + getObjectAlias() + ".id) "
                + "from Taxon as " + getObjectAlias() + " " // taxon
                + "left join " + getObjectAlias() + ".externalDatabase as ED " // external db
                + "where " + getObjectAlias() + ".id is not null "; // needed to use formRestrictionCause()

        queryString += ObjectFilterQueryUtils.formRestrictionClause( filters );

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        ObjectFilterQueryUtils.addRestrictionParameters( query, filters );

        return query;
    }

    @Override
    public String getObjectAlias() {
        return ObjectFilter.DAO_TAXON_ALIAS;
    }
}