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
     * 🛑 The rule, and the reason this class no longer tests a harvest: a curated statement does NOT become
     * a relation.
     *
     * <p>The harvest keyed each statement by the TERM, so what a curator wrote about one experiment's
     * material came back as a property of that term on every other experiment using it. Experiment 24976 --
     * mouse EAE astrocytes under fingolimod -- showed three such chips, none true of it, each traceable to a
     * single unrelated experiment. Filtering did not rescue it: 15,007 of ~18,000 curated triples on prod
     * were attested by one experiment, and the recurring ones were common curation patterns rather than
     * truths. Paul ruled the harvest out on 2026-08-28.</p>
     *
     * <p>This goes red the moment the insert is restored, which is the point.</p>
     */
    @Test
    public void testACuratedStatementIsNotHarvestedIntoARelation() {
        givenEe2cStatement( "left ventricular hypertrophy", LVH, "induced by", INDUCED_BY, "aortic banding", null );

        int written = tableMaintenanceUtil.updateAnnotationRelationEntries( null );

        assertThat( written ).isZero();
        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( LVH ) ) ) ).isEmpty();
    }

    /**
     * A run still REMOVES the curated rows already in the table -- that is how the ones harvested before the
     * rule changed leave -- and the ONTOLOGY row beside it is the control: the delete is scoped to CURATED
     * and is not a table wipe. Without that second row a delete of everything would pass just as well.
     */
    @Test
    public void testARunRemovesCuratedRowsAndLeavesEveryOtherBasis() {
        givenRelationRow( "astrocyte", "organoid", "CURATED" );
        givenRelationRow( "22Rv1", "prostate carcinoma", "ONTOLOGY" );

        tableMaintenanceUtil.updateAnnotationRelationEntries( null );

        assertThat( countRelationRows( "CURATED" ) ).isZero();
        assertThat( countRelationRows( "ONTOLOGY" ) ).isEqualTo( 1 );
    }

    private void givenRelationRow( String subject, String object, String basis ) {
        sessionFactory.getCurrentSession().createNativeQuery(
                        "insert into ANNOTATION_RELATION (SUBJECT_VALUE, OBJECT_VALUE, BASIS, STATUS,"
                                + " ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK, GENERATED_AT)"
                                + " values (:s, :o, :b, 'ASSERTED', 1, now())" )
                .setParameter( "s", subject )
                .setParameter( "o", object )
                .setParameter( "b", basis )
                .executeUpdate();
    }

    private long countRelationRows( String basis ) {
        return ( ( Number ) sessionFactory.getCurrentSession()
                .createNativeQuery( "select count(*) from ANNOTATION_RELATION where BASIS = :b" )
                .setParameter( "b", basis )
                .uniqueResult() ).longValue();
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
