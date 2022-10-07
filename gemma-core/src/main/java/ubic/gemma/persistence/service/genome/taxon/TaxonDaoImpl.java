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
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractQueryFilteringVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilterQueryUtils;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * @author pavlidis
 * @see Taxon
 */
@Repository
@ParametersAreNonnullByDefault
public class TaxonDaoImpl extends AbstractQueryFilteringVoEnabledDao<Taxon, TaxonValueObject> implements TaxonDao {

    @Autowired
    public TaxonDaoImpl( SessionFactory sessionFactory ) {
        super( TaxonDao.OBJECT_ALIAS, Taxon.class, sessionFactory );
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
    public Taxon findByNcbiId( Integer ncbiId ) {
        return this.findOneByProperty( "ncbiId", ncbiId );
    }

    @Override
    protected TaxonValueObject doLoadValueObject( Taxon entity ) {
        return new TaxonValueObject( entity );
    }

    @Override
    protected Query getLoadValueObjectsQuery( @Nullable Filters filters, @Nullable Sort sort, EnumSet<QueryHint> hints ) {
        //noinspection JpaQlInspection // the constants for aliases is messing with the inspector
        //language=HQL
        String queryString = MessageFormat.format( "select {0} "
                + "from Taxon as {0} " // taxon
                + "left join {0}.externalDatabase as ED " // external db
                + "where {0}.id is not null ", getObjectAlias() ); // needed to use formRestrictionCause()

        queryString += ObjectFilterQueryUtils.formRestrictionAndGroupByAndOrderByClauses( filters, null, sort );

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        if ( filters != null ) {
            ObjectFilterQueryUtils.addRestrictionParameters( query, filters );
        }

        return query;
    }

    @Override
    protected Query getCountValueObjectsQuery( @Nullable Filters filters ) {
        //noinspection JpaQlInspection // the constants for aliases is messing with the inspector
        //language=HQL
        String queryString = MessageFormat.format( "select count({0}) "
                + "from Taxon as {0} " // taxon
                + "left join {0}.externalDatabase as ED " // external db
                + "where {0}.id is not null ", getObjectAlias() ); // needed to use formRestrictionCause()

        if ( filters != null ) {
            queryString += ObjectFilterQueryUtils.formRestrictionClause( filters );
        }

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        if ( filters != null ) {
            ObjectFilterQueryUtils.addRestrictionParameters( query, filters );
        }

        return query;
    }
}