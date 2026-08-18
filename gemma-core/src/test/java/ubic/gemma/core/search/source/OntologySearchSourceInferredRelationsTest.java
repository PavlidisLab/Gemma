package ubic.gemma.core.search.source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.model.common.description.AnnotationRelationBasis;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.persistence.service.common.description.AnnotationRelationDao;
import ubic.gemma.persistence.service.common.description.AnnotationRelationService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The opt-in widening of a search into inferred relations.
 *
 * <p>Driven directly rather than through the whole ontology search path, because what can go wrong
 * here is not "does search work" — it is whether the flag is honoured, whether an inferred hit can
 * outscore a direct one, whether a term the query already carries silently loses its score to the
 * inference penalty, and whether one prolific seed can swamp the query.</p>
 *
 * <p>The case throughout is the one that motivated the feature: GSE134453 carries
 * {@code genotype: SURF1 patient} and no disease annotation, and a user searching Leigh syndrome has
 * to find it. If the widening does not happen, dropping the redundant disease tag on the grounds that
 * it is inferable does not relocate the fact, it deletes it.</p>
 */
public class OntologySearchSourceInferredRelationsTest {

    private static final String LEIGH = "http://purl.obolibrary.org/obo/MONDO_0009723";
    private static final String SURF1 = "http://purl.org/commons/record/ncbi_gene/6834";
    private static final String COMPLEX_IV = "http://purl.obolibrary.org/obo/MONDO_0700250";
    private static final String TRP53 = "http://purl.org/commons/record/ncbi_gene/22059";

    private OntologySearchSource source;
    private AnnotationRelationService annotationRelationService;

    private Collection<String> uris;
    private Map<String, String> uri2value;
    private Map<String, Double> uri2score;

    @BeforeEach
    public void setUp() {
        source = new OntologySearchSource();
        annotationRelationService = mock( AnnotationRelationService.class );
        ReflectionTestUtils.setField( source, "annotationRelationService", annotationRelationService );
        ReflectionTestUtils.setField( source, "maxInferredTermsPerSeed", 5 );
        ReflectionTestUtils.setField( source, "minInferredSpecificity", 0d );
        ReflectionTestUtils.setField( source, "maxInferredObjectBreadth", 0 );

        uris = new HashSet<>( Collections.singleton( LEIGH ) );
        uri2value = new HashMap<>( Collections.singletonMap( LEIGH, "Leigh syndrome" ) );
        uri2score = new HashMap<>( Collections.singletonMap( LEIGH, 1.0 ) );
    }

    /**
     * Off by default: an existing search must not change shape because this feature landed.
     */
    @Test
    public void testWideningDoesNotHappenUnlessAskedFor() {
        Set<String> added = source.expandByInferredRelations( uris, uri2value, uri2score, settings( false ) );

        assertThat( added ).isEmpty();
        assertThat( uris ).containsExactly( LEIGH );
        verify( annotationRelationService, never() ).findRelations( any() );
    }

    /**
     * The acceptance criterion: searching the disease reaches the genotype that stands for it.
     */
    @Test
    public void testSearchingADiseaseReachesTheGenotypeThatStandsForIt() {
        stub( relation( LEIGH, SURF1, "SURF1", AnnotationRelationBasis.CURATED ) );

        Set<String> added = source.expandByInferredRelations( uris, uri2value, uri2score, settings( true ) );

        assertThat( added ).contains( SURF1 );
        assertThat( uris ).contains( LEIGH, SURF1 );
        assertThat( uri2value ).containsEntry( SURF1, "SURF1" );
    }

    /**
     * An inferred hit must never outrank a dataset actually annotated with the query.
     *
     * <p>Getting this backwards would put "we think this genotype means your disease" above "this
     * dataset is annotated with your disease", which reads as a broken search rather than a ranking
     * choice.</p>
     */
    @Test
    public void testAnInferredTermScoresBelowTheTermActuallyAskedFor() {
        stub( relation( LEIGH, SURF1, "SURF1", AnnotationRelationBasis.CURATED ) );

        source.expandByInferredRelations( uris, uri2value, uri2score, settings( true ) );

        assertThat( uri2score.get( SURF1 ) ).isLessThan( uri2score.get( LEIGH ) );
    }

    /**
     * A term the query already matched directly keeps its own score.
     *
     * <p>Relations are read in both directions, so a term can legitimately come back as related to a
     * seed the query already carries. Overwriting it with the inference penalty would demote a direct
     * hit for the sole reason that something else in the corpus also points at it.</p>
     */
    @Test
    public void testADirectHitIsNotDemotedByAlsoBeingInferred() {
        stub( relation( LEIGH, LEIGH, "Leigh syndrome", AnnotationRelationBasis.CURATED ) );

        Set<String> added = source.expandByInferredRelations( uris, uri2value, uri2score, settings( true ) );

        assertThat( added ).isEmpty();
        assertThat( uri2score.get( LEIGH ) ).isEqualTo( 1.0 );
    }

