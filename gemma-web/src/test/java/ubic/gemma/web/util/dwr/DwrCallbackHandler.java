package ubic.gemma.web.util.dwr;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;

import java.util.function.Consumer;

public class DwrCallbackHandler<T> extends AbstractDwrReplyParser implements ResultHandler {

    private static final JsonPath jsonPath = JsonPath.compile( "$.data" );

    private final Consumer<T> doWithReply;

    protected DwrCallbackHandler( int batchId, int callId, Consumer<T> doWithReply ) {
        super( "dwr.engine._remoteHandleCallback", batchId, callId );
        this.doWithReply = doWithReply;
    }

    @Override
    public void handle( MvcResult result ) throws Exception {
        String payload = "{\"data\":" + getReply( result ) + "}";
        doWithReply.accept( jsonPath.read( payload ) );
    }
}
