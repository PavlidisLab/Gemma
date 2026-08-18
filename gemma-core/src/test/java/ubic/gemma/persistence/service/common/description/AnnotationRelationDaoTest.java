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
import ubic.gemma.model.common.description.RelationInferenceDirection;
import ubic.gemma.model.common.description.RelationTopicality;
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
    private static final String INDUCED_BY = "http://gemma.msl.ubc.ca/ont/TGEMO_00171";
    private static final String DISEASE_MODEL = "http://gemma.msl.ubc.ca/ont/TGEMO_00101";
    private static final String CELL_TYPE = "http://www.ebi.ac.uk/efo/EFO_0000324";
    private static final String MOTOR_NEURON = "http://purl.obolibrary.org/obo/CL_0011001";
    private static final String IPSC_LINE = "http://purl.obolibrary.org/obo/CLO_0037279";
    private static final String OXIDOPAMINE = "http://purl.obolibrary.org/obo/CHEBI_78741";

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
        assertThat( RelationInferenceDirection.of( "http://purl.obolibrary.org/obo/RO_0001000", diseaseModel ) )
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
                    assertThat( r.getImpliedPredicate() ).isEqualTo( "is model of" );
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
     * <i>is model of Parkinson disease</i> and {@code oxidopamine} came back <i>has disease Parkinson
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
                    assertThat( r.getImpliedPredicate() ).isEqualTo( "is model of" );
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
