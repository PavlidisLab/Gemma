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
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.BusinessKey;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;

/**
 * @author pavlidis
 * @see    ubic.gemma.model.association.Gene2GOAssociation
 */
@Repository
public class Gene2GOAssociationDaoImpl extends AbstractDao<Gene2GOAssociation> implements Gene2GOAssociationDao {

    @Autowired
    protected Gene2GOAssociationDaoImpl( SessionFactory sessionFactory ) {
        super( Gene2GOAssociation.class, sessionFactory );
    }

    @Override
    public Gene2GOAssociation find( Gene2GOAssociation gene2GOAssociation ) {
        BusinessKey.checkValidKey( gene2GOAssociation );
        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( Gene2GOAssociation.class );
        BusinessKey.addRestrictions( queryObject, gene2GOAssociation );
        return ( Gene2GOAssociation ) queryObject.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Gene2GOAssociation> findAssociationByGene( Gene gene ) {
        return this.findByProperty( "gene", gene );
    }

    @Override
    public Collection<Characteristic> findByGene( Gene gene ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct geneAss.ontologyEntry from Gene2GOAssociation as geneAss  where geneAss.gene = :gene" )
                .setParameter( "gene", gene ).list();
    }

    @Override
    public Map<Gene, Collection<Characteristic>> findByGenes( Collection<Gene> needToFind ) {
        Map<Gene, Collection<Characteristic>> result = new HashMap<>();
        StopWatch timer = new StopWatch();
        timer.start();
        int batchSize = 200;
        Set<Gene> batch = new HashSet<>();
        int i = 0;
        for ( Gene gene : needToFind ) {
            batch.add( gene );
            if ( batch.size() == batchSize ) {
                result.putAll( this.fetchBatch( batch ) );
                batch.clear();
            }
            if ( ++i % 1000 == 0 ) {
                AbstractDao.log.info( "Fetched GO associations for " + i + "/" + needToFind.size() + " genes" );
            }
        }
        if ( !batch.isEmpty() )
            result.putAll( this.fetchBatch( batch ) );

        if ( timer.getTime() > 1000 ) {
            AbstractDao.log
                    .info( "Fetched GO annotations for " + needToFind.size() + " genes in " + timer.getTime() + "ms" );
        }
        return result;
    }

    @Override
    public Collection<Gene> findByGoTerm( String goId, Taxon taxon ) {
        //noinspection unchecked
        return super.getSessionFactory().getCurrentSession().createQuery(
                "select distinct geneAss.gene from Gene2GOAssociation as geneAss  "
                        + "where geneAss.ontologyEntry.value = :goID and geneAss.gene.taxon = :taxon" )
                .setParameter( "goID", goId.replaceFirst( ":", "_" ) ).setParameter( "taxon", taxon ).list();
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
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct geneAss.gene from Gene2GOAssociation as geneAss  "
                        + "where geneAss.ontologyEntry.value in ( :goIDs)" )
                .setParameterList( "goIDs", ids ).list();
    }

    @Override
    public Collection<Gene> getGenes( Collection<String> ids, Taxon taxon ) {
        if ( taxon == null )
            return this.getGenes( ids );

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct  " + "  gene from Gene2GOAssociation as geneAss join geneAss.gene as gene "
                        + "where geneAss.ontologyEntry.value in ( :goIDs) and gene.taxon = :tax" )
                .setParameterList( "goIDs", ids ).setParameter( "tax", taxon ).list();
    }

    @Override
    public void removeAll() {

        int total = 0;
        Session session = this.getSessionFactory().getCurrentSession();

        while ( true ) {
            Query q = session.createQuery( "from Gene2GOAssociation" );
            q.setMaxResults( 10000 );
            //noinspection unchecked
            List<Gene2GOAssociation> list = q.list();
            if ( list.isEmpty() )
                break;

            total += list.size();

            remove( list );
            AbstractDao.log.info( "Deleted " + total + " so far..." );
        }

        AbstractDao.log.info( "Deleted: " + total );
    }

    private Map<? extends Gene, ? extends Collection<Characteristic>> fetchBatch( Set<Gene> batch ) {
        Map<Long, Gene> giMap = EntityUtils.getIdMap( batch );
        //language=HQL
        final String queryString = "select g.id, geneAss.ontologyEntry from Gene2GOAssociation as geneAss join geneAss.gene g where g.id in (:genes)";
        Map<Gene, Collection<Characteristic>> results = new HashMap<>();
        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );
        query.setFetchSize( batch.size() );
        query.setParameterList( "genes", giMap.keySet() );
        List<?> o = query.list();

        for ( Object object : o ) {
            Object[] oa = ( Object[] ) object;
            Long g = ( Long ) oa[0];
            Characteristic vc = ( Characteristic ) oa[1];
            Gene gene = giMap.get( g );
            assert gene != null;
            if ( !results.containsKey( gene ) ) {
                results.put( gene, new HashSet<Characteristic>() );
            }
            results.get( gene ).add( vc );
        }

        return results;
    }

}