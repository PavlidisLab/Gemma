package ubic.gemma.core.analysis.expression.diff;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;

import ubic.gemma.model.expression.biomaterial.BioMaterial;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BaselineSelectionTest {

    /**
     * A factor none of the given samples carries has no baseline to pick, and getBaselineConditions says so by
     * throwing. The caller therefore has to hand it the factors the samples actually use --
     * {@code LinearModelAnalyzer}'s subset path computes exactly that list with {@code fixFactorsForSubset} and
     * used to ask for baselines with the unfiltered one. On GSE198008.1 that killed the run: `treatment` applies
     * to two of the four `collection of material` subsets (32/24 and 11/11) and to neither of the other two
     * (15/0 and 15/0).
     */
    @Test
    public void testGetBaselineConditionsRefusesAFactorTheSamplesDoNotCarry() {
        ExperimentalFactor carried = ExperimentalFactor.Factory.newInstance( "cell type", FactorType.CATEGORICAL );
        FactorValue fibroblast = FactorValue.Factory.newInstance( carried );
        fibroblast.getCharacteristics().add( createStatement( "fibroblast", null ) );
        carried.getFactorValues().add( fibroblast );

        ExperimentalFactor absent = ExperimentalFactor.Factory.newInstance( "treatment", FactorType.CATEGORICAL );
        FactorValue treated = FactorValue.Factory.newInstance( absent );
        treated.getCharacteristics().add( createStatement( "treated", null ) );
        absent.getFactorValues().add( treated );

        BioMaterial sample = BioMaterial.Factory.newInstance( "sample-1" );
        sample.getFactorValues().add( fibroblast );
        List<BioMaterial> samples = Collections.singletonList( sample );

        assertThatThrownBy( () -> BaselineSelection.getBaselineConditions( samples, List.of( carried, absent ) ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "None of the samplesUsed have a value for factor" );

        // the same call with only the factors the samples use is what the subset path must make
        assertThatCode( () -> BaselineSelection.getBaselineConditions( samples, Collections.singletonList( carried ) ) )
                .doesNotThrowAnyException();
    }

    @Test
    public void testBaseline() {
        FactorValue fv = new FactorValue();
        assertFalse( BaselineSelection.isBaselineCondition( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "control", "http://www.ebi.ac.uk/efo/EFO_0001461" ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "control", null ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "CONTROL", null ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "  control    ", null ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "  initial  time point", null ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "initial_time_point", null ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );

        // a "control" term is used, but it's not a control term URI
        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "control", "http://www.ebi.ac.uk/efo/EFO_0001462" ) );
        assertFalse( BaselineSelection.isBaselineCondition( fv ) );

        fv = new FactorValue();
        fv.setMeasurement( new Measurement() );
        fv.getCharacteristics().add( createStatement( "control", "http://www.ebi.ac.uk/efo/EFO_0001461" ) );
        assertFalse( BaselineSelection.isBaselineCondition( fv ) );
    }

    @Test
    public void testForcedBaseline() {
        FactorValue fv = new FactorValue();
        fv.setIsBaseline( true );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );
        assertTrue( BaselineSelection.isForcedBaseline( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "control", "http://www.ebi.ac.uk/efo/EFO_0001461" ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );
        assertTrue( BaselineSelection.isForcedBaseline( fv ) );

        fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "control", "http://www.ebi.ac.uk/EfO/efo_0001461" ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );
        assertTrue( BaselineSelection.isForcedBaseline( fv ) );

        fv = new FactorValue();
        fv.setIsBaseline( false );
        fv.getCharacteristics().add( createStatement( "control", "http://www.ebi.ac.uk/efo/EFO_0001461" ) );
        assertFalse( BaselineSelection.isBaselineCondition( fv ) );
        assertFalse( BaselineSelection.isForcedBaseline( fv ) );
    }

    /**
     * The curation guide's "do not use, Gemma won't pick them up" list. Detection is deliberately wider
     * than the guide: these plainly say "this is the control", so they get picked up rather than losing
     * the baseline to an arbitrary choice.
     */
    @Test
    public void testTermsDiscouragedByTheCurationGuideAreStillDetected() {
        for ( String term : new String[] { "Baseline participant role", "Control group", "Control role",
                "Normal control group", "Negative control role", "Normal littermates", "normal littermate" } ) {
            FactorValue fv = new FactorValue();
            fv.getCharacteristics().add( createStatement( term, null ) );
            assertTrue( BaselineSelection.isBaselineCondition( fv ), term + " should be detected as a baseline" );
        }
        // of these, only "baseline participant role" has a URI in an ontology Gemma loads; the rest are
        // free text, matched above.
        FactorValue fv = new FactorValue();
        fv.getCharacteristics().add( createStatement( "baseline participant role", "http://purl.obolibrary.org/obo/OBI_0000143" ) );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );
    }

    /**
     * An explicit isBaseline decides on its own, whatever else the factor value carries.
     */
    @Test
    public void testExplicitIsBaselineAlwaysDecides() {
        FactorValue fv = new FactorValue();
        fv.setIsBaseline( true );
        fv.setMeasurement( new Measurement() );
        assertTrue( BaselineSelection.isBaselineCondition( fv ) );
        assertTrue( BaselineSelection.isForcedBaseline( fv ) );

        // ... and it also decides in the negative, against a term that would otherwise match
        fv = new FactorValue();
        fv.setIsBaseline( false );
        fv.getCharacteristics().add( createStatement( "control", "http://www.ebi.ac.uk/efo/EFO_0001461" ) );
        assertFalse( BaselineSelection.isBaselineCondition( fv ) );
        assertFalse( BaselineSelection.isForcedBaseline( fv ) );
    }

    /**
     * A marked factor value wins the baseline for its factor even when another value matches a control
     * term, regardless of which of the two the factor happens to iterate first.
     */
    @Test
    public void testMarkedFactorValueWinsOverATermMatch() {
        for ( boolean markedFirst : new boolean[] { true, false } ) {
            ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance( "treatment", FactorType.CATEGORICAL );
            FactorValue marked = new FactorValue();
            marked.setExperimentalFactor( ef );
            marked.setIsBaseline( true );
            marked.getCharacteristics().add( createStatement( "some untagged treatment", null ) );
            FactorValue termMatch = new FactorValue();
            termMatch.setExperimentalFactor( ef );
            termMatch.getCharacteristics().add( createStatement( "control", "http://www.ebi.ac.uk/efo/EFO_0001461" ) );
            // LinkedHashSet so the iteration order under test is the one we set up
            ef.setFactorValues( new java.util.LinkedHashSet<>( markedFirst
                    ? java.util.Arrays.asList( marked, termMatch )
                    : java.util.Arrays.asList( termMatch, marked ) ) );

            assertSame( marked, BaselineSelection.getBaselineLevels( java.util.Collections.singleton( ef ) ).get( ef ) );
        }
    }

    private Statement createStatement( String subject, String subjectUri ) {
        Statement s = new Statement();
        s.setSubject( subject );
        s.setSubjectUri( subjectUri );
        return s;
    }

    /**
     * More than one explicitly marked baseline on a factor is legitimate — a dataset holding two experiments has a
     * reference level per experiment. {@code getExplicitBaselines} is what lets the analyzer notice that and demand
     * a subset instead of silently taking whichever one it reached first.
     */
    @Test
    public void testGetExplicitBaselinesReturnsEveryMarkedValue() {
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setType( FactorType.CATEGORICAL );
        // Distinct ids matter: two blank transient FactorValues are equal, so a Set would collapse them and the
        // count would silently read 1. Same hashCode footgun the repo conventions warn about.
        FactorValue a = new FactorValue();
        a.setId( 1L );
        a.setIsBaseline( true );
        FactorValue b = new FactorValue();
        b.setId( 2L );
        b.setIsBaseline( true );
        FactorValue c = new FactorValue();
        c.setId( 3L );
        c.setIsBaseline( false );
        FactorValue d = new FactorValue(); // never marked either way
        d.setId( 4L );
        ef.getFactorValues().add( a );
        ef.getFactorValues().add( b );
        ef.getFactorValues().add( c );
        ef.getFactorValues().add( d );

        assertEquals( 2, BaselineSelection.getExplicitBaselines( ef, null ).size() );
    }

    /**
     * 🛑 Only the explicit flag counts. The inference finds two candidates on a great many factors — any design
     * with both a "control" and an "untreated" level — and treating those as an error would fail analyses that run
     * correctly today. Two INFERRED baselines is ambiguity to resolve; two MARKED ones is a curator's statement.
     */
    @Test
    public void testGetExplicitBaselinesIgnoresInferredOnes() {
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setType( FactorType.CATEGORICAL );
        FactorValue inferredA = new FactorValue();
        inferredA.setId( 1L );
        inferredA.getCharacteristics().add( createStatement( "control", null ) );
        FactorValue inferredB = new FactorValue();
        inferredB.setId( 2L );
        inferredB.getCharacteristics().add( createStatement( "initial time point", null ) );
        ef.getFactorValues().add( inferredA );
        ef.getFactorValues().add( inferredB );

        assertTrue( BaselineSelection.isBaselineCondition( inferredA ) );
        assertTrue( BaselineSelection.isBaselineCondition( inferredB ) );
        assertTrue( BaselineSelection.getExplicitBaselines( ef, null ).isEmpty(),
                "two inferred candidates are ambiguity, not a curator saying the design has two reference levels" );
    }
}
