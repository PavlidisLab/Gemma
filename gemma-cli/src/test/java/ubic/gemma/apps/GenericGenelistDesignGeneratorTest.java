package ubic.gemma.apps;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.cli.audit.CliArrayDesignAuditService;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.security.authentication.ManualAuthenticationService;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneProductService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.AnnotationAssociationService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class GenericGenelistDesignGeneratorTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class GenericGenelistDesignGeneratorTestContextConfiguration {

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public GenericGenelistDesignGenerator genericGenelistDesignGenerator() {
            return new GenericGenelistDesignGenerator();
        }

        @Bean
        public ManualAuthenticationService manualAuthenticationService() {
            return mock();
        }

        @Bean
        public AnnotationAssociationService annotationAssociationService() {
            return mock();
        }

        @Bean
        public ArrayDesignAnnotationService arrayDesignAnnotationService() {
            return mock();
        }

        @Bean
        public ArrayDesignReportService arrayDesignReportService() {
            return mock();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock();
        }

        @Bean
        public BioSequenceService bioSequenceService() {
            return mock();
        }

        @Bean
        public CompositeSequenceService compositeSequenceService() {
            return mock();
        }

        @Bean
        public GeneService geneService() {
            return mock();
        }

        @Bean
        public GeneProductService geneProductService() {
            return mock();
        }

        @Bean
        public CliArrayDesignAuditService cliArrayDesignAuditService() {
            return mock();
        }

        @Bean
        public GemmaRestApiClient gemmaRestApiClient() {
            return mock();
        }

        @Bean
        public EntityLocator entityLocator() {
            return mock();
        }
    }

    @TempDir
    Path dir;

    @Autowired
    private ObjectProvider<GenericGenelistDesignGenerator> cli;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ArrayDesignReportService arrayDesignReportService;

    @Autowired
    private ArrayDesignAnnotationService arrayDesignAnnotationService;

    @Autowired
    private BioSequenceService bioSequenceService;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    @Autowired
    private CliArrayDesignAuditService cliArrayDesignAuditService;

    @Autowired
    private EntityLocator entityLocator;

    private ArrayDesign platform;

    @BeforeEach
    void setUp() {
        platform = ArrayDesign.Factory.newInstance( "Generic_human_ncbiIds", null );
        platform.setId( 1L );
        platform.setTechnologyType( TechnologyType.GENELIST );
        Taxon human = Taxon.Factory.newInstance( "human" );
        when( entityLocator.locateArrayDesign( platform.getShortName() ) ).thenReturn( platform );
        when( entityLocator.locateTaxon( "human" ) ).thenReturn( human );
        when( arrayDesignService.thaw( any( ArrayDesign.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        // persisting assigns an id
        AtomicLong ids = new AtomicLong( 100 );
        when( bioSequenceService.create( any( BioSequence.class ) ) ).thenAnswer( a -> withId( a.getArgument( 0 ), ids ) );
        when( compositeSequenceService.create( any( CompositeSequence.class ) ) ).thenAnswer( a -> withId( a.getArgument( 0 ), ids ) );
    }

    @AfterEach
    void tearDown() {
        reset( arrayDesignService, arrayDesignReportService, arrayDesignAnnotationService, bioSequenceService,
                compositeSequenceService, cliArrayDesignAuditService, entityLocator );
    }

    /**
     * The report service logs a report it cannot write and returns null; that used to end the run with exit 0.
     */
    @Test
    @WithMockUser
    void reportThatCannotBeWrittenFailsTheRun() throws Exception {
        Path genes = Files.write( dir.resolve( "genes.txt" ), List.of( "123" ) );
        when( arrayDesignReportService.generateArrayDesignReport( 1L ) ).thenReturn( null );

        assertThat( cli.getObject() )
                .withArguments( "-a", platform.getShortName(), "-t", "human", "-f", genes.toString() )
                .fails();

        // the rest of the run still happens
        verify( cliArrayDesignAuditService ).recordAnnotationBasedGeneMapping( same( platform ), anyString() );
        verify( arrayDesignAnnotationService ).deleteExistingFiles( platform );
    }

    @Test
    @WithMockUser
    void reportWrittenSucceeds() throws Exception {
        Path genes = Files.write( dir.resolve( "genes.txt" ), List.of( "123" ) );
        when( arrayDesignReportService.generateArrayDesignReport( 1L ) ).thenReturn( new ArrayDesignValueObject( 1L ) );

        assertThat( cli.getObject() )
                .withArguments( "-a", platform.getShortName(), "-t", "human", "-f", genes.toString() )
                .succeeds();
    }

    private static <T extends AbstractIdentifiable> T withId( T entity, AtomicLong ids ) {
        entity.setId( ids.incrementAndGet() );
        return entity;
    }
}
