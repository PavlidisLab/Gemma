package ubic.gemma.web.util.dwr;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.util.AssertionErrors.assertTrue;

public abstract class AbstractDwrReplyMatchers {

    private final Pattern replyPattern;

    protected AbstractDwrReplyMatchers( String callback, int batchId, int callId ) {
        replyPattern = Pattern.compile( "^" + Pattern.quote( callback ) + "\\('" + batchId + "','" + callId + "',(.*?)\\);$", Pattern.MULTILINE );
    }

    /**
     * Derive a new DWR reply matcher for the given batch ID.
     */
    public abstract AbstractDwrReplyMatchers batch( int batchId );

    public ResultMatcher doesNotExist() {
        return result -> {
            assertTrue( "Expected no line matching " + replyPattern, !replyPattern.matcher( result.getResponse().getContentAsString() ).find() );
        };
    }

    protected String getReply( MvcResult result ) throws UnsupportedEncodingException {
        Matcher m = replyPattern.matcher( result.getResponse().getContentAsString() );
        assertTrue( "Expected a line matching " + replyPattern, m.find() );
        return m.group( 1 );
    }
}
