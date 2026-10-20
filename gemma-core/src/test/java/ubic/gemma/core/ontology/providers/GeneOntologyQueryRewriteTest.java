package ubic.gemma.core.ontology.providers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /**
     * The defect oganm reported on gemma-ui PR #11: only AND was stripped, so an infix OR survived
     * the rejoin as an OPERAND — `tumour OR normal` came out `tumour AND OR AND normal`, which does
     * not parse. `/annotations/search` then answered 400 for a legal query that should simply have
     * found nothing.
     */
    @Test
    public void testInfixOrIsASeparatorAndNeverBecomesAnOperand() {
        assertEquals( "tumour AND normal", GeneOntologyServiceImpl.requireAllTerms( "tumour OR normal" ) );
        assertEquals( "zqx AND zqy", GeneOntologyServiceImpl.requireAllTerms( "zqx OR zqy" ) );
        assertEquals( "liver AND brain", GeneOntologyServiceImpl.requireAllTerms( "liver OR brain" ) );
    }

    /**
     * A DANGLING operator is left alone on purpose. It is malformed input, it still fails to parse,
     * and making it search is a separate behaviour change that was declined — so the separator match
     * requires whitespace on both sides.
     */
    @Test
    public void testDanglingOperatorIsNotTreatedAsASeparator() {
        assertEquals( "cell AND OR", GeneOntologyServiceImpl.requireAllTerms( "cell OR" ) );
        assertEquals( "OR AND cell", GeneOntologyServiceImpl.requireAllTerms( "OR cell" ) );
    }

    /**
     * Lucene treats only UPPERCASE as operators, so lowercase is an ordinary token and must survive
     * as one — `cell or` is a search for two words, and it answers 200 today.
     */
    @Test
    public void testLowercaseKeywordsAreOrdinaryTokens() {
        assertEquals( "cell AND or", GeneOntologyServiceImpl.requireAllTerms( "cell or" ) );
        assertEquals( "cell AND not AND neuron", GeneOntologyServiceImpl.requireAllTerms( "cell not neuron" ) );
    }

    /**
     * NOT is NOT a separator: stripping it would invert the query into a match for the very thing
     * the caller excluded. GO requires every token by construction and cannot express an exclusion,
     * so the fallback sits the query out.
     */
    @Test
    public void testInfixNotMakesTheFallbackSitTheQueryOut() {
        assertTrue( GeneOntologyServiceImpl.excludesTerms( "tumour NOT normal" ) );
        assertTrue( GeneOntologyServiceImpl.excludesTerms( "  a   NOT   b  " ) );
        assertFalse( GeneOntologyServiceImpl.excludesTerms( "cell not neuron" ) );
        assertFalse( GeneOntologyServiceImpl.excludesTerms( "tumour OR normal" ) );
        assertFalse( GeneOntologyServiceImpl.excludesTerms( "NOT" ) );
        assertFalse( GeneOntologyServiceImpl.excludesTerms( null ) );
    }
}
