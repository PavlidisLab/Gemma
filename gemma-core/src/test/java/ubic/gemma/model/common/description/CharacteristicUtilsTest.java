package ubic.gemma.model.common.description;

import org.junit.Test;

import static org.junit.Assert.*;
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
    }

    @Test
    public void testCompareTerm() {
        // terms with identical URIs are collapsed
        assertEquals( 0, CharacteristicUtils.compareTerm( "a", "test", "b", "test" ) );
        // terms with different URIs are compared by label
        assertEquals( -1, CharacteristicUtils.compareTerm( "a", "test", "b", "bar" ) );
        assertEquals( 1, CharacteristicUtils.compareTerm( "b", "test", "a", "bar" ) );
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