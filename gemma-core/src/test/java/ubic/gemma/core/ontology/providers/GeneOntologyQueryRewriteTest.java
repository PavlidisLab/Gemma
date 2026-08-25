package ubic.gemma.core.ontology.providers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards {@link GeneOntologyServiceImpl#requireAllTerms(String)}, the rewrite that makes the GO
 * fallback require every token instead of the parser's default OR.
 * <p>
 * Plain unit test on purpose: {@code GeneOntologyServiceTest} is {@code @Tag("slow")} and needs a
 * real GO index, so a guard placed there would not run in {@code mvn verify}.
 */
public class GeneOntologyQueryRewriteTest {

    /**
     * The defect: the separator replacement yielded the empty string, so the operands were welded
     * into one token and the rejoin pass had nothing left to split on. GO then searched
     * `cellneuron`, which cannot match — silently, with no error.
     */
    @Test
    public void testExistingAndSeparatorsDoNotWeldOperands() {
        assertEquals( "cell AND neuron", GeneOntologyServiceImpl.requireAllTerms( "cell AND neuron" ) );
        assertEquals( "a AND b AND c", GeneOntologyServiceImpl.requireAllTerms( "a AND b AND c" ) );
    }

    @Test
    public void testWhitespaceSeparatedTokensAreJoinedOnAnd() {
        assertEquals( "synaptic AND vesicle", GeneOntologyServiceImpl.requireAllTerms( "synaptic vesicle" ) );
        assertEquals( "cell", GeneOntologyServiceImpl.requireAllTerms( "cell" ) );
    }

    /**
     * Idempotent, and surrounding/repeated whitespace collapses — the rewrite runs on whatever the
     * caller sent, including a query that already reads as an AND chain.
     */
    @Test
    public void testRewriteIsIdempotentAndCollapsesWhitespace() {
        String once = GeneOntologyServiceImpl.requireAllTerms( "  cell   AND   neuron  " );
        assertEquals( "cell AND neuron", once );
        assertEquals( once, GeneOntologyServiceImpl.requireAllTerms( once ) );
    }
}
