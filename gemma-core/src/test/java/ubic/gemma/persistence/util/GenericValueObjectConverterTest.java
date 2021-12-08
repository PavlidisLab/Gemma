package ubic.gemma.persistence.util;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GenericValueObjectConverterTest {

    private final ConfigurableConversionService converter = new GenericConversionService();

    @Before
    public void setUp() {
        converter.addConverter( new GenericValueObjectConverter<>( ExpressionExperimentValueObject::new, ExpressionExperiment.class, ExpressionExperimentValueObject.class ) );
    }

    @Test
    public void test() {
        Object converted = converter.convert( new ExpressionExperiment(), ExpressionExperimentValueObject.class );
        assertThat( converted ).isInstanceOf( ExpressionExperimentValueObject.class );
    }

    @Test
    public void testConvertToSuperClass() {
        Object converted = converter.convert( new ExpressionExperiment(), IdentifiableValueObject.class );
        assertThat( converted ).isInstanceOf( ExpressionExperimentValueObject.class );
    }

    @Test
    public void testConvertFromSubClass() {
        Object converted = converter.convert( new SpecificExpressionExperiment(), ExpressionExperimentValueObject.class );
        assertThat( converted ).isInstanceOf( ExpressionExperimentValueObject.class );
    }

    private static class SpecificExpressionExperiment extends ExpressionExperiment {

    }

    @Test
    public void testConvertCollection() {
        Object converted = converter.convert( Collections.singleton( new ExpressionExperiment() ), List.class );
        assertThat( converted ).isInstanceOf( List.class );
    }

    @Test
    public void testConvertCollectionToListSuperType() {
        Object converted = converter.convert( Collections.singleton( new ExpressionExperiment() ), Collection.class );
        assertThat( converted ).isInstanceOf( List.class );
    }

    @Test(expected = ConverterNotFoundException.class)
    public void testConvertUnsupportedType() {
        converter.convert( new ArrayDesign(), ArrayDesignValueObject.class );
    }

    @Test
    public void testConvertNull() {
        assertThat( converter.convert( null, ExpressionExperimentValueObject.class ) ).isNull();
    }
}