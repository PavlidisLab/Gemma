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

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.*;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.LongType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableDao;
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

import java.util.*;

/**
 * @author pavlidis
 * @see ubic.gemma.model.expression.experiment.ExpressionExperiment
 */
@Repository
public class ExpressionExperimentDaoImpl extends AbstractCuratableDao<ExpressionExperiment> {

    private static final Log log = LogFactory.getLog( ExpressionExperimentDaoImpl.class.getName() );

    private static final int BATCH_SIZE = 1000;

    @Autowired
    public ExpressionExperimentDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public Collection<? extends ExpressionExperiment> create(
            final Collection<? extends ExpressionExperiment> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNewSession( new HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                for ( ExpressionExperiment entity : entities ) {
                    create( entity );
                }
                return null;
            }
        } );
        return entities;
    }

    @Override
    public ExpressionExperiment create( final ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperiment.create - 'expressionExperiment' can not be null" );
        }
        this.getHibernateTemplate().save( expressionExperiment );
        return expressionExperiment;
    }

    @Override
    public Collection<ExpressionExperiment> load( Collection<Long> ids ) {
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

    @SuppressWarnings("unchecked")
    private Collection<ExpressionExperiment> findByInvestigator( final String queryString,
            final Contact investigator ) {
        List<String> argNames = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        args.add( investigator );
        argNames.add( "investigator" );
        List<?> results = this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() );

        return ( Collection<ExpressionExperiment> ) results;
    }

    public Collection<ExpressionExperiment> findByInvestigator( final Contact investigator ) {
        return this.findByInvestigator(
                "from InvestigationImpl i inner join Contact c on c in elements(i.investigators) or c == i.owner where c == :investigator",
                investigator );
    }

    public Collection<ExpressionExperiment> findByName( final String name ) {
        return this.findByName( "from ExpressionExperimentImpl a where a.name=:name", name );
    }

    private Collection<ExpressionExperiment> findByName( final String queryString, final String name ) {
        List<String> argNames = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        args.add( name );
        argNames.add( "name" );
        return this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() );

    }

    public ExpressionExperiment findByShortName( final String shortName ) {
        return this.findByShortName( "from ExpressionExperimentImpl a where a.shortName=:shortName", shortName );
    }

    private ExpressionExperiment findByShortName( final String queryString, final String shortName ) {
        List<String> argNames = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        args.add( shortName );
        argNames.add( "shortName" );
        Set<ExpressionExperiment> results = new LinkedHashSet<>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        ExpressionExperiment result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ExpressionExperiment" + "' was found when executing query --> '"
                            + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return result;
    }

    public ExpressionExperiment load( final Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ExpressionExperiment.class, id );
        return ( ExpressionExperiment ) entity;
    }

    @Override
    public void remove( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.remove - 'id' can not be null" );
        }
        ExpressionExperiment entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    @Override
    public void remove( Collection<? extends ExpressionExperiment> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    public ExpressionExperiment thaw( final ExpressionExperiment expressionExperiment ) {
        return this.handleThaw( expressionExperiment, true );
    }

    public ExpressionExperiment thawBioAssays( final ExpressionExperiment expressionExperiment ) {
        return this.handleThaw( expressionExperiment, false );
    }

    public ExpressionExperiment thawBioAssaysLiter( final ExpressionExperiment expressionExperiment ) {
        return this.handleThawLiter( expressionExperiment, false );
    }

    public ExpressionExperimentValueObject toExpressionExperimentValueObject( final ExpressionExperiment entity ) {
        final ExpressionExperimentValueObject target = new ExpressionExperimentValueObject();
        this.toExpressionExperimentValueObject( entity, target );
        return target;
    }

    private void toExpressionExperimentValueObject( ExpressionExperiment source,
            ExpressionExperimentValueObject target ) {
        target.setId( source.getId() );
        target.setName( source.getName() );
        target.setSource( source.getSource() );
        // No conversion for target.accession (can't convert
        // source.getAccession():ubic.gemma.model.common.description.DatabaseEntry to String)
        target.setShortName( source.getShortName() );
    }

    @Override
    public void update( final Collection<? extends ExpressionExperiment> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.update - 'entities' can not be null" );
        }
        for ( ExpressionExperiment e : entities ) {
            update( e );
        }
    }

    public void update( ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperiment.update - 'expressionExperiment' can not be null" );
        }
        this.getSessionFactory().getCurrentSession().update( expressionExperiment );
    }

    public List<ExpressionExperiment> browse( Integer start, Integer limit ) {
        Query query = this.getSessionFactory().getCurrentSession().createQuery( "from ExpressionExperimentImpl" );
        query.setMaxResults( limit );
        query.setFirstResult( start );
        return query.list();
    }

    public List<ExpressionExperiment> browse( Integer start, Integer limit, String orderField, boolean descending ) {
        Query query = this.getSessionFactory().getCurrentSession().createQuery(
                "from ExpressionExperimentImpl order by " + orderField + " " + ( descending ? "desc" : "" ) );
        query.setMaxResults( limit );
        query.setFirstResult( start );
        return query.list();
    }

    public List<ExpressionExperiment> browseSpecificIds( Integer start, Integer limit, Collection<Long> ids ) {
        Query query = this.getSessionFactory().getCurrentSession()
                .createQuery( "from ExpressionExperimentImpl where id in (:ids) " );
        query.setParameterList( "ids", ids );
        query.setMaxResults( limit );
        query.setFirstResult( start );
        return query.list();
    }

    public List<ExpressionExperiment> browseSpecificIds( Integer start, Integer limit, String orderField,
            boolean descending, Collection<Long> ids ) {

        Query query = this.getSessionFactory().getCurrentSession().createQuery(
                "from ExpressionExperimentImpl where id in (:ids) order by " + orderField + " " + ( descending ?
                        "desc" :
                        "" ) );
        query.setParameterList( "ids", ids );
        query.setMaxResults( limit );
        query.setFirstResult( start );
        return query.list();
    }

    public Integer count() {
        return ( ( Long ) getHibernateTemplate().find( "select count(*) from ExpressionExperimentImpl" ).iterator()
                .next() ).intValue();
    }

    private ExpressionExperiment find( ExpressionExperiment expressionExperiment ) {

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

    public Collection<ExpressionExperiment> findByAccession( DatabaseEntry accession ) {

        DetachedCriteria crit = DetachedCriteria.forClass( ExpressionExperiment.class );

        BusinessKey.checkKey( accession );
        BusinessKey.attachCriteria( crit, accession, "accession" );
        return this.getHibernateTemplate().findByCriteria( crit );
    }

    public Collection<ExpressionExperiment> findByAccession( String accession ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select e from ExpressionExperimentImpl e inner join e.accession a where a.accession = :accession",
                "accession", accession );
    }

    public List<ExpressionExperiment> findByTaxon( Taxon taxon, Integer limit ) {
        final String queryString =
                "select distinct ee from ExpressionExperimentImpl as ee " + "inner join ee.bioAssays as ba "
                        + "inner join ba.sampleUsed as sample join ee.status s where sample.sourceTaxon = :taxon"
                        + " or sample.sourceTaxon.parentTaxon = :taxon order by s.lastUpdateDate desc";
        HibernateTemplate tpl = new HibernateTemplate( this.getSessionFactory() );
        if ( limit != null )
            tpl.setMaxResults( limit );
        return tpl.findByNamedParam( queryString, "taxon", taxon );
    }

    public List<ExpressionExperiment> findByUpdatedLimit( Collection<Long> ids, Integer limit ) {
        if ( ids.isEmpty() || limit <= 0 )
            return new ArrayList<>();

        Session s = this.getSessionFactory().getCurrentSession();

        String queryString = "select e from ExpressionExperimentImpl e join e.status s where e.id in (:ids) order by s.lastUpdateDate desc ";

        Query q = s.createQuery( queryString );
        q.setParameterList( "ids", ids );
        q.setMaxResults( limit );
        return q.list();

    }

    public List<ExpressionExperiment> findByUpdatedLimit( Integer limit ) {
        if ( limit == null )
            throw new IllegalArgumentException( "Limit must not be null" );
        if ( limit == 0 )
            return new ArrayList<>();
        Session s = this.getSessionFactory().getCurrentSession();
        String queryString = "select e from ExpressionExperimentImpl e join e.status s order by s.lastUpdateDate " + (
                limit < 0 ?
                        "asc" :
                        "desc" );
        Query q = s.createQuery( queryString );
        q.setMaxResults( Math.abs( limit ) );
        return q.list();
    }

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

    public Collection<ArrayDesign> getArrayDesignsUsed( BioAssaySet bas ) {

        ExpressionExperiment ee;
        if ( bas instanceof ExpressionExperimentSubSet ) {
            ee = ( ( ExpressionExperimentSubSet ) bas ).getSourceExperiment();
        } else {
            ee = ( ExpressionExperiment ) bas;
        }

        assert ee != null;

        return CommonQueries.getArrayDesignsUsed( ee, this.getSessionFactory().getCurrentSession() );
    }

    public Map<ArrayDesign, Collection<Long>> getArrayDesignsUsed( Collection<Long> eeids ) {
        return CommonQueries.getArrayDesignsUsed( eeids, this.getSessionFactory().getCurrentSession() );
    }

    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment ) {
        String queryString = "select distinct b from BioAssayDimensionImpl b, ExpressionExperimentImpl e "
                + "inner join b.bioAssays bba inner join e.bioAssays eb where eb = bba and e = :ee ";
        return getHibernateTemplate().findByNamedParam( queryString, "ee", expressionExperiment );
    }

    public Collection<ExpressionExperiment> getExperimentsWithOutliers() {
        return this.getHibernateTemplate()
                .find( "select distinct e from ExpressionExperimentImpl e join e.bioAssays b where b.isOutlier = 1" );
    }

    public Collection<ProcessedExpressionDataVector> getProcessedDataVectors(
            ExpressionExperiment expressionExperiment ) {
        final String queryString = "from ProcessedExpressionDataVectorImpl where expressionExperiment = :ee";
        return this.getHibernateTemplate().findByNamedParam( queryString, "ee", expressionExperiment );
    }

    @Override
    public Collection<ExpressionExperiment> loadAll() {
        Collection<ExpressionExperiment> ees;
        final String queryString = "from ExpressionExperimentImpl";

        Session session = this.getSessionFactory().getCurrentSession();
        org.hibernate.Query queryObject = session.createQuery( queryString );
        queryObject.setReadOnly( true );
        queryObject.setCacheable( true );
        StopWatch timer = new StopWatch();
        timer.start();
        ees = queryObject.list();
        if ( timer.getTime() > 1000 ) {
            log.info( ees.size() + " EEs loaded in " + timer.getTime() + "ms" );
        }

        return ees;
    }

    public List<ExpressionExperimentValueObject> loadAllValueObjects() {

        final String queryString = getLoadValueObjectsQueryString( null, null );

        Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( queryString );
        List<?> list = queryObject.list();

        Map<Long, ExpressionExperimentValueObject> vo = getExpressionExperimentValueObjectMap( list );

        return new ArrayList<>( vo.values() );

    }

    public List<ExpressionExperimentValueObject> loadAllValueObjectsTaxon( Taxon taxon ) {

        String idRestrictionClause = "where taxon.id = (:tid) or taxon.parentTaxon.id = (:tid) ";

        final String queryString = getLoadValueObjectsQueryString( idRestrictionClause, null );

        Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( queryString );

        queryObject.setParameter( "tid", taxon.getId() );

        List<?> list = queryObject.list();

        Map<Long, ExpressionExperimentValueObject> vo = getExpressionExperimentValueObjectMap( list );

        return new ArrayList<>( vo.values() );
    }

    public Collection<ExpressionExperiment> loadLackingEvent( Class<? extends AuditEventType> eventType ) {
        /*
         * I cannot figure out a way to do this with a left join in HQL.
         */
        Collection<ExpressionExperiment> allEEs = this.loadAll();
        allEEs.removeAll( loadWithEvent( eventType ) );
        return allEEs;
    }

    public Collection<ExpressionExperiment> loadLackingFactors() {
        return this.getHibernateTemplate()
                .find( "select e from ExpressionExperimentImpl e join e.experimentalDesign d where d.experimentalFactors.size =  0" );
    }

    public Collection<ExpressionExperiment> loadLackingTags() {
        return this.getHibernateTemplate()
                .find( "select e from ExpressionExperimentImpl e where e.characteristics.size = 0" );
    }

    public ExpressionExperimentValueObject loadValueObject( Long eeId ) {
        Collection<ExpressionExperimentValueObject> r = this.loadValueObjects( Collections.singleton( eeId ), false );
        if ( r.isEmpty() )
            return null;
        return r.iterator().next();
    }

    private Collection<ExpressionExperimentValueObject> loadValueObjects( Collection<Long> ids,
            boolean maintainOrder ) {

        boolean isList = ( ids != null && ids instanceof List );
        if ( ids == null || ids.size() == 0 ) {
            if ( isList ) {
                return new ArrayList<>();
            }
            return new HashSet<>();
        }

        String idRestrictionClause = "where ee.id in (:ids) ";

        final String queryString = getLoadValueObjectsQueryString( idRestrictionClause, null );

        Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( queryString );

        List<Long> idl = new ArrayList<>( ids );
        Collections.sort( idl ); // so it's consistent and therefore cacheable.

        queryObject.setParameterList( "ids", idl );
        queryObject.setCacheable( true );
        List<?> list = queryObject.list();

        Map<Long, ExpressionExperimentValueObject> vo = getExpressionExperimentValueObjectMap( list,
                getQuantitationTypeMap( idl ), ids.size() );

        /*
         * Remove items we didn't get back out. This is defensiveness!
         */

        Collection<ExpressionExperimentValueObject> finalValues = new LinkedHashSet<>();

        Set<Long> voIds = vo.keySet();
        if ( maintainOrder ) {
            Set<Long> orderedVoIds = new LinkedHashSet<>( voIds.size() );
            for ( Long eeId : ids ) {
                if ( voIds.contains( eeId ) ) {
                    orderedVoIds.add( eeId );
                }
            }
            voIds = orderedVoIds;
        }

        for ( Long id : voIds ) {
            if ( vo.get( id ).getId() != null ) {
                finalValues.add( vo.get( id ) );
            } else {
                log.warn( "No value object was fetched for EE with id=" + id );
            }
        }

        if ( finalValues.isEmpty() ) {
            log.error( "No values were retrieved for the ids provided" );
        }

        if ( isList ) {
            return new ArrayList<>( finalValues );
        }

        return finalValues;

    }

    private Collection<ExpressionExperiment> loadWithEvent( Class<? extends AuditEventType> eventType ) {
        String className = eventType.getSimpleName().endsWith( "Impl" ) ?
                eventType.getSimpleName() :
                eventType.getSimpleName() + "Impl";
        return this.getHibernateTemplate().findByNamedParam(
                "select distinct e from ExpressionExperimentImpl e join e.auditTrail a join a.events ae join ae.eventType t where t.class = :type ",
                "type", className );
    }

    @Override
    public void remove( final ExpressionExperiment toDelete ) {

        if ( toDelete == null )
            throw new IllegalArgumentException();

        Session session = this.getSessionFactory().getCurrentSession();

        // Note that links and analyses are deleted separately - see the ExpressionExperimentService.

        // At this point, the ee is probably still in the session, as the service already has gotten it
        // in this transaction.
        session.flush();
        // session.clear();

        session.buildLockRequest( LockOptions.NONE ).lock( toDelete );

        Hibernate.initialize( toDelete.getAuditTrail() );

        Set<BioAssayDimension> dims = new HashSet<>();
        Set<QuantitationType> qts = new HashSet<>();
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
        Map<BioAssay, BioMaterial> copyOfRelations = new HashMap<>();

        Collection<BioMaterial> bioMaterialsToDelete = new HashSet<>();
        Collection<BioAssay> bioAssays = toDelete.getBioAssays();

        for ( BioAssay ba : bioAssays ) {
            // relations to files cascade, so we only have to worry about biomaterials, which aren't cascaded from
            // anywhere. BioAssay -> BioMaterial is many-to-one, but bioassayset (experiment) owns the bioAssay.

            BioMaterial biomaterial = ba.getSampleUsed();

            if ( biomaterial == null )
                continue; // shouldn't...

            bioMaterialsToDelete.add( biomaterial );

            copyOfRelations.put( ba, biomaterial );

            // see bug 855
            session.buildLockRequest( LockOptions.NONE ).lock( biomaterial );
            Hibernate.initialize( biomaterial );

            // this can easily end up with an unattached object.
            Hibernate.initialize( biomaterial.getBioAssaysUsedIn() );

            biomaterial.getFactorValues().clear();
            biomaterial.getBioAssaysUsedIn().clear();

            ba.setSampleUsed( null );
        }

        log.info( "Last bits ..." );

        // We delete them here in case they are associated to more than one bioassay-- no cascade is possible.
        for ( BioMaterial bm : bioMaterialsToDelete ) {
            session.delete( bm );
        }

        for ( QuantitationType qt : qts ) {
            session.delete( qt );
        }

        session.flush();
        session.delete( toDelete );

            /*
             * Put transient instances back. This is possibly useful for clearing ACLS.
             */
        toDelete.setProcessedExpressionDataVectors( processedVectors );
        toDelete.setRawExpressionDataVectors( designElementDataVectors );
        for ( BioAssay ba : toDelete.getBioAssays() ) {
            ba.setSampleUsed( copyOfRelations.get( ba ) );
        }

        log.info( "Deleted " + toDelete );

    }

    private Integer countAll() {
        final String queryString = "select count(*) from ExpressionExperimentImpl";
        List<?> list = getHibernateTemplate().find( queryString );
        return ( ( Long ) list.iterator().next() ).intValue();
    }

    private Collection<ExpressionExperiment> findByBibliographicReference( Long bibRefID ) {
        final String queryString =
                "select distinct ee FROM ExpressionExperimentImpl as ee left join ee.otherRelevantPublications as eeO"
                        + " WHERE ee.primaryPublication.id = :bibID OR (eeO.id = :bibID) ";
        return getHibernateTemplate().findByNamedParam( queryString, "bibID", bibRefID );
    }

    private ExpressionExperiment findByBioAssay( BioAssay ba ) {

        final String queryString =
                "select distinct ee from ExpressionExperimentImpl as ee inner join ee.bioAssays as ba "
                        + "where ba = :ba";
        List<?> list = getHibernateTemplate().findByNamedParam( queryString, "ba", ba );
        if ( list.size() == 0 ) {
            log.warn( "No expression experiment for " + ba );
            return null;
        }
        if ( list.size() > 1 ) {
            /*
             * This really shouldn't happen!
             */
            log.warn( "Found " + list.size() + " expression experiment for the given bio assay: " + ba
                    + " Only 1 returned." );
        }
        return ( ExpressionExperiment ) list.iterator().next();
    }

    private ExpressionExperiment findByBioMaterial( BioMaterial bm ) {

        final String queryString = "select distinct ee from ExpressionExperimentImpl as ee "
                + "inner join ee.bioAssays as ba inner join ba.sampleUsed as sample where sample = :bm";
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

    private Collection<ExpressionExperiment> findByBioMaterials( Collection<BioMaterial> bms ) {
        if ( bms == null || bms.size() == 0 ) {
            return new HashSet<>();
        }
        final String queryString = "select distinct ee from ExpressionExperimentImpl as ee "
                + "inner join ee.bioAssays as ba inner join ba.sampleUsed as sample where sample in (:bms)";
        Collection<ExpressionExperiment> results = new HashSet<>();
        Collection<BioMaterial> batch = new HashSet<>();
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

    private Collection<ExpressionExperiment> findByExpressedGene( Gene gene, Double rank ) {

        final String queryString = "SELECT DISTINCT ee.ID AS eeID FROM "
                + "GENE2CS g2s, COMPOSITE_SEQUENCE cs, PROCESSED_EXPRESSION_DATA_VECTOR dedv, INVESTIGATION ee "
                + "WHERE g2s.CS = cs.ID AND cs.ID = dedv.DESIGN_ELEMENT_FK AND dedv.EXPRESSION_EXPERIMENT_FK = ee.ID"
                + " AND g2s.gene = :geneID AND dedv.RANK_BY_MEAN >= :rank";

        Collection<Long> eeIds;

        try {
            Session session = super.getSessionFactory().getCurrentSession();
            org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
            queryObject.setLong( "geneID", gene.getId() );
            queryObject.setDouble( "rank", rank );
            queryObject.addScalar( "eeID", new LongType() );
            ScrollableResults results = queryObject.scroll();

            eeIds = new HashSet<>();

            // Post Processing
            while ( results.next() )
                eeIds.add( results.getLong( 0 ) );

            session.clear();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return ( Collection<ExpressionExperiment> ) this.load( eeIds );
    }

    private ExpressionExperiment findByFactor( ExperimentalFactor ef ) {
        final String queryString =
                "select distinct ee from ExpressionExperimentImpl as ee inner join ee.experimentalDesign ed "
                        + "inner join ed.experimentalFactors ef where ef = :ef ";

        List<?> results = getHibernateTemplate().findByNamedParam( queryString, "ef", ef );

        if ( results.size() == 0 ) {
            log.info( "There is no expression experiment that has factor = " + ef );
            return null;
        }
        return ( ExpressionExperiment ) results.iterator().next();
    }

    private ExpressionExperiment findByFactorValue( FactorValue fv ) {
        return findByFactorValue( fv.getId() );
    }

    private ExpressionExperiment findByFactorValue( Long factorValueId ) {
        final String queryString =
                "select distinct ee from ExpressionExperimentImpl as ee inner join ee.experimentalDesign ed "
                        + "inner join ed.experimentalFactors ef inner join ef.factorValues fv where fv.id = :fvId ";

        List<ExpressionExperiment> results = getHibernateTemplate()
                .findByNamedParam( queryString, "fvId", factorValueId );

        if ( results.size() == 0 ) {
            log.info( "There is no expression experiment that has factorValue ID= " + factorValueId );
            return null;
        }

        return results.get( 0 );
    }

    private Collection<ExpressionExperiment> FindByFactorValues( Collection<FactorValue> fvs ) {

        if ( fvs.isEmpty() )
            return new HashSet<>();

        // thaw the factor values.
        Collection<ExperimentalDesign> eds = this.getHibernateTemplate().findByNamedParam(
                "select ed from FactorValueImpl f join f.experimentalFactor ef "
                        + " join ef.experimentalDesign ed where f.id in (:ids)", "ids", EntityUtils.getIds( fvs ) );

        if ( eds.isEmpty() ) {
            return new HashSet<>();
        }

        final String queryString = "select distinct ee from ExpressionExperimentImpl as ee where ee.experimentalDesign in (:eds) ";
        Collection<ExpressionExperiment> results = new HashSet<>();
        Collection<ExperimentalDesign> batch = new HashSet<>();
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

    private Collection<ExpressionExperiment> FindByGene( Gene gene ) {

        /*
         * uses GENE2CS table.
         */
        final String queryString = "SELECT DISTINCT ee.ID AS eeID FROM "
                + "GENE2CS g2s, COMPOSITE_SEQUENCE cs, ARRAY_DESIGN ad, BIO_ASSAY ba, INVESTIGATION ee "
                + "WHERE g2s.CS = cs.ID AND ad.ID = cs.ARRAY_DESIGN_FK AND ba.ARRAY_DESIGN_USED_FK = ad.ID AND"
                + " ba.EXPRESSION_EXPERIMENT_FK = ee.ID AND g2s.GENE = :geneID";

        Collection<Long> eeIds;

        Session session = super.getSessionFactory().getCurrentSession();
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
        queryObject.setLong( "geneID", gene.getId() );
        queryObject.addScalar( "eeID", new LongType() );
        ScrollableResults results = queryObject.scroll();

        eeIds = new HashSet<>();

        while ( results.next() ) {
            eeIds.add( results.getLong( 0 ) );
        }

        return ( Collection<ExpressionExperiment> ) this.load( eeIds );
    }

    private Collection<ExpressionExperiment> FindByParentTaxon( Taxon taxon ) {
        final String queryString =
                "select distinct ee from ExpressionExperimentImpl as ee " + "inner join ee.bioAssays as ba "
                        + "inner join ba.sampleUsed as sample "
                        + "inner join sample.sourceTaxon as childtaxon where childtaxon.parentTaxon  = :taxon ";
        return getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    private ExpressionExperiment FindByQuantitationType( QuantitationType quantitationType ) {
        final String queryString =
                "select ee from ExpressionExperimentImpl as ee " + "inner join ee.quantitationTypes qt where qt = :qt ";
        List<?> results = getHibernateTemplate().findByNamedParam( queryString, "qt", quantitationType );
        if ( results.size() == 1 ) {
            return ( ExpressionExperiment ) results.iterator().next();
        } else if ( results.size() == 0 ) {
            return null;
        }
        throw new IllegalStateException( "More than one ExpressionExperiment associated with " + quantitationType );

    }

    private Collection<ExpressionExperiment> FindByTaxon( Taxon taxon ) {
        final String queryString =
                "select distinct ee from ExpressionExperimentImpl as ee " + "inner join ee.bioAssays as ba "
                        + "inner join ba.sampleUsed as sample where sample.sourceTaxon = :taxon or sample.sourceTaxon.parentTaxon = :taxon";
        return getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    private Map<Long, Integer> GetAnnotationCounts( Collection<Long> ids ) {
        Map<Long, Integer> results = new HashMap<>();
        for ( Long id : ids ) {
            results.put( id, 0 );
        }
        if ( ids.size() == 0 ) {
            return results;
        }
        String queryString = "select e.id,count(c.id) from ExpressionExperimentImpl e inner join e.characteristics c where e.id in (:ids) group by e.id";
        List<?> res = this.getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

        addIdsToResults( results, res );
        return results;
    }

    private void addIdsToResults( Map<Long, Integer> results, List<?> res ) {
        for ( Object r : res ) {
            Object[] ro = ( Object[] ) r;
            Long id = ( Long ) ro[0];
            Integer count = ( ( Long ) ro[1] ).intValue();
            results.put( id, count );
        }
    }

    @Deprecated

    protected Map<Long, Map<Long, Collection<AuditEvent>>> GetArrayDesignAuditEvents( Collection<Long> ids ) {
        final String queryString =
                "select ee.id, ad.id, event " + "from ExpressionExperimentImpl ee " + "inner join ee.bioAssays b "
                        + "inner join b.arrayDesignUsed ad " + "inner join ad.auditTrail trail "
                        + "inner join trail.events event " + "where ee.id in (:ids) ";

        List<?> result = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

        Map<Long, Map<Long, Collection<AuditEvent>>> eventMap = new HashMap<>();
        // process list of expression experiment ids that have events
        for ( Object o : result ) {
            Object[] row = ( Object[] ) o;
            Long eeId = ( Long ) row[0];
            Long adId = ( Long ) row[1];
            AuditEvent event = ( AuditEvent ) row[2];

            Map<Long, Collection<AuditEvent>> adEventMap = eventMap.get( eeId );
            if ( adEventMap == null ) {
                adEventMap = new HashMap<>();
                eventMap.put( eeId, adEventMap );
            }

            Collection<AuditEvent> events = adEventMap.get( adId );
            if ( events == null ) {
                events = new ArrayList<>();
                adEventMap.put( adId, events );
            }

            events.add( event );
        }
        return eventMap;

    }

    private Map<Long, Collection<AuditEvent>> GetAuditEvents( Collection<Long> ids ) {
        final String queryString =
                "select ee.id, auditEvent from ExpressionExperimentImpl ee inner join ee.auditTrail as auditTrail inner join auditTrail.events as auditEvent "
                        + " where ee.id in (:ids) ";

        List<?> result = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

        Map<Long, Collection<AuditEvent>> eventMap = new HashMap<>();

        for ( Object o : result ) {
            Object[] row = ( Object[] ) o;
            Long id = ( Long ) row[0];
            AuditEvent event = ( AuditEvent ) row[1];

            addEventsToMap( eventMap, id, event );
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

    private Integer GetBioAssayCountById( long Id ) {
        final String queryString =
                "select count(ba) from ExpressionExperimentImpl ee " + "inner join ee.bioAssays dedv where ee.id = :ee";
        List<?> list = getHibernateTemplate().findByNamedParam( queryString, "ee", Id );
        if ( list.size() == 0 ) {
            log.warn( "No vectors for experiment with id " + Id );
            return 0;
        }
        return ( ( Long ) list.iterator().next() ).intValue();
    }

    private Integer GetBioMaterialCount( ExpressionExperiment expressionExperiment ) {
        final String queryString =
                "select count(distinct sample) from ExpressionExperimentImpl as ee " + "inner join ee.bioAssays as ba "
                        + "inner join ba.sampleUsed as sample where ee.id = :eeId ";
        List<?> result = getHibernateTemplate().findByNamedParam( queryString, "eeId", expressionExperiment.getId() );
        return ( ( Long ) result.iterator().next() ).intValue();
    }

    private Integer GetDesignElementDataVectorCountById( long Id ) {

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

    private Collection<DesignElementDataVector> GetDesignElementDataVectors(
            Collection<CompositeSequence> designElements, QuantitationType quantitationType ) {
        if ( designElements == null || designElements.size() == 0 )
            return new HashSet<>();

        assert quantitationType.getId() != null;

        final String queryString =
                "select dev from RawExpressionDataVectorImpl as dev inner join dev.designElement as de "
                        + " where de in (:des) and dev.quantitationType = :qt";
        return getHibernateTemplate().findByNamedParam( queryString, new String[] { "des", "qt" },
                new Object[] { designElements, quantitationType } );
    }

    @SuppressWarnings("unchecked")
    private Collection<DesignElementDataVector> GetDesignElementDataVectors(
            Collection<QuantitationType> quantitationTypes ) {

        if ( quantitationTypes.isEmpty() ) {
            throw new IllegalArgumentException( "Must provide at least one quantitation type" );
        }

        // this essentially does a partial thaw.
        String queryString =
                "select dev from RawExpressionDataVectorImpl dev" + " inner join fetch dev.bioAssayDimension bd "
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

    private Map<Long, Date> GetLastArrayDesignUpdate( Collection<ExpressionExperiment> expressionExperiments ) {
        final String queryString = "select ee.id, max(s.lastUpdateDate) from ExpressionExperimentImpl as ee inner join "
                + "ee.bioAssays b inner join b.arrayDesignUsed a join a.status s "
                + " where ee in (:ees) group by ee.id ";

        List<?> res = this.getHibernateTemplate().findByNamedParam( queryString, "ees", expressionExperiments );

        assert ( !res.isEmpty() );

        Map<Long, Date> result = new HashMap<>();
        for ( Object o : res ) {
            Object[] oa = ( Object[] ) o;
            Long id = ( Long ) oa[0];
            Date d = ( Date ) oa[1];
            result.put( id, d );
        }
        return result;
    }

    private Date GetLastArrayDesignUpdate( ExpressionExperiment ee ) {

        final String queryString = "select max(s.lastUpdateDate) from ExpressionExperimentImpl as ee inner join "
                + "ee.bioAssays b inner join b.arrayDesignUsed a join a.status s " + " where ee = :ee ";

        List<?> res = this.getHibernateTemplate().findByNamedParam( queryString, "ee", ee );

        assert ( !res.isEmpty() );

        return ( Date ) res.iterator().next();
    }

    private QuantitationType GetMaskedPreferredQuantitationType( ExpressionExperiment ee ) {
        String queryString = "select q from ExpressionExperimentImpl e inner join e.quantitationTypes q where e = :ee and q.isMaskedPreferred = true";
        List<?> k = this.getHibernateTemplate().findByNamedParam( queryString, "ee", ee );
        if ( k.size() == 1 ) {
            return ( QuantitationType ) k.iterator().next();
        } else if ( k.size() > 1 ) {
            throw new IllegalStateException(
                    "There should only be one masked preferred quantitationtype per expressionexperiment (" + ee
                            + ")" );
        }
        return null;
    }

    private Map<Taxon, Long> GetPerTaxonCount() {

        Map<Taxon, Taxon> taxonParents = new HashMap<>();
        List<Object[]> tp = super.getHibernateTemplate()
                .find( "select t, p from TaxonImpl t left outer join t.parentTaxon p" );
        for ( Object[] o : tp ) {
            taxonParents.put( ( Taxon ) o[0], ( Taxon ) o[1] );
        }

        Map<Taxon, Long> taxonCount = new LinkedHashMap<>();
        String queryString = "select t, count(distinct ee) from ExpressionExperimentImpl "
                + "ee inner join ee.bioAssays as ba inner join ba.sampleUsed su "
                + "inner join su.sourceTaxon t group by t order by t.scientificName ";

        // it is important to cache this, as it gets called on the home page. Though it's actually fast.
        org.hibernate.Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( queryString );
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

    private Map<Long, Integer> GetPopulatedFactorCounts( Collection<Long> ids ) {
        Map<Long, Integer> results = new HashMap<>();
        if ( ids.size() == 0 ) {
            return results;
        }

        for ( Long id : ids ) {
            results.put( id, 0 );
        }

        String queryString =
                "select e.id,count(distinct ef.id) from ExpressionExperimentImpl e inner join e.bioAssays ba"
                        + " inner join ba.sampleUsed bm inner join bm.factorValues fv inner join fv.experimentalFactor ef where e.id in (:ids) group by e.id";
        List<?> res = this.getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

        addIdsToResults( results, res );
        return results;
    }

    private Map<Long, Integer> GetPopulatedFactorCountsExcludeBatch( Collection<Long> ids ) {
        Map<Long, Integer> results = new HashMap<>();
        if ( ids.size() == 0 ) {
            return results;
        }

        for ( Long id : ids ) {
            results.put( id, 0 );
        }

        String queryString =
                "select e.id,count(distinct ef.id) from ExpressionExperimentImpl e inner join e.bioAssays ba"
                        + " inner join ba.sampleUsed bm inner join bm.factorValues fv inner join fv.experimentalFactor ef "
                        + " inner join ef.category cat where e.id in (:ids) and cat.category != (:category) and ef.name != (:name) group by e.id";

        String[] names = { "ids", "category", "name" };
        Object[] values = { ids, ExperimentalFactorService.BATCH_FACTOR_CATEGORY_NAME,
                ExperimentalFactorService.BATCH_FACTOR_NAME };
        List<?> res = this.getHibernateTemplate().findByNamedParam( queryString, names, values );

        addIdsToResults( results, res );
        return results;
    }

    @SuppressWarnings("unchecked")
    private Map<QuantitationType, Integer> GetQuantitationTypeCountById( Long Id ) {

        final String queryString = "select quantType,count(*) as count "
                + "from ubic.gemma.model.expression.experiment.ExpressionExperimentImpl ee "
                + "inner join ee.rawExpressionDataVectors as vectors "
                + "inner join vectors.quantitationType as quantType " + "where ee.id = :id GROUP BY quantType.name";

        List<?> list = getHibernateTemplate().findByNamedParam( queryString, "id", Id );

        Map<QuantitationType, Integer> qtCounts = new HashMap<>();
        for ( Object[] tuple : ( List<Object[]> ) list ) {
            qtCounts.put( ( QuantitationType ) tuple[0], ( ( Long ) tuple[1] ).intValue() );
        }

        return qtCounts;
    }

    @SuppressWarnings("unchecked")
    private Collection<QuantitationType> GetQuantitationTypes( final ExpressionExperiment expressionExperiment ) {
        final String queryString = "select distinct quantType "
                + "from ubic.gemma.model.expression.experiment.ExpressionExperimentImpl ee "
                + "inner join ee.quantitationTypes as quantType fetch all properties where ee  = :ee ";

        List<?> qtypes = getHibernateTemplate().findByNamedParam( queryString, "ee", expressionExperiment );
        return ( Collection<QuantitationType> ) qtypes;

    }

    private Collection<QuantitationType> GetQuantitationTypes( ExpressionExperiment expressionExperiment,
            ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            return GetQuantitationTypes( expressionExperiment );
        }

        final String queryString = "select distinct quantType "
                + "from ubic.gemma.model.expression.experiment.ExpressionExperimentImpl ee "
                + "inner join  ee.quantitationTypes as quantType " + "inner join ee.bioAssays as ba "
                + "inner join ba.arrayDesignUsed ad " + "where ee = :ee and ad = :ad";

        return getHibernateTemplate().findByNamedParam( queryString, new String[] { "ee", "ad" },
                new Object[] { expressionExperiment, arrayDesign } );
    }

    private Map<ExpressionExperiment, Collection<AuditEvent>> GetSampleRemovalEvents(
            Collection<ExpressionExperiment> expressionExperiments ) {
        final String queryString = "select ee,ev from ExpressionExperimentImpl ee inner join ee.bioAssays ba "
                + "inner join ba.auditTrail trail inner join trail.events ev inner join ev.eventType et "
                + "inner join fetch ev.performer where ee in (:ees) and et.class = 'SampleRemovalEvent'";

        Map<ExpressionExperiment, Collection<AuditEvent>> result = new HashMap<>();
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

    private Collection<ExpressionExperimentSubSet> GetSubSets( ExpressionExperiment expressionExperiment ) {
        String queryString = "select eess from ExpressionExperimentSubSetImpl eess inner join eess.sourceExperiment ee where ee = :ee";
        return this.getHibernateTemplate().findByNamedParam( queryString, "ee", expressionExperiment );
    }

    private Taxon GetTaxon( BioAssaySet ee ) {

        HibernateTemplate tp = new HibernateTemplate( this.getSessionFactory() );
        tp.setMaxResults( 1 );

        if ( ee instanceof ExpressionExperiment ) {
            String queryString = "select SU.sourceTaxon from ExpressionExperimentImpl as EE "
                    + "inner join EE.bioAssays as BA inner join BA.sampleUsed as SU where EE = :ee";
            List<?> list = tp.findByNamedParam( queryString, "ee", ee );
            if ( list.size() > 0 )
                return ( Taxon ) list.iterator().next();
        } else if ( ee instanceof ExpressionExperimentSubSet ) {
            String queryString =
                    "select su.sourceTaxon from ExpressionExperimentSubSetImpl eess inner join eess.sourceExperiment ee"
                            + " inner join ee.bioAssays as BA inner join BA.sampleUsed as su where eess = :ee";
            List<?> list = tp.findByNamedParam( queryString, "ee", ee );
            if ( list.size() > 0 )
                return ( Taxon ) list.iterator().next();
        } else {
            throw new UnsupportedOperationException(
                    "Can't get taxon of BioAssaySet of class " + ee.getClass().getName() );
        }

        return null;
    }

    public <T extends BioAssaySet> Map<T, Taxon> getTaxa( Collection<T> bioAssaySets ) {

        HibernateTemplate tp = new HibernateTemplate( this.getSessionFactory() );

        Map<T, Taxon> result = new HashMap<>();

        if ( bioAssaySets.isEmpty() )
            return result;

        // is this going to run into problems if there are too many ees given? Need to batch?
        T example = bioAssaySets.iterator().next();
        List<?> list;
        if ( ExpressionExperiment.class.isAssignableFrom( example.getClass() ) ) {
            String queryString = "select EE, SU.sourceTaxon from ExpressionExperimentImpl as EE "
                    + "inner join EE.bioAssays as BA inner join BA.sampleUsed as SU where EE in (:ees)";
            list = tp.findByNamedParam( queryString, "ees", bioAssaySets );
        } else if ( ExpressionExperimentSubSet.class.isAssignableFrom( example.getClass() ) ) {
            String queryString =
                    "select eess, su.sourceTaxon from ExpressionExperimentSubSetImpl eess inner join eess.sourceExperiment ee"
                            + " inner join ee.bioAssays as BA inner join BA.sampleUsed as su where eess in (:ees)";
            list = tp.findByNamedParam( queryString, "ees", bioAssaySets );
        } else {
            throw new UnsupportedOperationException(
                    "Can't get taxon of BioAssaySet of class " + example.getClass().getName() );
        }

        for ( Object o : list ) {
            Object[] oa = ( Object[] ) o;

            @SuppressWarnings("unchecked") T e = ( T ) oa[0];
            Taxon t = ( Taxon ) oa[1];
            result.put( e, t );

        }

        return result;
    }

    private Collection<ExpressionExperiment> Load( Collection<Long> ids ) {
        StopWatch timer = new StopWatch();
        timer.start();
        if ( ids == null || ids.size() == 0 ) {
            return new ArrayList<>();
        }

        Collection<ExpressionExperiment> ees;
        final String queryString = "from ExpressionExperimentImpl ee where ee.id in (:ids) ";
        List<Long> idList = new ArrayList<>( ids );
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

    private ExpressionExperiment handleThaw( ExpressionExperiment ee, boolean vectorsAlso ) {
        if ( ee == null ) {
            return null;
        }

        if ( ee.getId() == null )
            throw new IllegalArgumentException( "Id cannot be null, cannot be thawed: " + ee );

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
        Hibernate.initialize( result.getMeanVarianceRelation() );
        Hibernate.initialize( result.getQuantitationTypes() );
        Hibernate.initialize( result.getCharacteristics() );
        Hibernate.initialize( result.getRawDataFile() );
        Hibernate.initialize( result.getPrimaryPublication() );
        Hibernate.initialize( result.getBioAssays() );
        Hibernate.initialize( result.getAuditTrail() );
        if ( result.getAuditTrail() != null )
            Hibernate.initialize( result.getAuditTrail().getEvents() );
        Hibernate.initialize( result.getCurationDetails() );

        for ( BioAssay ba : result.getBioAssays() ) {
            Hibernate.initialize( ba.getArrayDesignUsed() );
            Hibernate.initialize( ba.getArrayDesignUsed().getDesignProvider() );
            Hibernate.initialize( ba.getDerivedDataFiles() );
            Hibernate.initialize( ba.getSampleUsed() );
            BioMaterial bm = ba.getSampleUsed();
            Hibernate.initialize( bm.getFactorValues() );
            Hibernate.initialize( bm.getTreatments() );

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
        thawMeanVariance( result );

        if ( vectorsAlso ) {
            /*
             * Optional because this could be slow.
             */
            Hibernate.initialize( result.getRawExpressionDataVectors() );
            Hibernate.initialize( result.getProcessedExpressionDataVectors() );

        }

        return result;
    }

    // this is for front end display by the web app,

    private ExpressionExperiment handleThawLiter( ExpressionExperiment ee, boolean vectorsAlso ) {
        if ( ee == null ) {
            return null;
        }

        if ( ee.getId() == null )
            throw new IllegalArgumentException( "Id cannot be null, cannot be thawed: " + ee );

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
        Hibernate.initialize( result.getPrimaryPublication() );
        Hibernate.initialize( result.getCurationDetails() );

        ExperimentalDesign experimentalDesign = result.getExperimentalDesign();
        if ( experimentalDesign != null ) {
            Hibernate.initialize( experimentalDesign );

            Hibernate.initialize( experimentalDesign.getExperimentalFactors() );
        }

        thawReferences( result );
        thawMeanVariance( result );

        if ( vectorsAlso ) {
            /*
             * Optional because this could be slow.
             */
            Hibernate.initialize( result.getRawExpressionDataVectors() );
            Hibernate.initialize( result.getProcessedExpressionDataVectors() );

        }

        return result;
    }

    private void fillQuantitationTypeInfo( Map<Long, Collection<QuantitationType>> qtMap,
            ExpressionExperimentValueObject v, Long eeId, String type ) {

        assert qtMap != null;

        if ( v.getTechnologyType() != null && !v.getTechnologyType().equals( type ) ) {
            v.setTechnologyType( "MIXED" );
        } else {
            v.setTechnologyType( type );
        }

        if ( !type.equals( TechnologyType.ONECOLOR.toString() ) && !type.equals( TechnologyType.NONE.toString() ) ) {
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

    private Map<Long, ExpressionExperimentValueObject> getExpressionExperimentValueObjectMap( List<?> list ) {
        return getExpressionExperimentValueObjectMap( list, null, null );
    }

    private Map<Long, ExpressionExperimentValueObject> getExpressionExperimentValueObjectMap( List<?> list,
            Map<Long, Collection<QuantitationType>> qtMap, Integer initialSize ) {

        Map<Long, ExpressionExperimentValueObject> vo;

        if ( initialSize == null ) {
            vo = new LinkedHashMap<>();
        } else {
            vo = new LinkedHashMap<>( initialSize );
        }

        for ( Object object : list ) {

            Object[] res = ( Object[] ) object;

            Long eeId = ( Long ) res[0];

            assert eeId != null;

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
            v.setTroubled( ( ( Boolean ) res[17] ) );
            v.setValidated( ( Boolean ) res[18] );
            v.setParentTaxonId( ( Long ) res[21] );

            Object technology = res[12];
            if ( technology != null ) {
                v.setTechnologyType( technology.toString() );
            }
            if ( qtMap != null && !qtMap.isEmpty() && v.getTechnologyType() != null ) {
                fillQuantitationTypeInfo( qtMap, v, eeId, v.getTechnologyType() );
            }

            v.setClazz( ( String ) res[13] );
            v.setExperimentalDesign( ( Long ) res[14] );
            v.setDateLastUpdated( ( ( Date ) res[15] ) );
            v.setBioMaterialCount( ( ( Long ) res[19] ).intValue() );

            if ( res[20] != null ) {
                v.setProcessedExpressionVectorCount( ( Integer ) res[20] );
            }

            vo.put( eeId, v );
        }

        if ( !vo.isEmpty() )
            populateAnalysisInformation( vo );

        return vo;

    }

    /**
     * Filling 'hasDifferentialExpressionAnalysis' and 'hasCoexpressionAnalysis'
     */
    private void populateAnalysisInformation( Map<Long, ExpressionExperimentValueObject> vo ) {

        StopWatch timer = new StopWatch();
        timer.start();
        List<Long> withCoex = this.getSessionFactory().getCurrentSession().createQuery(
                "select experimentAnalyzed.id from CoexpressionAnalysisImpl where experimentAnalyzed.id in (:ids)" )
                .setParameterList( "ids", vo.keySet() ).list();

        for ( Long id : withCoex ) {
            vo.get( id ).setHasCoexpressionAnalysis( true );
        }

        List<Long> withDiffEx = this.getSessionFactory().getCurrentSession().createQuery(
                "select experimentAnalyzed.id from DifferentialExpressionAnalysisImpl where experimentAnalyzed.id in (:ids)" )
                .setParameterList( "ids", vo.keySet() ).list();

        for ( Long id : withDiffEx ) {
            vo.get( id ).setHasDifferentialExpressionAnalysis( true );
        }

        if ( timer.getTime() > 200 ) {
            log.info( "Populate analysis info for " + vo.size() + " eevos: " + timer.getTime() + "ms" );
        }

    }

    private String getLoadValueObjectsQueryString( String idRestrictionClause, String orderByClause ) {
        String queryString = "select ee.id as id, " // 0
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
                + " s.lastUpdateDate, " // 15
                + " AD.status, " // 16
                + " s.troubled, " // 17
                + " s.validated, " // 18
                + " count(distinct SU), " // 19
                + " ee.numberOfDataVectors, " // 20
                + " ptax.id " // 21
                + " from ExpressionExperimentImpl as ee " + " inner join ee.bioAssays as BA  "
                + " left join BA.sampleUsed as SU left join BA.arrayDesignUsed as AD "
                + " left join SU.sourceTaxon as taxon left join ee.accession acc left join acc.externalDatabase as ED "
                + " left join taxon.parentTaxon as ptax "
                + " inner join ee.experimentalDesign as EDES join ee.status as s ";

        if ( idRestrictionClause != null ) {
            queryString = queryString + idRestrictionClause;
        }

        queryString = queryString + " group by ee.id ";

        if ( orderByClause != null ) {
            queryString = queryString + orderByClause;
        }

        return queryString;

    }

    /**
     * @return map of EEids to Qts.
     */
    private Map<Long, Collection<QuantitationType>> getQuantitationTypeMap( Collection<Long> eeids ) {
        String queryString = "select ee, qts  from ExpressionExperimentImpl as ee inner join ee.quantitationTypes as qts ";
        if ( eeids != null ) {
            queryString = queryString + " where ee.id in (:eeids)";
        }
        org.hibernate.Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( queryString );
        // make sure we use the cache.
        if ( eeids != null ) {
            List<Long> idList = new ArrayList<>( eeids );
            Collections.sort( idList );
            queryObject.setParameterList( "eeids", idList );
        }
        queryObject.setReadOnly( true );
        queryObject.setCacheable( true );

        Map<Long, Collection<QuantitationType>> results = new HashMap<>();

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

    private void thawReferences( final ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment.getPrimaryPublication() != null ) {
            Hibernate.initialize( expressionExperiment.getPrimaryPublication() );
            Hibernate.initialize( expressionExperiment.getPrimaryPublication().getPubAccession() );
            Hibernate
                    .initialize( expressionExperiment.getPrimaryPublication().getPubAccession().getExternalDatabase() );
            Hibernate.initialize( expressionExperiment.getPrimaryPublication().getPublicationTypes() );
        }
        if ( expressionExperiment.getOtherRelevantPublications() != null ) {
            Hibernate.initialize( expressionExperiment.getOtherRelevantPublications() );
            for ( BibliographicReference bf : expressionExperiment.getOtherRelevantPublications() ) {
                Hibernate.initialize( bf.getPubAccession() );
                Hibernate.initialize( bf.getPubAccession().getExternalDatabase() );
                Hibernate.initialize( bf.getPublicationTypes() );
            }
        }
    }

    private void thawMeanVariance( final ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment.getMeanVarianceRelation() != null ) {
            Hibernate.initialize( expressionExperiment.getMeanVarianceRelation() );
            Hibernate.initialize( expressionExperiment.getMeanVarianceRelation().getMeans() );
            Hibernate.initialize( expressionExperiment.getMeanVarianceRelation().getVariances() );
        }
    }

}