    /**
     * 🛑 One seed may not swamp the query.
     *
     * <p>This is the {@code Trp53} case: it pairs with 15 diseases in our corpus and a null in it
     * genuinely models several malignancies, so a search that expanded through all of them would stop
     * being a search for the disease the user typed and become a search for every dataset that ever
     * mutated the gene. The ranked list is cut per seed, so the best few contribute and the tail does
     * not.</p>
     */
    @Test
    public void testOneProlificSeedCannotSwampTheQuery() {
        ReflectionTestUtils.setField( source, "maxInferredTermsPerSeed", 3 );
        uris.add( TRP53 );
        uri2value.put( TRP53, "Trp53" );
        uri2score.put( TRP53, 1.0 );

        List<AnnotationRelationDao.RelationSummary> fifteen = new ArrayList<>();
        for ( int i = 0; i < 15; i++ ) {
            fifteen.add( relation( TRP53, "http://purl.obolibrary.org/obo/MONDO_000" + i, "disease " + i,
                    AnnotationRelationBasis.CORPUS ) );
        }
        when( annotationRelationService.findRelations( any() ) ).thenReturn( fifteen );

        Set<String> added = source.expandByInferredRelations( uris, uri2value, uri2score, settings( true ) );

        // three per seed, and the seed appears on the subject side of both direction passes
        assertThat( added ).hasSizeLessThanOrEqualTo( 6 );
        assertThat( added ).hasSizeGreaterThan( 0 );
    }

    /**
     * Within the per-seed budget, ambiguity survives: nothing picks one of the candidates.
     */
    @Test
    public void testCandidatesWithinTheBudgetAllGoIn() {
        when( annotationRelationService.findRelations( any() ) ).thenReturn( Arrays.asList(
                relation( LEIGH, SURF1, "SURF1", AnnotationRelationBasis.CURATED ),
                relation( LEIGH, COMPLEX_IV, "complex IV deficiency", AnnotationRelationBasis.ONTOLOGY ) ) );

        Set<String> added = source.expandByInferredRelations( uris, uri2value, uri2score, settings( true ) );

        assertThat( added ).contains( SURF1, COMPLEX_IV );
    }

    /**
     * An ungrounded related term is dropped rather than matched on its label.
     *
     * <p>The lookup this feeds matches on {@code VALUE_URI}, so a term with no URI has nothing to
     * match against. Falling back to the string would be label matching, which is what cost two
     * tokenisation bugs in a single day elsewhere ({@code B-cell} destroyed by treating {@code cell}
     * as a stopword; {@code lymphoma.} not equal to {@code lymphoma}).</p>
     */
    @Test
    public void testAnUngroundedRelatedTermIsDropped() {
        stub( relation( LEIGH, null, "aortic banding", AnnotationRelationBasis.CURATED ) );

        Set<String> added = source.expandByInferredRelations( uris, uri2value, uri2score, settings( true ) );

        assertThat( added ).isEmpty();
        assertThat( uris ).containsExactly( LEIGH );
    }

    /**
     * Both directions are consulted: which end of a relation the user typed is not something to guess.
     * A curated statement puts the disease in the subject and the gene in the object.
     */
    @Test
    public void testBothDirectionsAreAsked() {
        when( annotationRelationService.findRelations( any() ) ).thenReturn( Collections.emptyList() );

        source.expandByInferredRelations( uris, uri2value, uri2score, settings( true ) );

        verify( annotationRelationService, org.mockito.Mockito.times( AnnotationRelationDao.Direction.values().length ) )
                .findRelations( any() );
    }

    /**
     * With no relation service wired the widening is a no-op rather than a failure — several contexts
     * construct this source by hand.
     */
    @Test
    public void testAbsentRelationServiceIsHarmless() {
        ReflectionTestUtils.setField( source, "annotationRelationService", null );

        assertThat( source.expandByInferredRelations( uris, uri2value, uri2score, settings( true ) ) ).isEmpty();
        assertThat( uris ).containsExactly( LEIGH );
    }

    private void stub( AnnotationRelationDao.RelationSummary... summaries ) {
        when( annotationRelationService.findRelations( any() ) ).thenReturn( Arrays.asList( summaries ) );
    }

    private static AnnotationRelationDao.RelationSummary relation( String subjectUri, String objectUri,
            String objectValue, AnnotationRelationBasis basis ) {
        return new AnnotationRelationDao.RelationSummary(
                "subject", subjectUri, "disease model", null,
                "has_genotype", "http://purl.obolibrary.org/obo/GENO_0000222",
                objectValue, objectUri, null, null,
                null, null, null, basis, null, null,
                1, 1, 0, 0, null, 1, 1 );
    }

    private static SearchSettings settings( boolean inferRelations ) {
        return SearchSettings.builder()
                .query( "Leigh syndrome" )
                .useInferredRelations( inferRelations )
                .build();
    }
}
