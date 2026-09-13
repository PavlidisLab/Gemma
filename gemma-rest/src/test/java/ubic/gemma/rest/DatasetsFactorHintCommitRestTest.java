package ubic.gemma.rest;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValueBasicValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest5;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A design-commit factor item carrying ONLY {@code gemmaId} plus one relevance hint leaves everything else about
 * the factor as it was: name, category, factor values and sample assignments.
 * <p>
 * cab writes {@code subsetRelevance} / {@code baselineRelevance} on an EXISTING factor this way
 * ({@code CAB_TO_GEMBRO_2026_09_12_YES_PIN_THE_FACTOR_ITEM_SURVIVORS_IN_THE_DEFAULT_SUITE}). The code carries omitted
 * fields forward — the service applies factor metadata only when non-null, and REST keeps every factor value a
 * section does not mention (declared-delete) — but the nearest existing test,
 * {@code DatasetsCurationCommitRestTest#testSubsetRelevanceRoundTripsAndAnOverrideOnlyCommitIsNotANoOp}, asserts
 * none of the survivors and is {@code @Tag("slow")}, so it is not in the default suite.
 * <p>
 * Runs end to end against gemdtest — REST mapping, service apply, persistence — because the survivors are a
 * property of that whole path, not of any one layer a mock could stand in for.
 * <p>
 * Out of scope here: {@code supportingEvidence}. A factor that HAS evidence answers 400 to an item that omits it
 * ({@code requireEvidenceEchoed}); the seeded factor carries none, so no echo is needed.
 */
@Tag("integration")
public class DatasetsFactorHintCommitRestTest extends BaseJerseyIntegrationTest5 {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private PersistentDummyObjectHelper testHelper;

    private ExpressionExperiment ee;

    @BeforeEach
    public void seedExperiment() {
        testHelper.resetSeed();
        ee = testHelper.getTestExpressionExperimentWithAllDependencies( false );
    }

    @AfterEach
    public void removeExperiment() {
        if ( ee != null ) {
            expressionExperimentService.remove( ee );
        }
    }

    @Test
    public void aSubsetRelevanceOnlyItemLeavesNameCategoryValuesAndAssignmentsIntact() {
        assertAHintOnlyItemLeavesTheFactorIntact( "subsetRelevance", "subsetRelevanceReason",
                ExperimentalDesignValueObject.ExperimentalFactorEntry::getSubsetRelevance,
                ExperimentalDesignValueObject.ExperimentalFactorEntry::getSubsetRelevanceReason );
    }

    @Test
    public void aBaselineRelevanceOnlyItemLeavesNameCategoryValuesAndAssignmentsIntact() {
        assertAHintOnlyItemLeavesTheFactorIntact( "baselineRelevance", "baselineRelevanceReason",
                ExperimentalDesignValueObject.ExperimentalFactorEntry::getBaselineRelevance,
                ExperimentalDesignValueObject.ExperimentalFactorEntry::getBaselineRelevanceReason );
    }

    private void assertAHintOnlyItemLeavesTheFactorIntact( String hintField, String reasonField,
            Function<ExperimentalDesignValueObject.ExperimentalFactorEntry, String> hint,
            Function<ExperimentalDesignValueObject.ExperimentalFactorEntry, String> reason ) {
        // Two values, each bound to a real sample, so every survivor below has something to lose.
        ExpressionExperiment thawed = expressionExperimentService.thawBioAssays( ee );
        List<String> gsms = thawed.getBioAssays().stream()
                .map( BioAssay::getAccession )
                .filter( Objects::nonNull )
                .map( a -> a.getAccession() )
                .limit( 2 )
                .collect( Collectors.toList() );
        assertThat( gsms ).as( "the seeded experiment needs two samples with accessions" ).hasSize( 2 );

        String seed = "{\"design\":{\"factors\":{\"items\":[{"
                + "\"clientRef\":\"F1\",\"name\":\"organism part\",\"category\":{\"label\":\"organism part\"},"
                + "\"factorValues\":{\"items\":["
                + "{\"clientRef\":\"FV1\",\"freeTextLabel\":\"cortex\",\"biomaterialShortNames\":[\"" + gsms.get( 0 ) + "\"]},"
                + "{\"clientRef\":\"FV2\",\"freeTextLabel\":\"liver\",\"biomaterialShortNames\":[\"" + gsms.get( 1 ) + "\"]}"
                + "]}}]}}}";
        put( seed );

        ExperimentalDesignValueObject beforeDesign = design();
        ExperimentalDesignValueObject.ExperimentalFactorEntry before = factorNamed( beforeDesign, "organism part" );
        Map<Long, String> valuesBefore = valueLabels( before );
        Map<Long, Set<Long>> assignmentsBefore = assignmentsTo( beforeDesign, valuesBefore.keySet() );
        // Guards against a vacuous pass: each survivor must exist before the edit.
        assertThat( before.getCategory() ).as( "seeded category" ).isNotNull();
        assertThat( valuesBefore ).as( "seeded values" ).hasSize( 2 );
        assertThat( assignmentsBefore ).as( "seeded sample bindings" ).hasSize( 2 );

        String hintOnly = "{\"design\":{\"factors\":{\"items\":[{"
                + "\"gemmaId\":" + before.getId() + ","
                + "\"" + hintField + "\":\"recommended\","
                + "\"" + reasonField + "\":\"pinned by DatasetsFactorHintCommitRestTest\""
                + "}]}}}";
        put( hintOnly );

        ExperimentalDesignValueObject afterDesign = design();
        ExperimentalDesignValueObject.ExperimentalFactorEntry after = factorNamed( afterDesign, "organism part" );

        assertThat( hint.apply( after ) ).as( hintField ).isEqualTo( "recommended" );
        assertThat( reason.apply( after ) ).as( reasonField ).isEqualTo( "pinned by DatasetsFactorHintCommitRestTest" );

        assertThat( after.getId() ).as( "same factor" ).isEqualTo( before.getId() );
        assertThat( after.getName() ).as( "name" ).isEqualTo( before.getName() );
        assertThat( after.getCategory() ).as( "category" ).usingRecursiveComparison().isEqualTo( before.getCategory() );
        assertThat( valueLabels( after ) ).as( "factor values, by id and label" ).isEqualTo( valuesBefore );
        assertThat( assignmentsTo( afterDesign, valuesBefore.keySet() ) ).as( "sample assignments" )
                .isEqualTo( assignmentsBefore );
    }

    private void put( String body ) {
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            String entity = r.readEntity( String.class );
            assertThat( r.getStatus() ).as( "PUT /curation answered %s", entity ).isEqualTo( 200 );
        }
    }

    private ExperimentalDesignValueObject design() {
        return expressionExperimentService.getExperimentalDesignValueObject( expressionExperimentService.load( ee.getId() ) );
    }

    private static ExperimentalDesignValueObject.ExperimentalFactorEntry factorNamed( ExperimentalDesignValueObject design, String name ) {
        return design.getExperimentalFactors().stream()
                .filter( f -> name.equals( f.getName() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "factor '" + name + "' was not persisted" ) );
    }

    /** Value id → label, for this factor. */
    @SuppressWarnings("deprecation")
    private static Map<Long, String> valueLabels( ExperimentalDesignValueObject.ExperimentalFactorEntry factor ) {
        Map<Long, String> out = new TreeMap<>();
        for ( FactorValueBasicValueObject v : factor.getValues() ) {
            out.put( v.getId(), v.getValue() );
        }
        return out;
    }

    /** Biomaterial id → the ids of THIS factor's values it is bound to, omitting biomaterials bound to none. */
    private static Map<Long, Set<Long>> assignmentsTo( ExperimentalDesignValueObject design, Set<Long> factorValueIds ) {
        Map<Long, Set<Long>> out = new TreeMap<>();
        for ( ExperimentalDesignValueObject.BioMaterialFactorValueAssignment a : design.getBioMaterialAssignments() ) {
            Set<Long> mine = new TreeSet<>( a.getFactorValueIds() );
            mine.retainAll( factorValueIds );
            if ( !mine.isEmpty() ) {
                out.put( a.getBioMaterialId(), mine );
            }
        }
        return out;
    }
}
