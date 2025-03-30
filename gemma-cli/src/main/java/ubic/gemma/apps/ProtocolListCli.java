package ubic.gemma.apps;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.persistence.service.common.protocol.ProtocolService;

import javax.annotation.Nullable;

/**
 * @author poirigui
 */
public class ProtocolListCli extends AbstractAuthenticatedCLI {

    private static final CSVFormat TSV_FORMAT = CSVFormat.TDF.builder()
            .setHeader( "id", "name", "description" )
            .get();

    @Autowired
    private ProtocolService protocolService;

    @Nullable
    @Override
    public String getCommandName() {
        return "listProtocols";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "List all available protocols";
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        try ( CSVPrinter printer = TSV_FORMAT.print( getCliContext().getOutputStream() ) ) {
            for ( Protocol protocol : protocolService.loadAllUniqueByName() ) {
                printer.printRecord( protocol.getId(), protocol.getName(), TsvUtils.format( protocol.getDescription() ) );
            }
        }
    }
}
