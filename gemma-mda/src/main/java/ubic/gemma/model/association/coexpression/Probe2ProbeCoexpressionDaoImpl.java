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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.association.coexpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.TaxonUtility;

/**
 * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression
 * @version $Id$
 * @author joseph
 * @author paul
 */
public class Probe2ProbeCoexpressionDaoImpl extends
        ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase {

    private static Log log = LogFactory.getLog( Probe2ProbeCoexpressionDaoImpl.class.getName() );

    /**
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#findCoexpressionRelationships(ubic.gemma.model.genome.Gene,
     *      java.util.Collection, ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @SuppressWarnings("unchecked")
    protected java.util.Collection handleFindCoexpressionRelationships( ubic.gemma.model.genome.Gene givenG,
            java.util.Collection ees, ubic.gemma.model.common.quantitationtype.QuantitationType qt ) {

        String p2pClassName;
        if ( TaxonUtility.isHuman( givenG.getTaxon() ) )
            p2pClassName = "HumanProbeCoExpressionImpl";
        else if ( TaxonUtility.isMouse( givenG.getTaxon() ) )
            p2pClassName = "MouseProbeCoExpressionImpl";
        else if ( TaxonUtility.isRat( givenG.getTaxon() ) )
            p2pClassName = "RatProbeCoExpressionImpl";
        else
            // must be other
            p2pClassName = "OtherProbeCoExpressionImpl";

        final String queryStringFirstVector =
        // source tables
        "select distinct p2pc.secondVector from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                // join table
                + p2pClassName
                + " as p2pc "
                // target tables
                + " where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and compositeSequence.designElementDataVectors.id=p2pc.firstVector.id "
                + " and p2pc.firstVector.expressionExperiment.id in (:collectionOfEE)"
                + " and p2pc.quantitationType.id = :givenQtId" + " and gene.id = :givenGId";

        final String queryStringSecondVector =
        // source tables
        "select distinct p2pc.firstVector from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                // join table
                + p2pClassName
                + " as p2pc "
                // target tables
                + " where gene.products.id=bs2gp.geneProduct.id "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and compositeSequence.designElementDataVectors.id=p2pc.secondVector.id "
                + " and p2pc.secondVector.expressionExperiment.id in (:collectionOfEE)"
                + " and p2pc.quantitationType.id = :givenQtId" + " and gene.id = :givenGId";

        Collection<DesignElementDataVector> dedvs = new HashSet<DesignElementDataVector>();

        try {
            // do query joining coexpressed genes through the firstVector to the secondVector
            Collection<Long> eeIds = new ArrayList<Long>();
            for ( Iterator iter = ees.iterator(); iter.hasNext(); ) {
                ExpressionExperiment e = ( ExpressionExperiment ) iter.next();
                eeIds.add( e.getId() );
            }

            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryStringFirstVector );
            queryObject.setParameterList( "collectionOfEE", eeIds );
            queryObject.setLong( "givenQtId", qt.getId() );
            queryObject.setLong( "givenGId", givenG.getId() );
            dedvs.addAll( queryObject.list() );
            // do query joining coexpressed genes through the secondVector to the firstVector
            queryObject = super.getSession( false ).createQuery( queryStringSecondVector );
            queryObject.setParameterList( "collectionOfEE", eeIds );
            queryObject.setLong( "givenQtId", qt.getId() );
            queryObject.setLong( "givenGId", givenG.getId() );
            dedvs.addAll( queryObject.list() );

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return dedvs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleDeleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void handleDeleteLinks( ExpressionExperiment ee ) throws Exception {

        // FIXME figure out the taxon instead of this iteration.
        String[] p2pClassNames = new String[] { "HumanProbeCoExpressionImpl", "MouseProbeCoExpressionImpl",
                "RatProbeCoExpressionImpl", "OtherProbeCoExpressionImpl" };

        int totalDone = 0;

        for ( String p2pClassName : p2pClassNames ) {

            /*
             * Note that we only have to query for the firstVector, because we're joining over all designelement
             * datavectors for this ee.
             */
            final String queryString = "select pp from ExpressionExperimentImpl ee inner join ee.designElementDataVectors as dv, "
                    + p2pClassName + " as pp where pp.firstVector" + " = dv and ee=:ee";

            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "ee", ee );

            final ScrollableResults results = queryObject.scroll( ScrollMode.FORWARD_ONLY );

            Integer numDone = ( Integer ) this.getHibernateTemplate().execute(
                    new org.springframework.orm.hibernate3.HibernateCallback() {
                        public Object doInHibernate( org.hibernate.Session session )
                                throws org.hibernate.HibernateException {
                            int i = 0;

                            while ( results.next() ) {

                                Object o = results.get( 0 );

                                session.delete( o );
                                if ( ++i % 10000 == 0 ) {
                                    log.info( "Delete Progress: " + i + " ..." );
                                    session.flush();
                                    session.clear();
                                    try {
                                        Thread.sleep( 100 );
                                    } catch ( InterruptedException e ) {
                                        //
                                    }
                                }
                            }
                            return i;
                        }
                    }, true );

            totalDone += numDone;
            if ( totalDone > 0 ) {
                break;
            }
        }

        if ( totalDone == 0 ) {
            log.info( "No coexpression results to remove for " + ee );
        } else {
            log.info( totalDone + " coexpression results removed for " + ee );
        }

    }

    @Override
    protected Integer handleCountLinks( ExpressionExperiment expressionExperiment ) throws Exception {
        // FIXME figure out the taxon instead of this iteration.
        String[] p2pClassNames = new String[] { "HumanProbeCoExpressionImpl", "MouseProbeCoExpressionImpl",
                "RatProbeCoExpressionImpl", "OtherProbeCoExpressionImpl" };

        for ( String p2pClassName : p2pClassNames ) {

            /*
             * Note that we only have to query for the firstVector, because we're joining over all designelement
             * datavectors for this ee.
             */
            final String queryString = "select count(pp) from ExpressionExperimentImpl ee inner join ee.designElementDataVectors as dv, "
                    + p2pClassName + " as pp where pp.firstVector" + " = dv and ee=:ee";

            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "ee", expressionExperiment );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of 'Integer" + "' was found when executing query --> '"
                                    + queryString + "'" );
                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
                return ( Integer ) result;
            }

        }

        return 0;

    }

}