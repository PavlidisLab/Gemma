package ubic.gemma.cli.util;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DuplicateRejectingOptionsTest {

    private final Options options = new DuplicateRejectingOptions( "someCli" );

    @Test
    public void testDistinctOptionsAreAccepted() {
        options.addOption( "f", "file", true, "A file" );
        options.addOption( "a", "array", true, "A platform" );
        options.addOption( Option.builder().longOpt( "long-only" ).build() );
        assertThat( options.getOptions() ).hasSize( 3 );
    }

    @Test
    public void testDuplicateShortNameIsRejected() {
        options.addOption( "f", "adListFile", true, "Platform list" );
        assertThatThrownBy( () -> options.addOption( "f", "file", true, "Sequence file" ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "someCli" )
                .hasMessageContaining( "'f'" );
        // the first definition is untouched
        assertThat( options.getOption( "f" ).getLongOpt() ).isEqualTo( "adListFile" );
    }

    @Test
    public void testDuplicateLongNameIsRejected() {
        options.addOption( "a", "all", false, "All" );
        assertThatThrownBy( () -> options.addOption( "b", "all", false, "All, again" ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "someCli" )
                .hasMessageContaining( "'all'" );
    }

    @Test
    public void testLongOnlyOptionClashingWithShortNameIsRejected() {
        options.addOption( "all", false, "All" );
        assertThatThrownBy( () -> options.addOption( Option.builder().longOpt( "all" ).build() ) )
                .isInstanceOf( IllegalStateException.class );
    }

    @Test
    public void testDuplicateThroughOptionGroupIsRejected() {
        options.addOption( "o", "output", true, "Output" );
        OptionGroup group = new OptionGroup();
        group.addOption( Option.builder( "o" ).longOpt( "other" ).hasArg().build() );
        assertThatThrownBy( () -> options.addOptionGroup( group ) )
                .isInstanceOf( IllegalStateException.class );
    }

    @Test
    public void testDuplicateThroughRequiredOptionIsRejected() {
        options.addOption( "f", "file", true, "A file" );
        assertThatThrownBy( () -> options.addRequiredOption( "f", "force", false, "Force" ) )
                .isInstanceOf( IllegalStateException.class );
    }
}
