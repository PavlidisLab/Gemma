package ubic.gemma.persistence.service.maintenance;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.mail.MailEngine;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.description.AnnotationRelationBasis;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.description.AnnotationRelationDao;
import ubic.gemma.persistence.service.common.description.AnnotationRelationDaoImpl;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The CURATED harvest: EE2C rows that already carry a predicate and an object become queryable
 * relations.
 *
 * <p>Driven from EE2C rows directly rather than from the EE2C rebuild, because the harvest's contract
 * is exactly "an EE2C row with a predicate in, a relation out" — running the rebuild first would test
 * that instead, and its correctness is not what is in question here.</p>
 *
 * <p>Nothing in this is an inference. The curator wrote the triple; all that was missing was an index
 * that can be read from the object end.</p>
 */
public class AnnotationRelationHarvestTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class AnnotationRelationHarvestTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer propertyPlaceholderConfigurer() throws IOException {
            Path gene2csInfoPath = Files.createTempDirectory( "DBReport" ).resolve( "gene2cs.info" );
            return new TestPropertyPlaceholderConfigurer( "gemma.gene2cs.path=" + gene2csInfoPath );
        }

        @Bean
        public ConversionService conversionService() {
            DefaultFormattingConversionService service = new DefaultFormattingConversionService();
            service.addConverter( String.class, Path.class, source -> Paths.get( ( String ) source ) );
            return service;
        }

        @Bean
        public TableMaintenanceUtil tableMaintenanceUtil() {
            return new TableMaintenanceUtilImpl();
        }

        @Bean
        public AnnotationRelationDao annotationRelationDao( SessionFactory sessionFactory ) {
            return new AnnotationRelationDaoImpl( sessionFactory );
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock( AuditEventService.class );
        }

        @Bean
        public MailEngine mailEngine() {
            return mock( MailEngine.class );
        }

        @Bean
        public ExternalDatabaseService externalDatabaseService() {
            return mock( ExternalDatabaseService.class );
        }
    }

    private static final String LVH = "http://www.ebi.ac.uk/efo/EFO_0003896";
    private static final String INDUCED_BY = "http://gemma.msl.ubc.ca/ont/TGEMO_00171";
    private static final String DISEASE_MODEL = "http://gemma.msl.ubc.ca/ont/TGEMO_00101";

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Autowired
    private AnnotationRelationDao annotationRelationDao;

    private Taxon taxon;

    @BeforeEach
    public void setUp() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication( new AnonymousAuthenticationToken( "test", "anonymousUser",
                Collections.singletonList( new SimpleGrantedAuthority( AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY ) ) ) );
        SecurityContextHolder.setContext( context );

        taxon = new Taxon();
        taxon.setCommonName( "human" );
        sessionFactory.getCurrentSession().persist( taxon );
    }

    /**
     * The shape a curator actually wrote, taken verbatim from production:
     * {@code disease model: left ventricular hypertrophy - induced by -> aortic banding}.
     *
     * <p>Note that the object has no URI. That is ordinary rather than a defect — a manipulation is
     * frequently free text — and it is why the value legs are indexed separately from the URI legs.</p>
     */
    @Test
    public void testACuratedStatementBecomesAQueryableRelation() {
        givenEe2cStatement( "left ventricular hypertrophy", LVH, "induced by", INDUCED_BY, "aortic banding", null );

        int written = tableMaintenanceUtil.updateAnnotationRelationEntries( null );

        assertThat( written ).isEqualTo( 1 );
        List<AnnotationRelationDao.RelationSummary> found = annotationRelationDao.findRelations(
                new AnnotationRelationDao.RelationQuery().subjectValueUris( Collections.singleton( LVH ) ) );
        assertThat( found ).singleElement().satisfies( r -> {
            assertThat( r.getSubjectValue() ).isEqualTo( "left ventricular hypertrophy" );
            assertThat( r.getPredicateUri() ).isEqualTo( INDUCED_BY );
            assertThat( r.getObjectValue() ).isEqualTo( "aortic banding" );
            assertThat( r.getObjectValueUri() ).isNull();
            assertThat( r.getBasis() ).isEqualTo( AnnotationRelationBasis.CURATED );
            assertThat( r.getSubjectCategoryUri() ).isEqualTo( DISEASE_MODEL );
            assertThat( r.getNumberOfExperiments() ).isEqualTo( 1 );
        } );
    }

    /**
     * The whole point: the relation can now be read from the object end.
     *
     * <p>"Which manipulations are asserted to induce left ventricular hypertrophy?" had no query
     * before this, because the only index on a statement is per-experiment.</p>
     */
    @Test
    public void testTheHarvestedRelationReadsFromTheObjectEnd() {
        givenEe2cStatement( "left ventricular hypertrophy", LVH, "induced by", INDUCED_BY, "aortic banding", null );

        tableMaintenanceUtil.updateAnnotationRelationEntries( null );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .objectValues( Collections.singleton( "aortic banding" ) ) ) )
                .singleElement()
                .satisfies( r -> assertThat( r.getSubjectValueUri() ).isEqualTo( LVH ) );
    }

    /**
     * An annotation with no predicate is not a relation and must not become one.
     */
    @Test
    public void testAPlainAnnotationIsNotHarvested() {
        givenEe2cStatement( "left ventricular hypertrophy", LVH, null, null, null, null );

        assertThat( tableMaintenanceUtil.updateAnnotationRelationEntries( null ) ).isZero();
    }

    /**
     * The second clause of a two-clause statement is as asserted as the first.
     *
     * <p>A dose or a duration rides in the second predicate/object pair, and so does the second half
     * of anything a curator expressed as two clauses. Harvesting only the first would lose a triple
     * silently.</p>
     */
    @Test
    public void testBothClausesOfATwoClauseStatementAreHarvested() {
        Statement s = newStatement( "asthma", "http://purl.obolibrary.org/obo/MONDO_0004979",
                "induced by", INDUCED_BY, "ovalbumin", null );
        s.setSecondPredicate( "delivered at dose" );
        s.setSecondPredicateUri( "http://gemma.msl.ubc.ca/ont/TGEMO_00166" );
        s.setSecondObject( "10 mg/kg" );
        persistEe2cRow( s );

        assertThat( tableMaintenanceUtil.updateAnnotationRelationEntries( null ) ).isEqualTo( 2 );
        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( "http://purl.obolibrary.org/obo/MONDO_0004979" ) )
                // unfiltered: `delivered at dose` is a per-experiment parameter and is correctly absent
                // from the default view, but the harvest still has to have stored it
                .termLevelOnly( false ) ) )
                .extracting( AnnotationRelationDao.RelationSummary::getObjectValue )
                .containsExactlyInAnyOrder( "ovalbumin", "10 mg/kg" );
    }

    /**
     * A rebuild replaces the basis rather than adding to it.
     *
     * <p>Running twice must not double the support, and a relation whose statement a curator has since
     * deleted must not survive. An upsert can only correct rows the new query still produces, which is
     * how EE2C ended up with 1,008 rows a full rebuild could not fix; delete-then-insert has no such
     * failure mode.</p>
     */
    @Test
    public void testRunningTheHarvestTwiceDoesNotDoubleTheEvidence() {
        givenEe2cStatement( "left ventricular hypertrophy", LVH, "induced by", INDUCED_BY, "aortic banding", null );

        tableMaintenanceUtil.updateAnnotationRelationEntries( null );
        tableMaintenanceUtil.updateAnnotationRelationEntries( null );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( LVH ) ) ) )
                .singleElement()
                .satisfies( r -> assertThat( r.getNumberOfExperiments() ).isEqualTo( 1 ) );
    }

    /**
     * The taxon rides across, because it decides what the relation says.
     */
    @Test
    public void testTheTaxonIsCarriedOntoTheRelation() {
        givenEe2cStatement( "left ventricular hypertrophy", LVH, "induced by", INDUCED_BY, "aortic banding", null );

        tableMaintenanceUtil.updateAnnotationRelationEntries( null );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( LVH ) ) ) )
                .singleElement()
                .satisfies( r -> assertThat( r.getTaxonCommonName() ).isEqualTo( "human" ) );
    }

    /**
     * 🛑 A statement whose object is a control-arm marker is not a relation between two concepts.
     *
     * <p>Found live: {@code OBI_0000220 reference subject role} appeared as the object of 10 curated
     * statements, and it is grounded, so a gate seeded with an experiment's term URIs would reach it
     * and conclude that every disease which ever had a control arm was implied by having one. The
     * recognition is {@code BaselineSelection}'s, the same list that picks a DEA baseline, so there is
     * one list and not two.</p>
     */
    @Test
    public void testAControlArmMarkerIsNotHarvestedAsARelation() {
        givenEe2cStatement( "Alzheimer disease", "http://purl.obolibrary.org/obo/MONDO_0004975",
                "has role", "http://purl.obolibrary.org/obo/RO_0000087",
                "reference subject role", "http://purl.obolibrary.org/obo/OBI_0000220" );

        assertThat( tableMaintenanceUtil.updateAnnotationRelationEntries( null ) ).isZero();
    }

    /**
     * An empty-string URI is not a URI, and stored as one it is unreachable.
     *
     * <p>Found live: rows arrived with {@code PREDICATE_URI = ''} rather than null, which passes an
     * {@code is not null} test and then matches nothing — a consumer filtering on the URI never finds
     * it, and a consumer checking for null does not either.</p>
     */
    @Test
    public void testAnEmptyStringUriIsStoredAsNull() {
        givenEe2cStatement( "asthma", "", "induced by", "", "ovalbumin", "" );

        assertThat( tableMaintenanceUtil.updateAnnotationRelationEntries( null ) ).isEqualTo( 1 );
        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValues( Collections.singleton( "asthma" ) )
                // unfiltered: the point is what was STORED. A row whose predicate has no URI cannot be
                // classified and so is not term-level, which is right, and not what this test is about.
                .termLevelOnly( false ) ) )
                .singleElement()
                .satisfies( r -> {
                    assertThat( r.getSubjectValueUri() ).isNull();
                    assertThat( r.getPredicateUri() ).isNull();
                    assertThat( r.getObjectValueUri() ).isNull();
                } );
    }

    /**
     * The evidence split has to actually split.
     *
     * <p>A statement lives on a factor value, but EE2C files design-level annotations under
     * {@code ExperimentalDesign} and never writes {@code FactorValue} as a level — so a count keyed on
     * {@code FactorValue} matched nothing and every row reported zero at every level. That failure is
     * invisible: it does not throw, it reports "attested nowhere in particular" for a relation with
     * real support behind it.</p>
     */
    @Test
    public void testTheEvidenceSplitCountsDesignLevelStatements() {
        Statement s = newStatement( "Alzheimer disease", "http://purl.obolibrary.org/obo/MONDO_0004975",
                "has_genotype", "http://purl.obolibrary.org/obo/GENO_0000222", "5xFAD", null );
        persistEe2cRow( s, ExperimentalDesign.class.getName() );

        tableMaintenanceUtil.updateAnnotationRelationEntries( null );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .objectValues( Collections.singleton( "5xFAD" ) ) ) )
                .singleElement()
                .satisfies( r -> {
                    assertThat( r.getNumberOfExperiments() ).isEqualTo( 1 );
                    assertThat( r.getNumberOfExperimentsAtFactorValue() )
                            .as( "a design-level statement has to land somewhere in the split" )
                            .isEqualTo( 1 );
                    assertThat( r.getNumberOfExperimentsAtTag() ).isZero();
                } );
    }

    private void givenEe2cStatement( String subject, String subjectUri, String predicate, String predicateUri,
            String object, String objectUri ) {
        persistEe2cRow( newStatement( subject, subjectUri, predicate, predicateUri, object, objectUri ) );
    }

    private Statement newStatement( String subject, String subjectUri, String predicate, String predicateUri,
            String object, String objectUri ) {
        Statement s = new Statement();
        s.setCategory( "disease model" );
        s.setCategoryUri( DISEASE_MODEL );
        s.setSubject( subject );
        s.setSubjectUri( subjectUri );
        s.setPredicate( predicate );
        s.setPredicateUri( predicateUri );
        s.setObject( object );
        s.setObjectUri( objectUri );
        return s;
    }

    /**
     * Write the EE2C row by hand rather than by running the EE2C rebuild.
     *
     * <p>The harvest's contract is "an EE2C row carrying a predicate in, a relation out". Rebuilding
     * EE2C first would put its correctness in the path of every assertion here and make a failure
     * ambiguous between the two.</p>
     */
    private void persistEe2cRow( Statement s ) {
        persistEe2cRow( s, ExpressionExperiment.class.getName() );
    }

    private void persistEe2cRow( Statement s, String level ) {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setTaxon( taxon );
        sessionFactory.getCurrentSession().persist( ee );
        sessionFactory.getCurrentSession().persist( s );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().createNativeQuery(
                        "insert into EXPRESSION_EXPERIMENT2CHARACTERISTIC (ID, CATEGORY, CATEGORY_URI, `VALUE`, VALUE_URI, "
                                + "PREDICATE, PREDICATE_URI, OBJECT, OBJECT_URI, SECOND_PREDICATE, SECOND_PREDICATE_URI, "
                                + "SECOND_OBJECT, SECOND_OBJECT_URI, EXPRESSION_EXPERIMENT_FK, "
                                + "ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK, `LEVEL`) "
                                + "values (:id, :category, :categoryUri, :value, :valueUri, :predicate, :predicateUri, "
                                + ":object, :objectUri, :secondPredicate, :secondPredicateUri, :secondObject, "
                                + ":secondObjectUri, :eeId, :mask, :level)" )
                .setParameter( "id", s.getId() )
                .setParameter( "category", s.getCategory() )
                .setParameter( "categoryUri", s.getCategoryUri() )
                .setParameter( "value", s.getSubject() )
                .setParameter( "valueUri", s.getSubjectUri() )
                .setParameter( "predicate", s.getPredicate() )
                .setParameter( "predicateUri", s.getPredicateUri() )
                .setParameter( "object", s.getObject() )
                .setParameter( "objectUri", s.getObjectUri() )
                .setParameter( "secondPredicate", s.getSecondPredicate() )
                .setParameter( "secondPredicateUri", s.getSecondPredicateUri() )
                .setParameter( "secondObject", s.getSecondObject() )
                .setParameter( "secondObjectUri", s.getSecondObjectUri() )
                .setParameter( "eeId", ee.getId() )
                .setParameter( "mask", BasePermission.READ.getMask() )
                .setParameter( "level", level )
                .executeUpdate();
        sessionFactory.getCurrentSession().flush();
    }
}
