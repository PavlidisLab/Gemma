package ubic.gemma.apps;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.cli.authentication.CLIAuthenticationManager;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.FileTools;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.persister.RelationshipPersister;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

/**
 * updateGOAnnots deletes every GO association and commits before it loads the new ones, so a failed load must end
 * the run with a non-zero status, say that the table was emptied, and leave the GO last-updated date alone.
 */
@ContextConfiguration
class NCBIGene2GOAssociationLoaderCLIFailureTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public NCBIGene2GOAssociationLoaderCLI ncbiGene2GOAssociationLoaderCLI() {
            return new NCBIGene2GOAssociationLoaderCLI();
        }

        @Bean
        public TaxonService taxonService() {
            return mock();
        }

        /**
         * Through a factory bean, so Spring does not try to autowire the mock's own fields.
         */
        @Bean
        public FactoryBean<RelationshipPersister> relationshipPersister() {
            RelationshipPersister mock = mock();
            return new FactoryBean<>() {
                @Override
                public RelationshipPersister getObject() {
                    return mock;
                }

                @Override
                public Class<?> getObjectType() {
                    return RelationshipPersister.class;
                }
            };
        }

        @Bean
        public Gene2GOAssociationService gene2GOAssociationService() {
            return mock();
        }

        @Bean
        public ExternalDatabaseService externalDatabaseService() {
            return mock();
        }

        @Bean
        public CLIAuthenticationManager cliAuthenticationManager() {
            return mock();
        }
    }

    @Autowired
    private NCBIGene2GOAssociationLoaderCLI cli;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    private RelationshipPersister relationshipPersister;
    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;
    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @AfterEach
    void resetMocks() {
        reset( taxonService, relationshipPersister, gene2GOAssociationService, externalDatabaseService );
    }

    @Test
    @WithMockUser(authorities = { "GROUP_ADMIN" })
    void aFailedLoadAfterTheTableWasEmptiedFailsTheRunAndKeepsTheDate() throws Exception {
        String gene2go = FileTools.resourceToPath( "/data/loader/association/gene2go.gz" );
        when( taxonService.loadAll() ).thenReturn( Collections.singletonList(
                Taxon.Factory.newInstance( "Homo sapiens", "human", 9606, true ) ) );
        when( relationshipPersister.persistGene2GOAssociations( anyCollection() ) )
                .thenThrow( new IllegalStateException( "database went away" ) );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThat( cli )
                        .withArguments( "-f", gene2go )
                        .failsWith( 1 )
                        .exitCause()
                        .hasMessageContaining( "Every existing GO association was deleted" )
                        .hasMessageContaining( "rerun updateGOAnnots" )
                        .hasRootCauseMessage( "database went away" ) );

        verify( gene2GOAssociationService ).removeAll();
        verify( externalDatabaseService, never() ).updateReleaseLastUpdated( any(), any(), any() );
    }
}
