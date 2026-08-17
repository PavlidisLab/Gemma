package ubic.gemma.rest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest5;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.FilterArg;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;
import ubic.gemma.rest.util.args.SortArg;

import org.springframework.lang.Nullable;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip for the {@code filter} strings {@code GET /annotations/diseaseModels} hands to clients.
 * <p>
 * The endpoint's contract is that a client can take a row's {@code filter} and pass it straight to
 * {@code GET /datasets} to see the evidence. That claim spans two subsystems — the filter is built over
 * {@code allCharacteristics}, and the dataset query rewrites clauses on that collection into subqueries
 * before running them — so it cannot be checked on either side alone. In particular the per-row filter names
 * the model AND the disease, which are two annotations on one dataset and never the same annotation: unless
 * the rewrite puts each in its own subquery, the two clauses land on a single join alias and the filter is
 * unsatisfiable, returning nothing while looking perfectly reasonable.
 * <p>
 * The unit-level checks on the emitted strings live in {@link AnnotationsWebServiceTest}; this is the one
 * that runs them.
 */
@Tag("integration")
public class AnnotationsWebServiceDiseaseModelsRestTest extends BaseJerseyIntegrationTest5 {

    private static final String ALZHEIMER = "http://purl.obolibrary.org/obo/MONDO_0004975";
    private static final String APP_PS1 = "http://purl.org/commons/record/ncbi_gene/11820";

    @Autowired
    private AnnotationsWebService annotationsWebService;

    @Autowired
    private DatasetsWebService datasetsWebService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Autowired
    private PersistentDummyObjectHelper testHelper;

    private final List<ExpressionExperiment> ees = new ArrayList<>();

    @AfterEach
    public void removeFixtures() {
        expressionExperimentService.remove( ees );
    }

    @Test
    public void testPerModelFilterReturnsTheDatasetsTheInferenceWasReadFrom() {
        ExpressionExperiment attesting = createExperiment( APP_PS1, "APP/PS1", ALZHEIMER, "Alzheimer disease" );
        // a dataset carrying only the genotype: it is what the inference exists to reach, but NOT what the
        // per-row filter promises, which is the evidence the inference was read from
        ExpressionExperiment genotypeOnly = createExperiment( APP_PS1, "APP/PS1", null, null );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );

        AnnotationsWebService.DiseaseModelInferenceValueObject payload = getDiseaseModels();
        assertThat( payload.getModels() )
                .as( "the genotype the two datasets share is inferred to model the disease" )
                .isNotEmpty();
        AnnotationsWebService.InferredDiseaseModelValueObject model = payload.getModels().stream()
                .filter( m -> "APP/PS1".equals( m.getValue() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "APP/PS1 was not inferred to model Alzheimer disease" ) );
        assertThat( model.getExampleDatasetId() ).isEqualTo( attesting.getId() );

        assertThat( datasetIdsMatching( model.getFilter() ) )
                .as( "the per-row filter returns the evidence, and only the evidence" )
                .contains( attesting.getId() )
                .doesNotContain( genotypeOnly.getId() );

        // ...and the widened filter reaches the dataset that carries no disease annotation at all, which is
        // the entire point of the endpoint.
        assertThat( datasetIdsMatching( payload.getFilter() ) )
                .as( "the widened filter reaches the study annotated with the genotype alone" )
                .contains( attesting.getId(), genotypeOnly.getId() );
    }

    /**
     * A genotype that was never grounded in an ontology has to travel as free text, and the free-text leg is
     * NOT one of the properties the dataset query rewrites into a subquery — so it takes a different path
     * through the filter machinery than the URI leg beside it and is worth running for itself.
     */
    @Test
    public void testPerModelFilterRoundTripsForAnUngroundedValue() {
        ExpressionExperiment attesting = createExperiment( null, "Tp53/Rb1 DKO", ALZHEIMER, "Alzheimer disease" );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );

        AnnotationsWebService.InferredDiseaseModelValueObject model = getDiseaseModels().getModels().stream()
                .filter( m -> "Tp53/Rb1 DKO".equals( m.getValue() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "the ungrounded genotype was not inferred" ) );
        assertThat( model.getValueUri() ).isNull();

        assertThat( datasetIdsMatching( model.getFilter() ) )
                .contains( attesting.getId() );
    }

    private AnnotationsWebService.DiseaseModelInferenceValueObject getDiseaseModels() {
        // null is how an unconstrained array parameter arrives: an array arg refuses to parse a blank string,
        // so Jersey injects nothing for one that was omitted.
        ResponseDataObject<AnnotationsWebService.DiseaseModelInferenceValueObject> response =
                annotationsWebService.getDiseaseModels( ALZHEIMER, null, null, "genotype", false, 1, 0, null, 50 );
        return response.getData();
    }

    private List<Long> datasetIdsMatching( String filter ) {
        return datasetsWebService.getDatasets( null, FilterArg.valueOf( filter ), OffsetArg.valueOf( "0" ),
                        LimitArg.valueOf( "100" ), SortArg.valueOf( "+id" ) )
                .getData().stream()
                .map( vo -> vo.getId() )
                .collect( java.util.stream.Collectors.toList() );
    }

    private ExpressionExperiment createExperiment( @Nullable String genotypeUri, String genotypeValue,
            @Nullable String diseaseUri, @Nullable String diseaseValue ) {
        ExpressionExperiment ee = testHelper.getTestPersistentExpressionExperiment();
        ee.getCharacteristics().add( characteristic( Categories.GENOTYPE, genotypeUri, genotypeValue ) );
        if ( diseaseUri != null ) {
            ee.getCharacteristics().add( characteristic( Categories.DISEASE, diseaseUri, diseaseValue ) );
        }
        expressionExperimentService.update( ee );
        ees.add( ee );
        return ee;
    }

    private static Characteristic characteristic( Category category, @Nullable String valueUri, String value ) {
        Characteristic c = new Characteristic();
        c.setCategory( category.getCategory() );
        c.setCategoryUri( category.getCategoryUri() );
        c.setValue( value );
        c.setValueUri( valueUri );
        return c;
    }
}
