package ubic.gemma.web.util.dwr;

import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.util.AssertionErrors.assertTrue;

public abstract class AbstractDwrReplyMatchers extends AbstractDwrReplyParser {

    protected AbstractDwrReplyMatchers( String callback, int batchId, int callId ) {
        super( callback, batchId, callId );
    }

    public ResultMatcher exist() {
        return result -> {
            assertTrue( "Expected a line matching " + replyPattern, replyPattern.matcher( result.getResponse().getContentAsString() ).find() );
        };
    }

    public ResultMatcher doesNotExist() {
        return result -> {
            assertTrue( "Expected no line matching " + replyPattern, !replyPattern.matcher( result.getResponse().getContentAsString() ).find() );
        };
    }
}
