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
package ubic.gemma.persistence.service.association;

import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.BusinessKey;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.association.Gene2GOAssociation
 */
@Repository
public class Gene2GOAssociationDaoImpl extends Gene2GOAssociationDaoBase {

    /* ********************************
     * Constructors
     * ********************************/

    @Autowired
    public Gene2GOAssociationDaoImpl( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    /* ********************************
     * Public methods
     * ********************************/

    @Override
    public Gene2GOAssociation find( Gene2GOAssociation gene2GOAssociation ) {
        BusinessKey.checkValidKey( gene2GOAssociation );
        Criteria queryObject = super.getSessionFactory().getCurrentSession().createCriteria( Gene2GOAssociation.class );
        BusinessKey.addRestrictions( queryObject, gene2GOAssociation );
        return ( Gene2GOAssociation ) queryObject.uniqueResult();
    }

    @Override
    public Map<Gene, Collection<VocabCharacteristic>> findByGenes( Collection<Gene> needToFind ) {
        Map<Gene, Collection<VocabCharacteristic>> result = new HashMap<>();
        StopWatch timer = new StopWatch();
        timer.start();
        int batchSize = 200;
        Set<Gene> batch = new HashSet<>();
        int i = 0;
        for ( Gene gene : needToFind ) {
            batch.add( gene );
            if ( batch.size() == batchSize ) {
                result.putAll( fetchBatch( batch ) );
                batch.clear();
            }
            if ( ++i % 1000 == 0 ) {
                log.info( "Fetched GO associations for " + i + "/" + needToFind.size() + " genes" );
            }
        }
        if ( !batch.isEmpty() )
            result.putAll( fetchBatch( batch ) );

        if ( timer.getTime() > 1000 ) {
            log.info( "Fetched GO annotations for " + needToFind.size() + " genes in " + timer.getTime() + "ms" );
        }
        return result;
    }

    @Override
    public Map<Taxon, Collection<Gene>> findByGoTermsPerTaxon( Collection<String> termsToFetch ) {
        Collection<Gene> genes = this.getGenes( termsToFetch );
        Map<Taxon, Collection<Gene>> results = new HashMap<>();

        for ( Gene g : genes ) {
            if ( !results.containsKey( g.getTaxon() ) ) {
                results.put( g.getTaxon(), new HashSet<Gene>() );
            }
            results.get( g.getTaxon() ).add( g );
        }
        return results;
    }

    @Override
    public Collection<Gene> getGenes( Collection<String> ids ) {
        //noinspection unchecked
        return this.getSession().createQuery( "select distinct geneAss.gene from Gene2GOAssociationImpl as geneAss  "
                + "where geneAss.ontologyEntry.value in ( :goIDs)" ).setParameterList( "goIDs", ids ).list();
    }

    @Override
    public Collection<Gene> getGenes( Collection<String> ids, Taxon taxon ) {
        if ( taxon == null )
            return getGenes( ids );

        //noinspection unchecked
        return this.getSession().createQuery(
                "select distinct  " + "  gene from Gene2GOAssociationImpl as geneAss join geneAss.gene as gene "
                        + "where geneAss.ontologyEntry.value in ( :goIDs) and gene.taxon = :tax" )
                .setParameterList( "goIDs", ids ).setParameter( "tax", taxon ).list();
    }

    @Override
    public Map<String, Collection<Gene>> getSets( Collection<String> ids ) {
        final String queryString = "select distinct geneAss.ontologyEntry.value, "
                + "geneAss.gene from Gene2GOAssociationImpl as geneAss  "
                + "where geneAss.ontologyEntry.value in ( :goIDs)";

        Map<String, Collection<Gene>> result = new HashMap<>();
        List<?> list = this.getSession().createQuery( queryString ).setParameterList( "goIDs", ids ).list();

        for ( Object o : list ) {
            Object[] oa = ( Object[] ) o;
            if ( !result.containsKey( oa[0] ) ) {
                result.put( ( String ) oa[0], new HashSet<Gene>() );
            }
            result.get( oa[0] ).add( ( Gene ) oa[1] );
        }

        return result;
    }

    /* ********************************
     * Protected methods
     * ********************************/

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Gene2GOAssociation> handleFindAssociationByGene( Gene gene ) {
        return this.findByProperty( "gene", gene );
    }

    @Override
    protected Collection<VocabCharacteristic> handleFindByGene( Gene gene ) {
        //noinspection unchecked
        return this.getSession().createQuery(
                "select distinct geneAss.ontologyEntry from Gene2GOAssociationImpl as geneAss  where geneAss.gene = :gene" )
                .setParameter( "gene", gene ).list();
    }

    @Override
    protected Collection<Gene> handleFindByGoTerm( String goId ) {
        //noinspection unchecked
        return super.getSession().createQuery( "select distinct geneAss.gene from Gene2GOAssociationImpl as geneAss  "
                + "where geneAss.ontologyEntry.value = :goID" ).setParameter( "goID", goId.replaceFirst( ":", "_" ) )
                .list();
    }

    @Override
    protected Collection<Gene> handleFindByGoTerm( String goId, Taxon taxon ) {
        //noinspection unchecked
        return super.getSession().createQuery( "select distinct geneAss.gene from Gene2GOAssociationImpl as geneAss  "
                + "where geneAss.ontologyEntry.value = :goID and geneAss.gene.taxon = :taxon" )
                .setParameter( "goID", goId.replaceFirst( ":", "_" ) ).setParameter( "taxon", taxon ).list();
    }

    @Override
    protected Collection<Gene> handleFindByGOTerm( Collection<String> goTerms, Taxon taxon ) {
        if ( goTerms.size() == 0 )
            return new HashSet<>();

        //noinspection unchecked
        return this.getSession().createQuery( "select distinct geneAss.gene from Gene2GOAssociationImpl as geneAss"
                + "  where geneAss.ontologyEntry.valueUri in (:goIDs) and geneAss.gene.taxon = :taxon" )
                .setParameter( "goIDs", goTerms ).setParameter( "taxon", taxon ).list();
    }

    @Override
    protected void handleRemoveAll() {

        int total = 0;
        Session session = this.getSession();

        while ( true ) {
            Query q = session.createQuery( "from Gene2GOAssociationImpl" );
            q.setMaxResults( 10000 );
            List<?> list = q.list();
            if ( list.isEmpty() )
                break;

            total += list.size();

            this.getHibernateTemplate().deleteAll( list );
            log.info( "Deleted " + total + " so far..." );
        }

        log.info( "Deleted: " + total );
    }

    /* ********************************
     * Private methods
     * ********************************/

    private Map<? extends Gene, ? extends Collection<VocabCharacteristic>> fetchBatch( Set<Gene> batch ) {
        Map<Long, Gene> giMap = EntityUtils.getIdMap( batch );
        final String queryString = "select g.id, geneAss.ontologyEntry from Gene2GOAssociationImpl as geneAss join geneAss.gene g where g.id in (:genes)";
        Map<Gene, Collection<VocabCharacteristic>> results = new HashMap<>();
        Query query = this.getHibernateTemplate().getSessionFactory().getCurrentSession().createQuery( queryString );
        query.setFetchSize( batch.size() );
        query.setParameterList( "genes", giMap.keySet() );
        List<?> o = query.list();

        for ( Object object : o ) {
            Object[] oa = ( Object[] ) object;
            Long g = ( Long ) oa[0];
            VocabCharacteristic vc = ( VocabCharacteristic ) oa[1];
            Gene gene = giMap.get( g );
            assert gene != null;
            if ( !results.containsKey( gene ) ) {
                results.put( gene, new HashSet<VocabCharacteristic>() );
            }
            results.get( gene ).add( vc );
        }

        return results;
    }

}