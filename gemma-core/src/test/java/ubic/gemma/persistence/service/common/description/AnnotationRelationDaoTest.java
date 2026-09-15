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
import ubic.gemma.model.common.description.AnnotationRelationStatus;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.RelationInferenceDirection;
import ubic.gemma.model.common.description.RelationTopicality;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import javax.annotation.Nullable;

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
    private static final String INDUCED_BY = "http://gemma.msl.ubc.ca/ont/TGEMO_00171";
    private static final String DISEASE_MODEL = "http://gemma.msl.ubc.ca/ont/TGEMO_00101";
    private static final String CELL_TYPE = "http://www.ebi.ac.uk/efo/EFO_0000324";
    private static final String MOTOR_NEURON = "http://purl.obolibrary.org/obo/CL_0011001";
    private static final String BRAIN = "http://purl.obolibrary.org/obo/UBERON_0000955";
    private static final String IPSC_LINE = "http://purl.obolibrary.org/obo/CLO_0037279";
    private static final String OXIDOPAMINE = "http://purl.obolibrary.org/obo/CHEBI_78741";
    private static final String HAS_ROLE = "http://purl.obolibrary.org/obo/RO_0000087";
    private static final String HAS_DISEASE = "http://purl.obolibrary.org/obo/RO_0016002";
    private static final String DERIVES_FROM_ANATOMIC_PART = "http://purl.obolibrary.org/obo/CLO_0037208";
    private static final String PROSTATE_GLAND = "http://purl.obolibrary.org/obo/UBERON_0002367";
    private static final String PROSTATE_LINE = "http://purl.obolibrary.org/obo/CLO_0002181";

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
     * 🛑 Within a basis, support has to order — the score used to be a constant.
     *
     * <p>{@code getScore()} returned {@code basisRank * 1000 + 1} for every self-sufficient basis, so
     * every {@code CURATED} row scored identically and the sort became a no-op: results fell through
     * to the alphabetical tiebreakers and a 2-dataset relation could be served above a 10-dataset one.
     * uib saw the strongest Alzheimer row arrive tenth.</p>
     */
    @Test
    public void testSupportOrdersWithinABasis() {
        AnnotationRelation weak = attested( LEIGH, SURF1, AnnotationRelationBasis.CURATED, readMask() );
        weak.setObjectValue( "zzz weak" );
        annotationRelationDao.create( weak );
        // three experiments attest the second one, and its label sorts last alphabetically
        for ( int i = 0; i < 3; i++ ) {
            AnnotationRelation strong = attested( LEIGH, SURF1, AnnotationRelationBasis.CURATED, readMask() );
            strong.setObjectValue( "aaa strong" );
            strong.setObjectValueUri( "http://example.com/strong" );
            annotationRelationDao.create( strong );
        }

        List<AnnotationRelationDao.RelationSummary> found = annotationRelationDao.findRelations(
                new AnnotationRelationDao.RelationQuery().subjectValueUris( Collections.singleton( LEIGH ) ) );

        assertThat( found ).hasSize( 2 );
        assertThat( found.get( 0 ).getNumberOfExperiments() )
                .as( "the better-attested relation has to come first, not the alphabetically earlier one" )
                .isEqualTo( 3 );
    }

    /**
     * 🛑 A vocabulary label spelled two ways is one relation, not two.
     *
     * <p>{@code Disease model} and {@code disease model} share {@code TGEMO_00101}; {@code toward} and
     * {@code towards} share {@code RO_0002503}. Grouping on the spelling splits one relation across two
     * rows and <b>fragments its support</b>, so every ranking built on per-row support ranks fragments
     * and whichever row a client renders understates the evidence.</p>
     *
     * <p>Normalized in SQL rather than by collation, so H2 and MySQL agree and this is actually
     * testable — unlike {@code objectBreadth}'s case bug, which is not.</p>
     */
    @Test
    public void testALabelSpelledTwoWaysIsOneRelation() {
        for ( String spelling : new String[] { "Disease model", "disease model", "disease model" } ) {
            AnnotationRelation r = attested( LEIGH, SURF1, AnnotationRelationBasis.CURATED, readMask() );
            r.setSubjectCategory( spelling );
            r.setSubjectCategoryUri( "http://gemma.msl.ubc.ca/ont/TGEMO_00101" );
            annotationRelationDao.create( r );
        }

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( LEIGH ) ) ) )
                .as( "one relation, its support whole" )
                .singleElement()
                .satisfies( r -> assertThat( r.getNumberOfExperiments() ).isEqualTo( 3 ) );
    }

    /**
     * 🛑 A relation is readable from both ends and inferable from only one.
     *
     * <p>Gemma stores {@code Alzheimer disease --has_genotype--> APP/PS1}. APP/PS1 implies an
     * Alzheimer disease model; Alzheimer implies nothing about APP/PS1, because not every Alzheimer
     * model is APP/PS1. A gate following both directions would suppress a correct
     * {@code genotype: APP/PS1} tag on the strength of the dataset also carrying
     * {@code disease: Alzheimer} — deleting curation for an inference nobody made.</p>
     *
     * <p>And the licensed direction flips per predicate, because the curation does not put the
     * specific end on the same side each time: a cell line's {@code derives from patient having
     * disease} runs subject-to-object, while {@code has_genotype} runs object-to-subject.</p>
     */
    /**
     * A cell line or strain's genetic background is a fact about that line, not about the experiment
     * that used it, and the implication runs one way: a knockout line implies C57BL/6, while C57BL/6
     * implies nothing about which of the thousands of lines on it is in hand.
     * <p>
     * Minted as TGEMO_00216 on 2026-08-29 so that a background the submitter reported as a constant
     * characteristic stops being recorded as the experimental strain itself.
     */
    @Test
    public void testABackgroundIsAFactAboutTheLineAndImpliesOnlyDownwards() {
        String hasBackground = "http://gemma.msl.ubc.ca/ont/TGEMO_00216";
        String cellLine = "http://purl.obolibrary.org/obo/CLO_0000031";

        assertThat( RelationTopicality.of( hasBackground, cellLine ) )
                .as( "the background belongs to the line, whatever the experiment did with it" )
                .isEqualTo( RelationTopicality.TERM_LEVEL );

        assertThat( RelationInferenceDirection.of( hasBackground, cellLine ) )
                .isEqualTo( RelationInferenceDirection.SUBJECT_IMPLIES_OBJECT );
        assertThat( RelationInferenceDirection.of( hasBackground, cellLine ).licenses( true ) )
                .as( "the line implies its background" ).isTrue();
        assertThat( RelationInferenceDirection.of( hasBackground, cellLine ).licenses( false ) )
                .as( "C57BL/6 must NOT imply any particular line" ).isFalse();
    }

    @Test
    public void testTheImplicationRunsOnlyOneWayAndTheWayDependsOnThePredicate() {
        String diseaseModel = "http://gemma.msl.ubc.ca/ont/TGEMO_00101";
        assertThat( RelationInferenceDirection.of( HAS_GENOTYPE, diseaseModel ) )
                .isEqualTo( RelationInferenceDirection.OBJECT_IMPLIES_SUBJECT );
        assertThat( RelationInferenceDirection.of( HAS_GENOTYPE, diseaseModel ).licenses( false ) )
                .as( "APP/PS1, the object, implies the disease" ).isTrue();
        assertThat( RelationInferenceDirection.of( HAS_GENOTYPE, diseaseModel ).licenses( true ) )
                .as( "the disease must NOT imply the genotype" ).isFalse();

        String derivesFromPatient = "http://purl.obolibrary.org/obo/CLO_0000015";
        assertThat( RelationInferenceDirection.of( derivesFromPatient, diseaseModel ) )
                .isEqualTo( RelationInferenceDirection.SUBJECT_IMPLIES_OBJECT );
        assertThat( RelationInferenceDirection.of( derivesFromPatient, diseaseModel ).licenses( true ) )
                .as( "MCF7, the subject, implies its disease" ).isTrue();
        assertThat( RelationInferenceDirection.of( derivesFromPatient, diseaseModel ).licenses( false ) )
                .as( "the disease must NOT imply the cell line" ).isFalse();

        // an unclassified predicate licenses nothing: a suppression must never rest on a relation
        // nobody has reasoned about
        assertThat( RelationInferenceDirection.of( "http://purl.obolibrary.org/obo/CLO_0037375", diseaseModel ) )
                .isEqualTo( RelationInferenceDirection.NEITHER );
        assertThat( RelationInferenceDirection.of( null, diseaseModel ) )
                .isEqualTo( RelationInferenceDirection.NEITHER );

        // 🛑 and the predicate alone does not decide. has_genotype on a sample descriptor is a
        // statement about one experiment's samples, so it licenses nothing -- the same predicate that
        // licenses an inference two lines above.
        assertThat( RelationInferenceDirection.of( HAS_GENOTYPE, "http://purl.obolibrary.org/obo/PATO_0000047" ) )
                .as( "has_genotype on `female` implies nothing" )
                .isEqualTo( RelationInferenceDirection.NEITHER );
        assertThat( RelationInferenceDirection.of( HAS_GENOTYPE, null ) )
                .as( "an unknown subject category is not a licence" )
                .isEqualTo( RelationInferenceDirection.NEITHER );
    }

    /**
     * 🛑 {@code RO_0001000 derives from} licenses nothing until some source has said what its object
     * IS.
     *
     * <p>It is the one predicate that carries both directions under a single subject category, so no
     * subject-side rule can classify it. Both of these are curated and both are filed under
     * {@code disease}:</p>
     *
     * <pre>
     * refractory anemia with excess blasts -- derives from --> myelodysplastic syndrome   subject is specific
     * influenza                            -- derives from --> H3N2                       object is specific
     * </pre>
     *
     * <p>Over the 666 curated rows measured 2026-08-18, topicality admits 250 and they are the wrong
     * ones — {@code Cachexia -> melanoma}, {@code infectious disease -> Borrellia burgdorferi},
     * {@code partial duplication of chromosome 7 -> maternal duplication} beside the same subject's
     * {@code -> paternal duplication}. An object category is set only by a producer and never by the
     * curated harvest, so requiring one admits CLO's flat rows and refuses every curated row at once.</p>
     */
    @Test
    public void testDerivesFromNeedsSomebodyToHaveSaidWhatTheObjectIs() {
        String derivesFrom = "http://purl.obolibrary.org/obo/RO_0001000";
        String cellLine = "http://purl.obolibrary.org/obo/CLO_0000031";
        String organismPart = "http://www.ebi.ac.uk/efo/EFO_0000635";
        String disease = "http://www.ebi.ac.uk/efo/EFO_0000408";

        // CLO says MCF7 derives from breast AND says breast is an organism part
        assertThat( RelationInferenceDirection.of( derivesFrom, cellLine, null, organismPart ) )
                .as( "a typed object is what makes this readable" )
                .isEqualTo( RelationInferenceDirection.SUBJECT_IMPLIES_OBJECT );

        // the same predicate on a curated row, which has one category and it belongs to the subject
        assertThat( RelationInferenceDirection.of( derivesFrom, cellLine, null, null ) )
                .as( "an untyped object licenses nothing, whatever the subject is" )
                .isEqualTo( RelationInferenceDirection.NEITHER );
        assertThat( RelationInferenceDirection.of( derivesFrom, disease, null, null ) )
                .as( "influenza must not be allowed to imply H3N2" )
                .isEqualTo( RelationInferenceDirection.NEITHER );

        // and the three-argument form cannot answer it, so it fails closed rather than guessing
        assertThat( RelationInferenceDirection.of( derivesFrom, cellLine, null ) )
                .isEqualTo( RelationInferenceDirection.NEITHER );

        // 🛑 the rule is scoped to this predicate: a typed object is NOT a general precondition, and
        // every other subject-side predicate still licenses without one. CLO_0000015 says what its
        // object is by saying its own name.
        assertThat( RelationInferenceDirection.of( "http://purl.obolibrary.org/obo/CLO_0000015",
                cellLine, null, null ) )
                .isEqualTo( RelationInferenceDirection.SUBJECT_IMPLIES_OBJECT );
    }

    /**
     * The derived claim is its own triple, and taxon picks its verb.
     *
     * <p>The store holds {@code Alzheimer disease --has_genotype--> APP/PS1}; what follows is
     * "APP/PS1 is a disease model of Alzheimer's" — different ends AND a different verb. Handing a
     * client only the stored row makes it invert and choose, and three clients will choose three
     * ways.</p>
     *
     * <p>A mouse carrying the genotype <i>models</i> the disease; a human line carrying a variant
     * <i>has</i> it. Unknown taxon takes the weaker claim rather than a guess.</p>
     */
    @Test
    public void testTheDerivedClaimIsItsOwnTripleWithATaxonChosenVerb() {
        AnnotationRelation mouse = attested( LEIGH, SURF1, AnnotationRelationBasis.CURATED, readMask() );
        Taxon mus = new Taxon();
        mus.setNcbiId( 10090 );
        mus.setCommonName( "mouse" );
        sessionFactory.getCurrentSession().persist( mus );
        mouse.setTaxon( mus );
        annotationRelationDao.create( mouse );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( LEIGH ) ) ) )
                .singleElement()
                .satisfies( r -> {
                    // stored one way round...
                    assertThat( r.getSubjectValueUri() ).isEqualTo( LEIGH );
                    assertThat( r.getObjectValueUri() ).isEqualTo( SURF1 );
                    // ...and the claim runs the other, with the genotype as its subject
                    assertThat( r.getImpliedSubjectUri() ).isEqualTo( SURF1 );
                    assertThat( r.getImpliedObjectUri() ).isEqualTo( LEIGH );
                    assertThat( r.getImpliedPredicate() ).isEqualTo( "has role in modeling" );
                } );

        // the human case is not a model of anything -- it has the disease
        AnnotationRelation human = attested( COMPLEX_IV, SURF1, AnnotationRelationBasis.CURATED, readMask() );
        human.setTaxon( taxon ); // the fixture taxon is human, NCBI 9606
        annotationRelationDao.create( human );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( COMPLEX_IV ) ) ) )
                .singleElement()
                .satisfies( r -> assertThat( r.getImpliedPredicate() ).isEqualTo( "has disease" ) );
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

    /**
     * The complement of {@code subjectCategoryUris}: everything except one kind of implying term.
     * <p>
     * Naming the categories to KEEP cannot express this — it would mean enumerating every category the
     * store holds and revisiting that list whenever one is added — which is why the exclusion exists
     * rather than the caller inverting the include.
     * <p>
     * 🛑 The uncategorised row is the one that matters. {@code NOT IN} against a NULL column evaluates to
     * NULL, never true, so the obvious clause drops every subject with no category alongside the excluded
     * one. An unknown category is not the excluded category.
     * <p>
     * {@code termLevelOnly(false)} because topicality is decided downstream of this clause and would
     * remove the uncategorised row for a different reason, which is the one thing that would make a green
     * result here meaningless.
     */
    @Test
    public void testExcludedSubjectCategoryDropsOnlyThatCategory() {
        seedThreeSubjectsOfOneObject();

        List<AnnotationRelationDao.RelationSummary> found = annotationRelationDao.findRelations(
                new AnnotationRelationDao.RelationQuery()
                        .objectValueUris( Collections.singleton( BRAIN ) )
                        .termLevelOnly( false )
                        .excludedSubjectCategoryUris( Collections.singleton( CELL_TYPE ) ) );

        assertThat( found ).extracting( AnnotationRelationDao.RelationSummary::getSubjectValueUri )
                .as( "the cell-type subject is gone, and the uncategorised one is NOT" )
                .containsExactlyInAnyOrder( IPSC_LINE, OXIDOPAMINE );
    }

    /** With no exclusion the same three rows all come back, so the filter is what removed the row above. */
    @Test
    public void testNoExclusionKeepsEveryCategory() {
        seedThreeSubjectsOfOneObject();

        List<AnnotationRelationDao.RelationSummary> found = annotationRelationDao.findRelations(
                new AnnotationRelationDao.RelationQuery()
                        .objectValueUris( Collections.singleton( BRAIN ) )
                        .termLevelOnly( false ) );

        assertThat( found ).extracting( AnnotationRelationDao.RelationSummary::getSubjectValueUri )
                .containsExactlyInAnyOrder( MOTOR_NEURON, IPSC_LINE, OXIDOPAMINE );
    }

    /** Three subjects of one object, differing only in subject category: cell type, cell line, none. */
    private void seedThreeSubjectsOfOneObject() {
        annotationRelationDao.create( categorisedOntologyRow( MOTOR_NEURON, "motor neuron",
                BRAIN, "brain", "cell type", CELL_TYPE ) );
        annotationRelationDao.create( categorisedOntologyRow( IPSC_LINE, "an iPSC line",
                BRAIN, "brain", "cell line", "http://purl.obolibrary.org/obo/CLO_0000031" ) );
        annotationRelationDao.create( categorisedOntologyRow( OXIDOPAMINE, "oxidopamine",
                BRAIN, "brain", null, null ) );
    }

    /** An ONTOLOGY row with the subject category spelled explicitly, including "no category at all". */
    private AnnotationRelation categorisedOntologyRow( String subjectUri, String subjectValue, String objectUri,
            String objectValue, @Nullable String category, @Nullable String categoryUri ) {
        AnnotationRelation r = new AnnotationRelation();
        r.setSubjectValue( subjectValue );
        r.setSubjectValueUri( subjectUri );
        r.setSubjectCategory( category );
        r.setSubjectCategoryUri( categoryUri );
        r.setPredicate( "part of" );
        r.setPredicateUri( "http://purl.obolibrary.org/obo/BFO_0000050" );
        r.setObjectValue( objectValue );
        r.setObjectValueUri( objectUri );
        r.setBasis( AnnotationRelationBasis.ONTOLOGY );
        r.setSource( "CL" );
        r.setGeneratedAt( new Date() );
        return r;
    }

    private AnnotationRelation relation( String subjectUri, String objectUri, AnnotationRelationBasis basis ) {
        AnnotationRelation r = new AnnotationRelation();
        r.setSubjectValue( "subject " + subjectUri );
        r.setSubjectValueUri( subjectUri );
        r.setSubjectCategory( "disease model" );
        // has_genotype is knowledge only when the subject IS one of the term-level categories --
        // without the URI these fixtures classify as per-experiment bookkeeping and are filtered out
        r.setSubjectCategoryUri( "http://gemma.msl.ubc.ca/ont/TGEMO_00101" );
        r.setPredicate( "has_genotype" );
        r.setPredicateUri( HAS_GENOTYPE );
        r.setObjectValue( "SURF1" );
        r.setObjectValueUri( objectUri );
        r.setTaxon( taxon );
        r.setBasis( basis );
        r.setGeneratedAt( new Date() );
        return r;
    }


    /**
     * 🛑 Two different stored relations converging on one claim must key the same.
     *
     * <p>uib on the {@code BRCA1} card, 2026-08-18: {@code BRCA1 --has disease--> breast cancer} and
     * {@code breast cancer --has_genotype--> BRCA1} render the identical implied triple, word for word,
     * and carry two different {@code tripleKey}s — so a consumer deduplicating on the stored row shows
     * the claim twice. {@code tripleKey} groups the rows one relation produces across bases; this is
     * the other shape.</p>
     */
    @Test
    public void testTwoRelationsDerivingOneClaimShareAnImpliedTripleKey() {
        String brca1 = "http://purl.org/commons/record/ncbi_gene/672";
        String breastCancer = "http://purl.obolibrary.org/obo/MONDO_0007254";
        String hasDisease = "http://purl.obolibrary.org/obo/RO_0016002";

        // stored one way: the gene has the disease -- subject is the specific end
        AnnotationRelation forward = attested( brca1, breastCancer, AnnotationRelationBasis.CURATED, readMask() );
        forward.setSubjectCategoryUri( DISEASE_MODEL );
        forward.setPredicate( "has disease" );
        forward.setPredicateUri( hasDisease );
        annotationRelationDao.create( forward );

        // and the other way: the disease has the genotype -- object is the specific end
        annotationRelationDao.create( attested( breastCancer, brca1, AnnotationRelationBasis.CURATED, readMask() ) );

        List<AnnotationRelationDao.RelationSummary> rows = new ArrayList<>();
        rows.addAll( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( brca1 ) ) ) );
        rows.addAll( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( breastCancer ) ) ) );

        assertThat( rows ).hasSize( 2 );
        assertThat( rows ).extracting( AnnotationRelationDao.RelationSummary::getTripleKey )
                .as( "as stored they are two different relations" )
                .doesNotHaveDuplicates();
        assertThat( rows ).extracting( AnnotationRelationDao.RelationSummary::getImpliedTripleKey )
                .as( "as claimed they are one, and a card must be able to fold them" )
                .allMatch( k -> k != null )
                .containsOnly( rows.get( 0 ).getImpliedTripleKey() );
    }

    /**
     * 🛑 The breadth cut runs BEFORE the row cap, or a threshold thins the answer instead of improving
     * it.
     *
     * <p>The cap used to be applied in SQL — with no {@code ORDER BY}, so an arbitrary N — and the
     * breadth bar in Java afterwards. A caller asking for "the best N that clear the bar" got "an
     * arbitrary N, minus the failures", which for a gate means missing a term that qualifies because
     * something unqualified was fetched ahead of it. oganm found the identical shape in #1685's
     * disease-model endpoint, where the specificity cut ran after the row cap.</p>
     *
     * <p>The fixture makes the two orders disagree: three broad objects and one specific one, with a
     * cap of two. Cut-then-cap returns the specific one; cap-then-cut can lose it entirely.</p>
     */
    @Test
    public void testTheBreadthCutRunsBeforeTheRowCap() {
        String seed = "http://purl.obolibrary.org/obo/CLO_9100001";
        String specific = "http://purl.obolibrary.org/obo/CHEBI_9100009";
        // three broad objects: each is borne by three other subjects, so breadth 4
        for ( int i = 0; i < 3; i++ ) {
            String broad = "http://purl.obolibrary.org/obo/CHEBI_910000" + i;
            annotationRelationDao.create( ontologyRow( seed, "seed", broad, "broad role " + i ) );
            for ( int j = 0; j < 3; j++ ) {
                // distinct VALUES, not just distinct URIs: breadth counts distinct SUBJECT_VALUE
                annotationRelationDao.create( ontologyRow( seed + "_" + i + j, "other " + i + j, broad, "broad role " + i ) );
            }
        }
        // and one the seed alone bears
        annotationRelationDao.create( ontologyRow( seed, "seed", specific, "the specific role" ) );

        List<String[]> kept = annotationRelationDao.findRelatedTerms(
                Collections.singleton( seed ), Collections.emptySet(),
                AnnotationRelationDao.Direction.SUBJECT_TO_OBJECT,
                EnumSet.of( AnnotationRelationBasis.ONTOLOGY ), Collections.emptySet(),
                null, Collections.emptySet(), 2, 2 );

        assertThat( kept )
                .as( "only the specific object clears a breadth bar of 2, whatever the cap fetched" )
                .extracting( t -> t[1] )
                .containsExactly( specific );
    }

    /**
     * 🛑 A refutation is not weak support — it is the opposite — so it stays out of ordinary reads and
     * out of every inference.
     *
     * <p>MGI publishes 1,211 of these ({@code MGI_Geno_NotDiseaseDO.rpt}): curated, cited rows saying a
     * genotype does NOT model a disease. Worth holding, and dangerous to hold carelessly — read by
     * anything unaware of the column it states the reverse of its source. Hence excluded by default,
     * reachable only by asking, and never able to license a claim.</p>
     */
    @Test
    public void testARefutedRelationIsExcludedUnlessAskedForAndNeverInfers() {
        String subject = "http://purl.obolibrary.org/obo/CLO_9200001";
        String denied = "http://purl.obolibrary.org/obo/CHEBI_9200002";
        AnnotationRelation r = ontologyRow( subject, "the genotype", denied, "the denied role" );
        r.setStatus( AnnotationRelationStatus.REFUTED );
        annotationRelationDao.create( r );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( subject ) ) ) )
                .as( "absent and denied are different answers; the default read gives neither" )
                .isEmpty();

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( subject ) )
                .includeRefuted( true ) ) )
                .singleElement()
                .satisfies( summary -> {
                    assertThat( summary.getStatus() ).isEqualTo( AnnotationRelationStatus.REFUTED );
                    assertThat( summary.getObjectValueUri() ).isEqualTo( denied );
                } );

        // 🛑 and the gate has no opt-in at all: there is no caller for whom "infer the thing the
        // source denied" is the right answer
        assertThat( annotationRelationDao.findRelatedTerms(
                Collections.singleton( subject ), Collections.emptySet(),
                AnnotationRelationDao.Direction.SUBJECT_TO_OBJECT,
                EnumSet.of( AnnotationRelationBasis.ONTOLOGY ), Collections.emptySet(),
                null, Collections.emptySet(), 0, 50 ) )
                .as( "a refutation must never license an inference" )
                .isEmpty();
    }

    /**
     * An assertion and a refutation of the same triple are two things a source said, so the grain must
     * keep them apart. Collapsed into one row, whichever won would hide the other.
     */
    @Test
    public void testAnAssertionAndARefutationOfOneTripleDoNotCollapse() {
        String subject = "http://purl.obolibrary.org/obo/CLO_9200003";
        String object = "http://purl.obolibrary.org/obo/CHEBI_9200004";
        annotationRelationDao.create( ontologyRow( subject, "subj", object, "obj" ) );
        AnnotationRelation refuted = ontologyRow( subject, "subj", object, "obj" );
        refuted.setStatus( AnnotationRelationStatus.REFUTED );
        annotationRelationDao.create( refuted );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( subject ) )
                .includeRefuted( true ) ) )
                .extracting( AnnotationRelationDao.RelationSummary::getStatus )
                .containsExactlyInAnyOrder( AnnotationRelationStatus.ASSERTED, AnnotationRelationStatus.REFUTED );
    }

    /**
     * 🛑 An asserted basis carries no support, so every row of it scores the same and the sort used to
     * fall through to alphabetical.
     *
     * <p>uib measured it on {@code imatinib} once CHEBI's roles came back whole: ten roles, all support
     * 0, ordered a–z, so {@code antihypertensive agent} (borne by 487 chemicals) led the card and
     * {@code tyrosine kinase inhibitor} (44) — the only role that identifies the compound — sat tenth
     * behind a "+5 more". Breadth ascending puts the specific end first, which is the advice we gave
     * for reading roles at all, applied at the one place a client cannot apply it: inside a
     * {@code ?limit=}.</p>
     *
     * <p>The fixture is built so the two orderings disagree — the specific object sorts LAST
     * alphabetically — or it would pass without the comparator.</p>
     */
    @Test
    public void testTiedAssertedRowsAreOrderedSpecificFirst() {
        String cell = "http://purl.obolibrary.org/obo/CLO_9000001";
        String generic = "http://purl.obolibrary.org/obo/CHEBI_9000001";
        String specific = "http://purl.obolibrary.org/obo/CHEBI_9000002";

        // "aaa" vs "zzz": alphabetically the generic one wins, so only breadth can flip them
        annotationRelationDao.create( ontologyRow( cell, "the cell", generic, "aaa generic role" ) );
        annotationRelationDao.create( ontologyRow( cell, "the cell", specific, "zzz specific role" ) );
        // two more subjects bear the generic role, so its breadth is 3 against the specific one's 1
        annotationRelationDao.create( ontologyRow( cell + "_b", "other b", generic, "aaa generic role" ) );
        annotationRelationDao.create( ontologyRow( cell + "_c", "other c", generic, "aaa generic role" ) );

        List<AnnotationRelationDao.RelationSummary> rows = annotationRelationDao.findRelations(
                new AnnotationRelationDao.RelationQuery().subjectValueUris( Collections.singleton( cell ) ) );

        assertThat( rows ).hasSize( 2 );
        assertThat( rows.get( 0 ).getObjectValue() )
                .as( "the role that identifies the subject leads, though it sorts last by name" )
                .isEqualTo( "zzz specific role" );
        assertThat( rows.get( 0 ).getObjectBreadth() ).isEqualTo( 1L );
        assertThat( rows.get( 1 ).getObjectBreadth() ).isEqualTo( 3L );
    }

    /**
     * 🛑 The curated CATEGORY is the unreliable half of the row; the subject's VOCABULARY is the
     * reliable half.
     *
     * <p>uib measured the same fact twice in one corpus, 2026-08-18:
     * {@code seizures MP_0002064 --induced by--> kainic acid} filed once as {@code Disease model} and
     * once as {@code treatment}. Licensing on the category alone made one a disease model and the
     * other nothing — one fact wearing two spellings of its metadata, which is the same class of
     * defect as label-spelling fragmentation.</p>
     *
     * <p>{@code media EFO_0000579} is the control that stops this becoming "license everything filed
     * as treatment": a growth condition really is a treatment condition, and LPS added to media
     * models nothing. EFO is deliberately absent from the namespace list for exactly that reason —
     * EFO diseases come in by their category, which is correct on those rows.</p>
     */
    @Test
    public void testASubjectsVocabularyOutranksAMiscategorizedRow() {
        String seizuresMp = "http://purl.obolibrary.org/obo/MP_0002064";
        String treatment = "http://www.ebi.ac.uk/efo/EFO_0000727";

        assertThat( RelationInferenceDirection.of( INDUCED_BY, treatment, seizuresMp ) )
                .as( "a phenotype subject is the disease-model shape however it was filed" )
                .isEqualTo( RelationInferenceDirection.OBJECT_IMPLIES_SUBJECT );
        assertThat( RelationInferenceDirection.of( INDUCED_BY, DISEASE_MODEL, seizuresMp ) )
                .as( "and the correctly-filed twin of the same fact agrees" )
                .isEqualTo( RelationInferenceDirection.OBJECT_IMPLIES_SUBJECT );

        assertThat( RelationInferenceDirection.of( INDUCED_BY, treatment,
                "http://www.ebi.ac.uk/efo/EFO_0000579" ) )
                .as( "a growth condition is a treatment condition and models nothing" )
                .isEqualTo( RelationInferenceDirection.NEITHER );

        // an EFO disease still qualifies -- by its category, since EFO is not a disease namespace
        assertThat( RelationInferenceDirection.of( INDUCED_BY, DISEASE_MODEL,
                "http://www.ebi.ac.uk/efo/EFO_0003896" ) )
                .isEqualTo( RelationInferenceDirection.OBJECT_IMPLIES_SUBJECT );
    }

    /**
     * 🛑 An {@code OBJECT_IMPLIES_SUBJECT} licence resolves to {@code has role in modeling} or
     * {@code has disease} — a claim ABOUT DISEASE — so it may only run where the subject is one.
     *
     * <p>uib, 2026-08-18, from the {@code C57BL/6} chip on GSE99114:</p>
     * <pre>
     * strain:   C10 Congenic (A.B6chr10) --has_genotype--> C57BL/6
     *   =&gt; C57BL/6 has role in modeling C10 Congenic     backwards: C57BL/6 is its BACKGROUND
     * genotype: Myrf [mouse]             --has_genotype--> C57BL/6
     *   =&gt; C57BL/6 has role in modeling Myrf             a strain is not a model of a gene
     * </pre>
     *
     * <p>The licence assumed the pattern {@code disease model: X --has_genotype--> <genotype>}, where
     * the specific end is the object. With a strain or a genotype as the subject the arrow simply runs
     * the other way, and inverting it asserts something nobody said.</p>
     *
     * <p>These rows stay TERM_LEVEL and stay on the card — they are facts about the term. Only the
     * claim is withdrawn. Two orthogonal filters, as designed.</p>
     */
    @Test
    public void testAStrainOrGenotypeSubjectLicensesNoDiseaseClaim() {
        String strain = "http://www.ebi.ac.uk/efo/EFO_0005135";
        String genotype = "http://www.ebi.ac.uk/efo/EFO_0000513";

        assertThat( RelationInferenceDirection.of( HAS_GENOTYPE, strain, null ) )
                .as( "a congenic line's background strain is not a model of the line" )
                .isEqualTo( RelationInferenceDirection.NEITHER );
        assertThat( RelationInferenceDirection.of( HAS_GENOTYPE, genotype, null ) )
                .as( "a strain is not a model of a gene" )
                .isEqualTo( RelationInferenceDirection.NEITHER );

        assertThat( RelationTopicality.of( HAS_GENOTYPE, strain, null ) )
                .as( "still a fact about the term, so it stays on the card" )
                .isEqualTo( RelationTopicality.TERM_LEVEL );

        // the motivating case is untouched: a disease subject still licenses its inference
        assertThat( RelationInferenceDirection.of( HAS_GENOTYPE, DISEASE_MODEL, null ) )
                .isEqualTo( RelationInferenceDirection.OBJECT_IMPLIES_SUBJECT );
    }

    /**
     * 🛑 uib, 2026-08-18: a curator's term card carried
     * {@code induced pluripotent stem cell line cell --has disease--> lower motor neuron}. A neuron is
     * not a disease and a cell line does not have one.
     *
     * <p>The stored row was right — {@code lower motor neuron --induced by--> iPSC line} is a
     * differentiation protocol, one of the commonest things curated here. What was wrong was reading
     * {@code induced by} as though it always meant the disease-model sense it carries on
     * {@code Parkinson disease --induced by--> MPTP}. The subject's category is what separates them,
     * and the inference now consults it.</p>
     *
     * <p>uib could not filter this out themselves: {@code objectCategory} is null on CURATED rows by
     * construction, so {@code has disease -> neuron} and {@code has disease -> glioblastoma} are
     * indistinguishable to a client. It had to be fixed here or it stayed on screen.</p>
     */
    @Test
    public void testInducedByOnACellTypeSubjectLicensesNothing() {
        annotationRelationDao.create( inducedBy( MOTOR_NEURON, IPSC_LINE, "cell type", CELL_TYPE ) );

        // it does not reach a default reader at all, which is where uib's card gets its rows...
        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( MOTOR_NEURON ) ) ) ).isEmpty();

        // ...and asked for explicitly, it is still there and still licenses nothing. Both halves
        // matter: the row is real curation and stays in the table, and no reader may infer from it.
        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( MOTOR_NEURON ) )
                .termLevelOnly( false ) ) )
                .singleElement()
                .satisfies( r -> {
                    assertThat( r.getTopicality() ).isEqualTo( RelationTopicality.EXPERIMENT_LEVEL );
                    assertThat( r.getInferenceDirection() ).isEqualTo( RelationInferenceDirection.NEITHER );
                    assertThat( r.getImpliedSubjectUri() ).as( "no claim to phrase" ).isNull();
                    assertThat( r.getImpliedPredicate() ).isNull();
                } );

        // ...and the disease sense of the SAME predicate still licenses its inference
        annotationRelationDao.create( inducedBy( LEIGH, OXIDOPAMINE, "Disease model", DISEASE_MODEL ) );
        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( LEIGH ) ) ) )
                .singleElement()
                .satisfies( r -> assertThat( r.getInferenceDirection() )
                        .isEqualTo( RelationInferenceDirection.OBJECT_IMPLIES_SUBJECT ) );
    }

    /**
     * 🛑 An inducer is never handed the disease, whatever the taxon.
     *
     * <p>uib measured four {@code induced by} rows on one subject: {@code MPTP},
     * {@code alpha-synuclein inclusion body} and {@code methamphetamine} came back
     * <i>has role in modeling Parkinson disease</i> and {@code oxidopamine} came back <i>has disease Parkinson
     * disease</i>. The only thing that differed was which taxon the attesting experiment carried.</p>
     *
     * <p>"A mouse carrying APP/PS1 models the disease; a human line carrying LRRK2 G2019S has it" is
     * the right rule for an organism or a line and unsatisfiable for a compound. The fixture taxon here
     * is human, so the taxon rule alone would say {@code has disease}.</p>
     */
    @Test
    public void testAnInducerIsNeverHandedTheDiseaseEvenForAHumanExperiment() {
        annotationRelationDao.create( inducedBy( LEIGH, OXIDOPAMINE, "Disease model", DISEASE_MODEL ) );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( LEIGH ) ) ) )
                .singleElement()
                .satisfies( r -> {
                    assertThat( r.getTaxonNcbiId() ).as( "the fixture taxon is human" ).isEqualTo( 9606 );
                    assertThat( r.getImpliedSubjectUri() ).isEqualTo( OXIDOPAMINE );
                    assertThat( r.getImpliedPredicate() ).isEqualTo( "has role in modeling" );
                } );
    }

    /**
     * A predicate whose object is a quantity does not relate two concepts, so the harvest does not
     * store it at all — 9,606 of 36,073 curated rows, whose objects are {@code 10 uM} and
     * {@code 10 mg/kg}. Caught by URI, and by label for the rows nobody grounded.
     */
    @Test
    public void testAQuantityValuedPredicateIsNotARelationBetweenConcepts() {
        assertThat( RelationTopicality.isQuantityValued(
                "http://gemma.msl.ubc.ca/ont/TGEMO_00166", "delivered at dose" ) ).isTrue();
        assertThat( RelationTopicality.isQuantityValued( null, "timepoint" ) )
                .as( "two curated rows use it as a bare label, with no URI to match on" ).isTrue();
        assertThat( RelationTopicality.isQuantityValued( HAS_GENOTYPE, "has_genotype" ) ).isFalse();
        assertThat( RelationTopicality.isQuantityValued( null, null ) ).isFalse();
    }

    /**
     * 🛑 An experiment seed may only walk FORWARDS: the dataset has to supply the end that implies.
     *
     * <p>Paul on GSE315959, 2026-08-27. Its one grounded annotation is {@code organism part: prostate
     * gland}, which is the CONCLUSION of {@code CLO_0037208 derives from anatomic part}, not its
     * premise. Seeded from the object side the card read 169 Cellosaurus prostate lines back out of
     * that one term and offered them as the dataset's inferred concepts. A cell line implies the organ
     * it was taken from; an organ implies no cell line.</p>
     *
     * <p>The control is the same stored row seeded from the other end, so an empty answer here is the
     * rule and not a fixture that never matched anything.</p>
     */
    @Test
    public void testAnExperimentSeedMustSupplyTheEndThatImplies() {
        annotationRelationDao.create( derivesFromAnatomicPart( PROSTATE_LINE, PROSTATE_GLAND ) );
        Long carriesTheLine = experimentAnnotatedWith( PROSTATE_LINE );
        Long carriesTheOrgan = experimentAnnotatedWith( PROSTATE_GLAND );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .seedFromExperimentId( carriesTheLine )
                .seedDirection( AnnotationRelationDao.Direction.SUBJECT_TO_OBJECT ) ) )
                .as( "the line is the premise, so the organ follows from it" )
                .singleElement()
                .satisfies( r -> assertThat( r.getImpliedObjectUri() ).isEqualTo( PROSTATE_GLAND ) );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .seedFromExperimentId( carriesTheOrgan )
                .seedDirection( AnnotationRelationDao.Direction.OBJECT_TO_SUBJECT ) ) )
                .as( "and the organ is the conclusion, so nothing follows from it" )
                .isEmpty();
    }

    /**
     * A conclusion the dataset already carries is not an inference, so it is not offered back to it.
     *
     * <p>Both legs seed from the SUBJECT side, so both clear the forward-walk rule and only this one
     * separates them: the experiment that carries the line alone is told what it came from, and the
     * experiment that already says {@code prostate gland} is told nothing.</p>
     */
    @Test
    public void testAConclusionTheDatasetAlreadyCarriesIsNotOfferedBackToIt() {
        annotationRelationDao.create( derivesFromAnatomicPart( PROSTATE_LINE, PROSTATE_GLAND ) );
        Long carriesTheLineOnly = experimentAnnotatedWith( PROSTATE_LINE );
        Long carriesBoth = experimentAnnotatedWith( PROSTATE_LINE, PROSTATE_GLAND );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .seedFromExperimentId( carriesTheLineOnly )
                .seedDirection( AnnotationRelationDao.Direction.SUBJECT_TO_OBJECT ) ) )
                .as( "it has not said where the line came from, so the relation tells it something" )
                .hasSize( 1 );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .seedFromExperimentId( carriesBoth )
                .seedDirection( AnnotationRelationDao.Direction.SUBJECT_TO_OBJECT ) ) )
                .as( "it already says prostate gland; restating it is not an inference" )
                .isEmpty();
    }

    /**
     * The shape the two rules must NOT cost us, measured on GSE28044.
     *
     * <p>{@code breast cancer --has_genotype--> BRCA1} is stored with the disease as the subject, so
     * the gene is the premise and the disease is what follows. A dataset carrying the gene supplies
     * the implying end and does not carry the disease, which is exactly the case the experiment page
     * exists to show.</p>
     */
    @Test
    public void testAGenotypeSeedStillReachesTheDiseaseItStandsFor() {
        annotationRelationDao.create( attested( LEIGH, SURF1, AnnotationRelationBasis.CURATED, readMask() ) );
        Long carriesTheGene = experimentAnnotatedWith( SURF1 );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .seedFromExperimentId( carriesTheGene )
                .seedDirection( AnnotationRelationDao.Direction.OBJECT_TO_SUBJECT ) ) )
                .singleElement()
                .satisfies( r -> {
                    assertThat( r.getInferenceDirection() )
                            .isEqualTo( RelationInferenceDirection.OBJECT_IMPLIES_SUBJECT );
                    assertThat( r.getImpliedSubjectUri() ).isEqualTo( SURF1 );
                    assertThat( r.getImpliedObjectUri() ).isEqualTo( LEIGH );
                } );
    }

    /**
     * 🛑 Subject breadth counts objects under ONE predicate, and the unscoped number it used to report
     * cannot do the job the field was added for.
     *
     * <p>Measured on gemma2 2026-08-27, unscoped: {@code dimethyl sulfoxide} 9, {@code BRCA1} 13,
     * {@code biotin} 15, {@code epithelial cell} 20. The one row a reader wanted
     * ({@code BRCA1 --has disease--> breast cancer}) sits between two ontology closures nobody wanted,
     * so no bar on that number separates them. Asking the endpoint one predicate at a time returned
     * 8, 1, 15 and 3 objects for those four.</p>
     *
     * <p>The fixture is that shape at small scale: one subject bearing three roles and one disease.
     * Unscoped every row of it reads 4 and the disease row is indistinguishable from a role.</p>
     */
    @Test
    public void testSubjectBreadthCountsObjectsUnderOnePredicateOnly() {
        String compound = threeRolesAndOneDisease();

        List<AnnotationRelationDao.RelationSummary> found = annotationRelationDao.findRelations(
                new AnnotationRelationDao.RelationQuery()
                        .subjectValueUris( Collections.singleton( compound ) ) );

        assertThat( found ).hasSize( 4 );
        assertThat( found ).filteredOn( r -> "has role".equals( r.getPredicate() ) )
                .as( "the roles enumerate a list, and the count sees all three of them" )
                .hasSize( 3 )
                .allSatisfy( r -> assertThat( r.getSubjectBreadth() ).isEqualTo( 3 ) );
        assertThat( found ).filteredOn( r -> "has disease".equals( r.getPredicate() ) )
                .as( "the roles are not counted against the disease -- a different predicate is a "
                        + "different question" )
                .singleElement()
                .satisfies( r -> assertThat( r.getSubjectBreadth() ).isEqualTo( 1 ) );
    }

    /**
     * The bar, on the same fixture. A subject enumerating a list under one predicate goes; the row it
     * makes one statement with stays.
     */
    @Test
    public void testAListEnumeratingSubjectIsDroppedWhenACallerSetsASubjectBreadthBar() {
        String compound = threeRolesAndOneDisease();

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( compound ) ) ) )
                .as( "with no bar the whole closure comes back, as it always has" )
                .hasSize( 4 );

        assertThat( annotationRelationDao.findRelations( new AnnotationRelationDao.RelationQuery()
                .subjectValueUris( Collections.singleton( compound ) )
                .maximumSubjectBreadth( 1 ) ) )
                .singleElement()
                .satisfies( r -> {
                    assertThat( r.getPredicate() ).isEqualTo( "has disease" );
                    assertThat( r.getSubjectBreadth() ).isEqualTo( 1 );
                } );
    }

    /**
     * Cellosaurus's provenance row: the cell line is the subject and the organ it was taken from is
     * the object, which is the orientation that made the reported case read backwards.
     */
    private AnnotationRelation derivesFromAnatomicPart( String cellLineUri, String organUri ) {
        AnnotationRelation r = new AnnotationRelation();
        r.setSubjectValue( "line " + cellLineUri );
        r.setSubjectValueUri( cellLineUri );
        r.setSubjectCategory( "cell line" );
        r.setSubjectCategoryUri( "http://purl.obolibrary.org/obo/CLO_0000031" );
        r.setPredicate( "derives from anatomic part" );
        r.setPredicateUri( DERIVES_FROM_ANATOMIC_PART );
        r.setObjectValue( "part " + organUri );
        r.setObjectValueUri( organUri );
        r.setBasis( AnnotationRelationBasis.EXTERNAL );
        r.setSource( "Cellosaurus" );
        r.setGeneratedAt( new Date() );
        return r;
    }

    /**
     * An experiment carrying the given terms, written into EE2C by hand.
     *
     * <p>The seed is matched against EE2C inside the query, so a seeded read needs rows there and
     * running the EE2C rebuild first would put its correctness in the path of every assertion.</p>
     */
    private Long experimentAnnotatedWith( String... valueUris ) {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setTaxon( taxon );
        sessionFactory.getCurrentSession().persist( ee );
        for ( String valueUri : valueUris ) {
            Characteristic c = new Characteristic();
            c.setValue( "annotation " + valueUri );
            c.setValueUri( valueUri );
            sessionFactory.getCurrentSession().persist( c );
            sessionFactory.getCurrentSession().flush();
            sessionFactory.getCurrentSession().createNativeQuery(
                            "insert into EXPRESSION_EXPERIMENT2CHARACTERISTIC (ID, `VALUE`, VALUE_URI, "
                                    + "EXPRESSION_EXPERIMENT_FK, ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK, `LEVEL`) "
                                    + "values (:id, :value, :valueUri, :eeId, :mask, :level)" )
                    .setParameter( "id", c.getId() )
                    .setParameter( "value", c.getValue() )
                    .setParameter( "valueUri", c.getValueUri() )
                    .setParameter( "eeId", ee.getId() )
                    .setParameter( "mask", readMask() )
                    .setParameter( "level", ExpressionExperiment.class.getName() )
                    .executeUpdate();
        }
        sessionFactory.getCurrentSession().flush();
        return ee.getId();
    }

    /**
     * An {@code induced by} row with the subject category the caller cares about — the predicate whose
     * meaning that category decides.
     */
    private AnnotationRelation inducedBy( String subjectUri, String objectUri, String category,
            String categoryUri ) {
        AnnotationRelation r = attested( subjectUri, objectUri, AnnotationRelationBasis.CURATED, readMask() );
        r.setSubjectCategory( category );
        r.setSubjectCategoryUri( categoryUri );
        r.setPredicate( "induced by" );
        r.setPredicateUri( INDUCED_BY );
        return r;
    }

    /**
     * An ONTOLOGY row: asserted, so it carries no experiment and therefore no support to sort on.
     */
    private AnnotationRelation ontologyRow( String subjectUri, String subjectValue, String objectUri,
            String objectValue ) {
        return ontologyRow( subjectUri, subjectValue, objectUri, objectValue, "has role", HAS_ROLE );
    }

    /**
     * @see #ontologyRow(String, String, String, String)
     */
    private AnnotationRelation ontologyRow( String subjectUri, String subjectValue, String objectUri,
            String objectValue, String predicate, String predicateUri ) {
        AnnotationRelation r = new AnnotationRelation();
        r.setSubjectValue( subjectValue );
        r.setSubjectValueUri( subjectUri );
        r.setSubjectCategory( "cell line" );
        r.setSubjectCategoryUri( "http://purl.obolibrary.org/obo/CLO_0000031" );
        r.setPredicate( predicate );
        r.setPredicateUri( predicateUri );
        r.setObjectValue( objectValue );
        r.setObjectValueUri( objectUri );
        r.setBasis( AnnotationRelationBasis.ONTOLOGY );
        r.setSource( "CHEBI" );
        r.setGeneratedAt( new Date() );
        return r;
    }

    /**
     * One subject bearing three roles and one disease — the shape gemma2 serves for a compound.
     *
     * @return the subject's URI
     */
    private String threeRolesAndOneDisease() {
        String compound = "http://purl.obolibrary.org/obo/CHEBI_9300001";
        for ( int i = 0; i < 3; i++ ) {
            annotationRelationDao.create( ontologyRow( compound, "the compound",
                    "http://purl.obolibrary.org/obo/CHEBI_930001" + i, "role " + i ) );
        }
        annotationRelationDao.create( ontologyRow( compound, "the compound",
                "http://purl.obolibrary.org/obo/MONDO_9300020", "the one disease",
                "has disease", HAS_DISEASE ) );
        return compound;
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
