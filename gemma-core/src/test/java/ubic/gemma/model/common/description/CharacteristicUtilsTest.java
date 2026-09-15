package ubic.gemma.model.common.description;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.association.GOEvidenceCode;
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

    /**
     * Identity is the content, not the Java type. This is what makes upgrading experiment tags to
     * statements safe: during the upgrade one side of a comparison carries whichever form the row or
     * the caller happens to have, and treating those as different would make {@code addAnnotation} stop
     * rejecting duplicates and {@code updateAnnotations} drop and re-add the whole set.
     */
    @Test
    public void testSameTagComparesContentNotType() {
        Characteristic plain = createCharacteristic( "treatment", null, "high-fat diet", null );

        // a Statement carrying no predicate/object is the SAME tag as the plain characteristic:
        // byte-identical in storage apart from the discriminator
        Statement bare = createStatement( "treatment", "high-fat diet", null, null );
        assertTrue( sameTag( plain, bare ) );
        assertTrue( sameTag( bare, plain ) );

        // but a composed Statement is still a different tag, in both directions
        Statement composed = createStatement( "treatment", "high-fat diet", "for", "12 weeks" );
        assertFalse( sameTag( plain, composed ) );
        assertFalse( sameTag( composed, plain ) );
        assertFalse( sameTag( bare, composed ) );
        assertFalse( sameTag( composed, bare ) );
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

    /**
     * The converter must carry every field {@link Characteristic} declares, not just category and
     * value. {@code Statement.Factory.newInstance( Characteristic )} copies only those two, so using it
     * here would silently drop the evidence code, the supporting evidence and the original value — the
     * provenance, on exactly the write path that records provenance.
     */
    @Test
    public void testAsStatementCarriesEveryField() {
        Characteristic c = createCharacteristic( "treatment", "http://x/EFO_1", "aspirin", "http://x/CHEBI_1" );
        c.setEvidenceCode( GOEvidenceCode.IEA );
        c.setOriginalValue( "Aspirin (300mg)" );
        c.setSupportingEvidence( "[{\"quote\":\"aspirin was administered\"}]" );
        c.setDescription( "a description" );

        Statement s = CharacteristicUtils.asStatement( c );

        assertEquals( "treatment", s.getCategory() );
        assertEquals( "http://x/EFO_1", s.getCategoryUri() );
        assertEquals( "aspirin", s.getSubject() );
        assertEquals( "http://x/CHEBI_1", s.getSubjectUri() );
        assertEquals( GOEvidenceCode.IEA, s.getEvidenceCode() );
        assertEquals( "Aspirin (300mg)", s.getOriginalValue() );
        assertEquals( "[{\"quote\":\"aspirin was administered\"}]", s.getSupportingEvidence() );
        assertEquals( "a description", s.getDescription() );
        // and it is the same tag as what it was converted from, so no diff sees a change
        assertTrue( sameTag( c, s ) );
    }

    /** An already-persistent Statement must keep its identity, not be replaced by a copy. */
    @Test
    public void testAsStatementReturnsAStatementUnchanged() {
        Statement s = createStatement( "treatment", "high-fat diet", "for", "12 weeks" );
        assertSame( s, CharacteristicUtils.asStatement( s ) );
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