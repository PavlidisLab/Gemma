/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.persistence.util;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.type.LongType;
import ubic.gemma.model.annotations.MayBeUninitialized;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Gene;

import java.util.*;

import static ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil.GENE2CS_BATCH_SIZE;
import static ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil.GENE2CS_QUERY_SPACE;
import static ubic.gemma.persistence.util.QueryUtils.*;

/**
 * Contains methods to perform 'common' queries that are needed across DAOs.
 *
 * @author paul
 */
public class CommonQueries {

    private static final Log log = LogFactory.getLog( CommonQueries.class.getName() );

    /**
     * Retrieve a list of array designs used in given expression experiment
     */
    public static Collection<ArrayDesign> getArrayDesignsUsed( BioAssaySet bas, Session session ) {
        //noinspection unchecked
        return session.createQuery( "select ad from ExpressionExperiment ee "
                        + "join ee.bioAssays ba "
                        + "join ba.arrayDesignUsed ad "
                        + "where ee = :ee "
                        + "group by ad" )
                .setParameter( "ee", getExperiment( bas ) )
                .list();
    }

    /**
     * Retrieve a list of array designs used by the given experiments.
     */
    public static Collection<ArrayDesign> getArrayDesignsUsed( Collection<? extends @MayBeUninitialized BioAssaySet> ees, Session session ) {
        // Safety 1st....
        if ( ees == null || ees.isEmpty() )
            return Collections.emptySet();
        return listByIdentifiableBatch( session.createQuery( "select ad from ExpressionExperiment as ee "
                + "join ee.bioAssays b join b.arrayDesignUsed ad "
                + "where ee in (:ees) "
                + "group by ad" ), "ees", getExperiments( ees ), 2048 );
    }

    private static Collection<@MayBeUninitialized ExpressionExperiment> getExperiments( Collection<? extends @MayBeUninitialized BioAssaySet> bioAssaySets ) {
        return bioAssaySets.stream().map( CommonQueries::getExperiment ).collect( IdentifiableUtils.toIdentifiableSet() );
    }

    private static ExpressionExperiment getExperiment( @MayBeUninitialized BioAssaySet bas ) {
        if ( bas instanceof ExpressionExperiment ) {
            return ( ExpressionExperiment ) bas;
        } else if ( bas instanceof ExpressionExperimentSubSet ) {
            return ( ( ExpressionExperimentSubSet ) bas ).getSourceExperiment();
        } else {
            throw new UnsupportedOperationException( "Couldn't handle a " + bas.getClass() );
        }
    }

    /**
     * Given a gene, get all the composite sequences that map to it.
     *
     * @param session session
     * @param gene    gene
     * @return composite sequences
     */
    public static Collection<CompositeSequence> getCompositeSequences( Gene gene, Session session ) {
        //noinspection unchecked
        return session.createQuery( "select cs from Gene as gene "
                        + "join gene.products gp, BioSequence2GeneProduct ba, CompositeSequence cs "
                        + "where ba.bioSequence = cs.biologicalCharacteristic and ba.geneProduct = gp and gene = :gene "
                        + "group by cs" )
                .setParameter( "gene", gene )
                .setCacheable( true )
                .list();
    }

    public static Map<CompositeSequence, Collection<Gene>> getCs2GeneMap( Collection<Gene> genes,
            Collection<ArrayDesign> arrayDesigns, Session session ) {
        if ( genes.isEmpty() || arrayDesigns.isEmpty() ) {
            return Collections.emptyMap();
        }
        StopWatch timer = StopWatch.createStarted();
        try {
            //noinspection unchecked
            return populateCs2GeneMap( session.createQuery( "select cs, gene from Gene as gene "
                            + "join gene.products gp, BioSequence2GeneProduct ba, CompositeSequence cs "
                            + "where ba.bioSequence=cs.biologicalCharacteristic and ba.geneProduct = gp and gene in (:genes) and cs.arrayDesign in (:ads) "
                            + "group by cs, gene" )
                    .setParameterList( "genes", optimizeIdentifiableParameterList( genes ) )
                    .setParameterList( "ads", optimizeIdentifiableParameterList( arrayDesigns ) )
                    .list() );
        } finally {
            if ( timer.getTime() > 200 ) {
                CommonQueries.log.info( "Get cs2gene for " + genes.size() + " :" + timer.getTime() + "ms" );
            }
        }
    }

