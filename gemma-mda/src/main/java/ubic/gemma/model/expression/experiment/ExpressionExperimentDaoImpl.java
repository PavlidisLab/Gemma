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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.LongType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.ChannelUtils;
import ubic.gemma.util.CommonQueries;
import ubic.gemma.util.EntityUtils;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.experiment.ExpressionExperiment
 */
@Repository
public class ExpressionExperimentDaoImpl extends ExpressionExperimentDaoBase {

    private static final int BATCH_SIZE = 1000;

    static Log log = LogFactory.getLog( ExpressionExperimentDaoImpl.class.getName() );

    @Autowired
    public ExpressionExperimentDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BrowsingDao#browse(java.lang.Integer, java.lang.Integer)
     */
    @Override
    public List<ExpressionExperiment> browse( Integer start, Integer limit ) {
        Query query = this.getSession().createQuery( "from ExpressionExperimentImpl" );
        query.setMaxResults( limit );
        query.setFirstResult( start );
        return query.list();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BrowsingDao#browse(java.lang.Integer, java.lang.Integer, java.lang.String, boolean)
     */
    @Override
    public List<ExpressionExperiment> browse( Integer start, Integer limit, String orderField, boolean descending ) {
        Query query = this.getSession().createQuery(
                "from ExpressionExperimentImpl order by " + orderField + " " + ( descending ? "desc" : "" ) );
        query.setMaxResults( limit );
        query.setFirstResult( start );
        return query.list();
    }

    @Override
    public List<ExpressionExperiment> browseSpecificIds( Integer start, Integer limit, Collection<Long> ids ) {
        Query query = this.getSession().createQuery( "from ExpressionExperimentImpl where id in (:ids) " );
        query.setParameterList( "ids", ids );
        query.setMaxResults( limit );
        query.setFirstResult( start );
        return query.list();
    }

    @Override
    public List<ExpressionExperiment> browseSpecificIds( Integer start, Integer limit, String orderField,
            boolean descending, Collection<Long> ids ) {

        Query query = this.getSession().createQuery(
                "from ExpressionExperimentImpl where id in (:ids) order by " + orderField + " "
                        + ( descending ? "desc" : "" ) );
        query.setParameterList( "ids", ids );
        query.setMaxResults( limit );
        query.setFirstResult( start );
        return query.list();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BrowsingDao#count()
     */
    @Override
    public Integer count() {
        return ( ( Long ) getHibernateTemplate().find( "select count(*) from ExpressionExperimentImpl" ).iterator()
                .next() ).intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#find(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment find( ExpressionExperiment expressionExperiment ) {

        DetachedCriteria crit = DetachedCriteria.forClass( ExpressionExperiment.class );

        if ( expressionExperiment.getAccession() != null ) {
            crit.add( Restrictions.eq( "accession", expressionExperiment.getAccession() ) );
        } else if ( expressionExperiment.getShortName() != null ) {
            crit.add( Restrictions.eq( "shortName", expressionExperiment.getShortName() ) );
        } else {
            crit.add( Restrictions.eq( "name", expressionExperiment.getName() ) );
        }

        List<?> results = this.getHibernateTemplate().findByCriteria( crit );
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
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#findByAccession(ubic.gemma.model.common.
     * description.DatabaseEntry)
     */
    @Override
    public Collection<ExpressionExperiment> findByAccession( DatabaseEntry accession ) {

        DetachedCriteria crit = DetachedCriteria.forClass( ExpressionExperiment.class );

        BusinessKey.checkKey( accession );
        BusinessKey.attachCriteria( crit, accession, "accession" );
        return this.getHibernateTemplate().findByCriteria( crit );
    }

    @Override
    public Collection<ExpressionExperiment> findByAccession( String accession ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select e from ExpressionExperimentImpl e inner join e.accession a where a.accession = :accession",
                "accession", accession );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDao#findByUpdatedLimit(java.util.Collection,
     * java.lang.Integer)
     */
    @Override
    public List<ExpressionExperiment> findByUpdatedLimit( Collection<Long> ids, Integer limit ) {
        if ( ids.isEmpty() || limit <= 0 ) return new ArrayList<ExpressionExperiment>();

        Session s = this.getSession();

        String queryString = "select e from ExpressionExperimentImpl e join e.status s where e.id in (:ids) order by s.lastUpdateDate desc ";

        Query q = s.createQuery( queryString );
        q.setParameterList( "ids", ids );
        q.setMaxResults( limit );
        return q.list();

    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#findOrCreate(ubic.gemma.model.expression.
     * experiment.ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment findOrCreate( ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment.getShortName() == null && expressionExperiment.getName() == null
                && expressionExperiment.getAccession() == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment must have name or external accession." );
        }
        ExpressionExperiment newExpressionExperiment = this.find( expressionExperiment );
        if ( newExpressionExperiment != null ) {
            return newExpressionExperiment;
        }
        log.debug( "Creating new expressionExperiment: " + expressionExperiment.getName() );
        newExpressionExperiment = create( expressionExperiment );
        return newExpressionExperiment;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDao#getArrayDesignsUsed(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    public Collection<ArrayDesign> getArrayDesignsUsed( ExpressionExperiment expressionExperiment ) {
        Session session = super.getSession();
        Collection<ArrayDesign> ADs = CommonQueries.getArrayDesignsUsed( expressionExperiment, session );
        super.releaseSession( session );
        return ADs;
    }

    @Override
    public Map<ArrayDesign, Collection<Long>> getArrayDesignsUsed( Collection<Long> eeids ) {
        return CommonQueries.getArrayDesignsUsed( eeids, this.getSession() );
    }

    @Override
    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment ) {
        String queryString = "select distinct b from BioAssayDimensionImpl b, ExpressionExperimentImpl e "
                + "inner join b.bioAssays bba inner join e.bioAssays eb where eb = bba and e = :ee ";
        return getHibernateTemplate().findByNamedParam( queryString, "ee", expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDao#getProcessedDataVectors(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors( ExpressionExperiment expressionExperiment ) {
        final String queryString = "from ProcessedExpressionDataVector where expressionExperiment = :ee";
        return this.getHibernateTemplate().findByNamedParam( queryString, "ee", expressionExperiment );
    }

    @Override
    public Collection<Long> getUntroubled( Collection<Long> ids ) {
        Collection<Long> batch = new HashSet<Long>();
        Collection<Long> firstPass = new HashSet<Long>();

        /*
         * First check the EEs themselves.
         */
        String eeQueryString = "select e.id from ExpressionExperimentImpl e join e.status s where s.troubled = 0 and e.id in (:ids)";
        for ( Long id : ids ) {
            batch.add( id );
            if ( batch.size() == BATCH_SIZE ) {
                firstPass.addAll( this.getHibernateTemplate().findByNamedParam( eeQueryString, "ids", batch ) );
                batch.clear();
            }

        }
        if ( !batch.isEmpty() ) {
            firstPass.addAll( this.getHibernateTemplate().findByNamedParam( eeQueryString, "ids", batch ) );
        }

        /*
         * Then we have to go to the array design. (we could attempt to do this in the same query, above)
         */

        Collection<Long> result = new HashSet<Long>();
        final String eeAdQuery = "select ee.id from ExpressionExperimentImpl as ee inner join "
                + "ee.bioAssays b inner join b.arrayDesignUsed ad inner join ad.status s where ee.id in (:ees) and s.troubled = 0";

        batch.clear();
        for ( Long id : firstPass ) {
            batch.add( id );
            if ( batch.size() == BATCH_SIZE ) {
                result.addAll( this.getHibernateTemplate().findByNamedParam( eeAdQuery, "ees", batch ) );
                batch.clear();
            }

        }

        if ( !batch.isEmpty() ) {
            result.addAll( this.getHibernateTemplate().findByNamedParam( eeAdQuery, "ees", batch ) );
        }

        return result;
    }

    @Override
    public List<ExpressionExperiment> findByUpdatedLimit( int limit ) {
        Session s = this.getSession();
        String queryString = "select e from ExpressionExperimentImpl e join e.status s order by s.lastUpdateDate desc ";
        Query q = s.createQuery( queryString );
        q.setMaxResults( limit );
        return q.list();
    }

    /*
     * Override to take advantage of query cache. (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#loadAll()
     */
    @Override
    public Collection<ExpressionExperiment> loadAll() {
        Collection<ExpressionExperiment> ees = null;
        final String queryString = "from ExpressionExperimentImpl";

        Session session = this.getSession();
        org.hibernate.Query queryObject = session.createQuery( queryString );
        queryObject.setReadOnly( true );
        queryObject.setCacheable( true );
        StopWatch timer = new StopWatch();
        timer.start();
        ees = queryObject.list();
        if ( timer.getTime() > 1000 ) {
            log.info( ees.size() + " EEs loaded in " + timer.getTime() + "ms" );
        }
        this.releaseSession( session );

        return ees;
    }

    @Override
    public List<ExpressionExperiment> loadAllOrdered( String orderField, boolean descending ) {
        String qs = "Select distinct ee from ExpressionExperimentImpl ee ";
        if ( orderField.equals( "taxon" ) ) {
            qs += "inner join ee.bioAssays as ba " + "inner join ba.samplesUsed as sample "
                    + "order by sample.sourceTaxon " + ( descending ? "desc" : "" );
        } else if ( orderField.equals( "bioAssayCount" ) ) {
            qs += "inner join ee.bioAssays as ba " + "group by ee.id " + "order by count(ba) "
                    + ( descending ? "desc" : "" );
        } else if ( orderField.equals( "troubled" ) ) {
            qs += "inner join ee.status as status " + "order by status.troubled " + ( descending ? "desc" : "" );
        } else { // (orderField.equals( "name" ) || orderField.equals( "shortName" ) || orderField.equals( "id" )){
            qs += " order by ee." + orderField + " " + ( descending ? "desc" : "" );
        }
        Query query = this.getSession().createQuery( qs );
        return query.list();
    }

    @Override
    public List<ExpressionExperiment> loadAllTaxonOrdered( String orderField, boolean descending, Taxon taxon ) {
        String qs = "select distinct ee from ExpressionExperimentImpl as ee " + "inner join ee.bioAssays as ba "
                + "inner join ba.samplesUsed as sample ";
        String where = " where sample.sourceTaxon = :taxon or sample.sourceTaxon.parentTaxon = :taxon ";

        if ( orderField.equals( "taxon" ) ) {
            qs += where + " order by sample.sourceTaxon " + ( descending ? "desc" : "" );
        } else if ( orderField.equals( "bioAssayCount" ) ) {
            qs += where + "group by ee.id order by count(distinct ba) " + ( descending ? "desc" : "" );
        } else if ( orderField.equals( "troubled" ) ) {
            qs += "inner join ee.status as status " + where + "order by status.troubled " + ( descending ? "desc" : "" );
        } else { // (orderField.equals( "name" ) || orderField.equals( "shortName" ) || orderField.equals( "id" )){
            qs += where + " order by ee." + orderField + " " + ( descending ? "desc" : "" );
        }
        Query query = this.getSession().createQuery( qs );
        query.setParameter( "taxon", taxon );
        return query.list();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDao#loadLackingEvent(java.lang.Class)
     */
    public Collection<ExpressionExperiment> loadLackingEvent( Class<? extends AuditEventType> eventType ) {
        /*
         * I cannot figure out a way to do this with a left join in HQL.
         */
        Collection<ExpressionExperiment> allEEs = this.loadAll();
        allEEs.removeAll( loadWithEvent( eventType ) );
        return allEEs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDao#loadLackingFactors()
     */
    @Override
    public Collection<ExpressionExperiment> loadLackingFactors() {
        return this
                .getHibernateTemplate()
                .find( "select e from ExpressionExperimentImpl e join e.experimentalDesign d where d.experimentalFactors.size =  0" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDao#loadLackingTags()
     */
    @Override
    public Collection<ExpressionExperiment> loadLackingTags() {
        return this.getHibernateTemplate().find(
                "select e from ExpressionExperimentImpl e where e.characteristics.size = 0" );
    }

    @Override
    public List<ExpressionExperiment> loadMultipleOrdered( String orderField, boolean descending, Collection<Long> ids ) {
        String qs = "Select distinct ee from ExpressionExperimentImpl ee ";
        if ( orderField.equals( "taxon" ) ) {
            qs += "inner join ee.bioAssays as ba " + "inner join ba.samplesUsed as sample " + "where ee.id in (:ids) "
                    + "order by sample.sourceTaxon " + ( descending ? "desc" : "" );
        } else if ( orderField.equals( "bioAssayCount" ) ) {
            qs += "inner join ee.bioAssays as ba " + "where ee.id in (:ids) " + "group by ee.id "
                    + "order by count(ba) " + ( descending ? "desc" : "" );
        } else { // (orderField.equals( "name" ) || orderField.equals( "shortName" ) || orderField.equals( "id" )){
            qs += " where ee.id in (:ids) ";
            qs += " order by ee." + orderField + " " + ( descending ? "desc" : "" );
        }
        Query query = this.getSession().createQuery( qs );
        query.setParameterList( "ids", ids );
        return query.list();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDao#loadWithEvent(java.lang.Class)
     */
    public Collection<ExpressionExperiment> loadWithEvent( Class<? extends AuditEventType> eventType ) {
        String className = eventType.getSimpleName().endsWith( "Impl" ) ? eventType.getSimpleName() : eventType
                .getSimpleName() + "Impl";
        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct e from ExpressionExperimentImpl e join e.auditTrail a join a.events ae join ae.eventType t where t.class = :type ",
                        "type", className );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#remove(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment)
     */
    @Override
    public void remove( final ExpressionExperiment toDelete ) {

        Session session = this.getSession();

        // Note that links and analyses are deleted separately - see the ExpressionExperimentService.

        // At this point, the ee is probably still in the session, as the service already has gotten it
        // in this transaction.
        session.flush();
        session.clear();

        session.lock( toDelete, LockMode.NONE );

        Hibernate.initialize( toDelete.getAuditTrail() );

        Set<BioAssayDimension> dims = new HashSet<BioAssayDimension>();
        Set<QuantitationType> qts = new HashSet<QuantitationType>();
        Collection<RawExpressionDataVector> designElementDataVectors = toDelete.getRawExpressionDataVectors();
        Hibernate.initialize( designElementDataVectors );
        toDelete.setRawExpressionDataVectors( null );

        /*
         * We don't delete the investigators, just breaking the association.
         */
        toDelete.getInvestigators().clear();

        int count = 0;
        if ( designElementDataVectors != null ) {
            log.info( "Removing Design Element Data Vectors ..." );
            for ( RawExpressionDataVector dv : designElementDataVectors ) {
                BioAssayDimension bad = dv.getBioAssayDimension();
                dims.add( bad );
                QuantitationType qt = dv.getQuantitationType();
                qts.add( qt );
                dv.setBioAssayDimension( null );
                dv.setQuantitationType( null );
                session.delete( dv );
                if ( ++count % 1000 == 0 ) {
                    session.flush();
                }
                // put back...
                dv.setBioAssayDimension( bad );
                dv.setQuantitationType( qt );

                if ( count % 20000 == 0 ) {
                    log.info( count + " design Element data vectors deleted" );
                }
            }
            count = 0;
            // designElementDataVectors.clear();
        }

        Collection<ProcessedExpressionDataVector> processedVectors = toDelete.getProcessedExpressionDataVectors();

        Hibernate.initialize( processedVectors );
        if ( processedVectors != null && processedVectors.size() > 0 ) {

            toDelete.setProcessedExpressionDataVectors( null );

            for ( ProcessedExpressionDataVector dv : processedVectors ) {
                BioAssayDimension bad = dv.getBioAssayDimension();
                dims.add( bad );
                QuantitationType qt = dv.getQuantitationType();
                qts.add( qt );
                dv.setBioAssayDimension( null );
                dv.setQuantitationType( null );
                session.delete( dv );
                if ( ++count % 1000 == 0 ) {
                    session.flush();
                }
                if ( count % 20000 == 0 ) {
                    log.info( count + " processed design Element data vectors deleted" );
                }

                // put back..
                dv.setBioAssayDimension( bad );
                dv.setQuantitationType( qt );
            }
            // processedVectors.clear();
        }

        session.flush();
        session.clear();
        session.update( toDelete );

        log.info( "Removing BioAssay Dimensions ..." );
        for ( BioAssayDimension dim : dims ) {
            dim.getBioAssays().clear();
            session.update( dim );
            session.delete( dim );
        }
        dims.clear();
        session.flush();

        log.info( "Removing Bioassays and biomaterials ..." );

        // keep to put back in the object.
        Map<BioAssay, Collection<BioMaterial>> copyOfRelations = new HashMap<BioAssay, Collection<BioMaterial>>();

        Collection<BioMaterial> bioMaterialsToDelete = new HashSet<BioMaterial>();
        Collection<BioAssay> bioAssays = toDelete.getBioAssays();

        for ( BioAssay ba : bioAssays ) {
            // relations to files cascade, so we only have to worry about biomaterials, which aren't cascaded from
            // anywhere.

            Collection<BioMaterial> biomaterials = ba.getSamplesUsed();

            ba.setSamplesUsed( null ); // let this cascade?? It doesn't. biomaterial-bioassay is many-to-many.

            copyOfRelations.put( ba, biomaterials );

            bioMaterialsToDelete.addAll( biomaterials );
            for ( BioMaterial bm : biomaterials ) {
                // see bug 855
                session.lock( bm, LockMode.NONE );
                Hibernate.initialize( bm );
                // this can easily end up with an unattached object.
                Hibernate.initialize( bm.getBioAssaysUsedIn() );

                bm.setFactorValues( null );
                bm.getBioAssaysUsedIn().clear();

            }
        }

        log.info( "Last bits ..." );

        // We delete them here in case they are associated to more than one bioassay-- no cascade is possible.
        for ( BioMaterial bm : bioMaterialsToDelete ) {
            session.delete( bm );
        }

        for ( QuantitationType qt : qts ) {
            session.delete( qt );
        }

        session.delete( toDelete );

        /*
         * Put transient instances back. This is possibly useful for clearing ACLS.
         */
        toDelete.setProcessedExpressionDataVectors( processedVectors );
        toDelete.setRawExpressionDataVectors( designElementDataVectors );
        for ( BioAssay ba : toDelete.getBioAssays() ) {
            ba.setSamplesUsed( copyOfRelations.get( ba ) );
        }

        log.info( "Deleted " + toDelete );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String queryString = "select count(*) from ExpressionExperimentImpl";
        List<?> list = getHibernateTemplate().find( queryString );
        return ( ( Long ) list.iterator().next() ).intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleFindByBibliographicReference(java.lang
     * .Long)
     */
    @Override
    protected Collection<ExpressionExperiment> handleFindByBibliographicReference( Long bibRefID ) throws Exception {
        final String queryString = "select distinct ee FROM ExpressionExperimentImpl as ee left join ee.otherRelevantPublications as eeO"
                + " WHERE ee.primaryPublication.id = :bibID OR (eeO.id = :bibID) ";
        return getHibernateTemplate().findByNamedParam( queryString, "bibID", bibRefID );
    }

    @Override
    protected ExpressionExperiment handleFindByBioMaterial( BioMaterial bm ) throws Exception {

        final String queryString = "select distinct ee from ExpressionExperimentImpl as ee "
                + "inner join ee.bioAssays as ba inner join ba.samplesUsed as sample where sample = :bm";
        List<?> list = getHibernateTemplate().findByNamedParam( queryString, "bm", bm );
        if ( list.size() == 0 ) {
            log.warn( "No expression experiment for " + bm );
            return null;
        }
        if ( list.size() > 1 ) {
            /*
             * This really shouldn't happen!
             */
            log.warn( "Found " + list.size() + " expression experiment for the given bm: " + bm + " Only 1 returned." );
        }
        return ( ExpressionExperiment ) list.iterator().next();
    }

    @Override
    protected Collection<ExpressionExperiment> handleFindByBioMaterials( Collection<BioMaterial> bms ) throws Exception {
        if ( bms == null || bms.size() == 0 ) {
            return new HashSet<ExpressionExperiment>();
        }
        final String queryString = "select distinct ee from ExpressionExperimentImpl as ee "
                + "inner join ee.bioAssays as ba inner join ba.samplesUsed as sample where sample in (:bms)";
        Collection<ExpressionExperiment> results = new HashSet<ExpressionExperiment>();
        Collection<BioMaterial> batch = new HashSet<BioMaterial>();
        for ( BioMaterial o : bms ) {
            batch.add( o );
            if ( batch.size() == BATCH_SIZE ) {
                results.addAll( getHibernateTemplate().findByNamedParam( queryString, "bms", batch ) );
                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {
            results.addAll( getHibernateTemplate().findByNamedParam( queryString, "bms", batch ) );
        }

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleFindByExpressedGene(ubic.gemma.model
     * .genome.Gene, java.lang.Double)
     */
    @Override
    protected Collection<ExpressionExperiment> handleFindByExpressedGene( Gene gene, Double rank ) throws Exception {

        final String queryString = "select distinct ee.ID as eeID FROM "
                + "GENE2CS g2s, COMPOSITE_SEQUENCE cs, PROCESSED_EXPRESSION_DATA_VECTOR dedv, INVESTIGATION ee "
                + "WHERE g2s.CS = cs.ID AND cs.ID = dedv.DESIGN_ELEMENT_FK AND dedv.EXPRESSION_EXPERIMENT_FK = ee.ID"
                + " AND g2s.gene = :geneID AND dedv.RANK_BY_MEAN >= :rank";

        Collection<Long> eeIds = null;

        try {
            Session session = super.getSession();
            org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
            queryObject.setLong( "geneID", gene.getId() );
            queryObject.setDouble( "rank", rank );
            queryObject.addScalar( "eeID", new LongType() );
            ScrollableResults results = queryObject.scroll();

            eeIds = new HashSet<Long>();

            // Post Processing
            while ( results.next() )
                eeIds.add( results.getLong( 0 ) );

            session.clear();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return this.load( eeIds );

    }

    @Override
    protected ExpressionExperiment handleFindByFactorValue( FactorValue fv ) {
        final String queryString = "select distinct ee from ExpressionExperimentImpl as ee inner join ee.experimentalDesign ed "
                + "inner join ed.experimentalFactors ef inner join ef.factorValues fv where fv = :fv ";

        List<?> results = getHibernateTemplate().findByNamedParam( queryString, "fv", fv );

        if ( results.size() == 0 ) {
            log.info( "There is no expression experiment that has factorValue = " + fv );
            return null;
        }
        return ( ExpressionExperiment ) results.iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleFindByFactorValues(java.util.Collection)
     */
    @Override
    protected Collection<ExpressionExperiment> handleFindByFactorValues( Collection<FactorValue> fvs ) {

        if ( fvs.isEmpty() ) return new HashSet<ExpressionExperiment>();

        // thaw the factor values.
        Collection<ExperimentalDesign> eds = this.getHibernateTemplate().findByNamedParam(
                "select ed from FactorValueImpl f join f.experimentalFactor ef "
                        + " join ef.experimentalDesign ed where f.id in (:ids)", "ids", EntityUtils.getIds( fvs ) );

        if ( eds.isEmpty() ) {
            return new HashSet<ExpressionExperiment>();
        }

        final String queryString = "select distinct ee from ExpressionExperimentImpl as ee where ee.experimentalDesign in (:eds) ";
        Collection<ExpressionExperiment> results = new HashSet<ExpressionExperiment>();
        Collection<ExperimentalDesign> batch = new HashSet<ExperimentalDesign>();
        for ( ExperimentalDesign o : eds ) {
            batch.add( o );
            if ( batch.size() == BATCH_SIZE ) {
                results.addAll( getHibernateTemplate().findByNamedParam( queryString, "eds", batch ) );
                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {
            results.addAll( getHibernateTemplate().findByNamedParam( queryString, "eds", batch ) );
        }

        return results;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleFindByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection<ExpressionExperiment> handleFindByGene( Gene gene ) throws Exception {

        /*
         * NOTE uses GENE2CS table.
         */
        final String queryString = "select distinct ee.ID as eeID FROM "
                + "GENE2CS g2s, COMPOSITE_SEQUENCE cs, ARRAY_DESIGN ad, BIO_ASSAY ba, INVESTIGATION ee "
                + "WHERE g2s.CS = cs.ID AND ad.ID = cs.ARRAY_DESIGN_FK AND ba.ARRAY_DESIGN_USED_FK = ad.ID AND"
                + " ba.EXPRESSION_EXPERIMENT_FK = ee.ID and g2s.GENE = :geneID";

        Collection<Long> eeIds = null;

        try {
            Session session = super.getSession();
            org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
            queryObject.setLong( "geneID", gene.getId() );
            queryObject.addScalar( "eeID", new LongType() );
            ScrollableResults results = queryObject.scroll();

            eeIds = new HashSet<Long>();

            while ( results.next() ) {
                eeIds.add( results.getLong( 0 ) );
            }
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return this.load( eeIds );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleFindByParentTaxon(ubic.gemma.model.genome
     * . Taxon)
     */
    @Override
    protected Collection<ExpressionExperiment> handleFindByParentTaxon( Taxon taxon ) throws Exception {
        final String queryString = "select distinct ee from ExpressionExperimentImpl as ee "
                + "inner join ee.bioAssays as ba " + "inner join ba.samplesUsed as sample "
                + "inner join sample.sourceTaxon as childtaxon where childtaxon.parentTaxon  = :taxon ";
        return getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    protected ExpressionExperiment handleFindByQuantitationType( QuantitationType quantitationType ) throws Exception {
        final String queryString = "select ee from ExpressionExperimentImpl as ee "
                + "inner join ee.quantitationTypes qt where qt = :qt ";
        List<?> results = getHibernateTemplate().findByNamedParam( queryString, "qt", quantitationType );
        if ( results.size() == 1 ) {
            return ( ExpressionExperiment ) results.iterator().next();
        } else if ( results.size() == 0 ) {
            return null;
        }
        throw new IllegalStateException( "More than one ExpressionExperiment associated with " + quantitationType );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleFindByTaxon(ubic.gemma.model.genome.
     * Taxon)
     */
    @Override
    protected Collection<ExpressionExperiment> handleFindByTaxon( Taxon taxon ) throws Exception {
        final String queryString = "select distinct ee from ExpressionExperimentImpl as ee "
                + "inner join ee.bioAssays as ba "
                + "inner join ba.samplesUsed as sample where sample.sourceTaxon = :taxon or sample.sourceTaxon.parentTaxon = :taxon";
        return getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDao#findByTaxon(ubic.gemma.model.genome.Taxon,
     * int)
     */
    @Override
    public List<ExpressionExperiment> findByTaxon( Taxon taxon, int limit ) {
        final String queryString = "select distinct ee from ExpressionExperimentImpl as ee "
                + "inner join ee.bioAssays as ba "
                + "inner join ba.samplesUsed as sample join ee.status s where sample.sourceTaxon = :taxon or sample.sourceTaxon.parentTaxon = :taxon order by s.lastUpdateDate desc";
        HibernateTemplate tpl = new HibernateTemplate( this.getSessionFactory() );
        tpl.setMaxResults( limit );
        return tpl.findByNamedParam( queryString, "taxon", taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetAnnotationCounts(java.util.Collection
     * )
     */
    @Override
    protected Map<Long, Integer> handleGetAnnotationCounts( Collection<Long> ids ) throws Exception {
        Map<Long, Integer> results = new HashMap<Long, Integer>();
        for ( Long id : ids ) {
            results.put( id, 0 );
        }
        if ( ids.size() == 0 ) {
            return results;
        }
        String queryString = "select e.id,count(c.id) from ExpressionExperimentImpl e inner join e.characteristics c where e.id in (:ids) group by e.id";
        List<?> res = this.getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

        for ( Object r : res ) {
            Object[] ro = ( Object[] ) r;
            Long id = ( Long ) ro[0];
            Integer count = ( ( Long ) ro[1] ).intValue();
            results.put( id, count );
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetArrayDesignAuditEvents(java.util.
     * Collection)
     */
    @Deprecated
    @Override
    protected Map<Long, Map<Long, Collection<AuditEvent>>> handleGetArrayDesignAuditEvents( Collection<Long> ids )
            throws Exception {
        final String queryString = "select ee.id, ad.id, event " + "from ExpressionExperimentImpl ee "
                + "inner join ee.bioAssays b " + "inner join b.arrayDesignUsed ad " + "inner join ad.auditTrail trail "
                + "inner join trail.events event " + "where ee.id in (:ids) ";

        List<?> result = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

        Map<Long, Map<Long, Collection<AuditEvent>>> eventMap = new HashMap<Long, Map<Long, Collection<AuditEvent>>>();
        // process list of expression experiment ids that have events
        for ( Object o : result ) {
            Object[] row = ( Object[] ) o;
            Long eeId = ( Long ) row[0];
            Long adId = ( Long ) row[1];
            AuditEvent event = ( AuditEvent ) row[2];

            Map<Long, Collection<AuditEvent>> adEventMap = eventMap.get( eeId );
            if ( adEventMap == null ) {
                adEventMap = new HashMap<Long, Collection<AuditEvent>>();
                eventMap.put( eeId, adEventMap );
            }

            Collection<AuditEvent> events = adEventMap.get( adId );
            if ( events == null ) {
                events = new ArrayList<AuditEvent>();
                adEventMap.put( adId, events );
            }

            events.add( event );
        }
        return eventMap;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetLastLinkAnalysis(java.util.Collection
     * )
     */
    @Override
    protected Map handleGetAuditEvents( Collection<Long> ids ) throws Exception {
        final String queryString = "select ee.id, auditEvent from ExpressionExperimentImpl ee inner join ee.auditTrail as auditTrail inner join auditTrail.events as auditEvent "
                + " where ee.id in (:ids) ";

        List<?> result = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

        Map<Long, Collection<AuditEvent>> eventMap = new HashMap<Long, Collection<AuditEvent>>();

        for ( Object o : result ) {
            Object[] row = ( Object[] ) o;
            Long id = ( Long ) row[0];
            AuditEvent event = ( AuditEvent ) row[1];

            if ( eventMap.containsKey( id ) ) {
                Collection<AuditEvent> events = eventMap.get( id );
                events.add( event );
            } else {
                Collection<AuditEvent> events = new ArrayList<AuditEvent>();
                events.add( event );
                eventMap.put( id, events );
            }
        }
        // add in expression experiment ids that do not have events. Set
        // their values to null.
        for ( Object object : ids ) {
            Long id = ( Long ) object;
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, null );
            }
        }
        return eventMap;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#getQuantitationTypeCountById(ubic.gemma.model
     * .expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Integer handleGetBioAssayCountById( long Id ) {
        final String queryString = "select count(ba) from ExpressionExperimentImpl ee "
                + "inner join ee.bioAssays dedv where ee.id = :ee";
        List<?> list = getHibernateTemplate().findByNamedParam( queryString, "ee", Id );
        if ( list.size() == 0 ) {
            log.warn( "No vectors for experiment with id " + Id );
            return 0;
        }
        return ( ( Long ) list.iterator().next() ).intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetBioMaterialCount(ubic.gemma.model
     * .expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Integer handleGetBioMaterialCount( ExpressionExperiment expressionExperiment ) throws Exception {
        final String queryString = "select count(distinct sample) from ExpressionExperimentImpl as ee "
                + "inner join ee.bioAssays as ba " + "inner join ba.samplesUsed as sample where ee.id = :eeId ";
        List<?> result = getHibernateTemplate().findByNamedParam( queryString, "eeId", expressionExperiment.getId() );
        return ( ( Long ) result.iterator().next() ).intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetDesignElementDataVectorCountById(
     * long)
     */
    @Override
    protected Integer handleGetDesignElementDataVectorCountById( long Id ) {

        /*
         * Note that this gets the count of RAW vectors.
         */

        final String queryString = "select count(dedv) from ExpressionExperimentImpl ee "
                + "inner join ee.rawExpressionDataVectors dedv where ee.id = :ee";

        List<?> list = getHibernateTemplate().findByNamedParam( queryString, "ee", Id );
        if ( list.size() == 0 ) {
            log.warn( "No vectors for experiment with id " + Id );
            return 0;
        }
        return ( ( Long ) list.iterator().next() ).intValue();

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetDesignElementDataVectors(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment, java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<DesignElementDataVector> handleGetDesignElementDataVectors(
            Collection<QuantitationType> quantitationTypes ) throws Exception {

        if ( quantitationTypes.isEmpty() ) {
            throw new IllegalArgumentException( "Must provide at least one quantitation type" );
        }

        // NOTE this essentially does a partial thaw.
        String queryString = "select dev from RawExpressionDataVectorImpl dev"
                + " inner join fetch dev.bioAssayDimension bd "
                + " inner join fetch dev.designElement de inner join fetch dev.quantitationType where dev.quantitationType in (:qts) ";

        List<?> results = getHibernateTemplate().findByNamedParam( queryString, "qts", quantitationTypes );

        if ( results.isEmpty() ) {
            queryString = "select dev from ProcessedExpressionDataVectorImpl dev"
                    + " inner join fetch dev.bioAssayDimension bd "
                    + " inner join fetch dev.designElement de inner join fetch dev.quantitationType where dev.quantitationType in (:qts) ";

            results.addAll( getHibernateTemplate().findByNamedParam( queryString, "qts", quantitationTypes ) );
        }

        return ( Collection<DesignElementDataVector> ) results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetDesignElementDataVectors(java.util
     * .Collection, ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    protected Collection<DesignElementDataVector> handleGetDesignElementDataVectors(
            Collection<CompositeSequence> designElements, QuantitationType quantitationType ) throws Exception {
        if ( designElements == null || designElements.size() == 0 ) return new HashSet<DesignElementDataVector>();

        assert quantitationType.getId() != null;

        final String queryString = "select dev from RawExpressionDataVectorImpl as dev inner join dev.designElement as de "
                + " where de in (:des) and dev.quantitationType = :qt";
        return getHibernateTemplate().findByNamedParam( queryString, new String[] { "des", "qt" },
                new Object[] { designElements, quantitationType } );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetLastArrayDesignUpdate(java.util.
     * Collection, java.lang.Class)
     */
    @Override
    protected Map<Long, Date> handleGetLastArrayDesignUpdate( Collection<ExpressionExperiment> expressionExperiments )
            throws Exception {
        final String queryString = "select ee.id, max(s.lastUpdateDate) from ExpressionExperimentImpl as ee inner join "
                + "ee.bioAssays b inner join b.arrayDesignUsed a join a.status s "
                + " where ee in (:ees) group by ee.id ";

        List<?> res = this.getHibernateTemplate().findByNamedParam( queryString, "ees", expressionExperiments );

        assert ( !res.isEmpty() );

        Map<Long, Date> result = new HashMap<Long, Date>();
        for ( Object o : res ) {
            Object[] oa = ( Object[] ) o;
            Long id = ( Long ) oa[0];
            Date d = ( Date ) oa[1];
            result.put( id, d );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetLastArrayDesignUpdate(ubic.gemma.
     * model.expression.experiment.ExpressionExperiment, java.lang.Class)
     */
    @Override
    protected Date handleGetLastArrayDesignUpdate( ExpressionExperiment ee ) throws java.lang.Exception {

        final String queryString = "select max(s.lastUpdateDate) from ExpressionExperimentImpl as ee inner join "
                + "ee.bioAssays b inner join b.arrayDesignUsed a join a.status s " + " where ee = :ee ";

        List<?> res = this.getHibernateTemplate().findByNamedParam( queryString, "ee", ee );

        assert ( !res.isEmpty() );

        return ( Date ) res.iterator().next();
    }

    @Override
    protected QuantitationType handleGetMaskedPreferredQuantitationType( ExpressionExperiment ee ) throws Exception {
        String queryString = "select q from ExpressionExperimentImpl e inner join e.quantitationTypes q where e = :ee and q.isMaskedPreferred = true";
        List<?> k = this.getHibernateTemplate().findByNamedParam( queryString, "ee", ee );
        if ( k.size() == 1 ) {
            return ( QuantitationType ) k.iterator().next();
        } else if ( k.size() > 1 ) {
            throw new IllegalStateException(
                    "There should only be one masked preferred quantitationtype per expressionexperiment (" + ee + ")" );
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetPerTaxonCount()
     */
    @Override
    protected Map<Taxon, Long> handleGetPerTaxonCount() throws Exception {

        Map<Taxon, Taxon> taxonParents = new HashMap<Taxon, Taxon>();
        List<Object[]> tp = super.getHibernateTemplate().find(
                "select t, p from TaxonImpl t left outer join t.parentTaxon p" );
        for ( Object[] o : tp ) {
            taxonParents.put( ( Taxon ) o[0], ( Taxon ) o[1] );
        }

        Map<Taxon, Long> taxonCount = new LinkedHashMap<Taxon, Long>();
        String queryString = "select t, count(distinct ee) from ExpressionExperimentImpl "
                + "ee inner join ee.bioAssays as ba inner join ba.samplesUsed su "
                + "inner join su.sourceTaxon t group by t order by t.scientificName ";

        // it is important to cache this, as it gets called on the home page. Though it's actually fast.
        org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
        queryObject.setCacheable( true );
        ScrollableResults list = queryObject.scroll();
        while ( list.next() ) {
            Taxon taxon = ( Taxon ) list.get( 0 );
            Taxon parent = taxonParents.get( taxon );
            Long count = list.getLong( 1 );

            if ( parent != null ) {
                if ( !taxonCount.containsKey( parent ) ) {
                    taxonCount.put( parent, 0L );
                }

                taxonCount.put( parent, taxonCount.get( parent ) + count );

            } else {
                taxonCount.put( taxon, count );
            }
        }
        return taxonCount;

    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetPopulatedFactorCounts(java.util.
     * Collection)
     */
    @Override
    protected Map<Long, Integer> handleGetPopulatedFactorCounts( Collection<Long> ids ) throws Exception {
        Map<Long, Integer> results = new HashMap<Long, Integer>();
        if ( ids.size() == 0 ) {
            return results;
        }

        for ( Long id : ids ) {
            results.put( id, 0 );
        }

        String queryString = "select e.id,count(distinct ef.id) from ExpressionExperimentImpl e inner join e.bioAssays ba"
                + " inner join ba.samplesUsed bm inner join bm.factorValues fv inner join fv.experimentalFactor ef where e.id in (:ids) group by e.id";
        List<?> res = this.getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

        for ( Object r : res ) {
            Object[] ro = ( Object[] ) r;
            Long id = ( Long ) ro[0];
            Integer count = ( ( Long ) ro[1] ).intValue();
            results.put( id, count );
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetPopulatedFactorCounts(java.util.
     * Collection)
     */
    @Override
    protected Map<Long, Integer> handleGetPopulatedFactorCountsExcludeBatch( Collection<Long> ids ) throws Exception {
        Map<Long, Integer> results = new HashMap<Long, Integer>();
        if ( ids.size() == 0 ) {
            return results;
        }

        for ( Long id : ids ) {
            results.put( id, 0 );
        }

        String queryString = "select e.id,count(distinct ef.id) from ExpressionExperimentImpl e inner join e.bioAssays ba"
                + " inner join ba.samplesUsed bm inner join bm.factorValues fv inner join fv.experimentalFactor ef "
                + " inner join ef.category cat where e.id in (:ids) and cat.category != (:category) and ef.name != (:name) group by e.id";

        String[] names = { "ids", "category", "name" };
        Object[] values = { ids, ExperimentalFactorService.BATCH_FACTOR_CATEGORY_NAME,
                ExperimentalFactorService.BATCH_FACTOR_NAME };
        List<?> res = this.getHibernateTemplate().findByNamedParam( queryString, names, values );

        for ( Object r : res ) {
            Object[] ro = ( Object[] ) r;
            Long id = ( Long ) ro[0];
            Integer count = ( ( Long ) ro[1] ).intValue();
            results.put( id, count );
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetPreferredDesignElementDataVectorCount
     * (ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Integer handleGetProcessedExpressionVectorCount( ExpressionExperiment expressionExperiment )
            throws Exception {
        final String queryString = "select count(v) from ProcessedExpressionDataVectorImpl v  where v.expressionExperiment = :ee ";

        List<?> result = getHibernateTemplate().findByNamedParam( queryString, "ee", expressionExperiment );
        return ( ( Long ) result.iterator().next() ).intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#getQuantitationTypeCountById(ubic.gemma.model
     * .expression.experiment.ExpressionExperiment)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<QuantitationType, Integer> handleGetQuantitationTypeCountById( Long Id ) {

        final String queryString = "select quantType,count(*) as count "
                + "from ubic.gemma.model.expression.experiment.ExpressionExperimentImpl ee "
                + "inner join ee.rawExpressionDataVectors as vectors "
                + "inner join vectors.quantitationType as quantType " + "where ee.id = :id GROUP BY quantType.name";

        List<?> list = getHibernateTemplate().findByNamedParam( queryString, "id", Id );

        Map<QuantitationType, Integer> qtCounts = new HashMap<QuantitationType, Integer>();
        for ( Object[] tuple : ( List<Object[]> ) list ) {
            qtCounts.put( ( QuantitationType ) tuple[0], ( ( Long ) tuple[1] ).intValue() );
        }

        return qtCounts;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<QuantitationType> handleGetQuantitationTypes( final ExpressionExperiment expressionExperiment ) {
        final String queryString = "select distinct quantType "
                + "from ubic.gemma.model.expression.experiment.ExpressionExperimentImpl ee "
                + "inner join ee.quantitationTypes as quantType fetch all properties where ee  = :ee ";

        List<?> qtypes = getHibernateTemplate().findByNamedParam( queryString, "ee", expressionExperiment );
        return ( Collection<QuantitationType> ) qtypes;

    }

    @Override
    protected Collection<QuantitationType> handleGetQuantitationTypes( ExpressionExperiment expressionExperiment,
            ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            return handleGetQuantitationTypes( expressionExperiment );
        }

        final String queryString = "select distinct quantType "
                + "from ubic.gemma.model.expression.experiment.ExpressionExperimentImpl ee "
                + "inner join  ee.quantitationTypes as quantType " + "inner join ee.bioAssays as ba "
                + "inner join ba.arrayDesignUsed ad " + "where ee = :ee and ad = :ad";

        return getHibernateTemplate().findByNamedParam( queryString, new String[] { "ee", "ad" },
                new Object[] { expressionExperiment, arrayDesign } );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetSampleRemovalEvents(java.util.Collection
     * )
     */
    @Override
    protected Map<ExpressionExperiment, Collection<AuditEvent>> handleGetSampleRemovalEvents(
            Collection<ExpressionExperiment> expressionExperiments ) {
        final String queryString = "select ee,ev from ExpressionExperimentImpl ee inner join ee.bioAssays ba "
                + "inner join ba.auditTrail trail inner join trail.events ev inner join ev.eventType et "
                + "inner join fetch ev.performer where ee in (:ees) and et.class = 'SampleRemovalEvent'";

        Map<ExpressionExperiment, Collection<AuditEvent>> result = new HashMap<ExpressionExperiment, Collection<AuditEvent>>();
        List<?> r = this.getHibernateTemplate().findByNamedParam( queryString, "ees", expressionExperiments );
        for ( Object o : r ) {
            Object[] ol = ( Object[] ) o;
            ExpressionExperiment e = ( ExpressionExperiment ) ol[0];
            if ( !result.containsKey( e ) ) {
                result.put( e, new HashSet<AuditEvent>() );
            }
            AuditEvent ae = ( AuditEvent ) ol[1];
            result.get( e ).add( ae );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetSamplingOfVectors(ubic.gemma.model
     * .common.quantitationtype.QuantitationType, java.lang.Integer)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<DesignElementDataVector> handleGetSamplingOfVectors( QuantitationType quantitationType,
            Integer limit ) throws Exception {
        final String queryString = "select dev from RawExpressionDataVectorImpl dev "
                + "inner join dev.quantitationType as qt where qt.id = :qtid";
        int oldmax = getHibernateTemplate().getMaxResults();
        getHibernateTemplate().setMaxResults( limit );
        List<?> list = getHibernateTemplate().findByNamedParam( queryString, "qtid", quantitationType.getId() );
        getHibernateTemplate().setMaxResults( oldmax );
        return ( Collection<DesignElementDataVector> ) list;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetSubSets(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    protected Collection<ExpressionExperimentSubSet> handleGetSubSets( ExpressionExperiment expressionExperiment )
            throws Exception {
        String queryString = "select from ExpressionExperimentSubSetImpl eess inner join eess.sourceExperiment ee where ee = :ee";
        return this.getHibernateTemplate().findByNamedParam( queryString, "ee", expressionExperiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetTaxon(java.lang.Long)
     */
    @Override
    protected Taxon handleGetTaxon( Long id ) throws Exception {
        final String queryString = "select SU.sourceTaxon from ExpressionExperimentImpl as EE "
                + "inner join EE.bioAssays as BA " + "inner join BA.samplesUsed as SU where EE.id = :id";
        List<?> list = getHibernateTemplate().findByNamedParam( queryString, "id", id );
        if ( list.size() > 0 ) return ( Taxon ) list.iterator().next();
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleLoad(java.util.Collection)
     */
    @Override
    protected Collection<ExpressionExperiment> handleLoad( Collection<Long> ids ) throws Exception {
        StopWatch timer = new StopWatch();
        timer.start();
        if ( ids == null || ids.size() == 0 ) {
            return new ArrayList<ExpressionExperiment>();
        }

        Collection<ExpressionExperiment> ees = null;
        final String queryString = "from ExpressionExperimentImpl ee where ee.id in (:ids) ";
        List<Long> idList = new ArrayList<Long>( ids );
        Collections.sort( idList );

        try {
            Session session = this.getSession( false );
            org.hibernate.Query queryObject = session.createQuery( queryString );
            queryObject.setCacheable( true );
            queryObject.setParameterList( "ids", idList );

            ees = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        if ( timer.getTime() > 1000 ) {
            log.info( ees.size() + " EEs loaded in " + timer.getTime() + "ms" );
        }
        return ees;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleLoadValueObjects(java.util.Collection)
     */
    @Override
    protected Collection<ExpressionExperimentValueObject> handleLoadValueObjects( Collection<Long> ids )
            throws Exception {
        Map<Long, ExpressionExperimentValueObject> vo = new LinkedHashMap<Long, ExpressionExperimentValueObject>();

        boolean isList = ids != null && ids instanceof List;
        if ( ids != null && ids.size() == 0 ) {
            if ( isList ) {
                return new ArrayList<ExpressionExperimentValueObject>();
            }
            return new HashSet<ExpressionExperimentValueObject>();
        }

        String idRestrictionClause = "";
        if ( ids != null ) idRestrictionClause = "where ee.id in (:ids) ";

        final String queryString = "select ee.id as id, " // 0
                + "ee.name, " // 1
                + "ED.name, " // 2
                + "ED.webUri, " // 3
                + "ee.source, " // 4
                + "acc.accession, " // 5
                + "taxon.commonName," // 6
                + "taxon.id," // 7
                + "count(distinct BA), " // 8
                + "count(distinct AD), " // 9
                + "ee.shortName, " // 10
                + "s.createDate, " // 11
                + "AD.technologyType, ee.class, " // 12, 13
                + " EDES.id,  " // 14
                + " s.lastUpdateDate " // 15
                + " from ExpressionExperimentImpl as ee inner join ee.bioAssays as BA  "
                + "left join BA.samplesUsed as SU left join BA.arrayDesignUsed as AD "
                + "left join SU.sourceTaxon as taxon left join ee.accession acc left join acc.externalDatabase as ED "
                + " inner join ee.experimentalDesign as EDES join ee.status as s "
                + idRestrictionClause
                + " group by ee.id ";

        Query queryObject = super.getSession().createQuery( queryString );
        Map<Long, Collection<QuantitationType>> qtMap;
        if ( ids != null ) {
            for ( Long id : ids ) {
                ExpressionExperimentValueObject v = new ExpressionExperimentValueObject();
                vo.put( id, v );
            }
            List<Long> idl = new ArrayList<Long>( ids );
            Collections.sort( idl ); // so it's consistent and therefore cacheable.
            qtMap = getQuantitationTypeMap( idl );
            queryObject.setParameterList( "ids", idl );
        } else {
            qtMap = getQuantitationTypeMap( null );
        }

        queryObject.setCacheable( true );
        List<?> list = queryObject.list();
        for ( Object object : list ) {

            Object[] res = ( Object[] ) object;

            Long eeId = ( Long ) res[0];

            ExpressionExperimentValueObject v;
            if ( vo.containsKey( eeId ) ) {
                v = vo.get( eeId );
            } else {
                v = new ExpressionExperimentValueObject();
                vo.put( eeId, v );
            }

            v.setId( eeId );
            v.setName( ( String ) res[1] );
            v.setExternalDatabase( ( String ) res[2] );
            v.setExternalUri( ( String ) res[3] );
            v.setSource( ( String ) res[4] );
            v.setAccession( ( String ) res[5] );
            v.setTaxon( ( String ) res[6] );
            v.setTaxonId( ( Long ) res[7] );
            v.setBioAssayCount( ( ( Long ) res[8] ).intValue() );
            v.setArrayDesignCount( ( ( Long ) res[9] ).intValue() );
            v.setShortName( ( String ) res[10] );
            v.setDateCreated( ( ( Date ) res[11] ) );
            Object technology = res[12];
            if ( technology != null ) v.setTechnologyType( ( ( TechnologyType ) technology ).toString() );
            if ( !qtMap.isEmpty() && v.getTechnologyType() != null ) {
                fillQuantitationTypeInfo( qtMap, v, eeId, v.getTechnologyType() );
            }
            v.setClazz( ( String ) res[13] );
            v.setExperimentalDesign( ( Long ) res[14] );
            v.setDateLastUpdated( ( ( Date ) res[15] ) );
            vo.put( eeId, v );
        }

        if ( isList ) {
            List<ExpressionExperimentValueObject> result = new ArrayList<ExpressionExperimentValueObject>();
            for ( Long id : vo.keySet() ) {
                result.add( vo.get( id ) );
            }
            return result;
        }
        return vo.values();

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleThaw(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, boolean)
     */
    @Override
    protected ExpressionExperiment handleThaw( ExpressionExperiment ee, boolean vectorsAlso ) {
        if ( ee == null ) {
            return null;
        }

        if ( ee.getId() == null ) throw new IllegalArgumentException( "Id cannot be null, cannot be thawed: " + ee );

        /*
         * Trying to do everything fails miserably, so we still need a hybrid approach. But returning the thawed object,
         * as opposed to thawing the one passed in, solves problems.
         */
        String thawQuery = "select distinct e from ExpressionExperimentImpl e "
                + " left join fetch e.accession acc left join fetch acc.externalDatabase " + "where e.id=:eeid";

        List<?> res = this.getHibernateTemplate().findByNamedParam( thawQuery, "eeid", ee.getId() );

        if ( res.size() == 0 ) {
            throw new IllegalArgumentException( "No experiment with id=" + ee.getId() + " could be loaded." );
        }
        ExpressionExperiment result = ( ExpressionExperiment ) res.iterator().next();
        Hibernate.initialize( result.getQuantitationTypes() );
        Hibernate.initialize( result.getCharacteristics() );
        Hibernate.initialize( result.getRawDataFile() );
        Hibernate.initialize( result.getPrimaryPublication() );
        Hibernate.initialize( result.getBioAssays() );
        for ( BioAssay ba : result.getBioAssays() ) {
            Hibernate.initialize( ba.getArrayDesignUsed() );
            Hibernate.initialize( ba.getArrayDesignUsed().getDesignProvider() );
            Hibernate.initialize( ba.getDerivedDataFiles() );
            Hibernate.initialize( ba.getSamplesUsed() );
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                Hibernate.initialize( bm.getFactorValues() );
                Hibernate.initialize( bm.getTreatments() );
            }
        }

        ExperimentalDesign experimentalDesign = result.getExperimentalDesign();
        if ( experimentalDesign != null ) {
            Hibernate.initialize( experimentalDesign );
            Hibernate.initialize( experimentalDesign.getExperimentalFactors() );
            experimentalDesign.getTypes().size();
            for ( ExperimentalFactor factor : experimentalDesign.getExperimentalFactors() ) {
                Hibernate.initialize( factor.getAnnotations() );
                for ( FactorValue f : factor.getFactorValues() ) {
                    Hibernate.initialize( f.getCharacteristics() );
                    if ( f.getMeasurement() != null ) {
                        Hibernate.initialize( f.getMeasurement() );
                        if ( f.getMeasurement().getUnit() != null ) {
                            Hibernate.initialize( f.getMeasurement().getUnit() );
                        }
                    }
                }
            }
        }

        thawReferences( result );

        if ( vectorsAlso ) {
            /*
             * Optional because this could be slow.
             */
            Hibernate.initialize( result.getRawExpressionDataVectors() );
            Hibernate.initialize( result.getProcessedExpressionDataVectors() );

        }

        return result;
    }

    /**
     * @param qtMap
     * @param v
     * @param eeId
     * @param type
     */
    private void fillQuantitationTypeInfo( Map<Long, Collection<QuantitationType>> qtMap,
            ExpressionExperimentValueObject v, Long eeId, String type ) {

        assert qtMap != null;

        if ( v.getTechnologyType() != null && !v.getTechnologyType().equals( type ) ) {
            v.setTechnologyType( "MIXED" );
        } else {
            v.setTechnologyType( type );
        }

        if ( !type.equals( TechnologyType.ONECOLOR.toString() ) ) {
            Collection<QuantitationType> qts = qtMap.get( eeId );

            if ( qts == null ) {
                log.warn( "No quantitation types for EE=" + eeId + "?" );
                return;
            }

            boolean hasIntensityA = false;
            boolean hasIntensityB = false;
            boolean hasBothIntensities = false;
            boolean mayBeOneChannel = false;
            for ( QuantitationType qt : qts ) {
                if ( qt.getIsPreferred() && !qt.getIsRatio() ) {
                    /*
                     * This could be a dual-mode array, or it could be mis-labeled as two-color; or this might actually
                     * be ratios. In either case, we should flag it; as it stands we shouldn't use two-channel missing
                     * value analysis on it.
                     */
                    mayBeOneChannel = true;
                    break;
                } else if ( ChannelUtils.isSignalChannelA( qt.getName() ) ) {
                    hasIntensityA = true;
                    if ( hasIntensityB ) {
                        hasBothIntensities = true;
                    }
                } else if ( ChannelUtils.isSignalChannelB( qt.getName() ) ) {
                    hasIntensityB = true;
                    if ( hasIntensityA ) {
                        hasBothIntensities = true;
                    }
                }
            }

            v.setHasBothIntensities( hasBothIntensities && !mayBeOneChannel );
            v.setHasEitherIntensity( hasIntensityA || hasIntensityB );
        }
    }

    /**
     * @return map of EEids to Qts.
     */
    private Map<Long, Collection<QuantitationType>> getQuantitationTypeMap( Collection<Long> eeids ) {
        String queryString = "select ee, qts  from ExpressionExperimentImpl as ee inner join ee.quantitationTypes as qts ";
        if ( eeids != null ) {
            queryString = queryString + " where ee.id in (:eeids)";
        }
        org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
        // make sure we use the cache.
        if ( eeids != null ) {
            List<Long> idList = new ArrayList<Long>( eeids );
            Collections.sort( idList );
            queryObject.setParameterList( "eeids", idList );
        }
        queryObject.setReadOnly( true );
        queryObject.setCacheable( true );

        Map<Long, Collection<QuantitationType>> results = new HashMap<Long, Collection<QuantitationType>>();

        StopWatch timer = new StopWatch();
        timer.start();
        List<?> resultsList = queryObject.list();
        timer.stop();
        if ( timer.getTime() > 1000 ) {
            log.info( "Got QT info in " + timer.getTime() + "ms" );
        }

        for ( Object object : resultsList ) {
            Object[] ar = ( Object[] ) object;
            ExpressionExperiment ee = ( ExpressionExperiment ) ar[0];
            QuantitationType qt = ( QuantitationType ) ar[1];
            Long id = ee.getId();
            if ( !results.containsKey( id ) ) {
                results.put( id, new HashSet<QuantitationType>() );
            }
            results.get( id ).add( qt );
        }
        return results;
    }

    /**
     * @param expressionExperiment
     */
    private void thawReferences( final ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment.getPrimaryPublication() != null ) {
            Hibernate.initialize( expressionExperiment.getPrimaryPublication() );
            Hibernate.initialize( expressionExperiment.getPrimaryPublication().getPubAccession() );
            Hibernate.initialize( expressionExperiment.getPrimaryPublication().getPubAccession().getExternalDatabase() );
        }
        if ( expressionExperiment.getOtherRelevantPublications() != null ) {
            Hibernate.initialize( expressionExperiment.getOtherRelevantPublications() );
            for ( BibliographicReference bf : expressionExperiment.getOtherRelevantPublications() ) {
                Hibernate.initialize( bf.getPubAccession() );
                Hibernate.initialize( bf.getPubAccession().getExternalDatabase() );
            }
        }
    }

}
