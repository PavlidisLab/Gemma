package ubic.gemma.apps;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.cli.util.test.AuthenticatedCliTestConfig;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.FactorValueOntologyService;
import ubic.gemma.core.util.locking.FileLockManager;
import ubic.gemma.core.util.locking.FileLockManagerImpl;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@ContextConfiguration
class FactorValueOntologyWriterCliTest extends BaseCliTest5 {

    private static Path tgfvoDir;

    @Configuration
    @TestComponent
    @Import(AuthenticatedCliTestConfig.class)
    static class CC {

        @Bean
        public static TestPropertyPlaceholderConfigurer testPropertyPlaceholderConfigurer() throws IOException {
            tgfvoDir = Files.createTempDirectory( "tgfvo" );
            return new TestPropertyPlaceholderConfigurer( "tgfvo.path=" + tgfvoDir.resolve( "TGFVO.OWL.gz" ) );
        }

        @Bean
        public FactorValueOntologyWriterCli factorValueOntologyWriterCli() {
            return new FactorValueOntologyWriterCli();
        }

        @Bean
        public FactorValueOntologyService factorValueOntologyService() {
            return mock();
        }

        @Bean
        public FileLockManager fileLockManager() {
            return new FileLockManagerImpl();
        }
    }

    @Autowired
    private FactorValueOntologyWriterCli cli;

    @Autowired
    private FactorValueOntologyService factorValueOntologyService;

    /**
     * A failed write left a truncated TGFVO.OWL.gz, and a rerun without -force refused to replace it.
     */
    @Test
    void whenTheWriteFails_thePreviousFileIsKept() throws IOException {
        Path tgfvo = tgfvoDir.resolve( "TGFVO.OWL.gz" );
        try ( Writer w = new OutputStreamWriter( new GZIPOutputStream( Files.newOutputStream( tgfvo ) ), StandardCharsets.UTF_8 ) ) {
            w.write( "previous ontology" );
        }
        when( factorValueOntologyService.getFactorValueUris() ).thenReturn( Collections.singletonList( "http://example.com/fv/1" ) );
        doAnswer( inv -> {
            Writer writer = inv.getArgument( 1 );
            writer.write( "partial ontology" );
            throw new IllegalStateException( "RDF serialization failed" );
        } ).when( factorValueOntologyService ).writeToRdfIgnoreAcls( any(), any() );

        assertThat( cli )
                .withArguments( "-force" )
                .fails();

        try ( InputStream is = new GZIPInputStream( Files.newInputStream( tgfvo ) ) ) {
            assertThat( new String( IOUtils.toByteArray( is ), StandardCharsets.UTF_8 ) ).isEqualTo( "previous ontology" );
        }
        try ( Stream<Path> files = Files.list( tgfvoDir ) ) {
            assertThat( files ).containsExactly( tgfvo );
        }
    }

    /**
     * {@code -o} was defined without an argument, so the path was left over and the output file was null.
     */
    @Test
    void outputFileOption() throws IOException {
        Path out = Files.createTempDirectory( "tgfvo-out" ).resolve( "TGFVO.OWL" );
        when( factorValueOntologyService.getFactorValueUris() ).thenReturn( Collections.singletonList( "http://example.com/fv/1" ) );
        doAnswer( inv -> {
            Writer writer = inv.getArgument( 1 );
            writer.write( "ontology" );
            return null;
        } ).when( factorValueOntologyService ).writeToRdfIgnoreAcls( any(), any() );

        assertThat( cli )
                .withArguments( "-o", out.toString() )
                .succeeds();

        assertThat( out ).hasContent( "ontology" );
    }
}
