package ubic.gemma.persistence.util;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericValueObjectConverterTest {

    private final ConfigurableConversionService converter = new GenericConversionService();

    @Before
    public void setUp() {
        converter.addConverter( new GenericValueObjectConverter<>( DatabaseEntryValueObject::new, DatabaseEntry.class, DatabaseEntryValueObject.class ) );
    }

    @Test
    public void test() {
        Object converted = converter.convert( new DatabaseEntry(), DatabaseEntryValueObject.class );
        assertThat( converted ).isInstanceOf( DatabaseEntryValueObject.class );
    }

    @Test
    public void testConvertToSuperClass() {
        Object converted = converter.convert( new DatabaseEntry(), IdentifiableValueObject.class );
        assertThat( converted ).isInstanceOf( DatabaseEntryValueObject.class );
    }

    @Test
    public void testConvertFromSubClass() {
        Object converted = converter.convert( new SpecificDatabaseEntry(), DatabaseEntryValueObject.class );
        assertThat( converted ).isInstanceOf( DatabaseEntryValueObject.class );
    }

    private static class SpecificDatabaseEntry extends DatabaseEntry {

    }

    @Test
    public void testConvertCollection() {
        Object converted = converter.convert( Collections.singleton( new DatabaseEntry() ), List.class );
        assertThat( converted ).isInstanceOf( List.class );
    }

    @Test
    public void testConvertCollectionToListSuperType() {
        Object converted = converter.convert( Collections.singleton( new DatabaseEntry() ), Collection.class );
        assertThat( converted ).isInstanceOf( List.class );
    }

    @Test(expected = ConverterNotFoundException.class)
    public void testConvertUnsupportedType() {
        converter.convert( new ArrayDesign(), ArrayDesignValueObject.class );
    }

    @Test
    public void testConvertNull() {
        assertThat( converter.convert( null, DatabaseEntryValueObject.class ) ).isNull();
    }
}