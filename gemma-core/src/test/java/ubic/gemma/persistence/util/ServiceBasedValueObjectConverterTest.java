package ubic.gemma.persistence.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class ServiceBasedValueObjectConverterTest extends BaseTest {

    @Configuration
    @TestComponent
    public static class VoConverterTestContextConfiguration {
        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock( ExpressionExperimentService.class );
        }
    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    /* fixtures */
    private ConfigurableConversionService conversionService;

    private ExpressionExperiment ee;

    @Before
    public void setUp() {
        ee = new ExpressionExperiment();
        ee.setId( 1L );
        conversionService = new GenericConversionService();
        conversionService.addConverter( new ServiceBasedValueObjectConverter<>( expressionExperimentService, ExpressionExperiment.class, ExpressionExperimentValueObject.class ) );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        //noinspection unchecked
        when( expressionExperimentService.load( anyCollection() ) )
                .thenAnswer( arg -> ( ( Collection<Long> ) arg.getArgument( 0, Collection.class ) ).stream().map( expressionExperimentService::load ).collect( Collectors.toList() ) );
        when( expressionExperimentService.loadValueObject( any( ExpressionExperiment.class ) ) )
                .thenAnswer( arg -> new ExpressionExperimentValueObject( arg.getArgument( 0, ExpressionExperiment.class ) ) );
        when( expressionExperimentService.loadValueObjectById( 1L ) )
                .thenAnswer( arg -> new ExpressionExperimentValueObject() );
        //noinspection unchecked
        when( expressionExperimentService.loadValueObjects( anyCollection() ) )
                .thenAnswer( arg -> ( ( Collection<ExpressionExperiment> ) arg.getArgument( 0, Collection.class ) ).stream().map( expressionExperimentService::loadValueObject ).collect( Collectors.toList() ) );
        //noinspection unchecked
        when( expressionExperimentService.loadValueObjectsByIds( anyCollection() ) )
                .thenAnswer( arg -> ( ( Collection<Long> ) arg.getArgument( 0, Collection.class ) ).stream().map( expressionExperimentService::loadValueObjectById ).collect( Collectors.toList() ) );
        ;
    }

    @After
    public void tearDown() {
        reset( expressionExperimentService );
    }

    @Test
    public void testConvertEntityFromId() {
        Object converted = conversionService.convert( ee.getId(), ExpressionExperiment.class );
        assertThat( converted ).isNotNull()
                .isInstanceOf( ExpressionExperiment.class );
        verify( expressionExperimentService ).load( ee.getId() );
    }

    @Test
    public void testConvertEntitiesFromIds() {
        Object converted = conversionService.convert( Collections.singleton( ee.getId() ), TypeDescriptor.collection( Collection.class, TypeDescriptor.valueOf( Long.class ) ), TypeDescriptor.collection( List.class, TypeDescriptor.valueOf( ExpressionExperiment.class ) ) );
        assertThat( converted ).isNotNull()
                .isInstanceOf( List.class );
        verify( expressionExperimentService ).load( Collections.singleton( ee.getId() ) );
    }

    @Test
    @WithMockUser
    public void testConvertSingleEntity() {
        Object converted = conversionService.convert( ee, ExpressionExperimentValueObject.class );
        assertThat( converted )
                .isNotNull()
                .isInstanceOf( ExpressionExperimentValueObject.class );
        verify( expressionExperimentService ).loadValueObject( ee );
    }

    @Test
    public void testConvertSingleEntityById() {
        Object converted = conversionService.convert( ee.getId(), ExpressionExperimentValueObject.class );
        assertThat( converted )
                .isNotNull()
                .isInstanceOf( ExpressionExperimentValueObject.class );
        verify( expressionExperimentService ).loadValueObjectById( ee.getId() );
    }

    @Test
    @WithMockUser
    public void testConvertSingleEntityToSuperType() {
        Object converted = conversionService.convert( ee, IdentifiableValueObject.class );
        assertThat( converted )
                .isNotNull()
                .isInstanceOf( ExpressionExperimentValueObject.class );
        verify( expressionExperimentService ).loadValueObject( ee );
    }

    @Test
    @WithMockUser
    public void testConvertSingleEntityFromSubType() {
        SpecificExpressionExperiment see = new SpecificExpressionExperiment();
        Object converted = conversionService.convert( see, IdentifiableValueObject.class );
        assertThat( converted )
                .isNotNull()
                .isInstanceOf( ExpressionExperimentValueObject.class );
        verify( expressionExperimentService ).loadValueObject( see );
    }

    private static class SpecificExpressionExperiment extends ExpressionExperiment {
    }

    @Test
    @WithMockUser
    public void testConvertCollection() {
        Collection<ExpressionExperiment> ees = Collections.singleton( ee );
        Object converted = conversionService.convert( ees,
                TypeDescriptor.collection( Collection.class, TypeDescriptor.valueOf( ExpressionExperiment.class ) ),
                TypeDescriptor.collection( List.class, TypeDescriptor.valueOf( ExpressionExperimentValueObject.class ) ) );
        assertThat( converted ).isInstanceOf( List.class );
        verify( expressionExperimentService ).loadValueObjects( ees );
    }

    @Test
    @WithMockUser
    public void testConvertCollectionToListSuperType() {
        Collection<ExpressionExperiment> ees = Collections.singleton( ee );
        Object converted = conversionService.convert( ees, Collection.class );
        assertThat( converted ).isInstanceOf( List.class );
        verify( expressionExperimentService ).loadValueObjects( ees );
    }

    @Test
    public void testConvertCollectionOfIds() {
        Collection<Long> ees = Collections.singleton( ee.getId() );
        Object converted = conversionService.convert( ees,
                TypeDescriptor.collection( Collection.class, TypeDescriptor.valueOf( Long.class ) ),
                TypeDescriptor.collection( List.class, TypeDescriptor.valueOf( ExpressionExperimentValueObject.class ) ) );
        assertThat( converted ).isInstanceOf( List.class );
        verify( expressionExperimentService ).loadValueObjectsByIds( ees );
    }

    @Test
    @WithMockUser
    public void testConvertCollectionToSuperType() {
        Collection<ExpressionExperiment> ees = Collections.singleton( ee );
        Object converted = conversionService.convert( ees,
                TypeDescriptor.collection( Collection.class, TypeDescriptor.valueOf( ExpressionExperiment.class ) ),
                TypeDescriptor.collection( List.class, TypeDescriptor.valueOf( IdentifiableValueObject.class ) ) );
        assertThat( converted ).isInstanceOf( List.class );
        verify( expressionExperimentService ).loadValueObjects( ees );
    }

    @Test
    public void testConvertNullEntity() {
        Object converted = conversionService.convert( null, ExpressionExperimentValueObject.class );
        assertThat( converted ).isNull();
        verifyNoInteractions( expressionExperimentService );
    }

    @Test(expected = ConverterNotFoundException.class)
    public void testConvertUnknownType() {
        conversionService.convert( new ArrayDesign(), TypeDescriptor.valueOf( ArrayDesign.class ), TypeDescriptor.valueOf( ArrayDesignValueObject.class ) );
    }

}