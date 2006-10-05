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
package ubic.gemma.model.expression.experiment;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.experiment.ExpressionExperiment
 */
public class ExpressionExperimentDaoImpl extends ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase {

    static Log log = LogFactory.getLog( ExpressionExperimentDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#find(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment find( ExpressionExperiment expressionExperiment ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( ExpressionExperiment.class );

            if ( expressionExperiment.getAccession() != null ) {
                queryObject.add( Restrictions.eq( "accession", expressionExperiment.getAccession() ) );
            } else {
                queryObject.add( Restrictions.eq( "name", expressionExperiment.getName() ) );
            }

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + ExpressionExperiment.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( ExpressionExperiment ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#findOrCreate(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment findOrCreate( ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment.getName() == null && expressionExperiment.getAccession() == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment must have name or external accession." );
        }
        ExpressionExperiment newExpressionExperiment = this.find( expressionExperiment );
        if ( newExpressionExperiment != null ) {
            return newExpressionExperiment;
        }
        log.debug( "Creating new expressionExperiment: " + expressionExperiment.getName() );
        return ( ExpressionExperiment ) create( expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#getQuantitationTypeCountById(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public Map handleGetQuantitationTypeCountById( Long Id ) {
        HashMap<String, Integer> qtCounts = new HashMap<String, Integer>();

        final String queryString = "select quantType.name,count(*) as count from ubic.gemma.model.expression.experiment.ExpressionExperimentImpl ee inner join ee.designElementDataVectors as designElements inner join  designElements.quantitationType as quantType where ee.id = :id GROUP BY quantType.name";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "id", Id );
            ScrollableResults list = queryObject.scroll();
            while ( list.next() ) {
                qtCounts.put( list.getString( 0 ), list.getInteger( 1 ) );
            }
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return qtCounts;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#getQuantitationTypeCountById(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public long handleGetDesignElementDataVectorCountById( long Id ) {
        long count = 0;

        final String queryString = "select count(*) as count from ubic.gemma.model.expression.experiment.ExpressionExperimentImpl ee inner join ee.designElementDataVectors as designElements inner join  designElements.quantitationType as quantType where ee.id = :id";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "id", Id );
            ScrollableResults list = queryObject.scroll();
            while ( list.next() ) {
                int c = list.getInteger( 0 );
                count = Long.parseLong( ( new Integer( c ) ).toString() );
            }
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return count;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#remove(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public void remove( ExpressionExperiment expressionExperiment ) {
        final ExpressionExperiment toDelete = expressionExperiment;
        this.getHibernateTemplate().execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( Session session ) throws HibernateException {
                ExpressionExperiment toDeletePers = ( ExpressionExperiment ) session.merge( toDelete );

                Set<BioAssayDimension> dims = new HashSet<BioAssayDimension>();

                Collection<DesignElementDataVector> designElementDataVectors = toDeletePers
                        .getDesignElementDataVectors();

                int count = 0;
                AuditTrail at; // reused a couple times to delete the audit trails

                for ( DesignElementDataVector dv : designElementDataVectors ) {
                    BioAssayDimension dim = dv.getBioAssayDimension();
                    dims.add( dim );
                    session.delete( dv );
                    if ( ++count % 1000 == 0 ) {
                        log.info( count + " vectors deleted" );
                    }
                }

                for ( BioAssayDimension dim : dims ) {

                    session.delete( dim );

                }

                //session.flush();
                // Delete BioMaterials
                for ( BioAssay ba : toDeletePers.getBioAssays() ) {

                    // fixme this needs to be here for lazy loading issues. Even though the AD isn't getting removed.
                    // Not happy about this at all. but what to do?
                    ba.getArrayDesignUsed().getCompositeSequences().size();

                    // Delete Biomaterial 
                    for ( BioMaterial bm : ba.getSamplesUsed() ) {
//                        //fixme Cascade happens for treatment and Protocol application but not protocol. 
//                        for (Treatment treat : bm.getTreatments())
//                            for (ProtocolApplication protoApp : treat.getProtocolApplications()) {
//                                 if (protoApp.getProtocol() != null) {
//                                     at = protoApp.getProtocol().getAuditTrail();
//                                     if ( at != null ) {
//                                         for ( AuditEvent event : at.getEvents() )
//                                             session.delete( event );
//                                         session.delete( at );
//                                     }                                     
//                                     session.delete( protoApp.getProtocol() );
//                                 }
//                            }
                        session.delete( bm );
                    }
                    // delete references to files on disk
                    for ( LocalFile lf : ba.getDerivedDataFiles() ) {
                        for ( LocalFile sf : lf.getSourceFiles() )                            
                            session.delete( sf );
                        session.delete( lf );
                    }
                    // Delete raw data files
                    if ( ba.getRawDataFile() != null ) session.delete( ba.getRawDataFile() );

                    // remove the bioassay audit trail
                    at = ba.getAuditTrail();
                    if ( at != null ) {
                        for ( AuditEvent event : at.getEvents() )
                            session.delete( event );
                        session.delete( at );
                    }
                }

                // Remove audit information for ee from the db. We might want to keep this but......
                at = toDeletePers.getAuditTrail();
                if ( at != null ) {
                    for ( AuditEvent event : at.getEvents() )
                        session.delete( event );

                    session.delete( at );
                }

                session.delete( toDeletePers );
                session.flush();
                session.clear();
                
                return null;
            }
        }, true );

    }

}