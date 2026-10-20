package ubic.gemma.core.loader.expression.geo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The real strings frinkbro measured in production on 2026-09-11, and the ordinary cases that must keep
 * splitting as they always did.
 *
 * @see GeoCharacteristicKey#split(String)
 */
public class GeoColonSplitTest {

    @Test
    @DisplayName("the ordinary case is unchanged: first colon separates")
    public void splitsTheOrdinaryCaseAtTheFirstColon() {
        assertThat( GeoCharacteristicKey.split( "Age: 8 weeks" ) )
                .containsExactly( "Age", " 8 weeks" );
        assertThat( GeoCharacteristicKey.split( "tissue: liver" ) )
                .containsExactly( "tissue", " liver" );
    }

    @Test
    @DisplayName("a colon inside the VALUE no longer becomes the cut — poly(I:C)")
    public void doesNotCutInsideAParenthesisedValue() {
        // frinkbro: stored category was "activated with p(I", value "C) and CL075"
        assertThat( GeoCharacteristicKey.split( "activated with p(I:C) and CL075" ) )
                .containsExactly( "activated with p(I:C) and CL075" );
    }

    @Test
    @DisplayName("a LATER colon is the separator when the first one is inside parentheses")
    public void findsTheSeparatorAfterAParenthesisedAside() {
        assertThat( GeoCharacteristicKey.split( "cervical cancer (post chemotherapy: TP 2cycle): IIB" ) )
                .containsExactly( "cervical cancer (post chemotherapy: TP 2cycle)", " IIB" );
        assertThat( GeoCharacteristicKey.split( "platinium sensitivity (resistant: DFS<180 days): yes" ) )
                .containsExactly( "platinium sensitivity (resistant: DFS<180 days)", " yes" );
    }

    @Test
    @DisplayName("a legend key with '=' is not a category, so the cut moves past it")
    public void doesNotCutInsideALegend() {
        assertThat( GeoCharacteristicKey.split( "status (1=recurrence, 0=non-recurrence): 1" ) )
                .containsExactly( "status (1=recurrence, 0=non-recurrence)", " 1" );
        assertThat( GeoCharacteristicKey.split( "dlda30 pred (1=pcr, 0=rd): 0" ) )
                .containsExactly( "dlda30 pred (1=pcr, 0=rd)", " 0" );
    }

    @Test
    @DisplayName("the GSE205450 shape: the key itself carries a colon")
    public void handlesAColonInsideTheKeysOwnParentheses() {
        assertThat( GeoCharacteristicKey.split( "gender (m: male, f: female): M" ) )
                .containsExactly( "gender (m: male, f: female)", " M" );
    }

    @Test
    @DisplayName("GSE290074: one pair whose value has commas is NOT shattered into fragments")
    public void doesNotShatterASinglePairOnItsValuesCommas() {
        // The raw GEO line, from the record uib pulled. Two colons -- one separating, one inside the
        // parenthesised aside -- which is what used to trip the comma split and produce the 34 rows.
        String raw = "group: 10-20 granulosa cells, preantral follicle, adult human ovary, Hs9, age36,"
                + " cervical cancer (post chemotherapy: TP 2cycle)";
        BioMaterial bm = BioMaterial.Factory.newInstance();
        new GeoConverterImpl().parseGEOSampleCharacteristicString( raw, bm );

        assertThat( bm.getCharacteristics() )
                .withFailMessage( "the line is ONE characteristic; splitting it invents rows the submitter never wrote" )
                .hasSize( 1 );
        Characteristic only = bm.getCharacteristics().iterator().next();
        assertThat( only.getCategory() ).isEqualTo( "group" );
        assertThat( only.getValue() ).contains( "cervical cancer (post chemotherapy: TP 2cycle)" );
    }

    @Test
    @DisplayName("a genuine multi-pair line still splits: every fragment carries its own colon")
    public void stillSplitsAGenuineMultiPairLine() {
        BioMaterial bm = BioMaterial.Factory.newInstance();
        new GeoConverterImpl().parseGEOSampleCharacteristicString( "Age: 8 weeks; Sex: M", bm );
        assertThat( bm.getCharacteristics() ).hasSize( 2 );
        // the categories are the ones convertVariableType assigns, not the submitter's raw keys:
        // a recognized GeoVariable type is mapped onto Gemma's vocabulary before it is stored
        assertThat( bm.getCharacteristics() ).extracting( Characteristic::getCategory )
                .containsExactlyInAnyOrder( "age", "biological sex" );
        assertThat( bm.getCharacteristics() ).extracting( Characteristic::getValue )
                .containsExactlyInAnyOrder( "8 weeks", "M" );
    }

    @Test
    @DisplayName("the blob builder divides the same way — GSE205450's key is no longer cut in half")
    public void theSourceMetadataBlobUsesTheSameRule() {
        // The blob is a verbatim cache of what GEO said, so a key cut in half there can only be undone
        // by re-fetching. It used to run its own indexOf(':') and stored "gender (m" for this string.
        assertThat( GeoCharacteristicKey.split( "gender (m: male, f: female): M" ) )
                .containsExactly( "gender (m: male, f: female)", " M" );
    }

    @Test
    @DisplayName("no acceptable colon leaves the string whole rather than inventing a category")
    public void leavesItWholeWhenNothingQualifies() {
        assertThat( GeoCharacteristicKey.split( "p(I:C stimulated" ) )
                .containsExactly( "p(I:C stimulated" );
        assertThat( GeoCharacteristicKey.split( "no colon here at all" ) )
                .containsExactly( "no colon here at all" );
    }
}
