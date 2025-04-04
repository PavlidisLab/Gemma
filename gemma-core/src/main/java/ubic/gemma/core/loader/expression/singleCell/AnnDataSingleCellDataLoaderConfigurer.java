package ubic.gemma.core.loader.expression.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.util.anndata.Dataframe;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.nio.file.Path;
import java.util.Collection;

@CommonsLog
public class AnnDataSingleCellDataLoaderConfigurer extends AbstractAnnDataSingleCellDataLoaderConfigurer {

    private final Collection<BioAssay> bioAssays;
    private final BioAssayMapper bioAssayMapper;

    /**
     * @param bioAssays      a collection of {@link BioAssay} that are used to detect the sample column
     * @param bioAssayMapper a mapper for {@link BioAssay} to sample name to interpret identifier in the file
     */
    public AnnDataSingleCellDataLoaderConfigurer( Path annDataFile, Collection<BioAssay> bioAssays, BioAssayMapper bioAssayMapper ) {
        super( annDataFile );
        this.bioAssays = bioAssays;
        this.bioAssayMapper = bioAssayMapper;
    }

    @Override
    protected boolean isSampleNameColumn( Dataframe.Column<?, String> column ) {
        // make sure that each BioAssay is represented in the data
        return bioAssayMapper.matchOne( bioAssays, column.uniqueValues() ).values().containsAll( bioAssays );
    }

    @Override
    public AnnDataSingleCellDataLoader configureLoader( SingleCellDataLoaderConfig config ) {
        AnnDataSingleCellDataLoader loader = super.configureLoader( config );
        loader.setBioAssayToSampleNameMapper( bioAssayMapper );
        return loader;
    }
}
