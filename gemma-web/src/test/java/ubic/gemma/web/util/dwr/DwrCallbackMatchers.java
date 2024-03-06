package ubic.gemma.web.util.dwr;

import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.MatcherAssertionErrors.assertThat;

/**
 * Matches successful DWR replies.
 * @author poirigui
 */
public class DwrCallbackMatchers extends AbstractDwrReplyMatchers {

    private final int callId;

    public DwrCallbackMatchers( int batchId, int callId ) {
        super( "dwr.engine._remoteHandleCallback", batchId, callId );
        this.callId = callId;
    }

    public ResultMatcher value( Object expected ) {
        return result -> {
            String reply = getReply( result );
            if ( "null".equals( reply ) ) {
                assertEquals( "Reply is 'null'.", null, expected );
            } else {
                new JsonPathExpectationsHelper( "$" ).assertValue( reply, expected );
            }
        };
    }

    public <T> ResultMatcher value( org.hamcrest.Matcher<T> matcher ) {
        return result -> {
            String reply = getReply( result );
            if ( "null".equals( reply ) ) {
                assertThat( "Reply is 'null'.", null, matcher );
            } else {
                new JsonPathExpectationsHelper( "$" ).assertValue( reply, matcher );
            }
        };
    }

    public DwrCallbackMatchers batch( int batchId ) {
        return new DwrCallbackMatchers( batchId, this.callId );
    }
}
