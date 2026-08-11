package ubic.gemma.model.common.description;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins what a characteristic puts on the wire. {@code originalValue} is the submitter's own wording,
 * and it is the only thing on the row that survives curation replacing {@code value} with an ontology
 * label — so a consumer auditing a grounding has nothing else to compare against.
 * <p>
 * It was carried on the VO but annotated {@code @GemmaWebOnly}, which is {@code @JsonIgnore}
 * underneath, so REST silently dropped it and callers concluded Gemma had never kept the original.
 */
class CharacteristicValueObjectSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private Map<String, Object> serialize( Characteristic c ) throws Exception {
        String json = mapper.writeValueAsString( new CharacteristicValueObject( c ) );
        //noinspection unchecked
        return mapper.readValue( json, Map.class );
    }

    @Test
    void theSubmittersOwnWordingReachesTheWire() throws Exception {
        // The GSE102415 shape: submitter wrote "Hypothalamus" under "tissue"; curation grounded it to
        // UBERON_0001898 and rewrote value to the ontology label. Without originalValue the question
        // "is that the right resolution of what they wrote" cannot be asked from the API.
        Characteristic c = Characteristic.Factory.newInstance( "organism part",
                "http://purl.obolibrary.org/obo/UBERON_0000105", "hypothalamus",
                "http://purl.obolibrary.org/obo/UBERON_0001898" );
        c.setOriginalValue( "Hypothalamus" );

        Map<String, Object> json = serialize( c );
        assertEquals( "Hypothalamus", json.get( "originalValue" ) );
        assertEquals( "hypothalamus", json.get( "value" ) );
    }

    @Test
    void anAbsentOriginalIsOmittedRatherThanSentAsNull() throws Exception {
        // Null means "not recorded", never "same as value" — it is unset for rows written through the
        // curation API and for anything GEO import never gave a distinct original. Omitting the key
        // keeps that distinct from a recorded empty string.
        Characteristic c = Characteristic.Factory.newInstance( "cell line", null, "N2a", null );

        Map<String, Object> json = serialize( c );
        assertFalse( json.containsKey( "originalValue" ),
                "an unrecorded original must be omitted, not serialized as null" );
        assertTrue( json.containsKey( "value" ) );
    }

    @Test
    void theOtherGemmaWebOnlyFieldsStayOffTheWire() throws Exception {
        // Un-hiding originalValue must not un-hide its neighbours: the rest of @GemmaWebOnly on this
        // VO is Phenocarta / editor-display state that no REST client has ever seen.
        Characteristic c = Characteristic.Factory.newInstance( "cell line", null, "N2a", null );
        c.setOriginalValue( "N2a cells" );

        Map<String, Object> json = serialize( c );
        assertTrue( json.containsKey( "originalValue" ) );
        for ( String hidden : new String[] { "urlId", "alreadyPresentInDatabase", "alreadyPresentOnGene",
                "child", "numTimesUsed", "ontologyUsed", "privateGeneCount", "publicGeneCount",
                "root", "taxon", "valueDefinition" } ) {
            assertFalse( json.containsKey( hidden ), hidden + " must stay off the wire" );
        }
    }
}
