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
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.VoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author pavlidis
 * @see Taxon
 */
@Repository
public class TaxonDaoImpl extends VoEnabledDao<Taxon, TaxonValueObject> implements TaxonDao {


    @Autowired
    public TaxonDaoImpl( SessionFactory sessionFactory ) {
        super( Taxon.class, sessionFactory );
    }



    @Override
    public Taxon find( Taxon taxon ) {

        BusinessKey.checkValidKey( taxon );

        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( Taxon.class ).setReadOnly( true );
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
    public Taxon findOrCreate( Taxon taxon ) {
        Taxon existingTaxon = find( taxon );
        if ( existingTaxon != null ) {
            if ( log.isDebugEnabled() )
                log.debug( "Found existing taxon: " + taxon );
            return existingTaxon;
        }

        if ( StringUtils.isBlank( taxon.getCommonName() ) && StringUtils.isBlank( taxon.getScientificName() ) ) {
            throw new IllegalArgumentException( "Cannot create a taxon without names: " + taxon );
        }

        log.warn( "Creating new taxon: " + taxon );
        return super.create( taxon );
    }

    @Override
    public Taxon findByAbbreviation( final String abbreviation ) {
        return this.findOneByStringProperty( "abbreviation", abbreviation );
    }

    @Override
    public Taxon findByCommonName( final String commonName ) {
        return this.findOneByStringProperty( "commonName", commonName );
    }

    @Override
    public Taxon findByScientificName( final String scientificName ) {
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
    public Collection<Taxon> findChildTaxaByParent( Taxon parentTaxon ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "from Taxon as taxon where taxon.parentTaxon = :parentTaxon" )
                .setParameter( "parentTaxon", parentTaxon ).list();
    }

    @Override
    public void thaw( final Taxon taxon ) {
        this.getSessionFactory().getCurrentSession().doWork( new Work() {
            @Override
            public void execute( Connection connection ) throws SQLException {
                getSession().buildLockRequest( LockOptions.NONE ).lock( taxon );
                Hibernate.initialize( taxon.getParentTaxon() );
                Hibernate.initialize( taxon.getExternalDatabase() );
                getSession().evict( taxon );
            }
        } );
    }

    @Override
    public TaxonValueObject loadValueObject( Taxon entity ) {
        return new TaxonValueObject( entity );
    }

    @Override
    public Collection<TaxonValueObject> loadValueObjects( Collection<Taxon> entities ) {
        Collection<TaxonValueObject> vos = new LinkedHashSet<TaxonValueObject>();
        for ( Taxon e : entities ) {
            vos.add( this.loadValueObject( e ) );
        }
        return vos;
    }
}