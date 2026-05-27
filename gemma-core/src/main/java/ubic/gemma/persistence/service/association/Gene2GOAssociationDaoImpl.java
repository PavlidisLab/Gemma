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
import org.hibernate.query.Query;
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
import ubic.gemma.persistence.util.IdentifiableUtils;
import ubic.gemma.persistence.util.QueryUtils;

import org.springframework.lang.Nullable;
import java.util.*;

import static ubic.gemma.persistence.util.QueryUtils.batchIdentifiableParameterList;
import static ubic.gemma.persistence.util.QueryUtils.listByBatch;

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
        this.geneBatchSize = HibernateUtils.getBatchSize( Gene.class, sessionFactory );
    }

    @Override
    public Gene2GOAssociation find( Gene2GOAssociation gene2GOAssociation ) {
        return BusinessKey.find( this.getSessionFactory().getCurrentSession(), gene2GOAssociation );
    }

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
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select geneAss.ontologyEntry from Gene2GOAssociation as geneAss "
                        + "where geneAss.gene = :gene "
                        + "group by geneAss.ontologyEntry" )
                .setParameter( "gene", gene )
                .list();
    }

    @Override
    public Map<Gene, Collection<Characteristic>> findByGenes( Collection<Gene> genes ) {
        Map<Gene, Collection<Characteristic>> result = new HashMap<>();
        StopWatch timer = new StopWatch();
        timer.start();
        int i = 0;
        for ( Collection<Gene> batch : batchIdentifiableParameterList( genes, geneBatchSize ) ) {
            Map<Long, Gene> giMap = IdentifiableUtils.getIdMap( batch );
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
                log.info( "Fetched GO associations for " + i + "/" + genes.size() + " genes" );
            }
        }
        if ( timer.getTime() > 1000 ) {
            log
                    .info( "Fetched GO annotations for " + genes.size() + " genes in " + timer.getTime() + " ms" );
        }
        return result;
    }

    @Override
    public Collection<Gene> findByGoTermUris( Collection<String> uris ) {
        if ( uris.isEmpty() ) {
            return Collections.emptyList();
        }
        return listByBatch( this.getSessionFactory().getCurrentSession().createQuery(
                "select geneAss.gene from Gene2GOAssociation as geneAss "
                        + "join geneAss.gene gene "
                        + "where geneAss.ontologyEntry.valueUri in (:uris) "
                        + "group by geneAss.gene" ), "uris", uris, 2048 );
    }

    @Override
    public Collection<Gene> findByGoTermUris( Collection<String> uris, @Nullable Taxon taxon ) {
        if ( uris.isEmpty() ) {
            return Collections.emptyList();
        }
        return listByBatch( this.getSessionFactory().getCurrentSession()
                .createQuery( "select gene from Gene2GOAssociation as geneAss "
                        + "join geneAss.gene as gene "
                        + "where geneAss.ontologyEntry.valueUri in (:uris) and gene.taxon = :tax "
                        + "group by gene" )
                .setParameter( "tax", taxon ), "uris", uris, 2048 );
    }

    @Override
    public long countByGoTermUris( Collection<String> uris ) {
        return distinctGeneIdsByGoUris( uris, null ).size();
    }

    @Override
    public long countByGoTermUris( Collection<String> uris, Taxon taxon ) {
        return distinctGeneIdsByGoUris( uris, taxon ).size();
    }

    /**
     * Resolve {@code uris} to a deduplicated set of gene ids, optionally taxon-scoped. Used by
     * both {@link #countByGoTermUris(Collection)} overloads. Returns IDs (not COUNT) so we can
     * union-dedup correctly when this layer needs it. Cheap over the wire: one Long per
     * gene-id, no entity hydration, no associations.
     * <p>
     * Single un-batched IN-list by design. An earlier version batched at 2048 URIs to keep
     * each fragment small, but EXPLAIN against prod showed that small IN-lists (2-4k entries)
     * push MySQL into a {@code CHARACTERISTIC.VALUE_URI} range scan that walks hundreds of
     * thousands of non-G2G characteristic rows; once the IN-list passes ~8k entries, the
     * planner switches to a full scan of {@code GENE2GO_ASSOCIATION} (~1.1M rows) and the
     * query drops from 5-8 s to ~2 s. Batching at 2048 was therefore pinning us in the
     * worst-plan zone of the optimizer. {@code max_allowed_packet} on prod is 256 MB; the
     * largest realistic IN-list (biological_process = ~24k URIs) is ~1 MB of SQL text.
     */
    private Set<Long> distinctGeneIdsByGoUris( Collection<String> uris, @Nullable Taxon taxon ) {
        if ( uris == null || uris.isEmpty() ) {
            return Collections.emptySet();
        }
        String hql = taxon != null
                ? "select distinct gene.id from Gene2GOAssociation as geneAss join geneAss.gene as gene "
                        + "where geneAss.ontologyEntry.valueUri in (:uris) and gene.taxon = :tax"
                : "select distinct gene.id from Gene2GOAssociation as geneAss join geneAss.gene as gene "
                        + "where geneAss.ontologyEntry.valueUri in (:uris)";
        org.hibernate.query.Query<Long> q = getSessionFactory().getCurrentSession()
                .createQuery( hql, Long.class )
                .setParameterList( "uris", uris );
        if ( taxon != null ) {
            q.setParameter( "tax", taxon );
        }
        return new HashSet<>( q.list() );
    }

    @Override
    public Map<Taxon, Collection<Gene>> findByGoTermUrisPerTaxon( Collection<String> uris ) {
        Collection<Gene> genes = this.findByGoTermUris( uris );
        Map<Taxon, Collection<Gene>> results = new HashMap<>();
        for ( Gene g : genes ) {
            if ( !results.containsKey( g.getTaxon() ) ) {
                results.put( g.getTaxon(), new HashSet<>() );
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