    /**
     * @param genes   genes
     * @param session session
     * @return map of probes to input genes they map to. Other genes those probes might detect are not included.
     */
    public static Map<CompositeSequence, Collection<Gene>> getCs2GeneMap( Collection<Gene> genes, Session session ) {
        if ( genes.isEmpty() ) {
            return Collections.emptyMap();
        }
        StopWatch timer = StopWatch.createStarted();
        try {
            //noinspection unchecked
            return populateCs2GeneMap( session.createQuery( "select cs, gene from Gene as gene "
                            + "join gene.products gp, BioSequence2GeneProduct ba, CompositeSequence cs "
                            + "where ba.bioSequence=cs.biologicalCharacteristic and ba.geneProduct = gp and gene in (:genes) "
                            + "group by cs, gene" )
                    .setParameterList( "genes", optimizeIdentifiableParameterList( genes ) )
                    .list() );
        } finally {
            if ( timer.getTime() > 200 ) {
                CommonQueries.log.info( "Get cs2gene for " + genes.size() + " :" + timer.getTime() + "ms" );
            }
        }
    }

    private static Map<CompositeSequence, Collection<Gene>> populateCs2GeneMap( List<Object[]> results ) {
        Map<CompositeSequence, Collection<Gene>> cs2gene = new HashMap<>();
        for ( Object[] row : results ) {
            CompositeSequence cs = ( CompositeSequence ) row[0];
            Gene g = ( Gene ) row[1];
            cs2gene.computeIfAbsent( cs, k -> new HashSet<>() ).add( g );
        }
        return cs2gene;
    }

    /**
     * Obtain a mapping of probe to gene IDs for the given gene IDs and platforms IDs.
     */
    public static Map<Long, Collection<Long>> getCs2GeneIdMapForGenes( Collection<Long> genes, Collection<Long> arrayDesigns, Session session ) {
        if ( genes.isEmpty() || arrayDesigns.isEmpty() ) {
            return Collections.emptyMap();
        }
        return populateCsId2GeneIdMap( listByBatch( session
                .createSQLQuery( "SELECT CS AS csid, GENE AS geneId FROM GENE2CS g WHERE g.GENE IN (:geneIds) AND g.AD IN (:ads)" )
                .addScalar( "csid", LongType.INSTANCE )
                .addScalar( "geneId", LongType.INSTANCE )
                .addSynchronizedQuerySpace( GENE2CS_QUERY_SPACE )
                .addSynchronizedEntityClass( ArrayDesign.class )
                .addSynchronizedEntityClass( CompositeSequence.class )
                .addSynchronizedEntityClass( Gene.class )
                .setParameterList( "ads", optimizeParameterList( arrayDesigns ) ), "geneIds", genes, GENE2CS_BATCH_SIZE ) );
    }

    /**
     * @param session session
     * @param probes  probes
     * @return map of probes to all the genes 'detected' by those probes. Probes that don't map to genes will have an
     * empty gene collection.
     */
    public static Map<Long, Collection<Long>> getCs2GeneMapForProbes( Collection<Long> probes, Session session ) {
        if ( probes.isEmpty() ) {
            return Collections.emptyMap();
        }
        return populateCsId2GeneIdMap( listByBatch( session
                .createSQLQuery( "SELECT CS AS csid, GENE AS geneId FROM GENE2CS g WHERE g.CS IN (:probes) " )
                .addScalar( "csid", LongType.INSTANCE )
                .addScalar( "geneId", LongType.INSTANCE )
                .addSynchronizedQuerySpace( GENE2CS_QUERY_SPACE )
                .addSynchronizedEntityClass( ArrayDesign.class )
                .addSynchronizedEntityClass( CompositeSequence.class )
                .addSynchronizedEntityClass( Gene.class ), "probes", probes, GENE2CS_BATCH_SIZE ) );
    }

    private static Map<Long, Collection<Long>> populateCsId2GeneIdMap( List<Object[]> results ) {
        Map<Long, Collection<Long>> cs2genes = new HashMap<>();
        for ( Object[] row : results ) {
            Long csid = ( Long ) row[0];
            Long geneId = ( Long ) row[1];
            cs2genes.computeIfAbsent( csid, k -> new HashSet<>() ).add( geneId );
        }
        return cs2genes;
    }

    public static Collection<Long> filterProbesByPlatform( Collection<Long> probes, Collection<Long> arrayDesignIds,
            Session session ) {
        if ( probes.isEmpty() || arrayDesignIds.isEmpty() ) {
            return Collections.emptyList();
        }
        Query queryObject = session.createSQLQuery( "SELECT CS AS csid FROM GENE2CS WHERE AD IN (:adids) AND CS IN (:probes)" )
                .addScalar( "csid", LongType.INSTANCE )
                .addSynchronizedQuerySpace( GENE2CS_QUERY_SPACE )
                .addSynchronizedEntityClass( ArrayDesign.class )
                .addSynchronizedEntityClass( CompositeSequence.class )
                .addSynchronizedEntityClass( Gene.class )
                .setParameterList( "adids", optimizeParameterList( arrayDesignIds ), LongType.INSTANCE );
        List<Long> results = new ArrayList<>();
        for ( Collection<Long> batch : batchParameterList( probes, GENE2CS_BATCH_SIZE ) ) {
            //noinspection unchecked
            results.addAll( queryObject.setParameterList( "probes", batch, LongType.INSTANCE ).list() );
        }
        return results;
    }
}
