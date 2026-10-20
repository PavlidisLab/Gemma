package ubic.gemma.core.ontology.lexical;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.model.OntologyResource;
import ubic.gemma.core.ontology.model.OntologyTerm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class LexicalOntologyTermTest {

    private static final String CVCL = "https://www.cellosaurus.org/CVCL_1108";

    /**
     * The whole point of this subclass: a consumer may walk a returned term, and a flat vocabulary
     * answers "nothing" rather than refusing to answer. Swept reflectively rather than method by
     * method, because the failure mode is an accessor that was never overridden — enumerating them
     * by hand is exactly what missed {@code getAnnotations(uri)} / {@code getAnnotation(uri)} and
     * put a 500 on {@code /annotations/search} for every Cellosaurus and MGI hit.
     */
    @Test
    void noAccessorInheritsTheUnsupportedOperationThrow() {
        LexicalOntologyTerm term = new LexicalOntologyTerm( CVCL, "Cal-33" );
        List<String> offenders = new ArrayList<>();
        for ( Class<?> iface : new Class<?>[] { OntologyTerm.class, OntologyResource.class } ) {
            for ( Method m : iface.getMethods() ) {
                Object[] args = new Object[m.getParameterCount()];
                for ( int i = 0; i < args.length; i++ ) {
                    Class<?> p = m.getParameterTypes()[i];
                    if ( p == boolean.class ) {
                        args[i] = Boolean.FALSE;
                    } else if ( p == String.class ) {
                        args[i] = "http://purl.obolibrary.org/obo/IAO_0000115";
                    } else {
                        // A signature this sweep can't call; extend the arg table rather than
                        // letting the method go unchecked.
                        fail( "cannot synthesise an argument of type " + p + " for " + m );
                    }
                }
                try {
                    m.invoke( term, args );
                } catch ( InvocationTargetException e ) {
                    if ( e.getCause() instanceof UnsupportedOperationException ) {
                        offenders.add( m.getName() + "/" + m.getParameterCount() );
                    } else {
                        throw new AssertionError( "unexpected failure from " + m, e.getCause() );
                    }
                } catch ( IllegalAccessException e ) {
                    throw new AssertionError( e );
                }
            }
        }
        assertEquals( List.of(), offenders,
                "these accessors still throw UnsupportedOperationException on a flat lexical term" );
    }

    @Test
    void predicateTargetedAnnotationLookupsAnswerEmpty() {
        LexicalOntologyTerm term = new LexicalOntologyTerm( CVCL, "Cal-33" );
        // getDefinition probes IAO_0000115; annotation-search's attribution probes the OBO synonym
        // predicates and hasDbXref. All arrive through these two overloads.
        assertNull( term.getAnnotation( "http://purl.obolibrary.org/obo/IAO_0000115" ) );
        assertTrue( term.getAnnotations( "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym" ).isEmpty() );
        assertTrue( term.getAnnotations( "http://www.geneontology.org/formats/oboInOwl#hasDbXref" ).isEmpty() );
        assertTrue( term.getAnnotations().isEmpty() );
    }

    @Test
    void aFlatTermHasNoHierarchyAndIsItsOwnRoot() {
        LexicalOntologyTerm term = new LexicalOntologyTerm( CVCL, "Cal-33" );
        assertTrue( term.getParents( true, true, false ).isEmpty() );
        assertTrue( term.getChildren( true, true, false ).isEmpty() );
        assertTrue( term.isRoot() );
        assertEquals( "Cal-33", term.getLabel() );
        assertEquals( CVCL, term.getUri() );
    }
}
