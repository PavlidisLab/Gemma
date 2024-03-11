package ubic.gemma.web.util.dwr;

import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Matches erroneous DWR replies.
 * @author poirigui
 */
public class DwrExceptionMatchers extends AbstractDwrReplyMatchers {

    public DwrExceptionMatchers( int batchId, int callId ) {
        super( "dwr.engine._remoteHandleException", batchId, callId );
    }

    public ResultMatcher javaClassName( String value ) {
        return result -> {
            new JsonPathExpectationsHelper( "$.javaClassName" ).assertValue( getReply( result ), value );
        };
    }

    public ResultMatcher message( String value ) {
        return result -> {
            new JsonPathExpectationsHelper( "$.message" ).assertValue( getReply( result ), value );
        };
    }

}
