package ubic.gemma.web.util.dwr;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultHandler;

import java.util.function.Consumer;

public class DwrExceptionHandler extends AbstractDwrReplyParser implements ResultHandler {

    private static final JsonPath
            javaClassNamePath = JsonPath.compile( "$.javaClassName" ),
            messagePath = JsonPath.compile( "$.message" );

    private final Consumer<DwrException> doWithException;

    public DwrExceptionHandler( int batchId, int callId, Consumer<DwrException> doWithException ) {
        super( "dwr.engine._remoteHandleException", batchId, callId );
        this.doWithException = doWithException;
    }

    @Override
    public void handle( MvcResult result ) throws Exception {
        doWithException.accept( parseException( result ) );
    }

    private DwrException parseException( MvcResult result ) throws Exception {
        String reply = getReply( result );
        return new DwrException( javaClassNamePath.read( reply ), messagePath.read( reply ) );
    }
}
