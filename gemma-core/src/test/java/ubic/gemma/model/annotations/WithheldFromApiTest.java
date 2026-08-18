package ubic.gemma.model.annotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ubic.gemma.model.annotations.WithheldFromApi.Reason;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the annotation itself has to do: suppress the member it is put on, suppress nothing else, and
 * carry the reason at runtime so {@link WithheldFromApiInventoryTest} can enforce against it.
 * <p>
 * The reason has to survive to runtime for the inventory guard to work at all, so
 * {@link #theReasonAndCommentSurviveToRuntime()} is not a tautology — it is the precondition the
 * other test class depends on.
 */
class WithheldFromApiTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unused") // read reflectively by Jackson
    static class Sample {
        public String visible = "shown";

        @WithheldFromApi(Reason.CALLER_IDENTITY)
        public boolean currentUserIsOwner = true;

        @WithheldFromApi(value = Reason.PUBLIC_PROJECTION_EXISTS, comment = "use PublicGeeqValueObject")
        public double qScoreOutliers = 0.5;

        @WithheldFromApi(Reason.DISCLOSURE)
        public String getFastqHeaders() {
            return "@M01234:...";
        }
    }

    private Map<String, Object> serialize() throws Exception {
        //noinspection unchecked
        return mapper.readValue( mapper.writeValueAsString( new Sample() ), Map.class );
    }

    @Test
    void itSuppressesFieldsAndGetters() throws Exception {
        Map<String, Object> json = serialize();
        assertFalse( json.containsKey( "currentUserIsOwner" ) );
        assertFalse( json.containsKey( "qScoreOutliers" ) );
        assertFalse( json.containsKey( "fastqHeaders" ), "annotated getters are suppressed too" );
    }

    @Test
    void itSuppressesOnlyWhatItIsPutOn() throws Exception {
        Map<String, Object> json = serialize();
        assertEquals( "shown", json.get( "visible" ) );
        assertEquals( 1, json.size(), "nothing but the unannotated property should survive: " + json );
    }

    @Test
    void theReasonAndCommentSurviveToRuntime() throws Exception {
        WithheldFromApi onField = Sample.class.getField( "qScoreOutliers" ).getAnnotation( WithheldFromApi.class );
        assertEquals( Reason.PUBLIC_PROJECTION_EXISTS, onField.value() );
        assertEquals( "use PublicGeeqValueObject", onField.comment() );

        WithheldFromApi onGetter = Sample.class.getMethod( "getFastqHeaders" ).getAnnotation( WithheldFromApi.class );
        assertEquals( Reason.DISCLOSURE, onGetter.value() );
        assertTrue( onGetter.comment().isEmpty(), "comment defaults to empty" );
    }
}
