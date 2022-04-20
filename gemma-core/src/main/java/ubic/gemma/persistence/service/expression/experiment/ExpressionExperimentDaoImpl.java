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
package ubic.gemma.persistence.service.expression.experiment;

import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclSid;
import lombok.NonNull;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.LongType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.core.analysis.expression.diff.BaselineSelection;
import ubic.gemma.core.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.core.util.StopWatchUtils;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AbstractCuratableDao;
import ubic.gemma.persistence.service.common.description.CharacteristicDao;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayDao;
import ubic.gemma.persistence.service.genome.taxon.TaxonDao;
import ubic.gemma.persistence.util.*;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author pavlidis
 * @see ubic.gemma.model.expression.experiment.ExpressionExperiment
 */
@Repository
public class ExpressionExperimentDaoImpl
        extends AbstractCuratableDao<ExpressionExperiment, ExpressionExperimentValueObject>
        implements ExpressionExperimentDao {

    private static final int BATCH_SIZE = 1000;

    @Autowired
    public ExpressionExperimentDaoImpl( SessionFactory sessionFactory ) {
        super( ExpressionExperimentDao.OBJECT_ALIAS, ExpressionExperiment.class, sessionFactory );
    }

    @Override
    public List<ExpressionExperiment> browse( Integer start, Integer limit ) {
        Query query = this.getSessionFactory().getCurrentSession().createQuery( "from ExpressionExperiment" );
        if ( limit > 0 )
            query.setMaxResults( limit );
        query.setFirstResult( start );

        //noinspection unchecked
        return query.list();
    }

    @Override
    public Integer countNotTroubled() {
        return ( ( Long ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select count( distinct ee ) from ExpressionExperiment as ee left join "
                                + " ee.bioAssays as ba left join ba.arrayDesignUsed as ad"
                                + " where ee.curationDetails.troubled = false and ad.curationDetails.troubled = false" )
                .uniqueResult() ).intValue();
    }

    @Override
    public Collection<Long> filterByTaxon( Collection<Long> ids, Taxon taxon ) {

        if ( ids == null || ids.isEmpty() )
            return new HashSet<>();

        //language=HQL
        final String queryString =
                "select distinct ee.id from ExpressionExperiment as ee " + "inner join ee.bioAssays as ba "
                        + "inner join ba.sampleUsed as sample where sample.sourceTaxon = :taxon and ee.id in (:ids) ";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "taxon", taxon )
                .setParameterList( "ids", ids ).list();
    }

    @Override
    public ExpressionExperiment find( ExpressionExperiment entity ) {

        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( ExpressionExperiment.class );

        if ( entity.getAccession() != null ) {
            criteria.add( Restrictions.eq( "accession", entity.getAccession() ) );
        } else if ( entity.getShortName() != null ) {
            criteria.add( Restrictions.eq( "shortName", entity.getShortName() ) );
        } else {
            criteria.add( Restrictions.eq( "name", entity.getName() ) );
        }

        return ( ExpressionExperiment ) criteria.uniqueResult();
    }

    @Override
    public Collection<ExpressionExperiment> findByAccession( DatabaseEntry accession ) {
        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( ExpressionExperiment.class );

        BusinessKey.checkKey( accession );
        BusinessKey.attachCriteria( criteria, accession, "accession" );

        //noinspection unchecked
        return criteria.list();
    }

    @Override
    public Collection<ExpressionExperiment> findByAccession( String accession ) {
        Query query = this.getSessionFactory().getCurrentSession().createQuery(
                        "select e from ExpressionExperiment e inner join e.accession a where a.accession = :accession" )
                .setParameter( "accession", accession );

        //noinspection unchecked
        return query.list();
    }

    @Override
    public Collection<ExpressionExperiment> findByBibliographicReference( Long bibRefID ) {
        //language=HQL
        final String queryString =
                "select distinct ee FROM ExpressionExperiment as ee left join ee.otherRelevantPublications as eeO"
                        + " WHERE ee.primaryPublication.id = :bibID OR (eeO.id = :bibID) ";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "bibID", bibRefID )
                .list();
    }

    @Override
    public ExpressionExperiment findByBioAssay( BioAssay ba ) {

        //language=HQL
        final String queryString =
                "select distinct ee from ExpressionExperiment as ee inner join ee.bioAssays as ba " + "where ba = :ba";
        List list = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ba", ba )
                .list();

        if ( list.size() == 0 ) {
            AbstractDao.log.warn( "No expression experiment for " + ba );
            return null;
        }

        if ( list.size() > 1 ) {
            /*
             * This really shouldn't happen!
             */
            AbstractDao.log.warn( "Found " + list.size() + " expression experiment for the given bio assay: " + ba
                    + " Only 1 returned." );
        }
        return ( ExpressionExperiment ) list.iterator().next();
    }

    @Override
    public ExpressionExperiment findByBioMaterial( BioMaterial bm ) {

        //language=HQL
        final String queryString = "select distinct ee from ExpressionExperiment as ee "
                + "inner join ee.bioAssays as ba inner join ba.sampleUsed as sample where sample = :bm";

        List list = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "bm", bm )
                .list();

        if ( list.size() == 0 ) {
            AbstractDao.log.warn( "No expression experiment for " + bm );
            return null;
        }
        if ( list.size() > 1 ) {
            /*
             * This really shouldn't happen!
             */
            AbstractDao.log.warn(
                    "Found " + list.size() + " expression experiment for the given bm: " + bm + " Only 1 returned." );
        }
        return ( ExpressionExperiment ) list.iterator().next();
    }

    @Override
    public Map<ExpressionExperiment, BioMaterial> findByBioMaterials( Collection<BioMaterial> bms ) {
        if ( bms == null || bms.size() == 0 ) {
            return new HashMap<>();
        }
        //language=HQL
        final String queryString = "select distinct ee, sample from ExpressionExperiment as ee "
                + "inner join ee.bioAssays as ba inner join ba.sampleUsed as sample where sample in (:bms) group by ee";

        Map<ExpressionExperiment, BioMaterial> results = new HashMap<>();
        Collection<BioMaterial> batch = new HashSet<>();

        for ( BioMaterial o : bms ) {
            batch.add( o );
            if ( batch.size() == ExpressionExperimentDaoImpl.BATCH_SIZE ) {

                //noinspection unchecked
                List<Object> r = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                        .setParameterList( "bms", batch ).list();
                for ( Object a : r ) {
                    ExpressionExperiment e = ( ExpressionExperiment ) ( ( Object[] ) a )[0];
                    BioMaterial b = ( BioMaterial ) ( ( Object[] ) a )[1]; // representative, there may have been multiple used as inputs
                    results.put( e, b );
                }
                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {

            //noinspection unchecked
            List<Object> r = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                    .setParameterList( "bms", batch ).list();
            for ( Object a : r ) {
                ExpressionExperiment e = ( ExpressionExperiment ) ( ( Object[] ) a )[0];
                BioMaterial b = ( BioMaterial ) ( ( Object[] ) a )[1]; // representative, there may have been multiple used as inputs
                results.put( e, b );
            }
        }

        return results;
    }

    @Override
    public Collection<ExpressionExperiment> findByExpressedGene( Gene gene, Double rank ) {

        //language=MySQL
        final String queryString = "SELECT DISTINCT ee.ID AS eeID FROM "
                + "GENE2CS g2s, COMPOSITE_SEQUENCE cs, PROCESSED_EXPRESSION_DATA_VECTOR dedv, INVESTIGATION ee "
                + "WHERE g2s.CS = cs.ID AND cs.ID = dedv.DESIGN_ELEMENT_FK AND dedv.EXPRESSION_EXPERIMENT_FK = ee.ID"
                + " AND g2s.gene = :geneID AND dedv.RANK_BY_MEAN >= :rank";

        Collection<Long> eeIds;

        Session session = this.getSessionFactory().getCurrentSession();
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

        return this.load( eeIds );
    }

    @Override
    public ExpressionExperiment findByFactor( ExperimentalFactor ef ) {
        //language=HQL
        final String queryString =
                "select distinct ee from ExpressionExperiment as ee inner join ee.experimentalDesign ed "
                        + "inner join ed.experimentalFactors ef where ef = :ef ";

        List results = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ef", ef )
                .list();

        if ( results.size() == 0 ) {
            AbstractDao.log.info( "There is no expression experiment that has factor = " + ef );
            return null;
        }
        return ( ExpressionExperiment ) results.iterator().next();
    }

    @Override
    public ExpressionExperiment findByFactorValue( FactorValue fv ) {
        return this.findByFactorValue( fv.getId() );
    }

    @Override
    public ExpressionExperiment findByFactorValue( Long factorValueId ) {
        //language=HQL
        final String queryString =
                "select distinct ee from ExpressionExperiment as ee inner join ee.experimentalDesign ed "
                        + "inner join ed.experimentalFactors ef inner join ef.factorValues fv where fv.id = :fvId ";

        //noinspection unchecked
        List<ExpressionExperiment> results = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "fvId", factorValueId ).list();

        if ( results.size() == 0 ) {
            return null;
        }

        return results.get( 0 );
    }

    @Override
    public Map<ExpressionExperiment, FactorValue> findByFactorValues( Collection<FactorValue> fvs ) {

        if ( fvs.isEmpty() )
            return new HashMap<>();

        //language=HQL
        final String queryString = "select distinct ee, f from ExpressionExperiment ee "
                + " join ee.experimentalDesign ed join ed.experimentalFactors ef join ef.factorValues f"
                + " where f in (:fvs) group by ee";
        Map<ExpressionExperiment, FactorValue> results = new HashMap<>();
        Collection<FactorValue> batch = new HashSet<>();
        for ( FactorValue o : fvs ) {
            batch.add( o );
            if ( batch.size() == ExpressionExperimentDaoImpl.BATCH_SIZE ) {

                //noinspection unchecked
                List<Object> r2 = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                        .setParameterList( "fvs", batch ).list();
                for ( Object o1 : r2 ) {
                    Object[] a = ( Object[] ) o1;
                    results.put( ( ExpressionExperiment ) a[0], ( FactorValue ) a[1] );
                }

                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {

            //noinspection unchecked
            List<Object> r2 = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                    .setParameterList( "fvs", batch ).list();
            for ( Object o1 : r2 ) {
                Object[] a = ( Object[] ) o1;
                results.put( ( ExpressionExperiment ) a[0], ( FactorValue ) a[1] );
            }

        }

        return results;

    }

    @Override
    public Collection<ExpressionExperiment> findByGene( Gene gene ) {

        /*
         * uses GENE2CS table.
         */
        //language=MySQL
        final String queryString = "SELECT DISTINCT ee.ID AS eeID FROM "
                + "GENE2CS g2s, COMPOSITE_SEQUENCE cs, ARRAY_DESIGN ad, BIO_ASSAY ba, INVESTIGATION ee "
                + "WHERE g2s.CS = cs.ID AND ad.ID = cs.ARRAY_DESIGN_FK AND ba.ARRAY_DESIGN_USED_FK = ad.ID AND"
                + " ba.EXPRESSION_EXPERIMENT_FK = ee.ID AND g2s.GENE = :geneID";

        Collection<Long> eeIds;

        Session session = this.getSessionFactory().getCurrentSession();
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
        queryObject.setLong( "geneID", gene.getId() );
        queryObject.addScalar( "eeID", new LongType() );
        ScrollableResults results = queryObject.scroll();

        eeIds = new HashSet<>();

        while ( results.next() ) {
            eeIds.add( results.getLong( 0 ) );
        }

        return this.load( eeIds );
    }

    @Override
    public ExpressionExperiment findByQuantitationType( QuantitationType quantitationType ) {
        //language=HQL
        final String queryString =
                "select ee from ExpressionExperiment as ee " + "inner join ee.quantitationTypes qt where qt = :qt ";

        List results = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "qt", quantitationType ).list();

        if ( results.size() == 1 ) {
            return ( ExpressionExperiment ) results.iterator().next();
        } else if ( results.size() == 0 ) {
            return null;
        }

        throw new IllegalStateException( "More than one ExpressionExperiment associated with " + quantitationType );
    }

    @Override
    public Collection<ExpressionExperiment> findByTaxon( Taxon taxon ) {
        //language=HQL
        //        final String queryString =
        //                "select distinct ee from ExpressionExperiment as ee " + "inner join ee.bioAssays as ba "
        //                        + "inner join ba.sampleUsed as sample where sample.sourceTaxon = :taxon ";
        final String queryString = "select ee from ExpressionExperiment as ee where ee.taxon = (:taxon)";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "taxon", taxon )
                .list();
    }

    @Override
    public List<ExpressionExperiment> findByTaxon( Taxon taxon, Integer limit ) {
        //language=HQL
        //        final String queryString =
        //                "select distinct ee from ExpressionExperiment as ee " + "inner join ee.bioAssays as ba "
        //                        + "inner join ba.sampleUsed as sample join ee.curationDetails s where sample.sourceTaxon = :taxon"
        //                        + " order by s.lastUpdated desc";
        final String queryString =
                "select ee from ExpressionExperiment as ee join ee.curationDetails s where ee.taxon = :taxon"
                        + " order by s.lastUpdated desc";
        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "taxon", taxon );

        if ( limit != null ) {
            query.setMaxResults( limit );
        }

        //noinspection unchecked
        return query.list();
    }

    @Override
    public List<ExpressionExperiment> findByUpdatedLimit( Collection<Long> ids, Integer limit ) {
        if ( ids.isEmpty() || limit <= 0 )
            return new ArrayList<>();

        Session s = this.getSessionFactory().getCurrentSession();

        String queryString = "select e from ExpressionExperiment e join e.curationDetails s where e.id in (:ids) order by s.lastUpdated desc ";

        Query q = s.createQuery( queryString );
        q.setParameterList( "ids", ids );
        q.setMaxResults( limit );

        //noinspection unchecked
        return q.list();

    }

    @Override
    public List<ExpressionExperiment> findByUpdatedLimit( Integer limit ) {
        if ( limit == 0 )
            return new ArrayList<>();
        Session s = this.getSessionFactory().getCurrentSession();
        String queryString = "select e from ExpressionExperiment e join e.curationDetails s order by s.lastUpdated " + (
                limit < 0 ?
                        "asc" :
                        "desc" );
        Query q = s.createQuery( queryString );
        q.setMaxResults( Math.abs( limit ) );

        //noinspection unchecked
        return q.list();
    }

    @Override
    public ExpressionExperiment findOrCreate( ExpressionExperiment entity ) {
        if ( entity.getShortName() == null && entity.getName() == null && entity.getAccession() == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment must have name or external accession." );
        }
        return super.findOrCreate( entity );
    }

    @Override
    public Collection<ExpressionExperiment> findUpdatedAfter( Date date ) {
        if ( date == null )
            return Collections.emptyList();
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select e from ExpressionExperiment e join e.curationDetails cd where cd.lastUpdated > :date" )
                .setDate( "date", date ).list();
    }

    @Override
    public Map<Long, Integer> getAnnotationCounts( Collection<Long> ids ) {
        Map<Long, Integer> results = new HashMap<>();
        for ( Long id : ids ) {
            results.put( id, 0 );
        }
        if ( ids.size() == 0 ) {
            return results;
        }
        String queryString = "select e.id,count(c.id) from ExpressionExperiment e inner join e.characteristics c where e.id in (:ids) group by e.id";
        List res = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();

        this.addIdsToResults( results, res );
        return results;
    }

    @Override
    public Collection<? extends AnnotationValueObject> getAnnotationsByBioMaterials( Long eeId ) {
        /*
         * Note we're not using 'distinct' here but the 'equals' for AnnotationValueObject should aggregate these. More
         * work to do.
         */
        List raw = this.getSessionFactory().getCurrentSession().createQuery(
                "select c from ExpressionExperiment e " + "join e.bioAssays ba join ba.sampleUsed bm "
                        + "join bm.characteristics c where e.id= :eeid" ).setParameter( "eeid", eeId ).list();

        Collection<AnnotationValueObject> results = new HashSet<>();
        /*
         * TODO we need to filter these better; some criteria could be included in the query
         */
        for ( Object o : raw ) {
            Characteristic c = ( Characteristic ) o;

            // filter. Could include this in the query if it isn't too complicated.
            if ( StringUtils.isBlank( c.getCategoryUri() ) ) {
                continue;
            }

            if ( StringUtils.isBlank( c.getValueUri() ) ) {
                continue;
            }

            if ( c.getCategory().equals( "MaterialType" ) || c.getCategory().equals( "molecular entity" )
                    || c.getCategory().equals( "LabelCompound" ) ) {
                continue;
            }

            AnnotationValueObject annotationValue = new AnnotationValueObject();
            annotationValue.setClassName( c.getCategory() );
            annotationValue.setTermName( c.getValue() );
            annotationValue.setEvidenceCode( c.getEvidenceCode() != null ? c.getEvidenceCode().toString() : "" );

            annotationValue.setClassUri( c.getCategoryUri() );

            annotationValue.setTermUri( c.getValueUri() );

            annotationValue.setObjectClass( BioMaterial.class.getSimpleName() );

            results.add( annotationValue );
        }

        return results;

    }

    @Override
    public Collection<? extends AnnotationValueObject> getAnnotationsByFactorvalues( Long eeId ) {
        List raw = this.getSessionFactory().getCurrentSession().createQuery( "select c from ExpressionExperiment e "
                + "join e.experimentalDesign ed join ed.experimentalFactors ef join ef.factorValues fv "
                + "join fv.characteristics c where e.id= :eeid " ).setParameter( "eeid", eeId ).list();

        /*
         * FIXME filtering here is going to have to be more elaborate for this to be useful.
         */
        Collection<AnnotationValueObject> results = new HashSet<>();
        for ( Object o : raw ) {
            Characteristic c = ( Characteristic ) o;

            // ignore free text values
            if ( StringUtils.isBlank( c.getValueUri() ) ) {
                continue;
            }

            // ignore baseline and batch factorvalues (could include in the query)
            if ( BaselineSelection.isBaselineCondition( c ) || ( StringUtils.isNotBlank( c.getCategory() )
                    && c.getCategory().equals( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME ) ) ) {
                continue;
            }

            // ignore timepoint.
            if ( StringUtils.isNotBlank( c.getCategoryUri() ) && c.getCategoryUri()
                    .equals( "http://www.ebi.ac.uk/efo/EFO_0000724" ) ) {
                continue;
            }

            if ( StringUtils.isNotBlank( c.getValueUri() ) ) {
                // DE_include/exclude
                if ( c.getValueUri().equals( "http://purl.obolibrary.org/obo/TGEMO_00013" ) )
                    continue;
                if ( c.getValueUri().equals( "http://purl.obolibrary.org/obo/TGEMO_00014" ) )
                    continue;
            }

            AnnotationValueObject annotationValue = new AnnotationValueObject();
            annotationValue.setClassName( c.getCategory() );
            annotationValue.setTermName( c.getValue() );
            annotationValue.setEvidenceCode( c.getEvidenceCode() != null ? c.getEvidenceCode().toString() : "" );

            annotationValue.setClassUri( c.getCategoryUri() );

            annotationValue.setTermUri( c.getValueUri() );

            annotationValue.setObjectClass( FactorValue.class.getSimpleName() );

            results.add( annotationValue );
        }

        return results;

    }

    @Override
    public Collection<ExpressionExperiment> getExperimentsLackingPublications() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "select e from ExpressionExperiment e where e.primaryPublication = null and e.shortName like 'GSE%'" ).list();
    }

    @Override
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

    @Override
    public Map<ArrayDesign, Collection<Long>> getArrayDesignsUsed( Collection<Long> eeids ) {
        return CommonQueries.getArrayDesignsUsed( eeids, this.getSessionFactory().getCurrentSession() );
    }

    @Override
    public Map<Long, Collection<AuditEvent>> getAuditEvents( Collection<Long> ids ) {
        //language=HQL
        final String queryString =
                "select ee.id, auditEvent from ExpressionExperiment ee inner join ee.auditTrail as auditTrail inner join auditTrail.events as auditEvent "
                        + " where ee.id in (:ids) ";

        List result = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();

        Map<Long, Collection<AuditEvent>> eventMap = new HashMap<>();

        for ( Object o : result ) {
            Object[] row = ( Object[] ) o;
            Long id = ( Long ) row[0];
            AuditEvent event = ( AuditEvent ) row[1];

            this.addEventsToMap( eventMap, id, event );
        }
        // add in expression experiment ids that do not have events. Set
        // their values to null.
        for ( Long id : ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, null );
            }
        }
        return eventMap;

    }

    @Override
    public Integer getBioAssayCountById( long Id ) {
        //language=HQL
        final String queryString =
                "select count(ba) from ExpressionExperiment ee " + "inner join ee.bioAssays ba where ee.id = :ee";

        List list = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ee", Id )
                .list();

        if ( list.size() == 0 ) {
            AbstractDao.log.warn( "No vectors for experiment with id " + Id );
            return 0;
        }

        return ( ( Long ) list.iterator().next() ).intValue();
    }

    @Override
    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment ) {
        String queryString = "select distinct b from BioAssayDimension b, ExpressionExperiment e "
                + "inner join b.bioAssays bba inner join e.bioAssays eb where eb = bba and e = :ee ";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ee", expressionExperiment ).list();
    }

    @Override
    public Integer getBioMaterialCount( ExpressionExperiment expressionExperiment ) {
        //language=HQL
        final String queryString =
                "select count(distinct sample) from ExpressionExperiment as ee " + "inner join ee.bioAssays as ba "
                        + "inner join ba.sampleUsed as sample where ee.id = :eeId ";

        List result = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "eeId", expressionExperiment.getId() ).list();

        return ( ( Long ) result.iterator().next() ).intValue();
    }

    /**
     * @param Id if of the expression experiment
     * @return count of RAW vectors.
     */
    @Override
    public Integer getDesignElementDataVectorCountById( long Id ) {

        //language=HQL
        final String queryString = "select count(dedv) from ExpressionExperiment ee "
                + "inner join ee.rawExpressionDataVectors dedv where ee.id = :ee";

        List list = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ee", Id )
                .list();
        if ( list.size() == 0 ) {
            AbstractDao.log.warn( "No vectors for experiment with id " + Id );
            return 0;
        }
        return ( ( Long ) list.iterator().next() ).intValue();

    }

    @Override
    public Collection<ExpressionExperiment> getExperimentsWithOutliers() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct e from ExpressionExperiment e join e.bioAssays b where b.isOutlier = true" ).list();
    }

    @Override
    public Map<Long, Date> getLastArrayDesignUpdate( Collection<ExpressionExperiment> expressionExperiments ) {
        //language=HQL
        final String queryString = "select ee.id, max(s.lastUpdated) from ExpressionExperiment as ee inner join "
                + "ee.bioAssays b inner join b.arrayDesignUsed a join a.curationDetails s "
                + " where ee in (:ees) group by ee.id ";

        List res = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ees", expressionExperiments ).list();

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

    @Override
    public Date getLastArrayDesignUpdate( ExpressionExperiment ee ) {

        //language=HQL
        final String queryString = "select max(s.lastUpdated) from ExpressionExperiment as ee inner join "
                + "ee.bioAssays b inner join b.arrayDesignUsed a join a.curationDetails s " + " where ee = :ee ";

        List res = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ee", ee )
                .list();

        assert ( !res.isEmpty() );

        return ( Date ) res.iterator().next();
    }

    @Override
    public QuantitationType getMaskedPreferredQuantitationType( ExpressionExperiment ee ) {
        String queryString = "select q from ExpressionExperiment e inner join e.quantitationTypes q where e = :ee and q.isMaskedPreferred = true";
        List k = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ee", ee )
                .list();

        if ( k.size() == 1 ) {
            return ( QuantitationType ) k.iterator().next();
        } else if ( k.size() > 1 ) {
            throw new IllegalStateException(
                    "There should only be one masked preferred quantitationType per expressionExperiment (" + ee
                            + ")" );
        }
        return null;
    }

    @Override
    public Map<Taxon, Long> getPerTaxonCount() {
        String queryString = "select t, count(distinct ee) from ExpressionExperiment ee "
                + "join ee.bioAssays as ba "
                + "join ba.sampleUsed su "
                + "join su.sourceTaxon t "
                + "group by t";

        // it is important to cache this, as it gets called on the home page. Though it's actually fast.
        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setCacheable( true )
                .list();

        return list.stream()
                .collect( Collectors.toMap( row -> ( Taxon ) row[0], row -> ( Long ) row[1] ) );
    }

    @Override
    public Map<Long, Integer> getPopulatedFactorCounts( Collection<Long> ids ) {
        Map<Long, Integer> results = new HashMap<>();
        if ( ids.size() == 0 ) {
            return results;
        }

        for ( Long id : ids ) {
            results.put( id, 0 );
        }

        String queryString = "select e.id,count(distinct ef.id) from ExpressionExperiment e inner join e.bioAssays ba"
                + " inner join ba.sampleUsed bm inner join bm.factorValues fv inner join fv.experimentalFactor "
                + "ef where e.id in (:ids) group by e.id";
        List res = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();

        this.addIdsToResults( results, res );
        return results;
    }

    @Override
    public Map<Long, Integer> getPopulatedFactorCountsExcludeBatch( Collection<Long> ids ) {
        Map<Long, Integer> results = new HashMap<>();
        if ( ids.size() == 0 ) {
            return results;
        }

        for ( Long id : ids ) {
            results.put( id, 0 );
        }

        String queryString = "select e.id,count(distinct ef.id) from ExpressionExperiment e inner join e.bioAssays ba"
                + " inner join ba.sampleUsed bm inner join bm.factorValues fv inner join fv.experimentalFactor ef "
                + " inner join ef.category cat where e.id in (:ids) and cat.category != (:category) and ef.name != (:name) group by e.id";

        List res = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ) // Set ids
                .setParameter( "category", ExperimentalFactorService.BATCH_FACTOR_CATEGORY_NAME ) // Set batch category
                .setParameter( "name", ExperimentalFactorService.BATCH_FACTOR_NAME ) // set batch name
                .list();

        this.addIdsToResults( results, res );
        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<QuantitationType, Integer> getQuantitationTypeCountById( Long id ) {

        //language=HQL
        final String queryString = "select quantType,count(*) as count "
                + "from ubic.gemma.model.expression.experiment.ExpressionExperiment ee "
                + "inner join ee.rawExpressionDataVectors as vectors "
                + "inner join vectors.quantitationType as quantType " + "where ee.id = :id GROUP BY quantType.name";

        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "id", id )
                .list();

        Map<QuantitationType, Integer> qtCounts = new HashMap<>();

        for ( Object[] tuple : list ) {
            qtCounts.put( ( QuantitationType ) tuple[0], ( ( Long ) tuple[1] ).intValue() );
        }

        return qtCounts;
    }

    @Override
    public Collection<QuantitationType> getQuantitationTypes( final ExpressionExperiment expressionExperiment ) {
        //language=HQL
        final String queryString = "select distinct quantType " + "from ExpressionExperiment ee "
                + "inner join ee.quantitationTypes as quantType fetch all properties where ee  = :ee ";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ee", expressionExperiment ).list();
    }

    @Override
    public Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment,
            ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            return this.getQuantitationTypes( expressionExperiment );
        }

        //language=HQL
        final String queryString =
                "select distinct quantType " + "from ubic.gemma.model.expression.experiment.ExpressionExperiment ee "
                        + "inner join  ee.quantitationTypes as quantType " + "inner join ee.bioAssays as ba "
                        + "inner join ba.arrayDesignUsed ad " + "where ee = :ee and ad = :ad";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ee", expressionExperiment ) // Set the EE
                .setParameter( "ad", arrayDesign ) // Set the AD
                .list();
    }

    @Override
    public Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            Collection<ExpressionExperiment> expressionExperiments ) {
        //language=HQL
        final String queryString = "select ee,ev from ExpressionExperiment ee inner join ee.auditTrail trail inner join"
                + " trail.events ev inner join ev.eventType et "
                + "inner join fetch ev.performer where ee in (:ees) and et.class = 'SampleRemovalEvent'";

        Map<ExpressionExperiment, Collection<AuditEvent>> result = new HashMap<>();
        List r = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ees", expressionExperiments ).list();

        for ( Object o : r ) {
            Object[] ol = ( Object[] ) o;
            ExpressionExperiment e = ( ExpressionExperiment ) ol[0];
            if ( !result.containsKey( e ) ) {
                result.put( e, new HashSet<>() );
            }
            AuditEvent ae = ( AuditEvent ) ol[1];
            result.get( e ).add( ae );
        }
        return result;
    }

    @Override
    public Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment ) {
        String queryString = "select eess from ExpressionExperimentSubSet eess inner join eess.sourceExperiment ee where ee = :ee";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ee", expressionExperiment ).list();
    }

    @Override
    public <T extends BioAssaySet> Map<T, Taxon> getTaxa( Collection<T> bioAssaySets ) {
        Map<T, Taxon> result = new HashMap<>();

        if ( bioAssaySets.isEmpty() )
            return result;

        // is this going to run into problems if there are too many ees given? Need to batch?
        T example = bioAssaySets.iterator().next();

        // FIXME: multiple taxon can be returned for a given EE
        String queryString;
        if ( ExpressionExperiment.class.isAssignableFrom( example.getClass() ) ) {
            queryString = "select EE, st from ExpressionExperiment as EE "
                    + "join EE.bioAssays as BA join BA.sampleUsed as SU join SU.sourceTaxon st where EE in (:ees) "
                    + "group by EE";
        } else if ( ExpressionExperimentSubSet.class.isAssignableFrom( example.getClass() ) ) {
            queryString = "select eess, st from ExpressionExperimentSubSet eess "
                    + "join eess.sourceExperiment ee join ee.bioAssays as BA join BA.sampleUsed as su "
                    + "join su.sourceTaxon as st where eess in (:ees) group by eess";
        } else {
            throw new UnsupportedOperationException(
                    "Can't get taxon of BioAssaySet of class " + example.getClass().getName() );
        }

        // FIXME: this query cannot be made cacheable because the taxon is not initialized when retrieved from the cache, defeating the purpose of caching altogether
        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ees", bioAssaySets )
                .list();

        return list.stream().collect( Collectors.toMap( row -> ( T ) row[0], row -> ( Taxon ) row[1] ) );
    }

    @Override
    public Taxon getTaxon( BioAssaySet ee ) {

        if ( ee instanceof ExpressionExperiment ) {
            String queryString = "select distinct SU.sourceTaxon from ExpressionExperiment as EE "
                    + "inner join EE.bioAssays as BA inner join BA.sampleUsed as SU where EE = :ee";
            List list = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ee", ee )
                    .list();
            if ( list.size() > 0 )
                return ( Taxon ) list.iterator().next();
        } else if ( ee instanceof ExpressionExperimentSubSet ) {
            String queryString =
                    "select distinct su.sourceTaxon from ExpressionExperimentSubSet eess inner join eess.sourceExperiment ee"
                            + " inner join ee.bioAssays as BA inner join BA.sampleUsed as su where eess = :ee";
            List list = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ee", ee )
                    .list();
            if ( list.size() > 0 )
                return ( Taxon ) list.iterator().next();
        } else {
            throw new UnsupportedOperationException(
                    "Can't get taxon of BioAssaySet of class " + ee.getClass().getName() );
        }

        return null;
    }

    @Override
    public List<ExpressionExperimentValueObject> loadAllValueObjects() {
        return this.loadValueObjectsPreFilter( null, null );
    }

    @Override
    public Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjects( Filters filters, Sort sort, int offset, int limit ) {
        EnumSet<QueryHint> hints = EnumSet.noneOf( QueryHint.class );

        if ( offset > 0 || limit > 0 )
            hints.add( QueryHint.PAGINATED );

        // Compose query
        Query query = this.getLoadValueObjectsQuery( filters, sort, hints );

        if ( offset > 0 ) {
            query.setFirstResult( offset );
        }
        if ( limit > 0 ) {
            query.setMaxResults( limit );
        }

        // overall timer
        StopWatch timer = StopWatch.createStarted();

        // timers for sub-steps
        StopWatch queryTimer = StopWatch.create();
        StopWatch countingTimer = StopWatch.create();
        StopWatch voTimer = StopWatch.create();
        StopWatch detailsTimer = StopWatch.create();
        StopWatch otherPartsTimer = StopWatch.create();

        queryTimer.start();
        //noinspection unchecked
        List<Object[]> list = query.list();
        queryTimer.stop();

        countingTimer.start();
        Long totalElements = ( Long ) this.getCountValueObjectsQuery( filters ).uniqueResult();
        countingTimer.stop();

        // sort + distinct for cache consistency
        List<ExpressionExperiment> expressionExperiments = list.stream()
                .map( row -> ( ExpressionExperiment ) row[0] )
                .sorted( Comparator.comparing( ExpressionExperiment::getId ) )
                .distinct()
                .collect( Collectors.toList() );

        // fetch some extras details
        // we could make this a single query in getLoadValueObjectDetails, but performing a jointure with the bioAssays
        // and arrayDesignUsed is inefficient in the general case, so we only fetch what we need here
        Map<ExpressionExperiment, List<Object[]>> detailsByEE;
        try ( StopWatchUtils.StopWatchRegion ignored = StopWatchUtils.measuredRegion( detailsTimer ) ) {
            detailsByEE = loadDetailsByEE( expressionExperiments );
        }

        List<ExpressionExperimentDetailsValueObject> vos = new ArrayList<>( list.size() );
        for ( Object[] row : list ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) row[0];
            AclObjectIdentity aoi = ( AclObjectIdentity ) row[1];
            AclSid sid = ( AclSid ) row[2];
            List<Object[]> details = detailsByEE.get( ee );

            ExpressionExperimentDetailsValueObject vo;
            try ( StopWatchUtils.StopWatchRegion ignored = StopWatchUtils.measuredRegion( voTimer ) ) {
                vo = new ExpressionExperimentDetailsValueObject( ee, aoi, sid );
            }

            Integer bioAssayCount = details.stream()
                    .map( row2 -> ( Integer ) row2[3] )
                    .findFirst()
                    .orElse( 0 );
            vo.setBioAssayCount( bioAssayCount );

            // we need those later for computing original platforms
            Collection<ArrayDesign> arrayDesignsUsed = details.stream()
                    .map( row2 -> ( ArrayDesign ) row2[1] )
                    .collect( Collectors.toSet() );

            Collection<ArrayDesignValueObject> adVos = arrayDesignsUsed.stream()
                    .map( ArrayDesignValueObject::new )
                    .collect( Collectors.toSet() );
            vo.setArrayDesigns( adVos ); // also sets taxon name, technology type, and number of ADs.

            // original platforms
            Collection<ArrayDesignValueObject> originalPlatformsVos = details.stream()
                    .map( row2 -> ( ArrayDesign ) row2[2] )
                    .filter( Objects::nonNull ) // on original platform for the bioAssay
                    .filter( op -> !arrayDesignsUsed.contains( op ) )
                    .map( ArrayDesignValueObject::new )
                    .collect( Collectors.toSet() );
            vo.setOriginalPlatforms( originalPlatformsVos );

            // other parts (maybe fetch in details query?)
            try ( StopWatchUtils.StopWatchRegion ignored = StopWatchUtils.measuredRegion( otherPartsTimer ) ) {
                vo.getOtherParts().addAll( ee.getOtherParts().stream().map( this::loadValueObject ).collect( Collectors.toList() ) );
            }

            vos.add( vo );
        }

        StopWatch analysisInformationTimer = StopWatch.create();
        try ( StopWatchUtils.StopWatchRegion ignored = StopWatchUtils.measuredRegion( analysisInformationTimer ) ) {
            this.populateAnalysisInformation( vos );
        }

        timer.stop();

        if ( timer.getTime() > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.info( "EE details VO query + postprocessing: " + timer.getTime() + " ms ("
                    + "query: " + queryTimer.getTime() + " ms, "
                    + "counting: " + countingTimer.getTime() + " ms, "
                    + "initializing VOs: " + voTimer.getTime() + " ms, "
                    + "loading details (bioAssays + bioAssays.arrayDesignUsed + originalPlatforms): " + detailsTimer.getTime() + " ms, "
                    + "otherParts: " + otherPartsTimer.getTime() + " ms, "
                    + "retrieving analysis information: " + analysisInformationTimer.getTime() + " ms)" );
        }

        return new Slice<>( vos, sort, offset, limit, totalElements );
    }

    private Map<ExpressionExperiment, List<Object[]>> loadDetailsByEE( Collection<ExpressionExperiment> expressionExperiments ) {
        if ( expressionExperiments.isEmpty() )
            return Collections.emptyMap();
        //noinspection unchecked
        List<Object[]> results = getSessionFactory().getCurrentSession()
                .createQuery( "select ee, ad, op, ee.bioAssays.size from ExpressionExperiment as ee "
                        + "join ee.bioAssays ba "
                        + "join ba.arrayDesignUsed ad "
                        + "left join ba.originalPlatform op " // not all bioAssays have an original platform
                        + "where ee in :eelist "
                        + "group by ee, ad, op" )
                .setParameterList( "eelist", expressionExperiments )
                .setCacheable( true )
                .list();
        return results.stream()
                .collect( Collectors.groupingBy( row -> ( ExpressionExperiment ) row[0], Collectors.toList() ) );
    }

    @Override
    public Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIds( Collection<Long> ids, Taxon taxon, Sort sort, int offset, int limit ) {
        Filters filters = new Filters();

        if ( ids != null ) {
            if ( ids.isEmpty() )
                return new Slice<>();
            List<Long> idList = new ArrayList<>( ids );
            Collections.sort( idList );
            filters.add( new ObjectFilter( getObjectAlias(), "id", Long.class, ObjectFilter.Operator.in, idList ) );
        }

        if ( taxon != null ) {
            filters.add( new ObjectFilter( TaxonDao.OBJECT_ALIAS, "id", Long.class, ObjectFilter.Operator.eq, taxon.getId() ) );
        }

        return this.loadDetailsValueObjects( filters, sort, offset, limit );
    }

    @Override
    public List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIds( @NonNull Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyList();
        }

        Filters filters = Filters.singleFilter( new ObjectFilter( getObjectAlias(), "id", Long.class, ObjectFilter.Operator.in, ids ) );

        return this.loadDetailsValueObjects( filters, null, 0, 0 );
    }

    @Override
    public Collection<ExpressionExperiment> loadLackingFactors() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select e from ExpressionExperiment e join e.experimentalDesign d where d.experimentalFactors.size =  0" )
                .list();
    }

    @Override
    public Collection<ExpressionExperiment> loadLackingTags() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select e from ExpressionExperiment e where e.characteristics.size = 0" ).list();
    }

    @Override
    public ExpressionExperimentValueObject loadValueObject( ExpressionExperiment entity ) {
        return this.loadValueObjectsByIds( Collections.singleton( entity.getId() ) ).stream().findFirst().orElse( null );
    }

    @Override
    public List<ExpressionExperimentValueObject> loadValueObjects( Collection<ExpressionExperiment> entities ) {
        return this.loadValueObjectsByIds( EntityUtils.getIds( entities ) );
    }

    @Override
    public List<ExpressionExperimentValueObject> loadValueObjectsByIds( List<Long> ids, boolean maintainOrder ) {
        List<ExpressionExperimentValueObject> results = this.loadValueObjectsByIds( ids );

        // sort results according to ids
        if ( maintainOrder ) {
            Map<Long, Integer> id2position = ListUtils.indexOfElements( ids );
            return results.stream()
                    .sorted( Comparator.comparing( vo -> id2position.get( vo.getId() ) ) )
                    .collect( Collectors.toList() );
        }

        return results;
    }

    @Override
    public List<ExpressionExperimentValueObject> loadValueObjectsByIds( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyList();
        }
        Filters filters = Filters.singleFilter( new ObjectFilter( getObjectAlias(), "id", Long.class, ObjectFilter.Operator.in, ids ) );
        // FIXME: this is silly, but we keep the results ordered by ID
        return loadValueObjectsPreFilter( filters, Sort.by( getObjectAlias(), "id", Sort.Direction.ASC ) );
    }

    @Override
    protected ExpressionExperimentValueObject processLoadValueObjectsQueryResult( Object result ) {
        Object[] row = ( Object[] ) result;

        ExpressionExperiment ee = ( ExpressionExperiment ) row[0];
        AclObjectIdentity aoi = ( AclObjectIdentity ) row[1];
        AclSid sid = ( AclSid ) row[2];

        return new ExpressionExperimentValueObject( ee, aoi, sid );
    }

    @Override
    public void remove( @NonNull final ExpressionExperiment ee ) {
        log.info( "Deleting " + ee.getShortName() + "..." );

        Session session = this.getSessionFactory().getCurrentSession();

        // Note that links and analyses are deleted separately - see the ExpressionExperimentService.

        // At this point, the ee is probably still in the session, as the service already has gotten it
        // in this transaction.
        session.flush();
        session.clear();

        log.debug( " ... clearing curation details associations" );

        // these are tied to the audit trail and will cause lock problems it we don't clear first (due to cascade=all on the curation details, but
        // this may be okay now with updated config - see CurationDetails.hbm.xml)
        ee.getCurationDetails().setLastNeedsAttentionEvent( null );
        ee.getCurationDetails().setLastNoteUpdateEvent( null );
        ee.getCurationDetails().setLastTroubledEvent( null );
        session.update( ee.getCurationDetails() );

        session.update( ee );

        /*
         * This will fail because of multiple cascade=all on audit events.
         */
        //    session.buildLockRequest( LockOptions.NONE ).lock( ee );

        Hibernate.initialize( ee.getAuditTrail() );

        Collection<BioAssayDimension> dims = this.getBioAssayDimensions( ee );
        Collection<QuantitationType> qts = this.getQuantitationTypes( ee );

        log.debug( " ... clearing vectors" );

        ee.getRawExpressionDataVectors().clear();

        ee.getProcessedExpressionDataVectors().clear();

        for ( ExpressionExperiment e : ee.getOtherParts() ) {
            e.getOtherParts().remove( ee );
            session.update( e );
        }
        ee.getOtherParts().clear();

        log.debug( " ... calling update&flush" );

        session.update( ee );
        session.flush();

        AbstractDao.log.debug( "... removing " + dims.size() + " BioAssayDimensions ..." );
        for ( BioAssayDimension dim : dims ) {
            dim.getBioAssays().clear();
            session.update( dim );
            session.delete( dim );
        }
        //   dims.clear();
        session.flush();

        AbstractDao.log.debug( "... removing Bioassays and biomaterials ..." );

        // keep to put back in the object.
        Map<BioAssay, BioMaterial> copyOfRelations = new HashMap<>();

        Collection<BioMaterial> bioMaterialsToDelete = new HashSet<>();
        Collection<BioAssay> bioAssays = ee.getBioAssays();
        this.removeBioAssays( session, copyOfRelations, bioMaterialsToDelete, bioAssays );

        AbstractDao.log.debug( ".. Last bits ..." );

        // We remove them here in case they are associated to more than one bioassay-- no cascade is possible.
        for ( BioMaterial bm : bioMaterialsToDelete ) {
            session.delete( bm );
        }

        for ( QuantitationType qt : qts ) {
            session.delete( qt );
        }

        log.info( ".... flush and final deletion ..." );

        session.flush();
        super.remove( ee );

        AbstractDao.log.info( "Deleted " + ee );
    }

    @Override
    public ExpressionExperiment thaw( final ExpressionExperiment expressionExperiment ) {
        return this.thaw( expressionExperiment, true );
    }

    @Override
    public ExpressionExperiment thawBioAssays( ExpressionExperiment expressionExperiment ) {
        String thawQuery = "select distinct e from ExpressionExperiment e "
                + " left join fetch e.accession acc left join fetch acc.externalDatabase where e.id=:eeId";

        List res = this.getSessionFactory().getCurrentSession().createQuery( thawQuery )
                .setParameter( "eeId", expressionExperiment.getId() ).list();

        ExpressionExperiment result = ( ExpressionExperiment ) res.iterator().next();

        Hibernate.initialize( result.getBioAssays() );

        for ( BioAssay ba : result.getBioAssays() ) {
            Hibernate.initialize( ba.getArrayDesignUsed() );
            Hibernate.initialize( ba.getSampleUsed() );
            Hibernate.initialize( ba.getOriginalPlatform() );
        }

        return result;
    }

    @Override
    public ExpressionExperiment thawForFrontEnd( final ExpressionExperiment expressionExperiment ) {
        return this.thawLiter( expressionExperiment );
    }

    // "thawLite"
    @Override
    public ExpressionExperiment thawWithoutVectors( final ExpressionExperiment expressionExperiment ) {
        return this.thaw( expressionExperiment, false );
    }

    private void addIdsToResults( Map<Long, Integer> results, List res ) {
        for ( Object r : res ) {
            Object[] ro = ( Object[] ) r;
            Long id = ( Long ) ro[0];
            Integer count = ( ( Long ) ro[1] ).intValue();
            results.put( id, count );
        }
    }

    @Override
    protected Query getLoadValueObjectsQuery( Filters filters, Sort sort, EnumSet<QueryHint> hints ) {
        if ( filters == null ) {
            filters = new Filters();
        }

        // Restrict to non-troubled EEs for non-administrators
        addNonTroubledFilter( filters, getObjectAlias() );

        // FIXME: this is triggering an AD jointure that simply we cannot afford, so we only perform it if necessary
        if ( FiltersUtils.containsAnyAlias( filters, ArrayDesignDao.OBJECT_ALIAS ) ) {
            addNonTroubledFilter( filters, ArrayDesignDao.OBJECT_ALIAS );
        }

        // the constants for aliases are messing with the inspector
        //language=HQL
        String queryString = MessageFormat.format(
                "select {0}, {1}, {2} "
                        + "from ExpressionExperiment as {0} "
                        + "left join fetch {0}.accession acc "
                        + "left join fetch acc.externalDatabase as ED "
                        + "left join fetch {0}.experimentalDesign as EDES "
                        + "left join fetch {0}.curationDetails as s " /* needed for trouble status */
                        + "left join fetch s.lastNeedsAttentionEvent as eAttn "
                        + "left join fetch s.lastNoteUpdateEvent as eNote "
                        + "left join fetch s.lastTroubledEvent as eTrbl "
                        + "left join fetch {0}.geeq as geeq "
                        + "left join fetch {0}.taxon as {3} ",
                getObjectAlias(), AclQueryUtils.AOI_ALIAS, AclQueryUtils.SID_ALIAS, TaxonDao.OBJECT_ALIAS );

        // fetching characteristics, bioAssays and arrayDesignUsed is costly, so we reserve these operations only if it
        // is mentioned in the filters

        if ( FiltersUtils.containsAnyAlias( filters, CharacteristicDao.OBJECT_ALIAS ) ) {
            log.warn( "Querying ee.characteristics, this might take some time..." );
            queryString += MessageFormat.format( "left join ee.characteristics as {0} ", CharacteristicDao.OBJECT_ALIAS );
        }

        if ( FiltersUtils.containsAnyAlias( filters, BioAssayDao.OBJECT_ALIAS, ArrayDesignDao.OBJECT_ALIAS ) ) {
            log.warn( "Querying ee.bioAssays, this might take some time..." );
            queryString += MessageFormat.format( "left join ee.bioAssays as {0} ", BioAssayDao.OBJECT_ALIAS );
        }

        if ( FiltersUtils.containsAnyAlias( filters, ArrayDesignDao.OBJECT_ALIAS ) ) {
            log.warn( "Querying ee.bioAssays.arrayDesignUsed, this might take some time..." );
            queryString += MessageFormat.format( "left join {0}.arrayDesignUsed as {1} ", BioAssayDao.OBJECT_ALIAS, ArrayDesignDao.OBJECT_ALIAS );
        }

        // parts of this query (above) are only needed for administrators: the notes, so it could theoretically be sped up even more
        queryString += AclQueryUtils.formAclJoinClause( getObjectAlias() );

        queryString += AclQueryUtils.formAclRestrictionClause();
        queryString += ObjectFilterQueryUtils.formRestrictionClause( filters );

        // FIXME: this is necessary because of the ACL jointure, it can also become necessary if bioAssays are included as well
        // unlike in ArrayDesignDaoImpl, a distinct is not possible because we select the ACL AOI and SID
        queryString += " group by " + getObjectAlias();

        if ( sort != null ) {
            queryString += ObjectFilterQueryUtils.formOrderByClause( sort );
        }

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        AclQueryUtils.addAclJoinParameters( query, ExpressionExperiment.class );
        AclQueryUtils.addAclRestrictionParameters( query );
        ObjectFilterQueryUtils.addRestrictionParameters( query, filters );

        // FIXME: caching does nto work for lazy relationship (accession + geeq)
        // query.setCacheable( true );

        return query;
    }

    @Override
    protected Query getCountValueObjectsQuery( Filters filters ) {
        if ( filters == null ) {
            filters = new Filters();
        }

        // Restrict to non-troubled EEs for non-administrators
        addNonTroubledFilter( filters, getObjectAlias() );
        if ( FiltersUtils.containsAnyAlias( filters, ArrayDesignDao.OBJECT_ALIAS ) ) {
            addNonTroubledFilter( filters, ArrayDesignDao.OBJECT_ALIAS );
        }

        //noinspection JpaQlInspection // the constants for aliases are messing with the inspector
        String queryString =
                "select count(distinct ee) from ExpressionExperiment as ee "
                        + "left join ee.accession acc "
                        + "left join acc.externalDatabase as ED "
                        + "left join ee.experimentalDesign as EDES "
                        + "left join ee.curationDetails as s " /* needed for trouble status */
                        + "left join s.lastNeedsAttentionEvent as eAttn "
                        + "left join ee.geeq as geeq "
                        + "left join s.lastNoteUpdateEvent as eNote "
                        + "left join s.lastTroubledEvent as eTrbl "
                        + "left join ee.taxon as " + TaxonDao.OBJECT_ALIAS;

        // fetching characteristics, bioAssays and arrayDesignUsed is costly, so we reserve these operations only if it
        // is mentioned in the filters

        if ( FiltersUtils.containsAnyAlias( filters, CharacteristicDao.OBJECT_ALIAS ) ) {
            log.warn( "Querying ee.characteristics, this might take some time..." );
            queryString += MessageFormat.format( "left join ee.characteristics as {0} ", CharacteristicDao.OBJECT_ALIAS );
        }

        if ( FiltersUtils.containsAnyAlias( filters, BioAssayDao.OBJECT_ALIAS, ArrayDesignDao.OBJECT_ALIAS ) ) {
            log.warn( "Querying ee.bioAssays, this might take some time..." );
            queryString += MessageFormat.format( "left join ee.bioAssays as {0} ", BioAssayDao.OBJECT_ALIAS );
        }

        if ( FiltersUtils.containsAnyAlias( filters, ArrayDesignDao.OBJECT_ALIAS ) ) {
            log.warn( "Querying ee.bioAssays.arrayDesignUsed, this might take some time..." );
            queryString += MessageFormat.format( "left join {0}.arrayDesignUsed as {1} ", BioAssayDao.OBJECT_ALIAS, ArrayDesignDao.OBJECT_ALIAS );
        }

        // parts of this query (above) are only needed for administrators: the notes, so it could theoretically be sped up even more
        queryString += AclQueryUtils.formAclJoinClause( getObjectAlias() );

        queryString += AclQueryUtils.formAclRestrictionClause();
        queryString += ObjectFilterQueryUtils.formRestrictionClause( filters );

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        AclQueryUtils.addAclJoinParameters( query, ExpressionExperiment.class );
        AclQueryUtils.addAclRestrictionParameters( query );
        ObjectFilterQueryUtils.addRestrictionParameters( query, filters );

        query.setCacheable( true );

        return query;
    }

    /**
     * Filling 'hasDifferentialExpressionAnalysis' and 'hasCoexpressionAnalysis'
     */
    private void populateAnalysisInformation( Collection<ExpressionExperimentDetailsValueObject> vos ) {
        if ( vos.isEmpty() ) {
            return;
        }

        // these are cached queries (thus super-fast)
        Set<Long> withCoexpression = new HashSet<>( getExpressionExperimentIdsWithCoexpression() );
        Set<Long> withDiffEx = new HashSet<>( getExpressionExperimentIdsWithDifferentialExpressionAnalysis() );

        for ( ExpressionExperimentDetailsValueObject vo : vos ) {
            vo.setHasCoexpressionAnalysis( withCoexpression.contains( vo.getId() ) );
            vo.setHasDifferentialExpressionAnalysis( withDiffEx.contains( vo.getId() ) );
        }
    }

    private List<Long> getExpressionExperimentIdsWithCoexpression() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select experimentAnalyzed.id from CoexpressionAnalysis" )
                .setCacheable( true )
                .list();
    }

    private List<Long> getExpressionExperimentIdsWithDifferentialExpressionAnalysis() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select experimentAnalyzed.id from CoexpressionAnalysis" )
                .setCacheable( true )
                .list();
    }

    private void removeBioAssays( Session session, Map<BioAssay, BioMaterial> copyOfRelations,
            Collection<BioMaterial> bioMaterialsToDelete, Collection<BioAssay> bioAssays ) {
        for ( BioAssay ba : bioAssays ) {
            // relations to files cascade, so we only have to worry about biomaterials, which aren't cascaded from
            // anywhere. BioAssay -> BioMaterial is many-to-one, but bioassaySet (experiment) owns the bioAssay.
            BioMaterial biomaterial = ba.getSampleUsed();

            if ( biomaterial == null )
                continue; // shouldn't...

            bioMaterialsToDelete.add( biomaterial );

            copyOfRelations.put( ba, biomaterial );

            session.buildLockRequest( LockOptions.NONE ).lock( biomaterial );

            Hibernate.initialize( biomaterial );

            // this can easily end up with an unattached object.
            Hibernate.initialize( biomaterial.getBioAssaysUsedIn() );

            biomaterial.getFactorValues().clear();
            biomaterial.getBioAssaysUsedIn().clear();

            ba.setSampleUsed( null );
        }
    }

    private int removeDataVectors( Session session, Set<BioAssayDimension> dims, Set<QuantitationType> qts,
            Collection<RawExpressionDataVector> designElementDataVectors, int count ) {
        AbstractDao.log.info( "Removing Design Element Data Vectors ..." );
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
                session.clear();
            }
            // put back...
            dv.setBioAssayDimension( bad );
            dv.setQuantitationType( qt );

            if ( count % 20000 == 0 ) {
                AbstractDao.log.info( count + " design Element data vectors deleted" );
            }
        }
        count = 0;
        return count;
    }

    private void removeProcessedVectors( Session session, Set<BioAssayDimension> dims, Set<QuantitationType> qts,
            int count, Collection<ProcessedExpressionDataVector> processedVectors ) {
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
                session.clear();
            }
            if ( count % 20000 == 0 ) {
                AbstractDao.log.info( count + " processed design Element data vectors deleted" );
            }

            // put back..
            dv.setBioAssayDimension( bad );
            dv.setQuantitationType( qt );
        }
    }

    private ExpressionExperiment thaw( ExpressionExperiment ee, boolean vectorsAlso ) {
        if ( ee == null ) {
            return null;
        }

        if ( ee.getId() == null )
            throw new IllegalArgumentException( "id cannot be null, cannot be thawed: " + ee );

        /*
         * Trying to do everything fails miserably, so we still need a hybrid approach. But returning the thawed object,
         * as opposed to thawing the one passed in, solves problems.
         */
        String thawQuery = "select distinct e from ExpressionExperiment e "
                + " left join fetch e.accession acc left join fetch acc.externalDatabase where e.id=:eeId";

        //noinspection unchecked
        List<ExpressionExperiment> res = this.getSessionFactory().getCurrentSession().createQuery( thawQuery )
                .setParameter( "eeId", ee.getId() ).list();

        if ( res.size() == 0 ) {
            throw new IllegalArgumentException( "No experiment with id=" + ee.getId() + " could be loaded." );
        }
        ExpressionExperiment result = res.iterator().next();
        Hibernate.initialize( result.getMeanVarianceRelation() );
        Hibernate.initialize( result.getQuantitationTypes() );
        Hibernate.initialize( result.getCharacteristics() );
        Hibernate.initialize( result.getPrimaryPublication() );
        Hibernate.initialize( result.getOtherRelevantPublications() );
        Hibernate.initialize( result.getBioAssays() );
        Hibernate.initialize( result.getAuditTrail() );
        Hibernate.initialize( result.getGeeq() );
        Hibernate.initialize( result.getOtherParts() );

        if ( result.getAuditTrail() != null )
            Hibernate.initialize( result.getAuditTrail().getEvents() );
        Hibernate.initialize( result.getCurationDetails() );

        for ( BioAssay ba : result.getBioAssays() ) {
            Hibernate.initialize( ba.getArrayDesignUsed() );
            Hibernate.initialize( ba.getArrayDesignUsed().getDesignProvider() );
            Hibernate.initialize( ba.getOriginalPlatform() );

            Hibernate.initialize( ba.getSampleUsed() );
            BioMaterial bm = ba.getSampleUsed();
            if ( bm != null ) {
                Hibernate.initialize( bm.getFactorValues() );
                Hibernate.initialize( bm.getTreatments() );
            }
        }

        ExperimentalDesign experimentalDesign = result.getExperimentalDesign();
        if ( experimentalDesign != null ) {
            Hibernate.initialize( experimentalDesign );
            Hibernate.initialize( experimentalDesign.getExperimentalFactors() );
            Hibernate.initialize( experimentalDesign.getTypes() );
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

        this.thawReferences( result );
        this.thawMeanVariance( result );

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
     * Method for the front end display
     *
     * @param ee expression experiment to be thawed
     * @return thawed expression experiment.
     */
    private ExpressionExperiment thawLiter( ExpressionExperiment ee ) {
        if ( ee == null ) {
            return null;
        }

        if ( ee.getId() == null )
            throw new IllegalArgumentException( "id cannot be null, cannot be thawed: " + ee );

        /*
         * Trying to do everything fails miserably, so we still need a hybrid approach. But returning the thawed object,
         * as opposed to thawing the one passed in, solves problems.
         */
        String thawQuery = "select distinct e from ExpressionExperiment e "
                + " left join fetch e.accession acc left join fetch acc.externalDatabase " + "where e.id=:eeId";

        List res = this.getSessionFactory().getCurrentSession().createQuery( thawQuery )
                .setParameter( "eeId", ee.getId() ).list();

        if ( res.size() == 0 ) {
            throw new IllegalArgumentException( "No experiment with id=" + ee.getId() + " could be loaded." );
        }
        ExpressionExperiment result = ( ExpressionExperiment ) res.iterator().next();
        Hibernate.initialize( result.getPrimaryPublication() );
        Hibernate.initialize( result.getCurationDetails() );
        Hibernate.initialize( result.getGeeq() );
        Hibernate.initialize( result.getOtherParts() );

        ExperimentalDesign experimentalDesign = result.getExperimentalDesign();
        if ( experimentalDesign != null ) {
            Hibernate.initialize( experimentalDesign );
            Hibernate.initialize( experimentalDesign.getExperimentalFactors() );
        }

        this.thawReferences( result );
        this.thawMeanVariance( result );

        return result;
    }

    private void thawMeanVariance( final ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment.getMeanVarianceRelation() != null ) {
            Hibernate.initialize( expressionExperiment.getMeanVarianceRelation() );
            Hibernate.initialize( expressionExperiment.getMeanVarianceRelation().getMeans() );
            Hibernate.initialize( expressionExperiment.getMeanVarianceRelation().getVariances() );
        }
    }

    private void thawReferences( final ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment.getPrimaryPublication() != null ) {
            Hibernate.initialize( expressionExperiment.getPrimaryPublication() );
            Hibernate.initialize( expressionExperiment.getPrimaryPublication().getPubAccession() );
            Hibernate.initialize(
                    expressionExperiment.getPrimaryPublication().getPubAccession().getExternalDatabase() );
            //   Hibernate.initialize( expressionExperiment.getPrimaryPublication().getPublicationTypes() );
        }
        if ( expressionExperiment.getOtherRelevantPublications() != null ) {
            Hibernate.initialize( expressionExperiment.getOtherRelevantPublications() );
            for ( BibliographicReference bf : expressionExperiment.getOtherRelevantPublications() ) {
                Hibernate.initialize( bf.getPubAccession() );
                Hibernate.initialize( bf.getPubAccession().getExternalDatabase() );
                //     Hibernate.initialize( bf.getPublicationTypes() );
            }
        }
    }
}
