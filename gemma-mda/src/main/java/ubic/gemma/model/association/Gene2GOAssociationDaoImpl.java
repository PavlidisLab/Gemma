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
package ubic.gemma.model.association;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.BusinessKey;

/**
 * @see ubic.gemma.model.association.Gene2GOAssociation
 * @author pavlidis
 * @version $Id$
 */
@Repository
public class Gene2GOAssociationDaoImpl extends ubic.gemma.model.association.Gene2GOAssociationDaoBase {

    private static Log log = LogFactory.getLog( Gene2GOAssociationDaoImpl.class );

    @Autowired
    public Gene2GOAssociationDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.Gene2GOAssociationDaoBase#find(ubic.gemma.model.association.Gene2GOAssociation)
     */
    @Override
    public Gene2GOAssociation find( Gene2GOAssociation gene2GOAssociation ) {
        try {

            BusinessKey.checkValidKey( gene2GOAssociation );
            Criteria queryObject = super.getSession().createCriteria( Gene2GOAssociation.class );
            BusinessKey.addRestrictions( queryObject, gene2GOAssociation );

            java.util.List<?> results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {

                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException( results.size() + " "
                            + Gene2GOAssociation.class.getName() + "s were found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( Gene2GOAssociation ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.Gene2GOAssociationDaoBase#findOrCreate(ubic.gemma.model.association.Gene2GOAssociation
     * )
     */
    @Override
    public Gene2GOAssociation findOrCreate( Gene2GOAssociation gene2GOAssociation ) {
        Gene2GOAssociation existing = this.find( gene2GOAssociation );
        if ( existing != null ) {
            assert existing.getId() != null;
            return existing;
        }
        return create( gene2GOAssociation );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.Gene2GOAssociationDaoBase#handleFindAssociationByGene(ubic.gemma.model.genome.Gene)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Gene2GOAssociation> handleFindAssociationByGene( Gene gene ) {

        final String queryString = "from Gene2GOAssociationImpl where gene = :gene";
        List<?> g2go = this.getHibernateTemplate().findByNamedParam( queryString, "gene", gene );
        return ( Collection<Gene2GOAssociation> ) g2go;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.Gene2GOAssociationDaoBase#handleFindByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection<VocabCharacteristic> handleFindByGene( Gene gene ) {

        final String queryString = "select distinct geneAss.ontologyEntry from Gene2GOAssociationImpl as geneAss  where geneAss.gene = :gene";

        List<?> vo = this.getHibernateTemplate().findByNamedParam( queryString, "gene", gene );

        @SuppressWarnings("unchecked")
        Collection<VocabCharacteristic> result = ( Collection<VocabCharacteristic> ) vo;

        return result;
    }

    @Override
    protected Collection<Gene> handleFindByGoTerm( String goId, Taxon taxon ) {

        final String queryString = "select distinct geneAss.gene from Gene2GOAssociationImpl as geneAss  "
                + "where geneAss.ontologyEntry.value = :goID and geneAss.gene.taxon = :taxon";

        // need to turn the collection of goTerms into a collection of GOId's

        Collection<Gene> results;

        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
            queryObject.setParameter( "goID", goId.replaceFirst( ":", "_" ) );
            queryObject.setParameter( "taxon", taxon );

            results = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.Gene2GOAssociationDaoBase#handleFindByGOTerm(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleFindByGOTerm( Collection goTerms, Taxon taxon ) {
        Collection<String> goIDs = new HashSet<String>();
        if ( goTerms.size() == 0 ) return goIDs;

        final String queryString = "select distinct geneAss.gene from Gene2GOAssociationImpl as geneAss"
                + "  where geneAss.ontologyEntry.valueUri in (:goIDs) and geneAss.gene.taxon = :taxon";

        // need to turn the collection of goTerms into a collection of GOId's
        for ( Object obj : goTerms ) {
            VocabCharacteristic oe = ( VocabCharacteristic ) obj;
            goIDs.add( oe.getValueUri() );
        }
        return this.getHibernateTemplate().findByNamedParam( queryString, new String[] { "goIDs", "taxon" },
                new Object[] { goIDs, taxon } );
    }

    @Override
    protected void handleRemoveAll() {
        // this does not delete the associated vocabCharacteristics, though it is fast.
        // "No joins, either implicit or explicit, can be specified in a bulk HQL query"
        // http://docs.jboss.org/hibernate/core/3.3/reference/en/html/batch.html#batch-direct
        // final String queryString = "delete from Gene2GOAssociationImpl go ";
        // this.getHibernateTemplate().bulkUpdate( queryString );

        int total = 0;
        Session sess = getSession();

        // this should do the deletion, right? -- Confirmed. (PP)
        while ( true ) {
            Query q = sess.createQuery( "from Gene2GOAssociationImpl" );
            q.setMaxResults( 10000 );
            List<?> list = q.list();
            if ( list.isEmpty() ) break;

            total += list.size();

            this.getHibernateTemplate().deleteAll( list );
        }

        log.info( "Deleted: " + total );

    }

}