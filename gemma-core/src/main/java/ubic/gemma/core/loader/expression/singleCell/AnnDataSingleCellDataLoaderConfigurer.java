package ubic.gemma.core.loader.expression.singleCell;

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.util.anndata.Dataframe;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

@CommonsLog
public class AnnDataSingleCellDataLoaderConfigurer extends AbstractAnnDataSingleCellDataLoaderConfigurer {

    private final Collection<BioAssay> bioAssays;
    private final BioAssayMapper bioAssayMapper;

    public AnnDataSingleCellDataLoaderConfigurer( Path annDataFile, Collection<BioAssay> bioAssays, BioAssayMapper bioAssayMapper ) {
        super( annDataFile );
        this.bioAssays = bioAssays;
        this.bioAssayMapper = bioAssayMapper;
    }

    @Override
    protected boolean isSampleNameColumn( Dataframe<?> df, String column, Set<String> vals ) {
        // make sure that each BioAssay is represented in the data
        return bioAssayMapper.matchOne( bioAssays, vals ).values().containsAll( bioAssays );
    }

    @Override
    public AnnDataSingleCellDataLoader configureLoader() {
        AnnDataSingleCellDataLoader loader = super.configureLoader();
        loader.setBioAssayToSampleNameMapper( bioAssayMapper );
        return loader;
    }
}
