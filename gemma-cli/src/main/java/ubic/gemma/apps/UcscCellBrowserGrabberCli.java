package ubic.gemma.apps;

import ubic.gemma.cli.util.AbstractCLI;
import ubic.gemma.core.loader.expression.ucsc.cellbrowser.UcscCellBrowserUtils;
import ubic.gemma.core.loader.expression.ucsc.cellbrowser.model.DatasetDescription;
import ubic.gemma.core.loader.expression.ucsc.cellbrowser.model.DatasetSummary;

import static ubic.gemma.core.util.TsvUtils.format;

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
        getCliContext().getOutputStream().println( "dataset_id\tdataset_name\ttaxa\ttissues\tdiseases\tgeo_accession\tpubmed_id\n" );
        for ( DatasetSummary dataset : UcscCellBrowserUtils.getDatasets() ) {
            DatasetDescription desc = UcscCellBrowserUtils.getDatasetDescription( dataset.getName() );
            getCliContext().getOutputStream().printf( "%s\t%s\t%s\t%s\t%s\t%s\t%s%n",
                    format( dataset.getName() ),
                    format( dataset.getShortLabel() ),
                    format( dataset.getOrganisms() ),
                    format( dataset.getBodyParts() ),
                    format( dataset.getDiseases() ),
                    format( desc.getGeoSeries() ),
                    format( desc.getPmid() ) );
        }
    }
}
