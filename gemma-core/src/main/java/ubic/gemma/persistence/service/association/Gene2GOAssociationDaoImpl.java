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
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.hibernate.HibernateUtils;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.BusinessKey;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.QueryUtils;

import javax.annotation.Nullable;
import java.util.*;

import static ubic.gemma.persistence.util.QueryUtils.batchIdentifiableParameterList;
import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

/**
 * @author pavlidis
 * @see    ubic.gemma.model.association.Gene2GOAssociation
 */
@Repository
public class Gene2GOAssociationDaoImpl extends AbstractDao<Gene2GOAssociation> implements Gene2GOAssociationDao {

    private final int geneBatchSize;

    @Autowired
    protected Gene2GOAssociationDaoImpl( SessionFactory sessionFactory ) {
        super( Gene2GOAssociation.class, sessionFactory );
        this.geneBatchSize = HibernateUtils.getBatchSize( sessionFactory, sessionFactory.getClassMetadata( Gene.class ) );
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
    public Collection<Gene2GOAssociation> findAssociationByGenes( Collection<Gene> genes ) {
        return this.findByPropertyIn( "gene", genes );
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
        int i = 0;
        for ( Collection<Gene> batch : batchIdentifiableParameterList( needToFind, geneBatchSize ) ) {
            Map<Long, Gene> giMap = EntityUtils.getIdMap( batch );
            //noinspection unchecked
            List<Object[]> o = this.getSessionFactory().getCurrentSession()
                    .createQuery( "select g.id, geneAss.ontologyEntry from Gene2GOAssociation as geneAss join geneAss.gene g where g.id in (:genes)" )
                    .setParameterList( "genes", giMap.keySet() )
                    .list();
            for ( Object[] object : o ) {
                Long g = ( Long ) object[0];
                Characteristic vc = ( Characteristic ) object[1];
                Gene gene = giMap.get( g );
                assert gene != null;
                result.computeIfAbsent( gene, k -> new HashSet<>() ).add( vc );
            }
            if ( ++i % 1000 == 0 ) {
                AbstractDao.log.info( "Fetched GO associations for " + i + "/" + needToFind.size() + " genes" );
            }
        }
        if ( timer.getTime() > 1000 ) {
            AbstractDao.log
                    .info( "Fetched GO annotations for " + needToFind.size() + " genes in " + timer.getTime() + " ms" );
        }
        return result;
    }

    @Override
    public Collection<Gene> findByGoTerms( Collection<String> goIds ) {
        if ( goIds.isEmpty() ) {
            return Collections.emptyList();
        }
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct geneAss.gene from Gene2GOAssociation as geneAss  "
                                + "where geneAss.ontologyEntry.value in ( :goIDs)" )
                .setParameterList( "goIDs", optimizeParameterList( goIds ) ).list();
    }

    @Override
    public Collection<Gene> findByGoTerms( Collection<String> goIds, @Nullable Taxon taxon ) {
        if ( goIds.isEmpty() ) {
            return Collections.emptyList();
        }
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct gene from Gene2GOAssociation as geneAss join geneAss.gene as gene "
                                + "where geneAss.ontologyEntry.value in ( :goIDs) and gene.taxon = :tax" )
                .setParameterList( "goIDs", optimizeParameterList( goIds ) )
                .setParameter( "tax", taxon )
                .list();
    }

    @Override
    public Map<Taxon, Collection<Gene>> findByGoTermsPerTaxon( Collection<String> termsToFetch ) {
        Collection<Gene> genes = this.findByGoTerms( termsToFetch );
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
    public int removeAll() {
        //noinspection unchecked
        List<Long> cIds = getSessionFactory().getCurrentSession()
                .createQuery( "select c.id from Gene2GOAssociation g2g join g2g.ontologyEntry c" )
                .list();
        int removedAssociations = this.getSessionFactory().getCurrentSession()
                .createQuery( "delete from Gene2GOAssociation" )
                .executeUpdate();
        int removedCharacteristics;
        if ( !cIds.isEmpty() ) {
            Query query = getSessionFactory().getCurrentSession()
                    .createQuery( "delete from Characteristic where id in :cIds" );
            removedCharacteristics = QueryUtils.executeUpdateByBatch( query, "cIds", cIds, 2048 );
        } else {
            removedCharacteristics = 0;
        }
        log.debug( String.format( "Removed all %d Gene2GOAssociation. %d Characteristic were removed in cascade.",
                removedAssociations, removedCharacteristics ) );
        return removedAssociations;
    }
}