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
package ubic.gemma.model.genome.biosequence;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.genome.biosequence.BioSequence
 */
public class BioSequenceDaoImpl extends ubic.gemma.model.genome.biosequence.BioSequenceDaoBase {

    private static Log log = LogFactory.getLog( BioSequenceDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#find(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @SuppressWarnings("unchecked")
    @Override
    public BioSequence find( BioSequence bioSequence ) {

        BusinessKey.checkValidKey( bioSequence );

        try {

            Criteria queryObject = BusinessKey.createQueryObject( this.getSession( false ), bioSequence );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    debug( results );

                    // Try to find the best match. See BusinessKey for more explanation of why this is needed.
                    BioSequence match = null;
                    for ( BioSequence res : ( Collection<BioSequence> ) results ) {
                        if ( res.equals( bioSequence ) ) {
                            if ( match != null ) {
                                log.warn( "More than one sequence in the database matches " + bioSequence );
                                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                                        "More than one instance of '" + BioSequence.class.getName()
                                                + "' was found when executing query for " + bioSequence );
                            }
                            match = res;
                        }
                    }

                    return match;

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( BioSequence ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#findOrCreate(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    public BioSequence findOrCreate( BioSequence bioSequence ) {
        BioSequence existingBioSequence = this.find( bioSequence );
        if ( existingBioSequence != null ) {
            return existingBioSequence;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new: " + bioSequence );
        return ( BioSequence ) create( bioSequence );
    }

    /**
     * @param results
     */
    private void debug( List results ) {
        StringBuilder sb = new StringBuilder();
        sb.append( "\nMultiple BioSequences found matching query:\n" );
        for ( Object object : results ) {
            BioSequence entity = ( BioSequence ) object;
            sb.append( "\tID=" + entity.getId() + " Name=" + entity.getName() );
            if ( StringUtils.isNotBlank( entity.getSequence() ) )
                sb.append( " Sequence="
                        + entity.getSequence().substring( 0, Math.min( 10, entity.getSequence().length() - 1 ) )
                        + " ..." );
            if ( entity.getSequenceDatabaseEntry() != null )
                sb.append( " acc=" + entity.getSequenceDatabaseEntry().getAccession() );
            sb.append( "\n" );
        }
        log.info( sb.toString() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#handleGetGenesByAccession(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetGenesByAccession( String search ) throws Exception {
        Collection<Gene> genes = null;
        final String queryString = "select distinct gene from GeneImpl as gene inner join gene.products gp,  BioSequence2GeneProductImpl as bs2gp"
                + " inner join bs2gp.bioSequence bs "
                + "inner join bs.sequenceDatabaseEntry de where gp=bs2gp.geneProduct " + " and de.accession = :search ";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setString( "search", search );
            genes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return genes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#handleGetGenesByName(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetGenesByName( String search ) throws Exception {
        Collection<Gene> genes = null;
        final String queryString = "select distinct gene from GeneImpl as gene inner join gene.products gp,  BioSequence2GeneProductImpl as bs2gp where gp=bs2gp.geneProduct "
                + " and bs2gp.bioSequence.name like :search ";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setString( "search", search );
            genes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return genes;
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from BioSequenceImpl";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    protected Collection handleFindByName( String name ) throws Exception {
        if ( name == null ) return null;
        final String query = "from BioSequenceImpl b where b.name = :name";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( query );
            queryObject.setParameter( "name", name );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public BioSequence findByAccession( DatabaseEntry databaseEntry ) {
        BusinessKey.checkValidKey( databaseEntry );
        try {

            Criteria queryObject = super.getSession( false ).createCriteria( BioSequence.class );

            BusinessKey.attachCriteria( queryObject, databaseEntry, "sequenceDatabaseEntry" );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    debug( results );
                    log.warn( "More than one instance of '" + BioSequence.class.getName()
                            + "' was found when executing query" );

                    // favor the one with name matching the accession.
                    for ( Object object : results ) {
                        BioSequence bs = ( BioSequence ) object;
                        if ( bs.getName().equals( databaseEntry.getAccession() ) ) {
                            return bs;
                        }
                    }

                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "No real match frond for  '" + DatabaseEntry.class.getName()
                                    + "' was found when executing query for " + databaseEntry );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                } else {
                    return null;
                }
            }
            return ( BioSequence ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#handleLoad(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleLoad( Collection ids ) throws Exception {
        Collection<BioSequence> bioSequences = null;
        final String queryString = "select distinct bs from BioSequenceImpl bs where bs.id in (:ids)";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            bioSequences = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return bioSequences;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#handleThaw(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    protected void handleThaw( final BioSequence bioSequence ) throws Exception {
        if ( bioSequence == null ) return;
        if ( bioSequence.getId() == null ) return;
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.update( bioSequence );
                bioSequence.getBioSequence2GeneProduct().size();

                bioSequence.getSequenceDatabaseEntry();
                session.update( bioSequence.getTaxon() );

                DatabaseEntry dbEntry = bioSequence.getSequenceDatabaseEntry();

                if ( dbEntry != null ) {
                    session.update( dbEntry );
                    session.update( dbEntry.getExternalDatabase() );
                }

                for ( BioSequence2GeneProduct bs2gp : bioSequence.getBioSequence2GeneProduct() ) {
                    GeneProduct geneProduct = bs2gp.getGeneProduct();
                    Gene g = geneProduct.getGene();
                    if ( g != null ) {
                        g.getAliases().size();
                    }
                }

                session.evict( bioSequence );
                return null;
            }
        }, true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#handleThaw(java.util.Collection)
     */
    @Override
    protected void handleThaw( final Collection bioSequences ) throws Exception {
        doThaw( bioSequences, true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceDaoBase#handleThaw(java.util.Collection)
     */
    @Override
    protected void handleThawLite( final Collection bioSequences ) throws Exception {
        doThaw( bioSequences, false );
    }

    /**
     * @param bioSequences
     * @param deep
     */
    private void doThaw( final Collection bioSequences, final boolean deep ) {
        if ( bioSequences == null ) return;
        if ( bioSequences.size() == 0 ) return;
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.setFetchSize( 300 );
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.setCacheMode( CacheMode.IGNORE ); // Don't hit the secondary cache
                session.setFlushMode( FlushMode.MANUAL ); // We're READ-ONLY so this is okay.
                int count = 0;
                for ( Object object : bioSequences ) {
                    BioSequence bioSequence = ( BioSequence ) object;
                    session.update( bioSequence );

                    if ( deep ) {
                        bioSequence.getTaxon();
                        bioSequence.getBioSequence2GeneProduct().size();
                    }

                    DatabaseEntry dbEntry = bioSequence.getSequenceDatabaseEntry();
                    if ( dbEntry != null ) {
                        session.update( dbEntry );
                        session.update( dbEntry.getExternalDatabase() );
                    }

                    if ( ++count % 2000 == 0 ) {
                        log.info( "Thawed " + count + " sequences ..." );
                    }

                }
                return null;
            }
        }, true );
    }
}
