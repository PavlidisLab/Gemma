package ubic.gemma.model.annotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ubic.gemma.model.annotations.NotForPublicApi.Reason;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The property this annotation has to have, and the only one worth a test: applying it to a member
 * already carrying {@link GemmaWebOnly} must change nothing on the wire.
 * <p>
 * If the two did not suppress identically, migrating a member from one to the other would be a
 * silent API change, and the migration this annotation exists to enable would be unsafe to do in
 * bulk — which is the whole point of it.
 */
class NotForPublicApiTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unused") // read reflectively by Jackson
    static class Sample {
        public String visible = "shown";

        @GemmaWebOnly
        public String vestigial = "hidden by the old marker";

        @NotForPublicApi(Reason.CALLER_IDENTITY)
        public boolean currentUserIsOwner = true;

        @NotForPublicApi(value = Reason.PUBLIC_PROJECTION_EXISTS, comment = "use PublicGeeqValueObject")
        public double qScoreOutliers = 0.5;

        @NotForPublicApi(Reason.DISCLOSURE)
        public String getFastqHeaders() {
            return "@M01234:...";
        }
    }

    private Map<String, Object> serialize() throws Exception {
        //noinspection unchecked
        return mapper.readValue( mapper.writeValueAsString( new Sample() ), Map.class );
    }

    @Test
    void itSuppressesExactlyLikeTheMarkerItReplaces() throws Exception {
        Map<String, Object> json = serialize();
        assertFalse( json.containsKey( "vestigial" ), "sanity: @GemmaWebOnly still suppresses" );
        assertFalse( json.containsKey( "currentUserIsOwner" ) );
        assertFalse( json.containsKey( "qScoreOutliers" ) );
        assertFalse( json.containsKey( "fastqHeaders" ), "annotated getters are suppressed too" );
    }

    @Test
    void itSuppressesOnlyWhatItIsPutOn() throws Exception {
        Map<String, Object> json = serialize();
        assertTrue( json.containsKey( "visible" ) );
        assertEquals( "shown", json.get( "visible" ) );
        assertEquals( 1, json.size(), "nothing but the unannotated property should survive: " + json );
    }

    /** Reason is required; a suppression that does not say why is what created the problem. */
    @Test
    void theReasonIsNotOptional() throws Exception {
        assertEquals( Reason.CALLER_IDENTITY,
                Sample.class.getField( "currentUserIsOwner" )
                        .getAnnotation( NotForPublicApi.class ).value() );
        assertEquals( "use PublicGeeqValueObject",
                Sample.class.getField( "qScoreOutliers" )
                        .getAnnotation( NotForPublicApi.class ).comment() );
    }
}
