package ubic.gemma.apps;

import ubic.gemma.cli.util.AbstractCLI;
import ubic.gemma.core.loader.expression.ucsc.cellbrowser.UcscCellBrowserUtils;
import ubic.gemma.core.loader.expression.ucsc.cellbrowser.model.DatasetSummary;

/**
 * Lists available datasets from the UCSC Cell Browser.
 *
 * @author poirigui
 */
public class UcscCellBrowserGrabberCli extends AbstractCLI {

    @Override
    public String getCommandName() {
        return "listUcscCellBrowserData";
    }

    @Override
    protected void doWork() throws Exception {
        getCliContext().getOutputStream().println( "dataset_id\tdataset_name\ttaxa\ttissues\tdiseases\n" );
        for ( DatasetSummary dataset : UcscCellBrowserUtils.getDatasets() ) {
            getCliContext().getOutputStream().printf( "%s\t%s\t%s\t%s\t%s%n", dataset.getName(), dataset.getShortLabel(),
                    dataset.getOrganisms() != null ? String.join( "|", dataset.getOrganisms() ) : "",
                    dataset.getBodyParts() != null ? String.join( "|", dataset.getBodyParts() ) : "",
                    dataset.getDiseases() != null ? String.join( "|", dataset.getDiseases() ) : "" );
        }
    }
}
