package ubic.gemma.core.apps;

import gemma.gsec.authentication.ManualAuthenticationService;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignMergeService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class ArrayDesignMergeCliTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    public static class ArrayDesignMergeCliTestContextConfiguration {

        @Bean
        public ManualAuthenticationService manualAuthenticationService() {
            return mock( ManualAuthenticationService.class );
        }

        @Bean
        public ArrayDesignMergeCli arrayDesignMergeCli() {
            return new ArrayDesignMergeCli();
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock( AuditTrailService.class );
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock( AuditEventService.class );
        }

        @Bean
        public ArrayDesignMergeService arrayDesignMergeService() {
            return mock( ArrayDesignMergeService.class );
        }

        @Bean
        public ArrayDesignReportService arrayDesignReportService() {
            return mock( ArrayDesignReportService.class );
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock( ArrayDesignService.class );
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock( ExpressionExperimentService.class );
        }

        @Bean
        public Persister persisterHelper() {
            return mock( Persister.class );
        }
    }

    @Autowired
    private ArrayDesignMergeCli arrayDesignMergeCli;

    @Autowired
    private ArrayDesignMergeService arrayDesignMergeService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @After
    public void tearDown() {
        reset( arrayDesignService );
    }

    @Test
    public void test() {
        ArrayDesign a = ArrayDesign.Factory.newInstance();
        ArrayDesign b = ArrayDesign.Factory.newInstance();
        ArrayDesign c = ArrayDesign.Factory.newInstance();
        when( arrayDesignService.findByShortName( "1" ) ).thenReturn( a );
        when( arrayDesignService.findByShortName( "2" ) ).thenReturn( b );
        when( arrayDesignService.findByShortName( "3" ) ).thenReturn( c );
        when( arrayDesignService.thaw( any() ) ).thenAnswer( args -> args.getArgument( 0 ) );
        Collection<ArrayDesign> otherPlatforms = new HashSet<>( Arrays.asList( b, c ) );
        assertThat( arrayDesignMergeCli.executeCommand( new String[] { "-a", "1", "-o", "2,3", "-s", "4", "-n", "four is better than one" } ) )
                .isEqualTo( AbstractCLI.SUCCESS );
        verify( arrayDesignMergeService ).merge( a, otherPlatforms, "four is better than one", "4", false );
    }
}
