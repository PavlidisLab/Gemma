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

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.type.LongType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
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
     * @param ees     collection of expression experiments.
     * @param session session
     * @return map of array designs to the experiments they were used in.
     */
    public static Map<ArrayDesign, Collection<Long>> getArrayDesignsUsed( Collection<Long> ees, Session session ) {
        Map<ArrayDesign, Collection<Long>> eeAdMap = new HashMap<>();

        // Safety 1st....
        if ( ees == null || ees.isEmpty() )
            return eeAdMap;

        List<Object[]> qr = listByBatch( session.createQuery( "select ee.id, ad from ExpressionExperiment as ee "
                + "join ee.bioAssays b join b.arrayDesignUsed ad "
                + "where ee.id in (:ees) "
                + "group by ee, ad" ), "ees", ees, 2048 );
        for ( Object[] o : qr ) {
            Long ee = ( Long ) o[0];
            ArrayDesign ad = ( ArrayDesign ) o[1];
            if ( !eeAdMap.containsKey( ad ) ) {
                eeAdMap.put( ad, new HashSet<Long>() );
            }
            eeAdMap.get( ad ).add( ee );
        }

        return eeAdMap;
    }

    /**
     * @param ees     experiments
     * @param session session
     * @return map of experiment to collection of array design ids. If any of the ids given are for subsets, then the
     * key in the return value will be for the subset, not the source experiment (so it is consistent with the
     * input)
     */
    public static Map<Long, Collection<Long>> getArrayDesignsUsedEEMap( Collection<Long> ees, Session session ) {
        Map<Long, Collection<Long>> ee2ads = new HashMap<>();

        if ( ees == null || ees.isEmpty() )
            return ee2ads;

        final String eeAdQuery = "select distinct ee.id,ad.id from ExpressionExperiment as ee inner join "
                + "ee.bioAssays b inner join b.arrayDesignUsed ad where ee.id in (:ees)";

        Query queryObject = session.createQuery( eeAdQuery );
        queryObject.setParameterList( "ees", optimizeParameterList( ees ) );
        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );

        List<?> qr = queryObject.list();
        CommonQueries.addAllAds( ee2ads, qr );

        if ( ee2ads.size() < ees.size() ) {
            // ids might be invalid, but also might be subsets. Note that the output key is for the subset, not the
            // source.
            String subsetQuery =
                    "select distinct ees.id,ad.id from ExpressionExperimentSubSet as ees join ees.sourceExperiment ee "
                            + " join ee.bioAssays b join b.arrayDesignUsed ad where ees.id in (:ees)";
            //noinspection unchecked
            Collection<Long> possibleEEsubsets = ListUtils.removeAll( ees, ee2ads.keySet() );
            // note: CollectionUtils.removeAll has a bug.

            qr = session.createQuery( subsetQuery ).setParameterList( "ees", optimizeParameterList( possibleEEsubsets ) ).list();
            CommonQueries.addAllAds( ee2ads, qr );
        }

        return ee2ads;
    }

    private static void addAllAds( Map<Long, Collection<Long>> ee2ads, List<?> qr ) {
        for ( Object o : qr ) {
            Object[] ar = ( Object[] ) o;
            Long ee = ( Long ) ar[0];
            Long ad = ( Long ) ar[1];
            if ( !ee2ads.containsKey( ee ) ) {
                ee2ads.put( ee, new HashSet<Long>() );
            }
            ee2ads.get( ee ).add( ad );
        }
    }

    /**
     * @param session session
     * @param ee      experiment
     * @return list of array designs used in given expression experiment
     */
    public static Collection<ArrayDesign> getArrayDesignsUsed( ExpressionExperiment ee, Session session ) {
        return CommonQueries.getArrayDesignsUsed( ee.getId(), session );
    }

    /**
     * @param session session
     * @param eeId    experiment id
     * @return list of array designs used in given expression experiment
     */
    @SuppressWarnings("unchecked")
    private static Collection<ArrayDesign> getArrayDesignsUsed( Long eeId, Session session ) {
        List<?> list = CommonQueries.createGetADsUsedQueryObject( eeId, session ).list();
        /*
         * Thaw the TT.
         */
        for ( ArrayDesign ad : ( Collection<ArrayDesign> ) list ) {
            //noinspection ResultOfMethodCallIgnored // Only thawing
            ad.getTechnologyType();
        }
        return ( Collection<ArrayDesign> ) list;
    }

    /**
     * @param session session
     * @param eeId    experiment id
     * @return list of array designs IDs used in given expression experiment
     */
    @SuppressWarnings("unchecked")
    public static Collection<Long> getArrayDesignIdsUsed( Long eeId, Session session ) {
        final String eeAdQuery = "select distinct ad.id from ExpressionExperiment as ee inner join "
                + "ee.bioAssays b inner join b.arrayDesignUsed ad where ee.id = :eeId";

        Query queryObject = session.createQuery( eeAdQuery );
        queryObject.setCacheable( true );
        queryObject.setParameter( "eeId", eeId );
        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );

        List<?> list = queryObject.list();
        return ( Collection<Long> ) list;
    }

    /**
     * Given a gene, get all the composite sequences that map to it.
     *
     * @param session session
     * @param gene    gene
     * @return composite sequences
     */
    public static Collection<CompositeSequence> getCompositeSequences( Gene gene, Session session ) {

        final String csQueryString = "select distinct cs from Gene as gene"
                + " join gene.products gp, BioSequence2GeneProduct ba, CompositeSequence cs "
                + " where ba.bioSequence.id=cs.biologicalCharacteristic.id and ba.geneProduct.id = gp.id and gene.id = :gene ";

        Query queryObject = session.createQuery( csQueryString );
        queryObject.setParameter( "gene", gene.getId(), LongType.INSTANCE );
        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );
        //noinspection unchecked
        return queryObject.list();
    }

    private static Query createGetADsUsedQueryObject( Long eeId, Session session ) {
        final String eeAdQuery = "select distinct ad from ExpressionExperiment as ee inner join "
                + "ee.bioAssays b inner join b.arrayDesignUsed ad inner join ad.primaryTaxon fetch all properties where ee.id = :eeId";

        Query queryObject = session.createQuery( eeAdQuery );
        queryObject.setCacheable( true );
        queryObject.setParameter( "eeId", eeId );
        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );
        return queryObject;
    }

    private static void addGeneIds( Map<Long, Collection<Long>> cs2genes, Query queryObject ) {
        //noinspection unchecked
        List<Object[]> results = queryObject.list();
        for ( Object[] row : results ) {
            Long csid = ( Long ) row[0];
            Long geneId = ( Long ) row[1];
            cs2genes.computeIfAbsent( csid, k -> new HashSet<>() ).add( geneId );
        }
    }

    /**
     * @param session      session
     * @param genes        genes
     * @param arrayDesigns array design
     * @return map of probe IDs to collections of gene IDs.
     */
    public static Map<Long, Collection<Long>> getCs2GeneIdMap( Collection<Long> genes, Collection<Long> arrayDesigns,
            Session session ) {
        if ( genes.isEmpty() || arrayDesigns.isEmpty() ) {
            return Collections.emptyMap();
        }

        Query queryObject = session.createSQLQuery( "SELECT CS AS csid, GENE AS geneId FROM GENE2CS g WHERE g.GENE IN (:geneIds) AND g.AD IN (:ads)" )
                .addScalar( "csid", LongType.INSTANCE )
                .addScalar( "geneId", LongType.INSTANCE )
                .addSynchronizedQuerySpace( GENE2CS_QUERY_SPACE )
                .addSynchronizedEntityClass( ArrayDesign.class )
                .addSynchronizedEntityClass( CompositeSequence.class )
                .addSynchronizedEntityClass( Gene.class )
                .setParameterList( "ads", optimizeParameterList( arrayDesigns ) )
                .setReadOnly( true );

        Map<Long, Collection<Long>> cs2genes = new HashMap<>();
        for ( Collection<Long> batch : batchParameterList( genes, GENE2CS_BATCH_SIZE ) ) {
            CommonQueries.addGeneIds( cs2genes, queryObject.setParameterList( "geneIds", batch ) );
        }
        return cs2genes;
    }

    public static Map<CompositeSequence, Collection<Gene>> getCs2GeneMap( Collection<Gene> genes,
            Collection<ArrayDesign> arrayDesigns, Session session ) {

        StopWatch timer = new StopWatch();
        timer.start();
        final String csQueryString = "select distinct cs, gene from Gene as gene"
                + " inner join gene.products gp, BioSequence2GeneProduct ba, CompositeSequence cs "
                + " where ba.bioSequence=cs.biologicalCharacteristic and ba.geneProduct = gp"
                + " and gene in (:genes) and cs.arrayDesign in (:ads) ";

        Map<CompositeSequence, Collection<Gene>> cs2gene = new HashMap<>();
        Query queryObject = session.createQuery( csQueryString );
        queryObject.setCacheable( true );
        queryObject.setParameterList( "genes", optimizeIdentifiableParameterList( genes ) );
        queryObject.setParameterList( "ads", optimizeIdentifiableParameterList( arrayDesigns ) );
        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );

        CommonQueries.addGenes( cs2gene, queryObject );
        if ( timer.getTime() > 200 ) {
            CommonQueries.log.info( "Get cs2gene for " + genes.size() + " :" + timer.getTime() + "ms" );
        }
        return cs2gene;

    }

    /**
     * @param genes   genes
     * @param session session
     * @return map of probes to input genes they map to. Other genes those probes might detect are not included.
     */
    public static Map<CompositeSequence, Collection<Gene>> getCs2GeneMap( Collection<Gene> genes, Session session ) {

        StopWatch timer = new StopWatch();
        timer.start();
        final String csQueryString = "select distinct cs, gene from Gene as gene"
                + " inner join gene.products gp, BioSequence2GeneProduct ba, CompositeSequence cs "
                + " where ba.bioSequence=cs.biologicalCharacteristic and ba.geneProduct = gp"
                + " and gene in (:genes)  ";

        Map<CompositeSequence, Collection<Gene>> cs2gene = new HashMap<>();
        Query queryObject = session.createQuery( csQueryString );
        queryObject.setCacheable( true );
        queryObject.setParameterList( "genes", optimizeIdentifiableParameterList( genes ) );
        queryObject.setReadOnly( true );
        queryObject.setFlushMode( FlushMode.MANUAL );

        CommonQueries.addGenes( cs2gene, queryObject );
        if ( timer.getTime() > 200 ) {
            CommonQueries.log.info( "Get cs2gene for " + genes.size() + " :" + timer.getTime() + "ms" );
        }
        return cs2gene;
    }

    private static void addGenes( Map<CompositeSequence, Collection<Gene>> cs2gene, Query queryObject ) {
        //noinspection unchecked
        List<Object[]> results = queryObject.list();
        for ( Object[] row : results ) {
            CompositeSequence cs = ( CompositeSequence ) row[0];
            Gene g = ( Gene ) row[1];
            cs2gene.computeIfAbsent( cs, k -> new HashSet<>() ).add( g );
        }
    }

    /**
     * @param session session
     * @param probes  probes
     * @return map of probes to all the genes 'detected' by those probes. Probes that don't map to genes will have an
     * empty gene collection.
     */
    public static Map<Long, Collection<Long>> getCs2GeneMapForProbes( Collection<Long> probes, Session session ) {
        if ( probes.isEmpty() )
            return new HashMap<>();

        Query queryObject = session.createSQLQuery( "SELECT CS AS csid, GENE AS geneId FROM GENE2CS g WHERE g.CS IN (:probes) " )
                .addScalar( "csid", LongType.INSTANCE )
                .addScalar( "geneId", LongType.INSTANCE )
                .addSynchronizedQuerySpace( GENE2CS_QUERY_SPACE )
                .addSynchronizedEntityClass( ArrayDesign.class )
                .addSynchronizedEntityClass( CompositeSequence.class )
                .addSynchronizedEntityClass( Gene.class )
                .setReadOnly( true );

        Map<Long, Collection<Long>> cs2genes = new HashMap<>();
        for ( Collection<Long> batch : batchParameterList( probes, GENE2CS_BATCH_SIZE ) ) {
            CommonQueries.addGeneIds( cs2genes, queryObject.setParameterList( "probes", batch ) );
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
