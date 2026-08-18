package ubic.gemma.persistence.service.common.description;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.common.description.AnnotationRelation;
import ubic.gemma.model.common.description.AnnotationRelationBasis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads over {@code ANNOTATION_RELATION}.
 *
 * <p>The interesting behaviour is not "does the query return rows" - it is the three places this can
 * be quietly wrong: an ACL clause that eats the asserted rows it should never have touched, a ranking
 * that lets co-occurrence outrank a curator, and an expansion that resolves ambiguity it was supposed
 * to preserve. Each has a test.</p>
 */
@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class AnnotationRelationDaoTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class AnnotationRelationDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public AnnotationRelationDao annotationRelationDao( SessionFactory sessionFactory ) {
            return new AnnotationRelationDaoImpl( sessionFactory );
        }
    }

    private static final String LEIGH = "http://purl.obolibrary.org/obo/MONDO_0009723";
    private static final String COMPLEX_IV = "http://purl.obolibrary.org/obo/MONDO_0700250";
    private static final String SURF1 = "http://purl.org/commons/record/ncbi_gene/6834";
    private static final String HAS_GENOTYPE = "http://purl.obolibrary.org/obo/GENO_0000222";

    @Autowired
    private AnnotationRelationDao annotationRelationDao;

    private Taxon taxon;

    @BeforeEach
    public void setUp() {
        // Anonymous, which is the user class whose ACL branch is a bitmask test on the row rather than
        // a join to the ACL tables. It is the branch a public API is read through, and the one where a
        // mistake is invisible to a logged-in developer.
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication( new AnonymousAuthenticationToken( "test", "anonymousUser",
                Collections.singletonList( new SimpleGrantedAuthority( AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY ) ) ) );
        SecurityContextHolder.setContext( context );

        taxon = new Taxon();
        taxon.setNcbiId( 9606 );
        taxon.setCommonName( "human" );
        sessionFactory.getCurrentSession().persist( taxon );
    }

    /**
     * An ontology's claim is about a term, not about anything Gemma holds, so no dataset permission
     * can be relevant to it.
     *
     * <p>This is the failure the ACL composition exists to prevent: the clause is a bitmask test on a
     * column that asserted rows have no value for, so writing it the obvious way silently deletes
     * every ontology-asserted relation from every anonymous response - and looks like "the ontology
     * producer never ran".</p>
     */
    @Test
    public void testAssertedRelationIsNotFilteredByDatasetPermissions() {
        AnnotationRelation asserted = relation( COMPLEX_IV, SURF1, AnnotationRelationBasis.ONTOLOGY );
        asserted.setSource( "MONDO" );
        asserted.setSourceVersion( "2026-06-19" );
        annotationRelationDao.create( asserted );

        List<AnnotationRelationDao.RelationSummary> found = annotationRelationDao.findRelations(
                new AnnotationRelationDao.RelationQuery().objectValueUris( Collections.singleton( SURF1 ) ) );

        assertThat( found ).singleElement().satisfies( r -> {
            assertThat( r.getSubjectValueUri() ).isEqualTo( COMPLEX_IV );
            assertThat( r.getBasis() ).isEqualTo( AnnotationRelationBasis.ONTOLOGY );
            assertThat( r.getSource() ).isEqualTo( "MONDO" );
            // nothing to count, which is not the same as no evidence
            assertThat( r.getNumberOfExperiments() ).isZero();
        } );
    }

    /**
     * An attested row names a dataset, so a private one must not contribute a count or an example.
     */
    @Test
    public void testAttestedRelationOnAPrivateDatasetIsHidden() {
        annotationRelationDao.create( attested( LEIGH, SURF1, AnnotationRelationBasis.CURATED, 0 ) );

        assertThat( annotationRelationDao.findRelations(
                new AnnotationRelationDao.RelationQuery().objectValueUris( Collections.singleton( SURF1 ) ) ) )
                .isEmpty();
    }

    /**
     * The same rows read from either end. A relation is not a direction, and the browse selector asks
     * it one way round while the experiment page asks it the other.
     */
    @Test
    public void testTheRelationReadsFromEitherEnd() {
        annotationRelationDao.create( attested( LEIGH, SURF1, AnnotationRelationBasis.CURATED, readMask() ) );

        List<AnnotationRelationDao.RelationSummary> fromDisease = annotationRelationDao.findRelations(
                new AnnotationRelationDao.RelationQuery().subjectValueUris( Collections.singleton( LEIGH ) ) );
        List<AnnotationRelationDao.RelationSummary> fromGene = annotationRelationDao.findRelations(
                new AnnotationRelationDao.RelationQuery().objectValueUris( Collections.singleton( SURF1 ) ) );

        assertThat( fromDisease ).hasSize( 1 );
        assertThat( fromGene ).hasSize( 1 );
        assertThat( fromDisease.get( 0 ).getTripleKey() ).isEqualTo( fromGene.get( 0 ).getTripleKey() );
    }

    /**
     * A curator's statement outranks any amount of co-occurrence.
     *
     * <p>Ranked on support alone the co-occurrence wins here by five experiments to one, which is the
     * whole reason the basis dominates the score: the corpus signal exists because a redundant tag was
     * written beside a genotype that already implied it, and quantity of that is not quality.</p>
     */
    @Test
    public void testAssertionOutranksAttestation() {
        annotationRelationDao.create( attested( LEIGH, SURF1, AnnotationRelationBasis.CURATED, readMask() ) );
        for ( int i = 0; i < 5; i++ ) {
            annotationRelationDao.create( attested( COMPLEX_IV, SURF1, AnnotationRelationBasis.CORPUS, readMask() ) );
        }

        List<AnnotationRelationDao.RelationSummary> found = annotationRelationDao.findRelations(
                new AnnotationRelationDao.RelationQuery().objectValueUris( Collections.singleton( SURF1 ) ) );

        assertThat( found ).hasSize( 2 );
        assertThat( found.get( 0 ).getBasis() ).isEqualTo( AnnotationRelationBasis.CURATED );
        assertThat( found.get( 0 ).getNumberOfExperiments() ).isEqualTo( 1 );
        assertThat( found.get( 1 ).getBasis() ).isEqualTo( AnnotationRelationBasis.CORPUS );
        assertThat( found.get( 1 ).getNumberOfExperiments() ).isEqualTo( 5 );
        assertThat( found.get( 0 ).getBasis().isSelfSufficient() ).isTrue();
        assertThat( found.get( 1 ).getBasis().isSelfSufficient() ).isFalse();
    }

    /**
     * Two bases naming different terms are reported side by side, not merged and not treated as a
     * disagreement.
     *
     * <p>This is the SURF1 case exactly: MONDO's germline axiom points at
     * {@code MONDO:0700250 mitochondrial complex IV deficiency, nuclear type 1} where the curator
     * wrote {@code MONDO:0009723 Leigh syndrome}. They share no xref, sit in different branches, and
     * neither subsumes the other, because the ontology is modelling the molecular diagnosis and the
     * curator the clinical syndrome. Collapsing them on term identity would drop one of two correct
     * answers.</p>
     */
    @Test
    public void testTwoFramingsOfTheSameFactBothSurvive() {
        annotationRelationDao.create( attested( LEIGH, SURF1, AnnotationRelationBasis.CURATED, readMask() ) );
        annotationRelationDao.create( relation( COMPLEX_IV, SURF1, AnnotationRelationBasis.ONTOLOGY ) );

        List<AnnotationRelationDao.RelationSummary> found = annotationRelationDao.findRelations(
                new AnnotationRelationDao.RelationQuery().objectValueUris( Collections.singleton( SURF1 ) ) );

        assertThat( found ).hasSize( 2 )
                .extracting( AnnotationRelationDao.RelationSummary::getSubjectValueUri )
                .containsExactlyInAnyOrder( LEIGH, COMPLEX_IV );
    }

    /**
     * Expansion returns the whole candidate set and resolves nothing.
     *
     * <p>Ambiguity is fatal to generation and harmless to membership. A caller asking "is the disease
     * I am about to tag among those this genotype is associated with?" gets the right answer whichever
     * of the three is meant; a caller that had been handed one confident answer would ship a wrong tag
     * two times in three.</p>
     */
    @Test
    public void testExpansionKeepsAmbiguityInsteadOfResolvingIt() {
        annotationRelationDao.create( attested( LEIGH, SURF1, AnnotationRelationBasis.CURATED, readMask() ) );
        annotationRelationDao.create( relation( COMPLEX_IV, SURF1, AnnotationRelationBasis.ONTOLOGY ) );
        annotationRelationDao.create( relation( "http://purl.obolibrary.org/obo/MONDO_0033885", SURF1,
                AnnotationRelationBasis.ONTOLOGY ) );

        List<String[]> related = annotationRelationDao.findRelatedTerms(
                Collections.singleton( SURF1 ), Collections.emptySet(),
                AnnotationRelationDao.Direction.OBJECT_TO_SUBJECT,
                EnumSet.allOf( AnnotationRelationBasis.class ), Collections.emptySet(),
                null, Collections.emptySet(), 0, -1 );

        assertThat( related ).hasSize( 3 )
                .extracting( r -> r[1] )
                .containsExactlyInAnyOrder( LEIGH, COMPLEX_IV, "http://purl.obolibrary.org/obo/MONDO_0033885" );
    }

    /**
     * 🛑 An object that relates to many subjects identifies none of them.
     *
     * <p>Measured on the corpus: {@code Homozygous negative} relates to 2,898 distinct subjects,
     * {@code Overexpression} to 1,839, {@code 24 h} to 448, {@code induced pluripotent stem cell line
     * cell} to 81 — while {@code MPTP} and {@code 5xFAD} sit in the low single digits. A gate seeded
     * with a broad object implies every one of those subjects, which is how a suppression rule ends up
     * dropping a tag because the experiment mentioned a dose.</p>
     *
     * <p>Not a quality judgement and not a list of bad terms: a dose is a perfectly good curated
     * statement, and one measured number covers zygosity, perturbation direction, dose, duration and
     * generic ontology classes without anyone maintaining it.</p>
     */
    @Test
    public void testABroadObjectIsDroppedWhenACallerSetsABar() {
        // one narrow object, one shared by three subjects
        annotationRelationDao.create( attested( LEIGH, SURF1, AnnotationRelationBasis.CURATED, readMask() ) );
        for ( String subject : new String[] { LEIGH, COMPLEX_IV, "http://purl.obolibrary.org/obo/MONDO_0033885" } ) {
            AnnotationRelation broad = attested( subject, "http://purl.obolibrary.org/obo/GENO_0000135",
                    AnnotationRelationBasis.CURATED, readMask() );
            broad.setObjectValue( "Heterozygous" );
            annotationRelationDao.create( broad );
        }

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( LEIGH ) ) ) )
                .hasSize( 2 );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( LEIGH ) )
                .maximumObjectBreadth( 2 ) ) )
                .singleElement()
                .satisfies( r -> {
                    assertThat( r.getObjectValue() ).isEqualTo( "SURF1" );
                    assertThat( r.getObjectBreadth() ).isEqualTo( 1 );
                } );
    }

    /**
     * Breadth counts subjects, not rows.
     *
     * <p>🛑 What this test CANNOT cover: the production failure was a case variant, and H2 in
     * {@code MODE=MYSQL} is case-sensitive where production MySQL is not — so a test asserting that
     * behaviour here passes against code that is wrong in production. The fix is therefore in SQL
     * ({@code group by lower(trim(...))}), where both engines agree, rather than in a Java-side
     * comparison a test could bless. Seen live as breadth 0 on {@code familial Alzheimer's disease}
     * and {@code intermediate}, where 0 means "maximally specific" and so fails OPEN.</p>
     */
    @Test
    public void testBreadthCountsDistinctSubjects() {
        for ( String subject : new String[] { LEIGH, COMPLEX_IV, "http://purl.obolibrary.org/obo/MONDO_0033885" } ) {
            AnnotationRelation r = attested( subject, null, AnnotationRelationBasis.CURATED, readMask() );
            r.setObjectValue( "intermediate" );
            annotationRelationDao.create( r );
        }
        AnnotationRelation narrow = attested( LEIGH, SURF1, AnnotationRelationBasis.CURATED, readMask() );
        annotationRelationDao.create( narrow );

        List<AnnotationRelationDao.RelationSummary> found = annotationRelationDao.findRelations(
                new AnnotationRelationDao.RelationQuery().subjectValueUris( Collections.singleton( LEIGH ) ) );

        assertThat( found ).hasSize( 2 );
        assertThat( found ).filteredOn( r -> "intermediate".equals( r.getObjectValue() ) )
                .singleElement()
                .satisfies( r -> assertThat( r.getObjectBreadth() ).isEqualTo( 3 ) );
        assertThat( found ).filteredOn( r -> "SURF1".equals( r.getObjectValue() ) )
                .singleElement()
                .satisfies( r -> assertThat( r.getObjectBreadth() ).isEqualTo( 1 ) );
    }

    /**
     * Refuse to enumerate the table. Every caller knows one end of the relation, and a query that
     * names neither is a mistake rather than a request for everything.
     */
    @Test
    public void testAnUnseededQueryReturnsNothing() {
        annotationRelationDao.create( attested( LEIGH, SURF1, AnnotationRelationBasis.CURATED, readMask() ) );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery() ) ).isEmpty();
    }

    /**
     * Excluding a dataset removes its contribution, which is what makes "this tag is inferable, so it
     * can be dropped" a claim about the rest of the corpus rather than about the dataset itself.
     */
    @Test
    public void testExcludingADatasetRemovesItsOwnEvidence() {
        AnnotationRelation r = attested( LEIGH, SURF1, AnnotationRelationBasis.CURATED, readMask() );
        annotationRelationDao.create( r );
        Long eeId = r.getExpressionExperiment().getId();

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .objectValueUris( Collections.singleton( SURF1 ) )
                .excludedExperimentIds( Collections.singleton( eeId ) ) ) )
                .isEmpty();
    }

    /**
     * A rebuild clears the basis it is rebuilding, so a relation whose source annotation was deleted
     * does not outlive it.
     */
    @Test
    public void testRemoveByBasisClearsOnlyThatBasis() {
        annotationRelationDao.create( attested( LEIGH, SURF1, AnnotationRelationBasis.CURATED, readMask() ) );
        annotationRelationDao.create( relation( COMPLEX_IV, SURF1, AnnotationRelationBasis.ONTOLOGY ) );

        annotationRelationDao.removeByBasis( AnnotationRelationBasis.CURATED, null );

        assertThat( annotationRelationDao.findRelations(
                new AnnotationRelationDao.RelationQuery().objectValueUris( Collections.singleton( SURF1 ) ) ) )
                .singleElement()
                .satisfies( r -> assertThat( r.getBasis() ).isEqualTo( AnnotationRelationBasis.ONTOLOGY ) );
    }

    private static int readMask() {
        return BasePermission.READ.getMask();
    }

    private AnnotationRelation relation( String subjectUri, String objectUri, AnnotationRelationBasis basis ) {
        AnnotationRelation r = new AnnotationRelation();
        r.setSubjectValue( "subject " + subjectUri );
        r.setSubjectValueUri( subjectUri );
        r.setSubjectCategory( "disease model" );
        r.setPredicate( "has_genotype" );
        r.setPredicateUri( HAS_GENOTYPE );
        r.setObjectValue( "SURF1" );
        r.setObjectValueUri( objectUri );
        r.setTaxon( taxon );
        r.setBasis( basis );
        r.setGeneratedAt( new Date() );
        return r;
    }

    private AnnotationRelation attested( String subjectUri, String objectUri, AnnotationRelationBasis basis, int mask ) {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setTaxon( taxon );
        sessionFactory.getCurrentSession().persist( ee );
        AnnotationRelation r = relation( subjectUri, objectUri, basis );
        r.setExpressionExperiment( ee );
        r.setLevel( ExpressionExperiment.class.getName() );
        r.setAclIsAuthenticatedAnonymouslyMask( mask );
        return r;
    }
}
