package ubic.gemma.core.analysis.sequence;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.config.Settings;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A stored BLAT result records the assembly its coordinates belong to only through the database it says it searched.
 * Named for the taxon, an rn7 alignment and an rn8 one are identical in every column but the coordinates.
 */
class ShellDelegatingBlatSearchedGenomeTest {

    @Test
    void theSearchedGenomeIsNamedForTheAssembly() {
        String humanAssembly = Settings.getString( "gemma.goldenpath.db.human" );
        assertThat( humanAssembly ).as( "the test configuration has a human assembly" ).isNotBlank();

        ExternalDatabase searched = ShellDelegatingBlat.getSearchedGenome( Taxon.Factory.newInstance( "human" ) );

        assertThat( searched.getName() ).isEqualTo( humanAssembly ).isNotEqualTo( "human" );
        assertThat( searched.getType() ).isEqualTo( DatabaseType.SEQUENCE );
    }

    /**
     * The taxon is resolved before the assembly is looked up, so a taxon with no BLAT server is still refused rather
     * than named after whatever the configuration happens to hold.
     */
    @Test
    void aTaxonThatCannotBeBlattedIsStillRefused() {
        assertThatThrownBy( () -> ShellDelegatingBlat.getSearchedGenome( Taxon.Factory.newInstance( "zebrafish" ) ) )
                .isInstanceOf( UnsupportedOperationException.class );
    }
}
