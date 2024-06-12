package ubic.gemma.core.util;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.validation.AbstractBindingResult;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.ValueObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

public class StrictBeanDefinitionValidatorTest extends BaseSpringContextTest {

    @ValueObject
    public static class TestAnnotatedVo implements Serializable {

    }

    public static class TestImplicitValueObject implements Serializable {

    }

    @Autowired
    private MessageSource messageSource;

    @Test
    public void test() throws Exception {
        ClassPathScanningCandidateComponentProvider valueObjectProvider = new ClassPathScanningCandidateComponentProvider( false );
        valueObjectProvider.addIncludeFilter( new RegexPatternTypeFilter( Pattern.compile( ".+ValueObject$" ) ) );
        valueObjectProvider.addIncludeFilter( new AnnotationTypeFilter( ValueObject.class ) );
        Set<BeanDefinition> beanDefinitions = valueObjectProvider.findCandidateComponents( "ubic.gemma" );
        assertThat( beanDefinitions )
                .extracting( "beanClassName" )
                .contains( TestAnnotatedVo.class.getName(), TestImplicitValueObject.class.getName() );
        Map<BeanDefinition, Errors> invalidBeans = new HashMap<>();
        StrictBeanDefinitionValidator validator = new StrictBeanDefinitionValidator( true );
        for ( BeanDefinition beanDefinition : beanDefinitions ) {
            Errors errors = new BeanDefinitionBindingResult( beanDefinition );
            validator.validate( beanDefinition, errors );
            if ( errors.hasErrors() ) {
                invalidBeans.put( beanDefinition, errors );
            }
        }
        String invalidBeansSummary = invalidBeans.entrySet().stream()
                .map( e -> String.format( "\t%s: %s\n\t\t%s", e.getKey().getBeanClassName(),
                        e.getValue().getGlobalErrors().stream()
                                .map( f -> messageSource.getMessage( f, Locale.getDefault() ) )
                                .collect( Collectors.joining( ", " ) ),
                        e.getValue().getFieldErrors().stream()
                                .map( f -> f.getField() + " " + messageSource.getMessage( f, Locale.getDefault() ) )
                                .collect( Collectors.joining( "\n\t\t" ) ) ) )
                .collect( Collectors.joining( "\n\n" ) );
        assumeThat( invalidBeans )
                .withFailMessage( String.format( "The following %d value object beans are invalid:\n\n%s", invalidBeans.size(), invalidBeansSummary ) )
                .isEmpty();
    }

    /**
     * Similar to {@link BeanPropertyBindingResult}, but applies to the bean structure instead of content.
     * @author poirigui
     */
    private static class BeanDefinitionBindingResult extends AbstractBindingResult {

        public BeanDefinitionBindingResult( BeanDefinition beanDefinition ) {
            super( beanDefinition.getBeanClassName() );
        }

        @Override
        public Object getTarget() {
            return null;
        }

        @Override
        protected Object getActualFieldValue( String field ) {
            return null;
        }
    }
}