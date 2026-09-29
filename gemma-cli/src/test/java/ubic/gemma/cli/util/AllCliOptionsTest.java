package ubic.gemma.cli.util;

import org.apache.commons.cli.Options;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;
import ubic.gemma.apps.InitializeDatabaseCli;
import ubic.gemma.apps.UpdateDatabaseCli;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Build the options of every CLI shipped in gemma-cli, the way {@link ubic.gemma.cli.main.GemmaCLI} does when it lists
 * commands, and check that none defines the same short or long option name twice.
 * <p>
 * A duplicate name is rejected by the {@link Options} that {@link AbstractCLI#getOptions()} hands to
 * {@code buildOptions}, so this fails for any CLI whose options clash, including one that reuses a name defined by its
 * base class.
 */
public class AllCliOptionsTest {

    /**
     * Packages scanned for CLIs by {@code CliComponentScanConfig}.
     */
    private static final String[] CLI_PACKAGES = { "ubic.gemma.apps", "ubic.gemma.cli", "ubic.gemma.contrib.apps" };

    @TestFactory
    public Stream<DynamicTest> testNoCliDefinesAnOptionTwice() {
        List<Class<?>> cliClasses = findShippedCliClasses();
        // guard against a scan that silently finds nothing, or skips the profile-gated CLIs
        assertThat( cliClasses )
                .hasSizeGreaterThan( 50 )
                .contains( InitializeDatabaseCli.class, UpdateDatabaseCli.class );
        GenericApplicationContext ctx = new GenericApplicationContext();
        ctx.refresh();
        return cliClasses.stream()
                .map( clazz -> DynamicTest.dynamicTest( clazz.getSimpleName(), () -> {
                    CLI cli = ( CLI ) BeanUtils.instantiateClass( clazz );
                    if ( cli instanceof ApplicationContextAware ) {
                        ( ( ApplicationContextAware ) cli ).setApplicationContext( ctx );
                    }
                    if ( cli instanceof EnvironmentAware ) {
                        ( ( EnvironmentAware ) cli ).setEnvironment( ctx.getEnvironment() );
                    }
                    if ( cli instanceof MessageSourceAware ) {
                        ( ( MessageSourceAware ) cli ).setMessageSource( ctx );
                    }
                    Options options = assertDoesNotThrow( cli::getOptions );
                    assertThat( options ).isNotNull();
                } ) );
    }

    /**
     * Concrete {@link CLI} classes from gemma-cli's own classes, leaving out the CLIs defined by tests.
     * <p>
     * Every profile is treated as active, so CLIs gated on one (e.g. {@code initializeDatabase}, which needs
     * {@code testdb}) are included.
     */
    private static List<Class<?>> findShippedCliClasses() {
        URL shippedLocation = CLI.class.getProtectionDomain().getCodeSource().getLocation();
        StandardEnvironment everyProfileActive = new StandardEnvironment() {
            @Override
            public boolean matchesProfiles( String... profileExpressions ) {
                return true;
            }

            @Override
            public boolean acceptsProfiles( Profiles profiles ) {
                return true;
            }
        };
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider( false, everyProfileActive );
        scanner.addIncludeFilter( new AssignableTypeFilter( CLI.class ) );
        return Stream.of( CLI_PACKAGES )
                .flatMap( pkg -> scanner.findCandidateComponents( pkg ).stream() )
                .map( BeanDefinition::getBeanClassName )
                .filter( Objects::nonNull )
                .distinct()
                .map( name -> ClassUtils.resolveClassName( name, AllCliOptionsTest.class.getClassLoader() ) )
                .filter( clazz -> shippedLocation.equals( clazz.getProtectionDomain().getCodeSource().getLocation() ) )
                .sorted( Comparator.comparing( Class::getName ) )
                .collect( Collectors.toList() );
    }
}
