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
package ubic.gemma.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.type.LongType;

import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * Contains methods to perform 'common' queries that are needed across DAOs.
 * 
 * @author paul
 * @version $Id$
 */
public class CommonQueries {

    private static Log log = LogFactory.getLog( CommonQueries.class.getName() );

    /**
     * @param ees collection of expression experiments.
     * @return map of array designs to the experiments they were used in.
     */     public static Map<ArrayDesign, Collection<ExpressionExperiment>> getArrayDesignsUsed(
            Collection<ExpressionExperiment> ees, Session session ) {
        Map<ArrayDesign, Collection<ExpressionExperiment>> eeAdMap = new HashMap<ArrayDesign, Collection<ExpressionExperiment>>();

        // Safety 1st....
        if ( ees == null || ees.isEmpty() ) return eeAdMap;

        final String eeAdQuery = "select distinct ee,ad from ExpressionExperimentImpl as ee inner join "
                + "ee.bioAssays b inner join b.arrayDesignUsed ad fetch all properties where ee in (:ees)";

        org.hibernate.Query queryObject = session.createQuery( eeAdQuery );
        queryObject.setCacheable( true );
        queryObject.setParameterList( "ees", ees );

        List<?> qr = queryObject.list();
        for ( Object o : qr ) {
            Object[] ar = ( Object[] ) o;
            ExpressionExperiment ee = ( ExpressionExperiment ) ar[0];
            ArrayDesign ad = ( ArrayDesign ) ar[1];
            if ( !eeAdMap.containsKey( ad ) ) {
                eeAdMap.put( ad, new HashSet<ExpressionExperiment>() );
            }
            eeAdMap.get( ad ).add( ee );
        }

        return eeAdMap;
    }

    /**
     * @param ees collection of expression experiments.
     * @return map of array designs to the experiments they were used in.
     */
    @SuppressWarnings("unchecked")
    public static Collection<ArrayDesign> getArrayDesignsUsed( ExpressionExperiment ee, Session session ) {

        if ( ee == null ) {
            return null;
        }

        final String eeAdQuery = "select distinct ad from ExpressionExperimentImpl as ee inner join "
                + "ee.bioAssays b inner join b.arrayDesignUsed ad fetch all properties where ee = :ee";

        org.hibernate.Query queryObject = session.createQuery( eeAdQuery );
        queryObject.setCacheable( true );
        queryObject.setParameter( "ee", ee );
        List list = queryObject.list();
        /*
         * Thaw the TT.
         */
        for ( ArrayDesign ad : ( Collection<ArrayDesign> ) list ) {
            ad.getTechnologyType();
        }
        return list;
    }

    /**
     * Given a gene, get all the composite sequences that map to it.
     * 
     * @param gene
     * @param session
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Collection<CompositeSequence> getCompositeSequences( Gene gene, Session session ) {

        final String csQueryString = "select distinct cs from GeneImpl as gene"
                + " join gene.products gp, BioSequence2GeneProductImpl ba, CompositeSequenceImpl cs "
                + " where ba.bioSequence=cs.biologicalCharacteristic and ba.geneProduct = gp and gene = :gene ";

        org.hibernate.Query queryObject = session.createQuery( csQueryString );
        queryObject.setParameter( "gene", gene );
        return queryObject.list();
    }

    /**
     * Given gene ids, get map of probes to genes for each probe --- starting from genes. The values will only contain
     * genes that were given, and is not filled in with other genes the probes may detect.
     * 
     * @param genes
     * @return
     */
    public static Map<Long, Collection<Long>> getCs2GeneIdMap( Collection<Long> genes, Session session ) {

        Map<Long, Collection<Long>> cs2genes = new HashMap<Long, Collection<Long>>();

        String queryString = "SELECT DISTINCT CS as csid, GENE as geneId FROM GENE2CS g WHERE g.GENE in (:geneIds)";
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
        queryObject.addScalar( "csid", new LongType() );
        queryObject.addScalar( "geneId", new LongType() );

        queryObject.setParameterList( "geneIds", genes );
        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {
            Long csid = results.getLong( 0 );
            Long geneId = results.getLong( 1 );

            if ( !cs2genes.containsKey( csid ) ) {
                cs2genes.put( csid, new HashSet<Long>() );
            }
            cs2genes.get( csid ).add( geneId );
        }
        results.close();

        return cs2genes;

    }

