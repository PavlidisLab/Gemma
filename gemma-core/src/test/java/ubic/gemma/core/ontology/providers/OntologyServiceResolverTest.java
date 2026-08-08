package ubic.gemma.core.ontology.providers;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.jena.TdbOntologyService;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link OntologyServiceResolver}. The ontologies are Mockito mocks of the real service
 * classes so the class-name matching is exercised against the actual class names without needing the
 * {@code url.*} configuration a real constructor reads.
 */
class OntologyServiceResolverTest {

    private final OntologyService clo = ontology( CellLineOntologyService.class, "cellLineOntology", null );
    private final OntologyService hpo = ontology( HumanPhenotypeOntologyService.class, "humanPhenotypeOntology", "Human Phenotype Ontology" );
    private final OntologyService tgemo = ontology( GemmaOntologyService.class, "gemmaOntology", null );
    private final OntologyService pato = ontology( PatoOntologyService.class, "patoOntology", "PATO - the Phenotype And Trait Ontology" );
    private final OntologyService tdb = ontology( TdbOntologyService.class, "unified", null );

    private final List<OntologyService> ontologies = Arrays.asList( clo, hpo, tgemo, pato, tdb );

    @Test
    void resolvesByAbbreviation() {
        // the three Paul asked for, plus the unified TDB which has neither a dc:title nor an obvious class name
        assertThat( OntologyServiceResolver.resolve( ontologies, "CLO" ) ).contains( clo );
        assertThat( OntologyServiceResolver.resolve( ontologies, "HPO" ) ).contains( hpo );
        assertThat( OntologyServiceResolver.resolve( ontologies, "HP" ) ).contains( hpo );
        assertThat( OntologyServiceResolver.resolve( ontologies, "TGEMO" ) ).contains( tgemo );
        assertThat( OntologyServiceResolver.resolve( ontologies, "TDB" ) ).contains( tdb );
    }

    @Test
    void resolvesByIdentifier() {
        assertThat( OntologyServiceResolver.resolve( ontologies, "cellLineOntology" ) ).contains( clo );
        assertThat( OntologyServiceResolver.resolve( ontologies, "unified" ) ).contains( tdb );
    }

    @Test
    void resolvesByClassName() {
        assertThat( OntologyServiceResolver.resolve( ontologies, "CellLineOntologyService" ) ).contains( clo );
        assertThat( OntologyServiceResolver.resolve( ontologies, "CellLine" ) ).contains( clo );
        assertThat( OntologyServiceResolver.resolve( ontologies, "Pato" ) ).contains( pato );
    }

    @Test
    void resolvesByTitleWithSpacesAndPunctuation() {
        // the whole point: "PATO - the Phenotype And Trait Ontology" is what the listing shows today
        assertThat( OntologyServiceResolver.resolve( ontologies, "PATO - the Phenotype And Trait Ontology" ) )
                .contains( pato );
        assertThat( OntologyServiceResolver.resolve( ontologies, "Human Phenotype Ontology" ) ).contains( hpo );
    }

    @Test
    void matchingIgnoresCaseAndPunctuation() {
        assertThat( OntologyServiceResolver.resolve( ontologies, "clo" ) ).contains( clo );
        assertThat( OntologyServiceResolver.resolve( ontologies, "cell-line-ontology" ) ).contains( clo );
        assertThat( OntologyServiceResolver.resolve( ontologies, "CELLLINEONTOLOGY" ) ).contains( clo );
    }

    @Test
    void unknownOrBlankNameResolvesToNothing() {
        assertThat( OntologyServiceResolver.resolve( ontologies, "FOOBAR" ) ).isEmpty();
        assertThat( OntologyServiceResolver.resolve( ontologies, "" ) ).isEmpty();
        assertThat( OntologyServiceResolver.resolve( ontologies, null ) ).isEmpty();
    }

    @Test
    void identifierIsNotShadowedByAnotherOntologysTitle() {
        // a dc:title that collides with another ontology's identifier must lose: identifiers,
        // abbreviations and class names are all matched before any title is considered.
        OntologyService impostor = ontology( MondoOntologyService.class, "mondoOntology", "cellLineOntology" );
        List<OntologyService> withImpostor = Arrays.asList( impostor, clo );
        assertThat( OntologyServiceResolver.resolve( withImpostor, "cellLineOntology" ) ).contains( clo );
    }

    @Test
    void aBeanThatThrowsDoesNotBreakResolutionOfTheOthers() {
        OntologyService broken = mock( CellTypeOntologyService.class );
        lenient().when( broken.getIdentifier() ).thenThrow( new IllegalStateException( "not loaded" ) );
        lenient().when( broken.getName() ).thenThrow( new IllegalStateException( "not loaded" ) );
        assertThat( OntologyServiceResolver.resolve( Arrays.asList( broken, clo ), "CLO" ) ).contains( clo );
    }

    @Test
    void namesLeadWithTheAbbreviationAndIncludeEverythingAccepted() {
        assertThat( OntologyServiceResolver.getPreferredName( hpo ) ).isEqualTo( "HPO" );
        assertThat( OntologyServiceResolver.getNames( hpo ) )
                .containsExactly( "HPO", "HP", "humanPhenotypeOntology", "HumanPhenotypeOntologyService",
                        "HumanPhenotype", "Human Phenotype Ontology" );
        // no abbreviation registered, and no dc:title loaded: the identifier leads
        OntologyService generic = ontology( GenericOntologyService.class, "someOntology", null );
        assertThat( OntologyServiceResolver.getPreferredName( generic ) ).isEqualTo( "someOntology" );
    }

    private OntologyService ontology( Class<? extends OntologyService> clazz, String identifier, String title ) {
        OntologyService o = mock( clazz );
        lenient().when( o.getIdentifier() ).thenReturn( identifier );
        lenient().when( o.getName() ).thenReturn( title );
        return o;
    }
}
