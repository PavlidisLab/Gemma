package ubic.gemma.web.util.dwr;

import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.util.AssertionErrors.assertTrue;

public class AbstractDwrReplyParser {

    protected final Pattern replyPattern;

    protected AbstractDwrReplyParser( String callback, int batchId, int callId ) {
        replyPattern = Pattern.compile( "^" + Pattern.quote( callback ) + "\\('" + batchId + "','" + callId + "',(.*?)\\);$", Pattern.MULTILINE );
    }

    protected String getReply( MvcResult result ) throws UnsupportedEncodingException {
        Matcher m = replyPattern.matcher( result.getResponse().getContentAsString() );
        assertTrue( "Expected a line matching " + replyPattern, m.find() );
        return m.group( 1 );
    }
}