    /**
     * @param genes
     * @param arrays restrict to probes on these arrays only
     * @param session
     * @return
     */
    public static Map<CompositeSequence, Collection<Gene>> getCs2GeneMap( Collection<Gene> genes,
            Collection<ArrayDesign> arrays, Session session ) {

        StopWatch timer = new StopWatch();
        timer.start();
        final String csQueryString = "select distinct cs, gene from GeneImpl as gene"
                + " inner join gene.products gp, BioSequence2GeneProductImpl ba, CompositeSequenceImpl cs "
                + " where ba.bioSequence=cs.biologicalCharacteristic and ba.geneProduct = gp"
                + " and gene in (:genes) and cs.arrayDesign in (:ars) ";

        Map<CompositeSequence, Collection<Gene>> cs2gene = new HashMap<CompositeSequence, Collection<Gene>>();
        org.hibernate.Query queryObject = session.createQuery( csQueryString );
        queryObject.setCacheable( true );
        queryObject.setParameterList( "genes", genes );
        queryObject.setParameterList( "ars", arrays );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {
            CompositeSequence cs = ( CompositeSequence ) results.get( 0 );
            Gene g = ( Gene ) results.get( 1 );
            if ( !cs2gene.containsKey( cs ) ) {
                cs2gene.put( cs, new HashSet<Gene>() );
            }
            cs2gene.get( cs ).add( g );
        }
        results.close();
        if ( timer.getTime() > 200 ) {
            log.info( "Get cs2gene for " + genes.size() + " on " + arrays.size() + " array platforms :"
                    + timer.getTime() + "ms" );
        }
        return cs2gene;
    }

    /**
     * @param genes
     * @param session
     * @return map of probes to input genes they map to. Other genes those probes might detect are not included.
     * @see getFullCs2GeneMap, which can be called on keyset of results of this method to get full mapping.
     */
    public static Map<CompositeSequence, Collection<Gene>> getCs2GeneMap( Collection<Gene> genes, Session session ) {

        StopWatch timer = new StopWatch();
        timer.start();
        final String csQueryString = "select distinct cs, gene from GeneImpl as gene"
                + " inner join gene.products gp, BioSequence2GeneProductImpl ba, CompositeSequenceImpl cs "
                + " where ba.bioSequence=cs.biologicalCharacteristic and ba.geneProduct = gp"
                + " and gene in (:genes)  ";

        Map<CompositeSequence, Collection<Gene>> cs2gene = new HashMap<CompositeSequence, Collection<Gene>>();
        org.hibernate.Query queryObject = session.createQuery( csQueryString );
        queryObject.setCacheable( true );
        queryObject.setParameterList( "genes", genes );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {
            CompositeSequence cs = ( CompositeSequence ) results.get( 0 );
            Gene g = ( Gene ) results.get( 1 );
            if ( !cs2gene.containsKey( cs ) ) {
                cs2gene.put( cs, new HashSet<Gene>() );
            }
            cs2gene.get( cs ).add( g );
        }
        results.close();
        if ( timer.getTime() > 200 ) {
            log.info( "Get cs2gene for " + genes.size() + " :" + timer.getTime() + "ms" );
        }
        return cs2gene;
    }

    /**
     * Determine the full set of AuditEventTypes that are needed (that is, subclasses of the given class)
     * 
     * @param type Class
     * @return A List of class names, including the given type.
     */
    public static List<String> getEventTypeClassHierarchy( Class<? extends AuditEventType> type, Session session ) {
        List<String> classes = new ArrayList<String>();
        classes.add( type.getCanonicalName() );

        // how to determine subclasses? There is no way to do this but the hibernate way.
        SingleTableEntityPersister classMetadata = ( SingleTableEntityPersister ) session.getSessionFactory()
                .getClassMetadata( type );
        if ( classMetadata == null ) return classes;

        if ( classMetadata.hasSubclasses() ) {
            String[] subclasses = classMetadata.getSubclassClosure(); // this includes the superclass, fully qualified
            // names.
            classes.clear();
            for ( String string : subclasses ) {
                string = string.replaceFirst( ".+\\.", "" );
                classes.add( string );
            }
        }
        return classes;
    }

