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
     */
    @SuppressWarnings("unchecked")
    public static Map<ArrayDesign, Collection<ExpressionExperiment>> getArrayDesignsUsed(
            Collection<ExpressionExperiment> ees, Session session ) {
        Map<ArrayDesign, Collection<ExpressionExperiment>> eeAdMap = new HashMap<ArrayDesign, Collection<ExpressionExperiment>>();

        // Safety 1st....
        if ( ees == null || ees.isEmpty() ) return eeAdMap;

        final String eeAdQuery = "select distinct ee,ad from ExpressionExperimentImpl as ee inner join "
                + "ee.bioAssays b inner join b.arrayDesignUsed ad fetch all properties where ee in (:ees)";

        org.hibernate.Query queryObject = session.createQuery( eeAdQuery );
        queryObject.setCacheable( true );
        queryObject.setParameterList( "ees", ees );

        List qr = queryObject.list();
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

        /*
         * TODO should there be a constraint on taxon for the array design?
         */
        final String csQueryString = "select distinct cs from GeneImpl as gene"
                + " inner join gene.products gp, BlatAssociationImpl ba, CompositeSequenceImpl cs "
                + " where ba.bioSequence=cs.biologicalCharacteristic and ba.geneProduct = gp and  gene = :gene ";

        org.hibernate.Query queryObject = session.createQuery( csQueryString );
        queryObject.setParameter( "gene", gene );
        return queryObject.list();
    }

    /**
     * @param genes
     * @return
     */
    public static Map<Long, Collection<Long>> getCs2GeneIdMap( Collection<Long> genes, Session session ) {

        Map<Long, Collection<Long>> cs2genes = new HashMap<Long, Collection<Long>>();

        String queryString = "SELECT CS as csid, GENE as geneId FROM GENE2CS g WHERE g.GENE in (:geneIds)";
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
     * @return
     */
    public static Map<CompositeSequence, Collection<Gene>> getCs2GeneMap( Collection<Gene> genes, Session session ) {

        final String csQueryString = "select distinct cs, gene from GeneImpl as gene"
                + " inner join gene.products gp, BlatAssociationImpl ba, CompositeSequenceImpl cs "
                + " where ba.bioSequence=cs.biologicalCharacteristic and ba.geneProduct = gp and gene in (:genes)";

        Map<CompositeSequence, Collection<Gene>> cs2gene = new HashMap<CompositeSequence, Collection<Gene>>();
        org.hibernate.Query queryObject = session.createQuery( csQueryString );
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
    //
    // /**
    // * @param auditTrail
    // * @param type
    // * @param session
    // * @return
    // * @throws java.lang.Exception
    // */
    // @SuppressWarnings("unchecked")
    // public static ubic.gemma.model.common.auditAndSecurity.AuditEvent getLastAuditEvent( final AuditTrail auditTrail,
    // AuditEventType type, Session session ) throws java.lang.Exception {
    //
    // /*
    // * For the = operator to work in hibernate the class or class name can't be passed in as a parameter :type -
    // * also queryObject.setParameter("type", type.getClass()); doesn't work. Although technically this is now
    // * vunerable to an sql injection attack, it seems moot as an attacker would have to have access to the JVM to
    // * inject a malformed AuditEventType class name and if they had access to the JVM then sql injection is the
    // * least of our worries. The real annoyance here is dealing with subclasses of event types.
    // */
    //
    // List<String> classes = getEventTypeClassHierarchy( type.getClass(), session );
    //
    // final String queryString = "select event " + "from ubic.gemma.model.common.auditAndSecurity.AuditTrail trail "
    // + "inner join trail.events event inner join event.eventType et inner join fetch event.performer "
    // + "where trail.id = :trail " + "and et.class in (" + StringUtils.join( classes, "," )
    // + ") order by event.date desc ";
    //
    // org.hibernate.Query queryObject = session.createQuery( queryString );
    // queryObject.setCacheable( true );
    // queryObject.setCacheRegion( "auditEvents" );
    //
    // queryObject.setParameter( "trail", auditTrail.getId() );
    // queryObject.setMaxResults( 1 );
    //
    // Collection<AuditEvent> results = queryObject.list();
    //
    // if ( results == null || results.isEmpty() ) return null;
    //
    // return results.iterator().next();
    //
    // }
    //
    // /**
    // * @param auditable
    // * @param type
    // * @param session
    // * @return
    // * @throws java.lang.Exception
    // */
    // public static ubic.gemma.model.common.auditAndSecurity.AuditEvent getLastAuditEvent( final Auditable auditable,
    // AuditEventType type, Session session ) throws java.lang.Exception {
    // return getLastAuditEvent( auditable.getAuditTrail(), type, session );
    // }

}
