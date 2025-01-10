package ubic.gemma.core.analysis.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration
public class ExpressionMetadataChangelogFileServiceTest extends BaseTest {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public static TestPropertyPlaceholderConfigurer propertyPlaceholderConfigurer() throws IOException {
            Path gemmaAppdataHome = Files.createTempDirectory( "gemmatmp" );
            return new TestPropertyPlaceholderConfigurer( "gemma.appdata.home=" + gemmaAppdataHome );
        }

        @Bean
        public ConversionService conversionService() {
            DefaultFormattingConversionService service = new DefaultFormattingConversionService();
            service.addConverter( String.class, Path.class, source -> Paths.get( ( String ) source ) );
            return service;
        }

        @Bean
        public ExpressionMetadataChangelogFileService expressionChangelogFileService() {
            return new ExpressionMetadataChangelogFileServiceImpl();
        }

        @Bean
        public UserManager userManager() {
            return mock();
        }
    }

    @Autowired
    private ExpressionMetadataChangelogFileService expressionMetadataChangelogFileService;

    @Autowired
    private UserManager userManager;

    @Test
    public void test() throws IOException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "GSE1" );
        assertThat( expressionMetadataChangelogFileService.readChangelog( ee ) ).isEmpty();

        User author = new User();
        author.setName( "Admin" );
        author.setEmail( "admin@gemma.msl.ubc.ca" );
        when( userManager.getCurrentUser() ).thenReturn( author );

        expressionMetadataChangelogFileService.addChangelogEntry( ee, "test\ntest2", LocalDate.of( 2025, 1, 6 ) );

        expressionMetadataChangelogFileService.addChangelogEntry( ee, "test2\ntest3", LocalDate.of( 2025, 1, 6 ) );

        assertThat( expressionMetadataChangelogFileService.readChangelog( ee ) )
                .isEqualTo( "2025-01-06  Admin  <admin@gemma.msl.ubc.ca>\n\n\ttest\n\ttest2\n\n2025-01-06  Admin  <admin@gemma.msl.ubc.ca>\n\n\ttest2\n\ttest3\n" );
    }
}