    /**
     * @param probes
     * @param session
     * @return map of probes to all the genes 'detected' by those probes (including PARs and predicted genes, if these
     *         are in use). Probes that don't map to genes will have an empty gene collection.
     */
    public static Map<CompositeSequence, Collection<Gene>> getFullCs2AllGeneMap( Collection<CompositeSequence> probes,
            Session session ) {
        if ( probes.isEmpty() ) return new HashMap<CompositeSequence, Collection<Gene>>();
        final String csQueryString = "select distinct cs, gene from GeneImpl as gene"
                + " left outer join gene.products gp, BioSequence2GeneProductImpl ba, CompositeSequenceImpl cs "
                + " where ba.bioSequence=cs.biologicalCharacteristic and ba.geneProduct = gp and cs in (:probes) ";

        return getFullCs2GeneMap( probes, session, csQueryString );
    }

    /**
     * @param probes
     * @param session
     * @return map of probes to all the genes 'detected' by those probes -- but excluding PARs and predicted genes.
     *         Probes that don't map to genes will have an empty gene collection.
     */
    public static Map<CompositeSequence, Collection<Gene>> getFullCs2GeneMap( Collection<CompositeSequence> probes,
            Session session ) {
        if ( probes.isEmpty() ) return new HashMap<CompositeSequence, Collection<Gene>>();

        final String csQueryString = "select distinct cs, gene from GeneImpl as gene"
                + " left outer join gene.products gp, BioSequence2GeneProductImpl ba, CompositeSequenceImpl cs "
                + " where ba.bioSequence=cs.biologicalCharacteristic and ba.geneProduct = gp and cs in (:probes) and gene.class='GeneImpl'";

        return getFullCs2GeneMap( probes, session, csQueryString );
    }

    // Removed because it uses the Gene2CS table, which isn't 100% safe: it has to be updated.
    // /**
    // * Given gene ids, return map of of gene id -> probes for that gene.
    // *
    // * @param genes
    // * @param session
    // * @return
    // */
    // public static Map<Long, Collection<Long>> getGene2CSMap( Collection<Long> genes, Session session ) {
    // Map<Long, Collection<Long>> cs2genes = new HashMap<Long, Collection<Long>>();
    //
    // String queryString = "SELECT CS as csid, GENE as geneId FROM GENE2CS g WHERE g.GENE in (:geneIds)";
    // org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
    // queryObject.addScalar( "csid", new LongType() );
    // queryObject.addScalar( "geneId", new LongType() );
    //
    // queryObject.setParameterList( "geneIds", genes );
    // ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
    // while ( results.next() ) {
    // Long csid = results.getLong( 0 );
    // Long geneId = results.getLong( 1 );
    //
    // if ( !cs2genes.containsKey( geneId ) ) {
    // cs2genes.put( geneId, new HashSet<Long>() );
    // }
    // cs2genes.get( geneId ).add( csid );
    // }
    // results.close();
    //
    // return cs2genes;
    // }

    private static Map<CompositeSequence, Collection<Gene>> getFullCs2GeneMap( Collection<CompositeSequence> probes,
            Session session, final String csQueryString ) {

        if ( probes.isEmpty() ) return new HashMap<CompositeSequence, Collection<Gene>>();

        StopWatch timer = new StopWatch();
        timer.start();
        Map<CompositeSequence, Collection<Gene>> cs2gene = new HashMap<CompositeSequence, Collection<Gene>>();
        org.hibernate.Query queryObject = session.createQuery( csQueryString );
        queryObject.setCacheable( true );
        queryObject.setParameterList( "probes", probes );

        ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( results.next() ) {
            CompositeSequence cs = ( CompositeSequence ) results.get( 0 );
            Gene g = ( Gene ) results.get( 1 );
            if ( !cs2gene.containsKey( cs ) ) {
                cs2gene.put( cs, new HashSet<Gene>() );
            }
            cs2gene.get( cs ).add( g );
        }

        /*
         * This shouldn't be necessary if we do the correct outer join, should it?
         */
        for ( CompositeSequence cs : probes ) {
            if ( !cs2gene.containsKey( cs ) ) {
                cs2gene.put( cs, new HashSet<Gene>() );
            }
        }

        results.close();
        if ( timer.getTime() > 200 ) {
            log.info( "Get full cs2gene map for " + probes.size() + " :" + timer.getTime() + "ms" );
        }
        return cs2gene;
    }
}
