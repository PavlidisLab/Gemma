package ubic.gemma.model.common.description;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.expression.experiment.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static ubic.gemma.model.common.description.CharacteristicUtils.*;

public class CharacteristicUtilsTest {

    @Test
    public void testUncategorized() {
        assertTrue( isUncategorized( createCharacteristic( null, null, null, null ) ) );
        assertFalse( isUncategorized( createCharacteristic( "a", null, null, null ) ) );
    }

    @Test
    public void testIsFreeTextCategory() {
        assertFalse( isFreeTextCategory( createCharacteristic( null, null, null, null ) ) );
        assertTrue( isFreeTextCategory( createCharacteristic( "a", null, null, null ) ) );
    }

    @Test
    public void testIsFreeText() {
        assertTrue( isFreeText( createCharacteristic( null, null, "foo", null ) ) );
        assertFalse( isFreeText( createCharacteristic( null, null, "foo", "bar" ) ) );
    }

    @Test
    public void testEquals() {
        assertTrue( CharacteristicUtils.equals( "a", "b", "a", "b" ) );
        assertTrue( CharacteristicUtils.equals( null, "b", "c", "b" ) );
        assertFalse( CharacteristicUtils.equals( null, "b", "c", "c" ) );
        assertTrue( CharacteristicUtils.equals( "A", null, "a", null ) );
        assertTrue( CharacteristicUtils.equals( null, null, null, null ) );
    }

    @Test
    public void testCompareTerm() {
        // terms with identical URIs are collapsed
        assertEquals( 0, CharacteristicUtils.compareTerm( "a", "test", "b", "test" ) );
        // terms with different URIs are compared by label
        assertEquals( -1, CharacteristicUtils.compareTerm( "a", "test", "b", "bar" ) );
        assertEquals( 1, CharacteristicUtils.compareTerm( "b", "test", "a", "bar" ) );
    }

    @Test
    public void testSameTagPlain() {
        // same (category, value) → same tag (case-insensitive)
        assertTrue( sameTag( createCharacteristic( "treatment", null, "aspirin", null ),
                createCharacteristic( "treatment", null, "Aspirin", null ) ) );
        // different value → not the same tag
        assertFalse( sameTag( createCharacteristic( "treatment", null, "aspirin", null ),
                createCharacteristic( "treatment", null, "ibuprofen", null ) ) );
    }

    @Test
    public void testSameTagPlainVsStatementNeverEqual() {
        // a plain Characteristic and a Statement with the same (category, value) are NOT the same tag —
        // a wire-shape change must round-trip as drop+add, not a no-op
        Characteristic plain = createCharacteristic( "treatment", null, "high-fat diet", null );
        Statement stmt = createStatement( "treatment", "high-fat diet", "for", "12 weeks" );
        assertFalse( sameTag( plain, stmt ) );
        assertFalse( sameTag( stmt, plain ) );
    }

    @Test
    public void testSameTagStatementComparesPredicateAndObject() {
        Statement base = createStatement( "treatment", "high-fat diet", "for", "12 weeks" );
        // identical subject + predicate + object → same
        assertTrue( sameTag( base, createStatement( "treatment", "high-fat diet", "for", "12 weeks" ) ) );
        // different predicate → not the same
        assertFalse( sameTag( base, createStatement( "treatment", "high-fat diet", "has dose", "12 weeks" ) ) );
        // different object → not the same
        assertFalse( sameTag( base, createStatement( "treatment", "high-fat diet", "for", "6 weeks" ) ) );
    }

    @Test
    public void testSameTagStatementComparesSecondPair() {
        Statement base = createStatement( "treatment", "high-fat diet", "for", "12 weeks" );
        base.setSecondPredicate( "at dose" );
        base.setSecondObject( "30%" );
        Statement sameSecond = createStatement( "treatment", "high-fat diet", "for", "12 weeks" );
        sameSecond.setSecondPredicate( "at dose" );
        sameSecond.setSecondObject( "30%" );
        assertTrue( sameTag( base, sameSecond ) );
        Statement diffSecond = createStatement( "treatment", "high-fat diet", "for", "12 weeks" );
        diffSecond.setSecondPredicate( "at dose" );
        diffSecond.setSecondObject( "50%" );
        assertFalse( sameTag( base, diffSecond ) );
    }

    private Statement createStatement( String category, String subject, String predicate, String object ) {
        Statement s = new Statement();
        s.setCategory( category );
        s.setSubject( subject );
        s.setPredicate( predicate );
        s.setObject( object );
        return s;
    }

    private Characteristic createCharacteristic( String category, String categoryUri, String value, String valueUri ) {
        Characteristic c = new Characteristic();
        c.setCategory( category );
        c.setCategoryUri( categoryUri );
        c.setValue( value );
        c.setValueUri( valueUri );
        return c;
    }

}