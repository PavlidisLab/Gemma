package ubic.gemma.core.loader.expression.geo;

import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.basecode.ontology.model.AnnotationProperty;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.*;
import ubic.gemma.core.ontology.providers.MondoOntologyService;
import ubic.gemma.core.ontology.providers.PatoOntologyService;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test replacements for GEO terms.
 */
@CommonsLog
@Category(SlowTest.class)
public class GeoTermReplacementTest {

    private static final List<OntologyService> ontologies = new ArrayList<>();

    static {
        addOntology( new CellLineOntologyService() );
        addOntology( new CellTypeOntologyService() );
        addOntology( new ObiService() );
        addOntology( new MondoOntologyService() );
        addOntology( new UberonOntologyService() );
        addOntology( new PatoOntologyService() );
        addOntology( new MammalianPhenotypeOntologyService() );
        // FIXME: addOntology( new ChebiOntologyService() );
        // EFO is a grab bag, so we list it last
        addOntology( new ExperimentalFactorOntologyService() );
    }

    private static void addOntology( OntologyService ontology ) {
        ontology.setInferenceMode( OntologyService.InferenceMode.NONE );
        ontology.setSearchEnabled( false );
        ontology.setProcessImports( false );
        ontologies.add( ontology );
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
                os.initialize( true, false );
                return os;
            } );
        }

        List<Rec> records = new ArrayList<>();
        try ( InputStream is = getClass().getResourceAsStream( "/ubic/gemma/core/ontology/valueStringToOntologyTermMappings.txt" ) ) {
            assertNotNull( is );
            for ( CSVRecord record : CSVParser.parse( is, StandardCharsets.UTF_8, CSVFormat.TDF.withCommentMarker( '#' ) ) ) {
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
                if ( valueUri.startsWith( "http://purl.obolibrary.org/obo/NCBITaxon_" ) ) {
                    continue;
                }
                records.add( new Rec( synonym, value, valueUri, category, categoryUri ) );
            }
        }

        Set<String> seen = new HashSet<>();
        int failures = 0;
        for ( int i = 0; i < ontologies.size(); i++ ) {
            OntologyService os = cs.take().get();
            log.info( "Looking up terms in " + os );
            for ( Rec rec : records ) {
                // skip undeclared terms
                OntologyTerm term = os.getTerm( rec.valueUri );
                if ( term == null ) {
                    continue;
                }
                // skip terms lacking a label
                if ( term.getAnnotations().stream().map( AnnotationProperty::getProperty ).noneMatch( "label"::equals ) ) {
                    continue;
                }
                // CLO has a typo, ignore it
                if ( "neuronal stem cell".equals( term.getLabel() ) ) {
                    continue;
                }
                seen.add( rec.synonym );
                if ( !term.getLabel().equals( rec.value ) ) {
                    log.warn( String.format( "%s: Replace '%s' with '%s' %s", rec.synonym, rec.value, term.getLabel(), term.getUri() ) );
                    failures++;
                }
            }
        }

        for ( Rec rec : records ) {
            if ( !seen.contains( rec.synonym ) ) {
                log.warn( String.format( "%s: No term found for %s in any ontology", rec.synonym, rec.valueUri ) );
                failures++;
            }
        }

        assertEquals( 0, failures );
    }
}