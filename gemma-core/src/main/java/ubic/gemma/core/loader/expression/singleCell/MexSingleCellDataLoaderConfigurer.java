package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Configure a {@link MexSingleCellDataLoader} for a given directory and collection of {@link BioAssay}s.
 * @author poirigui
 */
public class MexSingleCellDataLoaderConfigurer extends AbstractMexSingleCellDataLoaderConfigurer {

    private final BioAssayMapper bioAssayMapper;
    private final List<String> sampleNames;
    private final List<Path> sampleDirs;

    public MexSingleCellDataLoaderConfigurer( Path mexDir, Collection<BioAssay> bioAssays, BioAssayMapper bioAssayMapper ) {
        this.bioAssayMapper = bioAssayMapper;
        this.sampleNames = new ArrayList<>();
        this.sampleDirs = new ArrayList<>();
        for ( BioAssay ba : bioAssays ) {
            Path sampleDir;
            if ( ba.getAccession() != null ) {
                sampleDir = mexDir.resolve( ba.getAccession().getAccession() );
            } else {
                sampleDir = mexDir.resolve( ba.getName() );
            }
            sampleNames.add( ba.getName() );
            sampleDirs.add( sampleDir );
        }
    }

    @Override
    public MexSingleCellDataLoader configureLoader() {
        MexSingleCellDataLoader loader = super.configureLoader();
        loader.setBioAssayToSampleNameMapper( bioAssayMapper );
        return loader;
    }

    @Override
    protected List<String> getSampleNames() {
        return sampleNames;
    }

    @Override
    protected List<Path> getSampleDirs() {
        return sampleDirs;
    }
}
