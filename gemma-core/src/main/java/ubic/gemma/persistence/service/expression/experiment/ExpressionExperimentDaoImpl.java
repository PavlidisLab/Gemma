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
import lombok.Value;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.core.analysis.expression.diff.BaselineSelection;
import ubic.gemma.core.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.core.util.StopWatchUtils;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
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
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingLong;
import static ubic.gemma.persistence.service.TableMaintenanceUtil.EE2C_QUERY_SPACE;

/**
 * @author pavlidis
 * @see ubic.gemma.model.expression.experiment.ExpressionExperiment
 */
@Repository
public class ExpressionExperimentDaoImpl
        extends AbstractCuratableDao<ExpressionExperiment, ExpressionExperimentValueObject>
        implements ExpressionExperimentDao {

    private static final int BATCH_SIZE = 1000;

    private static final String
            CHARACTERISTIC_ALIAS = CharacteristicDao.OBJECT_ALIAS,
            BIO_MATERIAL_CHARACTERISTIC_ALIAS = "bmc",
            FACTOR_VALUE_CHARACTERISTIC_ALIAS = "fvc",
            ALL_CHARACTERISTIC_ALIAS = "ac",
            BIO_ASSAY_ALIAS = BioAssayDao.OBJECT_ALIAS,
            TAXON_ALIAS = TaxonDao.OBJECT_ALIAS,
            ARRAY_DESIGN_ALIAS = ArrayDesignDao.OBJECT_ALIAS,
            EXTERNAL_DATABASE_ALIAS = "ED";

    /**
     * Aliases applicable for one-to-many relations.
     */
    private static final String[] ONE_TO_MANY_ALIASES = { CHARACTERISTIC_ALIAS, BIO_MATERIAL_CHARACTERISTIC_ALIAS,
            FACTOR_VALUE_CHARACTERISTIC_ALIAS, ALL_CHARACTERISTIC_ALIAS, BIO_ASSAY_ALIAS, ARRAY_DESIGN_ALIAS };

    @Autowired
    public ExpressionExperimentDaoImpl( SessionFactory sessionFactory ) {
        super( ExpressionExperimentDao.OBJECT_ALIAS, ExpressionExperiment.class, sessionFactory );
    }

    @Override
    public List<ExpressionExperiment> browse( int start, int limit ) {
        Query query = this.getSessionFactory().getCurrentSession().createQuery( "from ExpressionExperiment" );
        query.setMaxResults( limit );
        query.setFirstResult( start );

        //noinspection unchecked
        return query.list();
    }

    @Override
    public List<ExpressionExperiment> browse( int start, int limit, String orderField, boolean descending ) {
        throw new NotImplementedException( "Browsing ExpressionExperiment in a specific order is not supported." );
    }

    @Override
    public BioAssaySet loadBioAssaySet( Long id ) {
        return ( BioAssaySet ) getSessionFactory().getCurrentSession().get( BioAssaySet.class, id );
    }

    @Override
    public Collection<Long> filterByTaxon( @Nullable Collection<Long> ids, Taxon taxon ) {

        if ( ids == null || ids.isEmpty() )
            return new HashSet<>();

        //language=HQL
        final String queryString =
                "select ee.id from ExpressionExperiment as ee " + "inner join ee.bioAssays as ba "
                        + "inner join ba.sampleUsed as sample where sample.sourceTaxon = :taxon and ee.id in (:ids) group by ee";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "taxon", taxon )
                .setParameterList( "ids", ids ).list();
    }

    @Override
    public ExpressionExperiment findByShortName( String shortName ) {
        return findOneByProperty( "shortName", shortName );
    }

    @Override
    public Collection<ExpressionExperiment> findByName( String name ) {
        return findByProperty( "name", name );
    }

    @Override
    public ExpressionExperiment find( ExpressionExperiment entity ) {

        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( ExpressionExperiment.class );

        if ( entity.getAccession() != null ) {
            criteria.add( Restrictions.eq( "accession", entity.getAccession() ) );
        } else if ( entity.getShortName() != null ) {
            criteria.add( Restrictions.eq( "shortName", entity.getShortName() ) );
        } else if ( entity.getName() != null ) {
            criteria.add( Restrictions.eq( "name", entity.getName() ) );
        } else {
            throw new IllegalArgumentException( "At least one of accession, shortName or name must be non-null to find an ExpressionExperiment." );
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
                "select ee FROM ExpressionExperiment as ee left join ee.otherRelevantPublications as eeO"
                        + " WHERE ee.primaryPublication.id = :bibID OR (eeO.id = :bibID) group by ee";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "bibID", bibRefID )
                .list();
    }

    @Override
    public ExpressionExperiment findByBioAssay( BioAssay ba ) {

        //language=HQL
        final String queryString =
                "select ee from ExpressionExperiment as ee inner join ee.bioAssays as ba " + "where ba = :ba group by ee";
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
        final String queryString = "select ee from ExpressionExperiment as ee "
                + "inner join ee.bioAssays as ba inner join ba.sampleUsed as sample where sample = :bm group by ee";

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
        if ( bms.size() == 0 ) {
            return new HashMap<>();
        }
        //language=HQL
        final String queryString = "select ee, sample from ExpressionExperiment as ee "
                + "inner join ee.bioAssays as ba inner join ba.sampleUsed as sample where sample in (:bms) group by ee, sample";

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
        queryObject.addScalar( "eeID", StandardBasicTypes.LONG );
        //noinspection unchecked
        List<Long> results = queryObject.list();

        eeIds = new HashSet<>( results );

        return this.load( eeIds );
    }

    @Override
    public ExpressionExperiment findByDesign( ExperimentalDesign ed ) {
        return ( ExpressionExperiment ) getSessionFactory().getCurrentSession()
                .createQuery( "select ee from ExpressionExperiment ee where ee.experimentalDesign = :ed" )
                .setParameter( "ed", ed )
                .uniqueResult();
    }

    @Override
    public ExpressionExperiment findByFactor( ExperimentalFactor ef ) {
        //language=HQL
        final String queryString =
                "select ee from ExpressionExperiment as ee inner join ee.experimentalDesign ed "
                        + "inner join ed.experimentalFactors ef where ef = :ef group by ee";

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
                "select ee from ExpressionExperiment as ee inner join ee.experimentalDesign ed "
                        + "inner join ed.experimentalFactors ef inner join ef.factorValues fv where fv.id = :fvId group by ee";

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
        final String queryString = "select ee, f from ExpressionExperiment ee "
                + " join ee.experimentalDesign ed join ed.experimentalFactors ef join ef.factorValues f"
                + " where f in (:fvs) group by ee, f";
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
        queryObject.addScalar( "eeID", StandardBasicTypes.LONG );
        //noinspection unchecked
        List<Long> results = queryObject.list();

        eeIds = new HashSet<>( results );

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
    public List<ExpressionExperiment> findByUpdatedLimit( Collection<Long> ids, int limit ) {
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
    public List<ExpressionExperiment> findByUpdatedLimit( int limit ) {
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
    public Collection<ExpressionExperiment> findUpdatedAfter( @Nullable Date date ) {
        if ( date == null )
            return Collections.emptyList();
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select e from ExpressionExperiment e join e.curationDetails cd where cd.lastUpdated > :date" )
                .setDate( "date", date ).list();
    }

    @Override
    public Map<Long, Long> getAnnotationCounts( Collection<Long> ids ) {
        Map<Long, Long> results = new HashMap<>();
        for ( Long id : ids ) {
            results.put( id, 0L );
        }
        if ( ids.size() == 0 ) {
            return results;
        }
        String queryString = "select e.id,count(c.id) from ExpressionExperiment e inner join e.characteristics c where e.id in (:ids) group by e.id";
        List<Object[]> res = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();

        for ( Object[] ro : res ) {
            Long id = ( Long ) ro[0];
            Long count = ( Long ) ro[1];
            results.put( id, count );
        }

        return results;
    }

    @Override
    public Collection<? extends AnnotationValueObject> getAnnotationsByBioMaterials( Long eeId ) {
        /*
         * Note we're not using 'distinct' here but the 'equals' for AnnotationValueObject should aggregate these. More
         * work to do.
         */
        List<Characteristic> raw = this.getSessionFactory().getCurrentSession().createQuery(
                "select c from ExpressionExperiment e " + "join e.bioAssays ba join ba.sampleUsed bm "
                        + "join bm.characteristics c where e.id= :eeid" ).setParameter( "eeid", eeId ).list();

        Collection<AnnotationValueObject> results = new HashSet<>();
        /*
         * TODO we need to filter these better; some criteria could be included in the query
         */
        for ( Characteristic c : raw ) {

            // filter. Could include this in the query if it isn't too complicated.
            if ( c.getCategoryUri() == null ) {
                continue;
            }

            if ( c.getValueUri() == null ) {
                continue;
            }

            if ( "MaterialType".equalsIgnoreCase( c.getCategory() )
                    || "molecular entity".equalsIgnoreCase( c.getCategory() )
                    || "LabelCompound".equalsIgnoreCase( c.getCategory() ) ) {
                continue;
            }

            if ( BaselineSelection.isBaselineCondition( c ) ) {
                continue;
            }

            AnnotationValueObject annotationValue = new AnnotationValueObject( c, BioMaterial.class );

            results.add( annotationValue );
        }

        return results;

    }

    @Override
    public Collection<? extends AnnotationValueObject> getAnnotationsByFactorValues( Long eeId ) {
        //noinspection unchecked
        List<Statement> raw = this.getSessionFactory().getCurrentSession().createQuery( "select c from ExpressionExperiment e "
                + "join e.experimentalDesign ed join ed.experimentalFactors ef join ef.factorValues fv "
                + "join fv.characteristics c where e.id= :eeid " ).setParameter( "eeid", eeId ).list();

        /*
         * FIXME filtering here is going to have to be more elaborate for this to be useful.
         */
        Collection<AnnotationValueObject> results = new HashSet<>();
        for ( Statement c : raw ) {
            // ignore baseline conditions
            if ( BaselineSelection.isBaselineCondition( c ) ) {
                continue;
            }

            // ignore batch factors
            if ( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME.equals( c.getCategory() )
                    || ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_URI.equals( c.getCategoryUri() ) ) {
                continue;
            }

            // ignore timepoints
            if ( "http://www.ebi.ac.uk/efo/EFO_0000724".equals( c.getCategoryUri() ) ) {
                continue;
            }

            // DE_include/exclude
            if ( "http://gemma.msl.ubc.ca/ont/TGEMO_00013".equals( c.getSubjectUri() ) )
                continue;
            if ( "http://gemma.msl.ubc.ca/ont/TGEMO_00014".equals( c.getSubjectUri() ) )
                continue;

            // ignore free text values
            if ( c.getSubjectUri() != null ) {
                results.add( new AnnotationValueObject( c, FactorValue.class ) );
            }

            if ( c.getObject() != null && c.getObjectUri() != null ) {
                results.add( new AnnotationValueObject( c.getCategoryUri(), c.getCategory(), c.getObjectUri(), c.getObject(), FactorValue.class ) );
            }

            if ( c.getSecondObject() != null && c.getSecondObjectUri() != null ) {
                results.add( new AnnotationValueObject( c.getCategoryUri(), c.getCategory(), c.getSecondObjectUri(), c.getSecondObject(), FactorValue.class ) );
            }
        }

        return results;

    }

    @Override
    public Map<Class<? extends Identifiable>, List<Characteristic>> getAllAnnotations( ExpressionExperiment expressionExperiment ) {
        //noinspection unchecked
        List<Object[]> result = ( ( List<Object[]> ) getSessionFactory().getCurrentSession().createSQLQuery(
                        "select {T.*}, {T}.LEVEL as LEVEL from EXPRESSION_EXPERIMENT2CHARACTERISTIC {T} "
                                + "where T.EXPRESSION_EXPERIMENT_FK = :eeId" )
                .addEntity( "T", Characteristic.class )
                .addScalar( "LEVEL", StandardBasicTypes.CLASS )
                .setParameter( "eeId", expressionExperiment.getId() )
                .list() );
        //noinspection unchecked
        return result.stream()
                .collect( Collectors.groupingBy( row -> ( Class<? extends Identifiable> ) row[1],
                        Collectors.mapping( row -> ( Characteristic ) row[0], Collectors.toList() ) ) );
    }

    @Override
    public List<Characteristic> getBioMaterialAnnotations( ExpressionExperiment expressionExperiment ) {
        return getAnnotationsByLevel( expressionExperiment, BioMaterial.class );
    }

    @Override
    public List<Characteristic> getExperimentalDesignAnnotations( ExpressionExperiment expressionExperiment ) {
        return getAnnotationsByLevel( expressionExperiment, ExperimentalDesign.class );
    }

    private List<Characteristic> getAnnotationsByLevel( ExpressionExperiment expressionExperiment, Class<? extends Identifiable> level ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession().createSQLQuery(
                        "select {T.*} from EXPRESSION_EXPERIMENT2CHARACTERISTIC {T} "
                                + "where {T}.LEVEL = :level and T.EXPRESSION_EXPERIMENT_FK = :eeId" )
                .addEntity( "T", Characteristic.class )
                .setParameter( "level", level )
                .setParameter( "eeId", expressionExperiment.getId() )
                .list();
    }

    @Override
    public Map<Characteristic, Long> getCategoriesUsageFrequency( @Nullable Collection<Long> eeIds, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, @Nullable Collection<String> retainedTermUris ) {
        if ( eeIds != null && eeIds.isEmpty() ) {
            return Collections.emptyMap();
        }
        // never exclude terms that are explicitly retained
        if ( excludedTermUris != null && retainedTermUris != null ) {
            excludedTermUris = new HashSet<>( excludedTermUris );
            excludedTermUris.removeAll( retainedTermUris );
        }
        boolean excludeFreeTextCategories = false;
        boolean excludeUncategorized = false;
        if ( excludedCategoryUris != null ) {
            if ( excludedCategoryUris.contains( null ) ) {
                excludeFreeTextCategories = true;
                excludedCategoryUris = excludedCategoryUris.stream().filter( Objects::nonNull ).collect( Collectors.toList() );
            }
            if ( excludedCategoryUris.contains( UNCATEGORIZED ) ) {
                excludeUncategorized = true;
                excludedCategoryUris = excludedCategoryUris.stream().filter( c -> !c.equals( UNCATEGORIZED ) ).collect( Collectors.toList() );
            }
        }
        boolean excludeFreeTextTerms = false;
        if ( excludedTermUris != null ) {
            if ( excludedTermUris.contains( null ) ) {
                excludeFreeTextTerms = true;
                excludedTermUris = excludedTermUris.stream().filter( Objects::nonNull ).collect( Collectors.toList() );
            }
        }
        String query = "select T.CATEGORY as CATEGORY, T.CATEGORY_URI as CATEGORY_URI, count(distinct T.EXPRESSION_EXPERIMENT_FK) as EE_COUNT from EXPRESSION_EXPERIMENT2CHARACTERISTIC T "
                + AclQueryUtils.formNativeAclJoinClause( "T.EXPRESSION_EXPERIMENT_FK" ) + " "
                + "where T.ID is not null ";
        if ( eeIds != null ) {
            query += " and T.EXPRESSION_EXPERIMENT_FK in :eeIds";
        }
        query += getExcludeUrisClause( "T.CATEGORY_URI", "T.CATEGORY", "excludedCategoryUris", excludedCategoryUris, excludeFreeTextCategories, excludeUncategorized, retainedTermUris );
        query += getExcludeUrisClause( "T.VALUE_URI", "T.`VALUE`", "excludedTermUris", excludedTermUris, excludeFreeTextTerms, false, retainedTermUris );
        query += AclQueryUtils.formNativeAclRestrictionClause( ( SessionFactoryImplementor ) getSessionFactory() ) + " "
                + "group by COALESCE(T.CATEGORY_URI, T.CATEGORY) "
                + "order by EE_COUNT desc";
        Query q = getSessionFactory().getCurrentSession().createSQLQuery( query )
                .addScalar( "CATEGORY", StandardBasicTypes.STRING )
                .addScalar( "CATEGORY_URI", StandardBasicTypes.STRING )
                .addScalar( "EE_COUNT", StandardBasicTypes.LONG )
                .addSynchronizedQuerySpace( EE2C_QUERY_SPACE )
                .addSynchronizedEntityClass( Characteristic.class )
                .setCacheable( true );
        if ( eeIds != null ) {
            q.setParameterList( "eeIds", new HashSet<>( eeIds ) );
        }
        if ( excludedCategoryUris != null && !excludedCategoryUris.isEmpty() ) {
            q.setParameterList( "excludedCategoryUris", excludedCategoryUris );
        }
        if ( excludedTermUris != null && !excludedTermUris.isEmpty() ) {
            q.setParameterList( "excludedTermUris", excludedTermUris );
        }
        if ( retainedTermUris != null && !retainedTermUris.isEmpty() ) {
            q.setParameterList( "retainedTermUris", retainedTermUris );
        }
        AclQueryUtils.addAclParameters( q, ExpressionExperiment.class );
        //noinspection unchecked
        List<Object[]> result = q.list();
        TreeMap<Characteristic, Long> byC = new TreeMap<>( Characteristic.getByCategoryComparator() );
        for ( Object[] row : result ) {
            Characteristic c = Characteristic.Factory.newInstance( null, null, null, null, ( String ) row[0], ( String ) row[1], null );
            byC.put( c, ( Long ) row[2] );
        }
        return byC;
    }

    /**
     * We're making two assumptions: a dataset cannot have a characteristic more than once and a dataset cannot have
     * the same characteristic at multiple levels to make counting more efficient.
     */
    @Override
    public Map<Characteristic, Long> getAnnotationsUsageFrequency( @Nullable Collection<Long> eeIds, @Nullable Class<? extends Identifiable> level, int maxResults, int minFrequency, @Nullable String category, @Nullable Collection<String> excludedCategoryUris, @Nullable Collection<String> excludedTermUris, @Nullable Collection<String> retainedTermUris ) {
        if ( eeIds != null && eeIds.isEmpty() ) {
            return Collections.emptyMap();
        }
        // never exclude terms that are explicitly retained
        if ( excludedTermUris != null && retainedTermUris != null ) {
            excludedTermUris = new HashSet<>( excludedTermUris );
            excludedTermUris.removeAll( retainedTermUris );
        }
        boolean excludeFreeTextCategories = false;
        boolean excludeUncategorized = false;
        if ( excludedCategoryUris != null ) {
            if ( excludedCategoryUris.contains( null ) ) {
                excludeFreeTextCategories = true;
                excludedCategoryUris = excludedCategoryUris.stream().filter( Objects::nonNull ).collect( Collectors.toList() );
            }
            if ( excludedCategoryUris.contains( UNCATEGORIZED ) ) {
                excludeUncategorized = true;
                excludedCategoryUris = excludedCategoryUris.stream().filter( c -> !c.equals( UNCATEGORIZED ) ).collect( Collectors.toList() );
            }
        }
        boolean excludeFreeTextTerms = false;
        if ( excludedTermUris != null ) {
            if ( excludedTermUris.contains( null ) ) {
                excludeFreeTextTerms = true;
                excludedTermUris = excludedTermUris.stream().filter( Objects::nonNull ).collect( Collectors.toList() );
            }
        }
        String query = "select T.`VALUE` as `VALUE`, T.VALUE_URI as VALUE_URI, T.CATEGORY as CATEGORY, T.CATEGORY_URI as CATEGORY_URI, T.EVIDENCE_CODE as EVIDENCE_CODE, count(distinct T.EXPRESSION_EXPERIMENT_FK) as EE_COUNT from EXPRESSION_EXPERIMENT2CHARACTERISTIC T "
                + AclQueryUtils.formNativeAclJoinClause( "T.EXPRESSION_EXPERIMENT_FK" ) + " "
                + "where T.ID is not null"; // this is necessary for the clause building since there might be no clause
        if ( eeIds != null ) {
            query += " and T.EXPRESSION_EXPERIMENT_FK in :eeIds";
        }
        if ( level != null ) {
            query += " and T.LEVEL = :level";
        }
        if ( category != null ) {
            if ( category.equals( UNCATEGORIZED ) ) {
                query += " and COALESCE(T.CATEGORY_URI, T.CATEGORY) is NULL";
            } else {
                query += " and COALESCE(T.CATEGORY_URI, T.CATEGORY) = :category";
            }
        }
        query += getExcludeUrisClause( "T.CATEGORY_URI", "T.CATEGORY", "excludedCategoryUris", excludedCategoryUris, excludeFreeTextCategories, excludeUncategorized, retainedTermUris );
        query += getExcludeUrisClause( "T.VALUE_URI", "T.`VALUE`", "excludedTermUris", excludedTermUris, excludeFreeTextTerms, false, retainedTermUris );
        query += AclQueryUtils.formNativeAclRestrictionClause( ( SessionFactoryImplementor ) getSessionFactory() ) + " "
                + "group by COALESCE(T.CATEGORY_URI, T.CATEGORY), COALESCE(T.VALUE_URI, T.`VALUE`) "
                + "having EE_COUNT >= :minFrequency ";
        if ( retainedTermUris != null && !retainedTermUris.isEmpty() ) {
            query += " or VALUE_URI in (:retainedTermUris)";
        }
        query += "order by EE_COUNT desc";
        Query q = getSessionFactory().getCurrentSession().createSQLQuery( query )
                .addScalar( "VALUE", StandardBasicTypes.STRING )
                .addScalar( "VALUE_URI", StandardBasicTypes.STRING )
                .addScalar( "CATEGORY", StandardBasicTypes.STRING )
                .addScalar( "CATEGORY_URI", StandardBasicTypes.STRING )
                // FIXME: use an EnumType for converting
                .addScalar( "EVIDENCE_CODE", StandardBasicTypes.STRING )
                .addScalar( "EE_COUNT", StandardBasicTypes.LONG )
                .addSynchronizedQuerySpace( "EXPRESSION_EXPERIMENT2CHARACTERISTIC" )
                .addSynchronizedEntityClass( Characteristic.class ) // ensures that the cache is invalidated if characteristics are added or removed
                .setCacheable( true )
                .setMaxResults( maxResults );
        if ( eeIds != null ) {
            q.setParameterList( "eeIds", new HashSet<>( eeIds ) );
        }
        if ( category != null && !category.equals( UNCATEGORIZED ) ) {
            q.setParameter( "category", category );
        }
        if ( excludedCategoryUris != null && !excludedCategoryUris.isEmpty() ) {
            q.setParameterList( "excludedCategoryUris", excludedCategoryUris );
        }
        if ( excludedTermUris != null && !excludedTermUris.isEmpty() ) {
            q.setParameterList( "excludedTermUris", excludedTermUris );
        }
        if ( retainedTermUris != null && !retainedTermUris.isEmpty() ) {
            q.setParameterList( "retainedTermUris", retainedTermUris );
        }
        if ( level != null ) {
            q.setParameter( "level", level );
        }
        q.setParameter( "minFrequency", minFrequency );
        AclQueryUtils.addAclParameters( q, ExpressionExperiment.class );
        //noinspection unchecked
        List<Object[]> result = q.list();
        TreeMap<Characteristic, Long> byC = new TreeMap<>( Characteristic.getByCategoryAndValueComparator() );
        for ( Object[] row : result ) {
            GOEvidenceCode evidenceCode;
            try {
                evidenceCode = row[4] != null ? GOEvidenceCode.valueOf( ( String ) row[4] ) : null;
            } catch ( IllegalArgumentException e ) {
                evidenceCode = null;
            }
            Characteristic c = Characteristic.Factory.newInstance( null, null, ( String ) row[0], ( String ) row[1], ( String ) row[2], ( String ) row[3], evidenceCode );
            byC.put( c, ( Long ) row[5] );
        }
        return byC;
    }

    /**
     * Produce a SQL clause for excluding URIs and free-text (i.e. null) URIs.
     * <p>
     * FIXME: There's a bug in Hibernate that that prevents it from producing proper tuples the excluded URIs and
     *        retained term URIs
     * @param column            column holding the URI to be excluded
     * @param labelColumn       column holding the label (only used if excludeFreeText or excludeUncategorized is true,
     *                          then we will check if the label is non-null to cover some edge cases)
     * @param excludedUrisParam name of the binding parameter for the excluded URIs
     * @param excludedUris      list of URIs to exclude
     * @param excludeFreeText   whether to exclude free-text URIs
     * @param retainedTermUris  list of terms that should bypass the exclusion
     */
    private String getExcludeUrisClause( String column, String labelColumn, String excludedUrisParam, @Nullable Collection<String> excludedUris, boolean excludeFreeText, boolean excludeUncategorized, @Nullable Collection<String> retainedTermUris ) {
        String query = "";
        if ( excludedUris != null && !excludedUris.isEmpty() ) {
            query += " and ((" + column + " not in (:" + excludedUrisParam + ")";
            if ( excludeUncategorized ) {
                query += " and COALESCE(" + column + ", " + labelColumn + ") is not null";
            }
            if ( excludeFreeText ) {
                query += " and (" + column + " is not null or " + labelColumn + " is null)";
            }
            query += ")";
            if ( retainedTermUris != null && !retainedTermUris.isEmpty() ) {
                query += " or T.VALUE_URI in (:retainedTermUris)";
            }
            query += ")";
        } else {
            if ( excludeUncategorized ) {
                query += " and COALESCE(" + column + ", " + labelColumn + ") is not null";
            }
            if ( excludeFreeText ) {
                query += " and (" + column + " is not null or " + labelColumn + " is null)";
            }
        }
        return query;
    }

    @Override
    public Collection<ExpressionExperiment> getExperimentsLackingPublications() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "select e from ExpressionExperiment e where e.primaryPublication = null and e.shortName like 'GSE%'" ).list();
    }

    @Override
    public MeanVarianceRelation updateMeanVarianceRelation( ExpressionExperiment ee, MeanVarianceRelation mvr ) {
        if ( mvr.getId() == null ) {
            getSessionFactory().getCurrentSession().persist( mvr );
        }
        ee.setMeanVarianceRelation( mvr );
        update( ee );
        return mvr;
    }

    @Override
    public long countBioMaterials( @Nullable Filters filters ) {
        //language=HQL
        Query query = finishFilteringQuery( "select count(distinct bm) from ExpressionExperiment ee "
                        + "join ee.bioAssays ba "
                        + "join ba.sampleUsed bm",
                filters, null, null );
        return ( Long ) query.setCacheable( true ).uniqueResult();
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
    public Map<TechnologyType, Long> getTechnologyTypeUsageFrequency() {
        Query query = getSessionFactory().getCurrentSession().createQuery(
                "select a.technologyType, oa.technologyType, count(distinct ee) from ExpressionExperiment ee "
                        + "join ee.bioAssays ba "
                        + "join ba.arrayDesignUsed a "
                        + "left join ba.originalPlatform oa "
                        + AclQueryUtils.formAclRestrictionClause( "ee.id" ) + " "
                        + "and (oa is null or a.technologyType <> oa.technologyType) "   // ignore noop switch
                        + formNonTroubledClause( "ee" )
                        + formNonTroubledClause( "a" ) + " "
                        + "group by a.technologyType, oa.technologyType" );
        AclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        //noinspection unchecked
        List<Object[]> result = query
                .setCacheable( true )
                .list();
        return aggregateTechnologyTypeCounts( result );
    }

    @Override
    public Map<TechnologyType, Long> getTechnologyTypeUsageFrequency( Collection<Long> eeIds ) {
        if ( eeIds.isEmpty() ) {
            return Collections.emptyMap();
        }
        //noinspection unchecked
        List<Object[]> result = getSessionFactory().getCurrentSession()
                .createQuery( "select a.technologyType, oa.technologyType, count(distinct ee) from ExpressionExperiment ee "
                        + "join ee.bioAssays ba "
                        + "join ba.arrayDesignUsed a "
                        + "left join ba.originalPlatform oa "
                        + "where ee.id in :ids "
                        + "and (oa is null or a.technologyType <> oa.technologyType) "   // ignore noop switch
                        + formNonTroubledClause( "ee" )
                        + formNonTroubledClause( "a" ) + " "
                        + "group by a.technologyType, oa.technologyType" )
                .setParameterList( "ids", eeIds )
                .setCacheable( true )
                .list();
        return aggregateTechnologyTypeCounts( result );
    }

    private Map<TechnologyType, Long> aggregateTechnologyTypeCounts( List<Object[]> result ) {
        Map<TechnologyType, Long> counts = new HashMap<>();
        for ( Object[] row : result ) {
            TechnologyType tt = ( TechnologyType ) row[0];
            TechnologyType originalTt = ( TechnologyType ) row[1];
            Long count = ( Long ) row[2];
            counts.compute( tt, ( k, v ) -> v == null ? count : v + count );
            if ( originalTt != null ) {
                counts.compute( originalTt, ( k, v ) -> v == null ? count : v + count );
            }
        }
        return counts;
    }

    @Override
    public Map<ArrayDesign, Long> getArrayDesignsUsageFrequency( int maxResults ) {
        Query query = getSessionFactory().getCurrentSession()
                .createQuery( "select a, count(distinct ee) from ExpressionExperiment ee "
                        + "join ee.bioAssays ba "
                        + "join ba.arrayDesignUsed a "
                        + AclQueryUtils.formAclRestrictionClause( "ee.id" )
                        + formNonTroubledClause( "ee" )
                        + formNonTroubledClause( "a" ) + " "
                        + "group by a"
                        + ( maxResults > 0 ? " order by count(distinct ee) desc" : "" ) );
        AclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        //noinspection unchecked
        List<Object[]> result = query
                .setCacheable( true )
                .setMaxResults( maxResults )
                .list();
        return result.stream().collect( groupingBy( row -> ( ArrayDesign ) row[0], summingLong( row -> ( Long ) row[1] ) ) );
    }

    @Override
    public Map<ArrayDesign, Long> getArrayDesignsUsageFrequency( Collection<Long> eeIds, int maxResults ) {
        if ( eeIds.isEmpty() ) {
            return Collections.emptyMap();
        }
        Query query = getSessionFactory().getCurrentSession()
                .createQuery( "select a, count(distinct ee) from ExpressionExperiment ee "
                        + "join ee.bioAssays ba "
                        + "join ba.arrayDesignUsed a "
                        + "where ee.id in :ids "
                        + formNonTroubledClause( "ee" )
                        + formNonTroubledClause( "a" ) + " "
                        + "group by a"
                        + ( maxResults > 0 ? " order by count(distinct ee) desc" : "" ) )
                .setParameterList( "ids", eeIds )
                .setMaxResults( maxResults );
        //noinspection unchecked
        List<Object[]> result = query.setCacheable( true ).list();
        return result.stream().collect( groupingBy( row -> ( ArrayDesign ) row[0], summingLong( row -> ( Long ) row[1] ) ) );
    }

    @Override
    public Map<ArrayDesign, Long> getOriginalPlatformsUsageFrequency( int maxResults ) {
        Query query = getSessionFactory().getCurrentSession()
                .createQuery( "select a, count(distinct ee) from ExpressionExperiment ee "
                        + "join ee.bioAssays ba "
                        + "join ba.originalPlatform a "
                        + "left join ba.arrayDesignUsed au "
                        + AclQueryUtils.formAclRestrictionClause( "ee.id" ) + " "
                        + "and a <> au "   // ignore noop switch
                        + formNonTroubledClause( "ee" )
                        + formNonTroubledClause( "a" ) + " "
                        + "group by a"
                        + ( maxResults > 0 ? " order by count(distinct ee) desc" : "" ) );
        AclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        //noinspection unchecked
        List<Object[]> result = query
                .setCacheable( true )
                .setMaxResults( maxResults )
                .list();
        return result.stream().collect( groupingBy( row -> ( ArrayDesign ) row[0], summingLong( row -> ( Long ) row[1] ) ) );
    }

    @Override
    public Map<ArrayDesign, Long> getOriginalPlatformsUsageFrequency( Collection<Long> eeIds, int maxResults ) {
        if ( eeIds.isEmpty() ) {
            return Collections.emptyMap();
        }
        Query query = getSessionFactory().getCurrentSession()
                .createQuery( "select a, count(distinct ee) from ExpressionExperiment ee "
                        + "join ee.bioAssays ba "
                        + "join ba.originalPlatform a "
                        + "left join ba.arrayDesignUsed au "
                        + "where ee.id in :ids "
                        + "and a <> au "   // ignore noop switch
                        + formNonTroubledClause( "ee" )
                        + formNonTroubledClause( "a" ) + " "
                        + "group by a"
                        + ( maxResults > 0 ? " order by count(distinct ee) desc" : "" ) );
        //noinspection unchecked
        List<Object[]> result = query
                .setMaxResults( maxResults )
                .setParameterList( "ids", eeIds )
                .setCacheable( true )
                .list();
        return result.stream().collect( groupingBy( row -> ( ArrayDesign ) row[0], summingLong( row -> ( Long ) row[1] ) ) );
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
    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment ) {
        String queryString = "select distinct b from BioAssayDimension b, ExpressionExperiment e "
                + "inner join b.bioAssays bba inner join e.bioAssays eb where eb = bba and e = :ee ";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ee", expressionExperiment ).list();
    }

    @Override
    public long getBioMaterialCount( ExpressionExperiment expressionExperiment ) {
        //language=HQL
        final String queryString =
                "select count(distinct sample) from ExpressionExperiment as ee "
                        + "inner join ee.bioAssays as ba "
                        + "inner join ba.sampleUsed as sample "
                        + "where ee = :ee";

        return ( Long ) this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "ee", expressionExperiment )
                .uniqueResult();
    }

    /**
     * @param ee the expression experiment
     * @return count of RAW vectors.
     */
    @Override
    public long getDesignElementDataVectorCount( ExpressionExperiment ee ) {
        //language=HQL
        final String queryString = "select count(distinct dedv) from ExpressionExperiment ee "
                + "inner join ee.rawExpressionDataVectors dedv where ee = :ee";
        return ( Long ) this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "ee", ee )
                .uniqueResult();
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
    public Map<Taxon, Long> getPerTaxonCount() {
        String queryString = "select ee.taxon, count(distinct ee) as EE_COUNT from ExpressionExperiment ee "
                + AclQueryUtils.formAclRestrictionClause( "ee.id" ) + " "
                + formNonTroubledClause( "ee" ) + " "
                + "group by ee.taxon "
                + "order by EE_COUNT desc";

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        AclQueryUtils.addAclParameters( query, ExpressionExperiment.class );

        // it is important to cache this, as it gets called on the home page. Though it's actually fast.
        //noinspection unchecked
        List<Object[]> list = query
                .setCacheable( true )
                .list();

        return list.stream()
                .collect( Collectors.toMap( row -> ( Taxon ) row[0], row -> ( Long ) row[1] ) );
    }

    @Override
    public Map<Taxon, Long> getPerTaxonCount( List<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyMap();
        }
        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery(
                        "select ee.taxon, count(distinct ee) as EE_COUNT from ExpressionExperiment ee "
                                + "where ee.id in :eeIds "
                                + "group by ee.taxon "
                                + "order by EE_COUNT desc" )
                .setParameterList( "eeIds", ids )
                .list();
        return list.stream()
                .collect( Collectors.toMap( row -> ( Taxon ) row[0], row -> ( Long ) row[1] ) );
    }

    public Map<Long, Long> getPopulatedFactorCounts( Collection<Long> ids ) {
        Map<Long, Long> results = new HashMap<>();
        if ( ids.size() == 0 ) {
            return results;
        }

        for ( Long id : ids ) {
            results.put( id, 0L );
        }

        String queryString = "select e.id,count(distinct ef.id) from ExpressionExperiment e inner join e.bioAssays ba"
                + " inner join ba.sampleUsed bm inner join bm.factorValues fv inner join fv.experimentalFactor "
                + "ef where e.id in (:ids) group by e.id";
        //noinspection unchecked
        List<Object[]> res = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();

        for ( Object[] ro : res ) {
            Long id = ( Long ) ro[0];
            Long count = ( Long ) ro[1];
            results.put( id, count );
        }
        return results;
    }

    @Override
    public Map<Long, Long> getPopulatedFactorCountsExcludeBatch( Collection<Long> ids ) {
        Map<Long, Long> results = new HashMap<>();
        if ( ids.size() == 0 ) {
            return results;
        }

        for ( Long id : ids ) {
            results.put( id, 0L );
        }

        String queryString = "select e.id,count(distinct ef.id) from ExpressionExperiment e inner join e.bioAssays ba"
                + " inner join ba.sampleUsed bm inner join bm.factorValues fv inner join fv.experimentalFactor ef "
                + " inner join ef.category cat where e.id in (:ids) and cat.category != (:category) and ef.name != (:name) group by e.id";

        //noinspection unchecked
        List<Object[]> res = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ) // Set ids
                .setParameter( "category", ExperimentalFactorService.BATCH_FACTOR_CATEGORY_NAME ) // Set batch category
                .setParameter( "name", ExperimentalFactorService.BATCH_FACTOR_NAME ) // set batch name
                .list();

        for ( Object[] ro : res ) {
            Long id = ( Long ) ro[0];
            Long count = ( Long ) ro[1];
            results.put( id, count );
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<QuantitationType, Long> getQuantitationTypeCount( ExpressionExperiment ee ) {

        //language=HQL
        final String queryString = "select quantType, count(distinct vectors) as count "
                + "from ubic.gemma.model.expression.experiment.ExpressionExperiment ee "
                + "join ee.rawExpressionDataVectors as vectors "
                + "join vectors.quantitationType as quantType "
                + "where ee = :ee "
                + "group by quantType";

        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "ee", ee )
                .list();

        Map<QuantitationType, Long> qtCounts = new HashMap<>();

        for ( Object[] tuple : list ) {
            qtCounts.put( ( QuantitationType ) tuple[0], ( Long ) tuple[1] );
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
            @Nullable ArrayDesign arrayDesign ) {
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
    public QuantitationType getPreferredQuantitationType( ExpressionExperiment ee ) {
        return ( QuantitationType ) getSessionFactory().getCurrentSession()
                .createQuery( "select qt from ExpressionExperiment ee "
                        + "join ee.rawExpressionDataVectors rv "
                        + "join rv.quantitationType qt "
                        + "where qt.isPreferred = true and ee = :ee "
                        + "group by qt" )
                .setParameter( "ee", ee )
                .uniqueResult();
    }

    @Override
    public QuantitationType getMaskedPreferredQuantitationType( ExpressionExperiment ee ) {
        return ( QuantitationType ) getSessionFactory().getCurrentSession()
                .createQuery( "select qt from ExpressionExperiment ee "
                        + "join ee.processedExpressionDataVectors pv "
                        + "join pv.quantitationType qt "
                        + "where qt.isMaskedPreferred = true and ee = :ee "
                        + "group by qt" )
                .setParameter( "ee", ee )
                .uniqueResult();
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

        //noinspection unchecked
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

    @Value
    private static class ExpressionExperimentDetail {

        public static ExpressionExperimentDetail fromRow( Object[] row ) {
            return new ExpressionExperimentDetail( ( Long ) row[1], ( Long ) row[2], ( Long ) row[3], ( Integer ) row[4] );
        }

        @Nullable
        Long arrayDesignUsedId;
        @Nullable
        Long originalPlatformId;
        @Nullable
        Long otherPartId;
        Integer bioAssaysCount;
    }

    /**
     * Gather various EE details and group them by ID.
     */
    private Map<Long, List<ExpressionExperimentDetail>> getExpressionExperimentDetailsById( List<Long> expressionExperimentIds, boolean cacheable ) {
        if ( expressionExperimentIds.isEmpty() ) {
            return Collections.emptyMap();
        }
        //noinspection unchecked
        List<Object[]> results = getSessionFactory().getCurrentSession()
                .createQuery( "select ee.id, ad.id, op.id, oe.id, ee.bioAssays.size from ExpressionExperiment as ee "
                        + "left join ee.bioAssays ba "
                        + "left join ba.arrayDesignUsed ad "
                        + "left join ba.originalPlatform op " // not all bioAssays have an original platform
                        + "left join ee.otherParts as oe "    // not all experiments are splitted
                        + "where ee.id in :eeIds "
                        // FIXME: apply ACLs, other parts or platform might be private
                        + "group by ee, ad, op, oe" )
                .setParameterList( "eeIds", expressionExperimentIds )
                .setCacheable( cacheable )
                .list();
        return results.stream().collect(
                groupingBy( row -> ( Long ) row[0],
                        Collectors.mapping( ExpressionExperimentDetail::fromRow, Collectors.toList() ) ) );
    }

    @Override
    public List<ExpressionExperiment> loadWithRelationsAndCache( List<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyList();
        }
        //noinspection unchecked
        return ( List<ExpressionExperiment> ) getSessionFactory().getCurrentSession()
                .createQuery( "select ee from ExpressionExperiment ee "
                        + "left join ee.accession acc "
                        + "left join ee.experimentalDesign as EDES "
                        + "left join ee.curationDetails as s " /* needed for trouble status */
                        + "left join s.lastNeedsAttentionEvent as eAttn "
                        + "left join s.lastNoteUpdateEvent as eNote "
                        + "left join s.lastTroubledEvent as eTrbl "
                        + "left join ee.geeq as geeq "
                        + "where ee.id in :ids" )
                .setParameterList( "ids", ids )
                .setCacheable( true )
                // this transformer performs initialization of cached results
                .setResultTransformer( getEntityTransformer() )
                .list();
    }

    @Override
    public Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjects
            ( @Nullable Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit ) {
        if ( ids != null && ids.isEmpty() ) {
            return new Slice<>( Collections.emptyList(), sort, offset, limit, 0L );
        }
        return this.doLoadDetailsValueObjects( getFiltersForIdsAndTaxon( ids, taxon ), sort, offset, limit, false );
    }

    @Override
    public Slice<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIdsWithCache( @Nullable Collection<Long> ids, @Nullable Taxon taxon, @Nullable Sort sort, int offset, int limit ) {
        if ( ids != null && ids.isEmpty() ) {
            return new Slice<>( Collections.emptyList(), sort, offset, limit, 0L );
        }
        return this.doLoadDetailsValueObjects( getFiltersForIdsAndTaxon( ids, taxon ), sort, offset, limit, true );
    }

    @Override
    public List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIds( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyList();
        }
        return this.doLoadDetailsValueObjects( getFiltersForIdsAndTaxon( ids, null ), null, 0, 0, false );
    }

    @Override
    public List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjectsByIdsWithCache( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyList();
        }
        return this.doLoadDetailsValueObjects( getFiltersForIdsAndTaxon( ids, null ), null, 0, 0, true );
    }

    private Filters getFiltersForIdsAndTaxon( @Nullable Collection<Long> ids, @Nullable Taxon taxon ) {
        Filters filters = Filters.empty();

        if ( ids != null ) {
            List<Long> idList = new ArrayList<>( ids );
            Collections.sort( idList );
            filters.and( OBJECT_ALIAS, "id", Long.class, Filter.Operator.in, idList );
        }

        if ( taxon != null ) {
            filters.and( TaxonDao.OBJECT_ALIAS, "id", Long.class, Filter.Operator.eq, taxon.getId() );
        }

        return filters;
    }

    private Slice<ExpressionExperimentDetailsValueObject> doLoadDetailsValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit, boolean cacheable ) {
        // Compose query
        Query query = this.getFilteringQuery( filters, sort );

        if ( offset > 0 ) {
            query.setFirstResult( offset );
        }
        if ( limit > 0 ) {
            query.setMaxResults( limit );
        }

        // overall timer
        StopWatch timer = StopWatch.createStarted();

        // timers for sub-steps
        StopWatch countingTimer = StopWatch.create();
        StopWatch postProcessingTimer = StopWatch.create();
        StopWatch detailsTimer = StopWatch.create();
        StopWatch analysisInformationTimer = StopWatch.create();

        query.setResultTransformer( getDetailedValueObjectTransformer( cacheable, postProcessingTimer, detailsTimer, analysisInformationTimer ) );

        //noinspection unchecked
        List<ExpressionExperimentDetailsValueObject> vos = query
                .setCacheable( cacheable )
                .list();

        countingTimer.start();
        Long totalElements;
        if ( limit > 0 && ( vos.isEmpty() || vos.size() == limit ) ) {
            totalElements = ( Long ) this.getFilteringCountQuery( filters )
                    .setCacheable( cacheable )
                    .uniqueResult();
        } else {
            totalElements = offset + ( long ) vos.size();
        }
        countingTimer.stop();

        timer.stop();

        if ( timer.getTime() > REPORT_SLOW_QUERY_AFTER_MS ) {
            log.warn( String.format( "EE details VO query + postprocessing: %d ms (query: %d ms, counting: %d ms, initializing VOs: %d ms, loading details (bioAssays + bioAssays.arrayDesignUsed + originalPlatforms + otherParts): %d ms, retrieving analysis information: %s ms)",
                    timer.getTime(),
                    timer.getTime() - postProcessingTimer.getTime(),
                    countingTimer.getTime(),
                    postProcessingTimer.getTime(),
                    detailsTimer.getTime(),
                    analysisInformationTimer.getTime() ) );
        }

        return new Slice<>( vos, sort, offset, limit, totalElements );
    }

    private TypedResultTransformer<ExpressionExperimentDetailsValueObject> getDetailedValueObjectTransformer( boolean cacheable, StopWatch postProcessingTimer, StopWatch detailsTimer, StopWatch analysisInformationTimer ) {
        return new TypedResultTransformer<ExpressionExperimentDetailsValueObject>() {
            @Override
            public ExpressionExperimentDetailsValueObject transformTuple( Object[] row, String[] aliases ) {
                ExpressionExperiment ee = ( ExpressionExperiment ) row[0];
                initializeCachedFilteringResult( ee );
                AclObjectIdentity aoi = ( AclObjectIdentity ) row[1];
                AclSid sid = ( AclSid ) row[2];
                return new ExpressionExperimentDetailsValueObject( ee, aoi, sid );
            }

            @Override
            public List<ExpressionExperimentDetailsValueObject> transformListTyped( List<ExpressionExperimentDetailsValueObject> vos ) {
                postProcessingTimer.start();

                // sort + distinct for cache consistency
                List<Long> expressionExperimentIds = vos.stream()
                        .map( Identifiable::getId )
                        .sorted()
                        .distinct()
                        .collect( Collectors.toList() );

                // fetch some extras details
                // we could make this a single query in getLoadValueObjectDetails, but performing a jointure with the bioAssays
                // and arrayDesignUsed is inefficient in the general case, so we only fetch what we need here
                detailsTimer.start();
                Map<Long, List<ExpressionExperimentDetail>> detailsByEE = getExpressionExperimentDetailsById( expressionExperimentIds, cacheable );
                detailsTimer.stop();

                for ( ExpressionExperimentDetailsValueObject vo : vos ) {
                    List<ExpressionExperimentDetail> details = detailsByEE.get( vo.getId() );

                    Set<Long> arrayDesignsUsedIds = details.stream()
                            .map( ExpressionExperimentDetail::getArrayDesignUsedId )
                            .filter( Objects::nonNull )
                            .collect( Collectors.toSet() );

                    // we need those later for computing original platforms
                    Collection<ArrayDesignValueObject> adVos = arrayDesignsUsedIds.stream()
                            .map( id -> ( ArrayDesign ) getSessionFactory().getCurrentSession().get( ArrayDesign.class, id ) )
                            .map( ArrayDesignValueObject::new )
                            .collect( Collectors.toSet() );
                    vo.setArrayDesigns( adVos ); // also sets taxon name, technology type, and number of ADs.

                    // original platforms
                    Collection<ArrayDesignValueObject> originalPlatformsVos = details.stream()
                            .map( ExpressionExperimentDetail::getOriginalPlatformId )
                            .filter( Objects::nonNull ) // on original platform for the bioAssay
                            .distinct()
                            .filter( op -> !arrayDesignsUsedIds.contains( op ) ) // omit noop switches
                            .map( id -> ( ArrayDesign ) getSessionFactory().getCurrentSession().get( ArrayDesign.class, id ) )
                            .map( ArrayDesignValueObject::new )
                            .collect( Collectors.toSet() );
                    vo.setOriginalPlatforms( originalPlatformsVos );

                    Integer bioAssayCount = details.stream()
                            .map( ExpressionExperimentDetail::getBioAssaysCount )
                            .findFirst()
                            .orElse( 0 );
                    vo.setNumberOfBioAssays( bioAssayCount );

                    Set<Long> otherPartsIds = details.stream()
                            .map( ExpressionExperimentDetail::getOtherPartId )
                            .filter( Objects::nonNull )
                            .collect( Collectors.toSet() );

                    List<ExpressionExperimentValueObject> otherPartsVos = loadValueObjectsByIds( otherPartsIds ).stream()
                            .sorted( Comparator.comparing( ExpressionExperimentValueObject::getShortName ) ).collect( Collectors.toList() );

                    // other parts (maybe fetch in details query?)
                    vo.setOtherParts( otherPartsVos );
                }

                try ( StopWatchUtils.StopWatchRegion ignored = StopWatchUtils.measuredRegion( analysisInformationTimer ) ) {
                    populateAnalysisInformation( vos, cacheable );
                }

                postProcessingTimer.stop();

                return vos;
            }
        };
    }

    @Override
    public Slice<ExpressionExperimentValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        //noinspection unchecked
        List<Object[]> result = getSessionFactory().getCurrentSession()
                .createQuery( "select be.shortName, ea.accession from BlacklistedExperiment be left join be.externalAccession ea" )
                .setCacheable( true )
                .list();
        if ( result.isEmpty() ) {
            return new Slice<>( Collections.emptyList(), sort, offset, limit, 0L );
        }
        if ( filters == null ) {
            filters = Filters.empty();
        } else {
            // we create a copy because we don't want to leak the lits of blacklisted EEs in the filter
            filters = Filters.by( filters );
        }
        Set<String> blacklistedShortNames = result.stream().map( row -> ( String ) row[0] ).filter( Objects::nonNull ).collect( Collectors.toSet() );
        Set<String> blacklistedAccessions = result.stream().map( row -> ( String ) row[1] ).filter( Objects::nonNull ).collect( Collectors.toSet() );
        Filters.FiltersClauseBuilder clause = filters.and();
        if ( !blacklistedShortNames.isEmpty() )
            clause = clause.or( "ee", "shortName", String.class, Filter.Operator.in, blacklistedShortNames );
        if ( !blacklistedAccessions.isEmpty() )
            clause = clause.or( "ee", "accession.accession", String.class, Filter.Operator.in, blacklistedAccessions );
        clause.build();
        return loadValueObjects( filters, sort, offset, limit );
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
    protected ExpressionExperimentValueObject doLoadValueObject( ExpressionExperiment entity ) {
        return new ExpressionExperimentValueObject( entity );
    }

    @Override
    protected void postProcessValueObjects( List<ExpressionExperimentValueObject> results ) {
        populateArrayDesignCount( results );
    }

    @Override
    public List<ExpressionExperimentValueObject> loadValueObjects( @Nullable Filters
            filters, @Nullable Sort sort ) {
        if ( sort == null ) {
            sort = Sort.by( OBJECT_ALIAS, "id", null, "id" );
        }
        return super.loadValueObjects( filters, sort );
    }

    @Override
    public Slice<ExpressionExperimentValueObject> loadValueObjects( @Nullable Filters
            filters, @Nullable Sort sort, int offset, int limit ) {
        if ( sort == null ) {
            sort = Sort.by( OBJECT_ALIAS, "id", null, "id" );
        }
        return super.loadValueObjects( filters, sort, offset, limit );
    }

    @Override
    protected TypedResultTransformer<ExpressionExperimentValueObject> getValueObjectTransformer() {
        TypedResultTransformer<ExpressionExperimentValueObject> transformer = super.getValueObjectTransformer();
        return new TypedResultTransformer<ExpressionExperimentValueObject>() {
            @Override
            public ExpressionExperimentValueObject transformTuple( Object[] row, String[] aliases ) {
                ExpressionExperiment ee = ( ExpressionExperiment ) row[0];
                AclObjectIdentity aoi = ( AclObjectIdentity ) row[1];
                AclSid sid = ( AclSid ) row[2];
                initializeCachedFilteringResult( ee );
                return new ExpressionExperimentValueObject( ee, aoi, sid );
            }

            @Override
            public List<ExpressionExperimentValueObject> transformListTyped( List<ExpressionExperimentValueObject> collection ) {
                return transformer.transformListTyped( collection );
            }
        };
    }

    @Override
    public void remove( ExpressionExperiment ee ) {
        log.info( "Deleting " + ee + "..." );

        // Note that links and analyses are deleted separately - see the ExpressionExperimentService.

        // these are tied to the audit trail and will cause lock problems it we don't clear first (due to cascade=all on the curation details, but
        // this may be okay now with updated config - see CurationDetails.hbm.xml)
        ee.getCurationDetails().setLastNeedsAttentionEvent( null );
        ee.getCurationDetails().setLastNoteUpdateEvent( null );
        ee.getCurationDetails().setLastTroubledEvent( null );

        // dissociate this EE from other parts
        for ( ExpressionExperiment e : ee.getOtherParts() ) {
            e.getOtherParts().remove( ee );
        }
        ee.getOtherParts().clear();

        // detach from BA dimensions
        Collection<BioAssayDimension> dims = this.getBioAssayDimensions( ee );
        for ( BioAssayDimension dim : dims ) {
            dim.getBioAssays().clear();
        }

        // first pass, detach BAs from the samples
        for ( BioAssay ba : ee.getBioAssays() ) {
            ba.getSampleUsed().getBioAssaysUsedIn().remove( ba );
        }

        // second pass, delete samples that are no longer attached to BAs
        Set<BioMaterial> samplesToRemove = new HashSet<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            if ( ba.getSampleUsed().getBioAssaysUsedIn().isEmpty() ) {
                samplesToRemove.add( ba.getSampleUsed() );
            } else {
                log.warn( String.format( "%s is attached to more than one ExpressionExperiment, the sample will not be deleted.", ba.getSampleUsed() ) );
            }
        }

        super.remove( ee );

        // those need to be removed afterward because otherwise the BioAssay.sampleUsed would become transient while
        // cascading and that is not allowed in the data model
        log.debug( String.format( "Removing %d BioMaterial that are no longer attached to any BioAssay", samplesToRemove.size() ) );
        for ( BioMaterial bm : samplesToRemove ) {
            getSessionFactory().getCurrentSession().delete( bm );
        }
    }

    @Override
    public void thaw( final ExpressionExperiment expressionExperiment ) {
        thawWithoutVectors( expressionExperiment );
        /*
         * Optional because this could be slow.
         */
        Hibernate.initialize( expressionExperiment.getRawExpressionDataVectors() );
        Hibernate.initialize( expressionExperiment.getProcessedExpressionDataVectors() );
    }

    // "thawLite"
    @Override
    public void thawWithoutVectors( final ExpressionExperiment ee ) {
        thawForFrontEnd( ee );

        Hibernate.initialize( ee.getQuantitationTypes() );
        Hibernate.initialize( ee.getCharacteristics() );

        if ( ee.getAuditTrail() != null ) {
            Hibernate.initialize( ee.getAuditTrail().getEvents() );
        }

        Hibernate.initialize( ee.getBioAssays() );
        for ( BioAssay ba : ee.getBioAssays() ) {
            Hibernate.initialize( ba.getArrayDesignUsed() );
            Hibernate.initialize( ba.getArrayDesignUsed().getDesignProvider() );
            if ( ba.getOriginalPlatform() != null ) {
                Hibernate.initialize( ba.getOriginalPlatform() );
            }
            BioMaterial bm = ba.getSampleUsed();
            if ( bm != null ) {
                Hibernate.initialize( bm.getFactorValues() );
                for ( FactorValue fv : bm.getFactorValues() ) {
                    Hibernate.initialize( fv.getExperimentalFactor() );
                }
                Hibernate.initialize( bm.getTreatments() );
            }
        }
    }

    @Override
    public void thawBioAssays( ExpressionExperiment expressionExperiment ) {
        thawForFrontEnd( expressionExperiment );

        Hibernate.initialize( expressionExperiment.getBioAssays() );

        for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
            Hibernate.initialize( ba.getArrayDesignUsed() );
            Hibernate.initialize( ba.getSampleUsed() );
            for ( FactorValue fv : ba.getSampleUsed().getFactorValues() ) {
                Hibernate.initialize( fv.getExperimentalFactor() );
            }
            Hibernate.initialize( ba.getOriginalPlatform() );
        }
    }

    @Override
    public void thawForFrontEnd( final ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment.getAccession() != null ) {
            Hibernate.initialize( expressionExperiment.getAccession() );
            Hibernate.initialize( expressionExperiment.getAccession().getExternalDatabase() );
        }

        if ( expressionExperiment.getMeanVarianceRelation() != null ) {
            Hibernate.initialize( expressionExperiment.getMeanVarianceRelation() );
            Hibernate.initialize( expressionExperiment.getMeanVarianceRelation().getMeans() );
            Hibernate.initialize( expressionExperiment.getMeanVarianceRelation().getVariances() );
        }

        if ( expressionExperiment.getPrimaryPublication() != null ) {
            Hibernate.initialize( expressionExperiment.getPrimaryPublication() );
            if ( expressionExperiment.getPrimaryPublication().getPublication() != null ) {
                Hibernate.initialize( expressionExperiment.getPrimaryPublication().getPubAccession() );
                Hibernate.initialize( expressionExperiment.getPrimaryPublication().getPubAccession().getExternalDatabase() );
            }
        }

        Hibernate.initialize( expressionExperiment.getAuditTrail() );
        Hibernate.initialize( expressionExperiment.getGeeq() );
        Hibernate.initialize( expressionExperiment.getCurationDetails() );

        Hibernate.initialize( expressionExperiment.getOtherParts() );

        if ( expressionExperiment.getExperimentalDesign() != null ) {
            Hibernate.initialize( expressionExperiment.getExperimentalDesign() );
            Hibernate.initialize( expressionExperiment.getExperimentalDesign().getExperimentalFactors() );
            for ( ExperimentalFactor ef : expressionExperiment.getExperimentalDesign().getExperimentalFactors() ) {
                Hibernate.initialize( ef );
                for ( FactorValue fv : ef.getFactorValues() ) {
                    Hibernate.initialize( fv.getExperimentalFactor() ); // is it even necessary?
                }
            }
            Hibernate.initialize( expressionExperiment.getExperimentalDesign().getTypes() );
        }

        if ( expressionExperiment.getPrimaryPublication() != null ) {
            Hibernate.initialize( expressionExperiment.getPrimaryPublication().getMeshTerms() );
            Hibernate.initialize( expressionExperiment.getPrimaryPublication().getKeywords() );
            Hibernate.initialize( expressionExperiment.getPrimaryPublication().getChemicals() );
        }

        for ( BibliographicReference br : expressionExperiment.getOtherRelevantPublications() ) {
            Hibernate.initialize( br.getMeshTerms() );
            Hibernate.initialize( br.getKeywords() );
            Hibernate.initialize( br.getChemicals() );
        }
    }

    @Override
    protected Query getFilteringQuery( @Nullable Filters filters, @Nullable Sort sort ) {
        // the constants for aliases are messing with the inspector
        //language=HQL
        return finishFilteringQuery( "select ee, " + AclQueryUtils.AOI_ALIAS + ", " + AclQueryUtils.SID_ALIAS + " "
                + "from ExpressionExperiment as ee "
                + "left join fetch ee.accession acc "
                + "left join fetch ee.experimentalDesign as EDES "
                + "left join fetch ee.curationDetails as s " /* needed for trouble status */
                + "left join fetch s.lastNeedsAttentionEvent as eAttn "
                + "left join fetch eAttn.eventType "
                + "left join fetch s.lastNoteUpdateEvent as eNote "
                + "left join fetch eNote.eventType "
                + "left join fetch s.lastTroubledEvent as eTrbl "
                + "left join fetch eTrbl.eventType "
                + "left join fetch ee.geeq as geeq", filters, sort, groupByIfNecessary( sort, ONE_TO_MANY_ALIASES ) );
    }

    @Override
    protected void initializeCachedFilteringResult( ExpressionExperiment ee ) {
        Hibernate.initialize( ee.getAccession() );
        Hibernate.initialize( ee.getExperimentalDesign() );
        Hibernate.initialize( ee.getCurationDetails() );
        Hibernate.initialize( ee.getGeeq() );
    }

    @Override
    protected Query getFilteringIdQuery( @Nullable Filters filters, @Nullable Sort sort ) {
        //language=HQL
        return finishFilteringQuery( "select ee.id "
                + "from ExpressionExperiment as ee "
                + "left join ee.accession acc "
                + "left join ee.experimentalDesign as EDES "
                + "left join ee.curationDetails as s " /* needed for trouble status */
                + "left join s.lastNeedsAttentionEvent as eAttn "
                + "left join s.lastNoteUpdateEvent as eNote "
                + "left join s.lastTroubledEvent as eTrbl "
                + "left join ee.geeq as geeq", filters, sort, groupByIfNecessary( sort, ONE_TO_MANY_ALIASES ) );
    }

    @Override
    protected Query getFilteringCountQuery( @Nullable Filters filters ) {
        //language=HQL
        return finishFilteringQuery( "select count(" + distinctIfNecessary() + "ee) "
                + "from ExpressionExperiment as ee "
                + "left join ee.accession acc "
                + "left join ee.experimentalDesign as EDES "
                + "left join ee.curationDetails as s " /* needed for trouble status */
                + "left join s.lastNeedsAttentionEvent as eAttn "
                + "left join s.lastNoteUpdateEvent as eNote "
                + "left join s.lastTroubledEvent as eTrbl "
                + "left join ee.geeq as geeq", filters, null, null );
    }

    private Query finishFilteringQuery( String queryString, @Nullable Filters filters, @Nullable Sort sort, @Nullable String groupBy ) {
        if ( filters == null ) {
            filters = Filters.empty();
        } else {
            filters = Filters.by( filters );
        }

        // Restrict to non-troubled EEs for non-administrators
        addNonTroubledFilter( filters, OBJECT_ALIAS );
        // Filtering by AD is costly because we need two jointures, so the results might in some rare cases return EEs
        // from troubled platforms that have not been mark themselves as troubled. Thus, we do it only if it is present
        // in the query
        if ( FiltersUtils.containsAnyAlias( filters, sort, ARRAY_DESIGN_ALIAS ) ) {
            addNonTroubledFilter( filters, ARRAY_DESIGN_ALIAS );
        }

        // The following two associations are retrieved eagerly via select, which is far more efficient since they are
        // commonly shared across EEs and may benefit from the second-level cache
        if ( FiltersUtils.containsAnyAlias( filters, sort, EXTERNAL_DATABASE_ALIAS ) ) {
            queryString += " left join acc.externalDatabase as " + EXTERNAL_DATABASE_ALIAS;  // this one will be fetched in a subsequent select clause since it's eagerly fetched from DatabaseEntry
        }
        if ( FiltersUtils.containsAnyAlias( filters, sort, TAXON_ALIAS ) ) {
            queryString += " left join ee.taxon as " + TAXON_ALIAS;
        }

        // fetching characteristics, bioAssays and arrayDesignUsed is costly, so we reserve these operations only if it
        // is mentioned in the filters

        if ( FiltersUtils.containsAnyAlias( null, sort, CHARACTERISTIC_ALIAS ) ) {
            queryString += " left join ee.characteristics as " + CHARACTERISTIC_ALIAS;
        }

        if ( FiltersUtils.containsAnyAlias( null, sort, FACTOR_VALUE_CHARACTERISTIC_ALIAS ) ) {
            queryString += " left join ee.experimentalDesign.experimentalFactors as ef";
            queryString += " left join ef.factorValues as fv";
            queryString += " left join fv.characteristics as " + FACTOR_VALUE_CHARACTERISTIC_ALIAS;
        }

        if ( FiltersUtils.containsAnyAlias( null, sort, BIO_ASSAY_ALIAS, BIO_MATERIAL_CHARACTERISTIC_ALIAS, ARRAY_DESIGN_ALIAS ) ) {
            queryString += " left join ee.bioAssays as " + BIO_ASSAY_ALIAS;
        }

        // this is a shorthand for all characteristics (direct + biomaterial + experimental design)
        if ( FiltersUtils.containsAnyAlias( null, sort, ALL_CHARACTERISTIC_ALIAS ) ) {
            queryString += " left join ee.allCharacteristics as " + ALL_CHARACTERISTIC_ALIAS;
        }

        if ( FiltersUtils.containsAnyAlias( null, sort, BIO_MATERIAL_CHARACTERISTIC_ALIAS ) ) {
            queryString += " left join " + BIO_ASSAY_ALIAS + ".sampleUsed.characteristics as " + BIO_MATERIAL_CHARACTERISTIC_ALIAS;
        }

        if ( FiltersUtils.containsAnyAlias( null, sort, ArrayDesignDao.OBJECT_ALIAS ) ) {
            queryString += " left join " + BIO_ASSAY_ALIAS + ".arrayDesignUsed as " + ARRAY_DESIGN_ALIAS;
        }

        // parts of this query (above) are only needed for administrators: the notes, so it could theoretically be sped up even more
        queryString += AclQueryUtils.formAclRestrictionClause( OBJECT_ALIAS + ".id" );

        queryString += FilterQueryUtils.formRestrictionClause( filters );

        if ( groupBy != null ) {
            queryString += " group by " + groupBy;
        }

        queryString += FilterQueryUtils.formOrderByClause( sort );

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        AclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        FilterQueryUtils.addRestrictionParameters( query, filters );

        return query;
    }

    @Override
    protected void configureFilterableProperties( FilterablePropertiesConfigurer configurer ) {
        super.configureFilterableProperties( configurer );

        configurer.registerProperties( "taxon", "bioAssayCount" );

        // irrelevant
        configurer.unregisterProperty( "accession.Uri" );
        configurer.unregisterProperty( "geeq.id" );
        configurer.unregisterProperty( "source" );
        configurer.unregisterProperty( "otherParts.size" );
        configurer.unregisterProperty( "otherRelevantPublications.size" );

        configurer.unregisterProperties( p -> p.endsWith( "externalDatabases.size" ) );

        // reserved for curators
        configurer.unregisterProperty( "curationDetails.curationNote" );

        // only expose selected fields for GEEQ
        configurer.unregisterEntity( "geeq.", Geeq.class );
        configurer.registerProperty( "geeq.publicQualityScore" );
        configurer.registerProperty( "geeq.publicSuitabilityScore" );

        // the primary publication is not very useful, but its attached database entry is
        configurer.unregisterEntity( "primaryPublication.", BibliographicReference.class );
        configurer.registerEntity( "primaryPublication.pubAccession.", DatabaseEntry.class, 2 );
        configurer.unregisterProperty( "primaryPublication.pubAccession.Uri" );

        // attached terms
        configurer.registerAlias( "characteristics.", CHARACTERISTIC_ALIAS, Characteristic.class, null, 1, true );
        configurer.unregisterProperty( "characteristics.originalValue" );
        configurer.unregisterProperty( "characteristics.migratedToStatement" );
        configurer.registerAlias( "experimentalDesign.experimentalFactors.factorValues.characteristics.", FACTOR_VALUE_CHARACTERISTIC_ALIAS, Characteristic.class, null, 1, true );
        configurer.unregisterProperty( "experimentalDesign.experimentalFactors.factorValues.characteristics.originalValue" );
        configurer.unregisterProperty( "experimentalDesign.experimentalFactors.factorValues.characteristics.migratedToStatement" );
        configurer.registerAlias( "bioAssays.sampleUsed.characteristics.", BIO_MATERIAL_CHARACTERISTIC_ALIAS, Characteristic.class, null, 1, true );
        configurer.unregisterProperty( "bioAssays.sampleUsed.characteristics.migratedToStatement" );
        configurer.unregisterProperty( "bioAssays.sampleUsed.characteristics.originalValue" );
        configurer.registerAlias( "allCharacteristics.", ALL_CHARACTERISTIC_ALIAS, Characteristic.class, null, 1, true );
        configurer.unregisterProperty( "allCharacteristics.originalValue" );
        configurer.unregisterProperty( "allCharacteristics.migratedToStatement" );

        configurer.registerAlias( "bioAssays.", BIO_ASSAY_ALIAS, BioAssay.class, null, 2, true );
        configurer.unregisterProperty( "bioAssays.accession.Uri" );
        configurer.unregisterProperty( "bioAssays.sampleUsed.factorValues.size" );
        configurer.unregisterProperty( "bioAssays.sampleUsed.treatments.size" );

        // this is not useful, unless we add an alias to the alternate names
        configurer.unregisterProperties( p -> p.endsWith( "alternateNames.size" ) );
    }

    /**
     * Checks for special properties that are allowed to be referenced on certain objects. E.g. characteristics on EEs.
     * {@inheritDoc}
     */
    @Override
    protected FilterablePropertyMeta getFilterablePropertyMeta( String propertyName ) {
        if ( propertyName.equals( "taxon" ) ) {
            return getFilterablePropertyMeta( TAXON_ALIAS, "id", Taxon.class )
                    .withDescription( "alias for taxon.id" );
        }

        if ( propertyName.equals( "bioAssayCount" ) ) {
            return getFilterablePropertyMeta( "bioAssays.size" )
                    .withDescription( "alias for bioAssays.size" );
        }

        if ( propertyName.equals( "geeq.publicQualityScore" ) ) {
            return new FilterablePropertyMeta( null, "(case when geeq.manualQualityOverride = true then geeq.manualQualityScore else geeq.detectedQualityScore end)", Double.class, null, null );
        }

        if ( propertyName.equals( "geeq.publicSuitabilityScore" ) ) {
            return new FilterablePropertyMeta( null, "(case when geeq.manualSuitabilityOverride = true then geeq.manualSuitabilityScore else geeq.detectedSuitabilityScore end)", Double.class, null, null );
        }

        return super.getFilterablePropertyMeta( propertyName );
    }

    /**
     * Filling 'hasDifferentialExpressionAnalysis' and 'hasCoexpressionAnalysis'
     */
    private void populateAnalysisInformation( Collection<ExpressionExperimentDetailsValueObject> vos, boolean cacheable ) {
        if ( vos.isEmpty() ) {
            return;
        }

        // these are cached queries (thus super-fast)
        Set<Long> withCoexpression = new HashSet<>( getExpressionExperimentIdsWithCoexpression( cacheable ) );
        Set<Long> withDiffEx = new HashSet<>( getExpressionExperimentIdsWithDifferentialExpressionAnalysis( cacheable ) );

        for ( ExpressionExperimentDetailsValueObject vo : vos ) {
            vo.setHasCoexpressionAnalysis( withCoexpression.contains( vo.getId() ) );
            vo.setHasDifferentialExpressionAnalysis( withDiffEx.contains( vo.getId() ) );
        }
    }

    private List<Long> getExpressionExperimentIdsWithCoexpression( boolean cacheable ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select experimentAnalyzed.id from CoexpressionAnalysis" )
                .setCacheable( cacheable )
                .list();
    }

    private List<Long> getExpressionExperimentIdsWithDifferentialExpressionAnalysis( boolean cacheable ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select experimentAnalyzed.id from DifferentialExpressionAnalysis" )
                .setCacheable( cacheable )
                .list();
    }

    private void populateArrayDesignCount( Collection<ExpressionExperimentValueObject> eevos ) {
        if ( eevos.isEmpty() ) {
            return;
        }
        //noinspection unchecked
        List<Object[]> results = getSessionFactory().getCurrentSession()
                .createQuery( "select ee.id, count(distinct ba.arrayDesignUsed) from ExpressionExperiment ee left join ee.bioAssays as ba where ee.id in (:ids) group by ee" )
                .setParameterList( "ids", EntityUtils.getIds( eevos ) )
                .list();
        Map<Long, Long> adCountById = results.stream().collect( Collectors.toMap( row -> ( Long ) row[0], row -> ( Long ) row[1] ) );
        for ( ExpressionExperimentValueObject eevo : eevos ) {
            eevo.setArrayDesignCount( adCountById.get( eevo.getId() ) );
        }
    }
}
