package ubic.gemma.cli.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class EnumConverterTest {

    enum MyEnum {
        lower_case,
        UPPER_CASE,
        mixed_CASE,
        MIXED_case
    }

    @Test
    public void test() {
        EnumConverter<MyEnum> converter = EnumConverter.of( MyEnum.class );
        assertEquals( MyEnum.UPPER_CASE, converter.apply( "upper_case" ) );
        assertEquals( MyEnum.UPPER_CASE, converter.apply( "UPPER_CASE" ) );
        assertEquals( MyEnum.UPPER_CASE, converter.apply( "upper-case" ) );
        assertEquals( MyEnum.lower_case, converter.apply( "lower_case" ) );
        assertEquals( MyEnum.lower_case, converter.apply( "LOWER_CASE" ) );
        assertEquals( MyEnum.lower_case, converter.apply( "lower-case" ) );
        assertEquals( MyEnum.mixed_CASE, converter.apply( "mixed_CASE" ) );
        assertEquals( MyEnum.mixed_CASE, converter.apply( "mixed-CASE" ) );
        assertEquals( MyEnum.MIXED_case, converter.apply( "MIXED_case" ) );
        assertEquals( MyEnum.MIXED_case, converter.apply( "MIXED-case" ) );
        assertThrows( IllegalArgumentException.class, () -> converter.apply( "unknown-case" ) );
    }

}