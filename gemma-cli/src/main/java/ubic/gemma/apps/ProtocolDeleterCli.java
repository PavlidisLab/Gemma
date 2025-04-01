package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.persistence.service.common.protocol.ProtocolService;

import javax.annotation.Nullable;

/**
 * @author poirigui
 */
public class ProtocolDeleterCli extends AbstractAuthenticatedCLI {

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private EntityLocator entityLocator;

    private String identifier;

    @Nullable
    @Override
    public String getCommandName() {
        return "deleteProtocol";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Delete a protocol";
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( "protocol", "protocol", true, "Protocol ID or name." );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        identifier = commandLine.getOptionValue( "protocol" );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        Protocol protocol = entityLocator.locateProtocol( identifier );
        promptConfirmationOrAbort( "Delete " + protocol + "?" );
        protocolService.remove( protocol );
        addSuccessObject( protocol, "Deleted" );
    }
}
