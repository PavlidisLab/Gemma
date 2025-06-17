package ubic.gemma.core.loader.expression.geo;

import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.time.StopWatch;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.*;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.core.ontology.providers.MondoOntologyService;
import ubic.gemma.core.ontology.providers.PatoOntologyService;
import ubic.gemma.core.util.concurrent.Executors;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeNoException;

/**
 * Test replacements for GEO terms.
 */
@CommonsLog
@Category(SlowTest.class)
public class GeoTermReplacementTest {

    private static final List<OntologyService> ontologies = new ArrayList<>();
    private static final Map<OntologyService, Collection<String>> prefixesByOntology = new HashMap<>();

    static {
        addOntology( new GemmaOntologyService(), "http://gemma.msl.ubc.ca/ont/TGEMO_" );
        addOntology( new ObiService(), "http://purl.obolibrary.org/obo/OBI_" );
        addOntology( new PatoOntologyService(), "http://purl.obolibrary.org/obo/PATO_" );
        addOntology( new CellTypeOntologyService(), "http://purl.obolibrary.org/obo/CL_" );
        addOntology( new CellLineOntologyService(), "http://purl.obolibrary.org/obo/CLO_" );
        addOntology( new MondoOntologyService(), "http://purl.obolibrary.org/obo/MONDO_" );
        addOntology( new UberonOntologyService(), "http://purl.obolibrary.org/obo/UBERON_" );
        addOntology( new HumanPhenotypeOntologyService(), "http://purl.obolibrary.org/obo/HP_" );
        addOntology( new MammalianPhenotypeOntologyService(), "http://purl.obolibrary.org/obo/MP_" );
        addOntology( new ExperimentalFactorOntologyService(), "http://www.ebi.ac.uk/efo/", "http://purl.obolibrary.org/obo/BTO_", "http://purl.obolibrary.org/obo/ECTO_" );
        // TODO: addOntology( new ChebiOntologyService(), "http://purl.obolibrary.org/obo/CHEBI_" );
    }

    private static void addOntology( OntologyService ontology, String... prefix ) {
        ontology.setInferenceMode( OntologyService.InferenceMode.NONE );
        ontology.setSearchEnabled( false );
        ontology.setProcessImports( false );
        ontologies.add( ontology );
        prefixesByOntology.put( ontology, Arrays.asList( prefix ) );
    }

    /**
     * Remove an ontology in order to free some memory.
     */
    private static void removeOntology( OntologyService ontology ) {
        ontologies.remove( ontology );
        prefixesByOntology.remove( ontology );
    }

    @Value
    static class Rec {
        String synonym;
        String value;
        String valueUri;
        String category;
        String categoryUri;
    }

    @Test
    public void test() throws Exception {
        ExecutorCompletionService<OntologyService> cs = new ExecutorCompletionService<>( Executors.newFixedThreadPool( 8 ) );
        for ( OntologyService os : ontologies ) {
            cs.submit( () -> {
                StopWatch watch = StopWatch.createStarted();
                os.initialize( true, false );
                log.info( "Initialized " + os + " in " + watch.getTime() + " ms" );
                return os;
            } );
        }

        List<Rec> records = new ArrayList<>();
        try ( InputStream is = getClass().getResourceAsStream( "/ubic/gemma/core/ontology/valueStringToOntologyTermMappings.txt" ) ) {
            for ( CSVRecord record : CSVParser.parse( requireNonNull( is ), StandardCharsets.UTF_8, CSVFormat.TDF.builder().setCommentMarker( '#' ).get() ) ) {
                String synonym = record.get( 0 );
                String value = record.get( 1 );
                String valueUri = record.get( 2 );
                String category = record.get( 3 );
                String categoryUri = record.get( 4 );
                // FIXME: enable CHEBI
                if ( valueUri.startsWith( "http://purl.obolibrary.org/obo/CHEBI_" ) ) {
                    continue;
                }
                // FIXME: enable HANCESTRO
                if ( valueUri.startsWith( "http://purl.obolibrary.org/obo/HANCESTRO_" ) ) {
                    continue;
                }
                // FIXME: enable AFPO
                if ( valueUri.startsWith( "http://purl.obolibrary.org/obo/AfPO_" ) ) {
                    continue;
                }
                // FIXME: enable GO
                if ( valueUri.startsWith( "http://purl.obolibrary.org/obo/GO_" ) ) {
                    continue;
                }
                if ( valueUri.startsWith( "http://purl.obolibrary.org/obo/NCBITaxon_" ) ) {
                    continue;
                }
                records.add( new Rec( synonym, value, valueUri, category, categoryUri ) );
            }
        }

        SoftAssertions assertions = new SoftAssertions();

        // verify category URIs against EFO.factor.categories.txt
        Map<String, String> categories;
        try ( BufferedReader reader = new BufferedReader( new InputStreamReader( requireNonNull( GeoTermReplacementTest.class.getResourceAsStream( "/ubic/gemma/core/ontology/EFO.factor.categories.txt" ) ) ) ) ) {
            categories = reader.lines()
                    .filter( line -> !line.startsWith( "#" ) )
                    .map( line -> line.split( "\t", 2 ) )
                    .collect( Collectors.toMap( row -> row[0], row -> row[1] ) );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        for ( Rec rec : records ) {
            assertions.assertThat( categories )
                    .withFailMessage( "%s: Unknown category URI %s", rec.synonym, rec.categoryUri )
                    .containsKey( rec.categoryUri )
                    .withFailMessage( "%s: Invalid URI %s for category %s", rec.synonym, rec.categoryUri, rec.category )
                    .containsEntry( rec.categoryUri, rec.category );
        }

        // verify that all term has a supported prefix
        for ( Rec rec : records ) {
            assertions.assertThat( prefixesByOntology.values() )
                    .withFailMessage( "%s: No prefix for %s", rec.synonym, rec.valueUri )
                    .flatMap( c -> c )
                    .anySatisfy( prefix -> {
                        assertThat( rec.valueUri ).startsWith( prefix );
                    } );
        }

        // fail now if possible, because the next step is going to be expensive
        assertions.assertAll();

        int numOntologies = ontologies.size();
        Set<String> seen = new HashSet<>();
        for ( int i = 0; i < numOntologies; i++ ) {
            OntologyService os;
            try {
                os = cs.take().get();
            } catch ( ExecutionException e ) {
                // skip the test if ontologies cannot be loaded
                assumeNoException( e );
                return;
            }
            for ( Rec rec : records ) {
                if ( prefixesByOntology.get( os ).stream().noneMatch( rec.valueUri::startsWith ) ) {
                    continue;
                }
                OntologyTerm term = os.getTerm( rec.valueUri );
                assertions.assertThat( term )
                        .withFailMessage( "%s: No term found for %s in %s", rec.synonym, rec.valueUri, os )
                        .isNotNull();
                if ( term == null ) {
                    continue;
                }
                assertions.assertThat( rec.value )
                        .withFailMessage( "%s: Replace '%s' with '%s' %s from %s", rec.synonym, rec.value, term.getLabel(), term.getUri(), os )
                        .isEqualTo( term.getLabel() );
                seen.add( rec.synonym );
            }
            assertions.assertAll();
            removeOntology( os );
        }

        for ( Rec rec : records ) {
            assertions.assertThat( seen )
                    .withFailMessage( "%s: No term found for %s in any ontology", rec.synonym, rec.valueUri )
                    .contains( rec.synonym );
        }

        assertions.assertAll();
    }
}
