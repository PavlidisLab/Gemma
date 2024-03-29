package ubic.gemma.web.util.dwr;

import org.hamcrest.Matcher;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.servlet.ResultMatcher;

import java.text.ParseException;

/**
 * Matches successful DWR replies.
 * @author poirigui
 */
public class DwrCallbackMatchers extends AbstractDwrReplyMatchers {

    /**
     * DWR replies with JavaScript objects which might not be in a suitable JSON container (i.e. {@code []}, {@code {}}.
     * <p>
     * Thus, this class modifies the behaviour by wrapping the object in a {@code {"data":reply}} container and
     * adjusting the assertion message if necessary.
     */
    private static final JsonPathExpectationsHelper jsonPathHelper = new JsonPathExpectationsHelper( "$.data" ) {

        @Override
        public void assertValue( String responseContent, Object expectedValue ) throws ParseException {
            try {
                super.assertValue( "{\"data\":" + responseContent + "}", expectedValue );
            } catch ( AssertionError e ) {
                throw new AssertionError( e.getMessage().replaceFirst( "\\$\\.data", "\\$" ), e.getCause() );
            }
        }

        @Override
        public <T> void assertValue( String content, Matcher<T> matcher ) throws ParseException {
            try {
                super.assertValue( "{\"data\":" + content + "}", matcher );
            } catch ( AssertionError e ) {
                throw new AssertionError( e.getMessage().replaceFirst( "\\$\\.data", "\\$" ), e.getCause() );
            }
        }
    };

    public DwrCallbackMatchers( int batchId, int callId ) {
        super( "dwr.engine._remoteHandleCallback", batchId, callId );
    }

    public ResultMatcher value( Object expected ) {
        return result -> {
            jsonPathHelper.assertValue( getReply( result ), expected );
        };
    }

    public <T> ResultMatcher value( org.hamcrest.Matcher<T> matcher ) {
        return result -> {
            jsonPathHelper.assertValue( getReply( result ), matcher );
        };
    }
}
