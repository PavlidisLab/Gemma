package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.persistence.service.common.protocol.ProtocolService;

import javax.annotation.Nullable;

/**
 * @author poirigui
 */
public class ProtocolAdderCli extends AbstractAuthenticatedCLI {

    @Autowired
    private ProtocolService protocolService;

    private String name;
    private String description;

    @Nullable
    @Override
    public String getCommandName() {
        return "addProtocol";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Add a new protocol";
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addRequiredOption( "name", "name", true, "Name for the protocol" );
        options.addOption( "description", "description", true, "Description for the protocol" );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        name = commandLine.getOptionValue( "name" );
        description = commandLine.getOptionValue( "description" );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        Protocol protocol = Protocol.Factory.newInstance();
        protocol.setName( name );
        protocol.setDescription( description );
        Protocol existingProtocol;
        if ( ( existingProtocol = protocolService.findByName( protocol.getName() ) ) == null ) {
            protocol = protocolService.create( protocol );
            addSuccessObject( protocol.getName(), "Created a new protocol object." );
        } else {
            addErrorObject( protocol.getName(), "There is already a protocol object with this name: " + existingProtocol + "." );
        }
    }
}
