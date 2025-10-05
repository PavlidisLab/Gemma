package ubic.gemma.core.loader.expression.singleCell;

import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.geo.singleCell.TenXCellRangerUtils;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Configure a {@link MexSingleCellDataLoader} for a given directory and collection of {@link BioAssay}s.
 * @author poirigui
 */
public class MexSingleCellDataLoaderConfigurer extends AbstractMexSingleCellDataLoaderConfigurer {

    private final BioAssayMapper bioAssayMapper;
    private final List<String> sampleNames;
    private final Map<String, BioAssay> bioAssayBySampleName;
    private final List<Path> sampleDirs;
    @Nullable
    private final GeoSeries geoSeries;

    /**
     * @param geoSeries         GEO series metadata, optional, but can be used to infer the 10x chemistry
     */
    public MexSingleCellDataLoaderConfigurer( Path mexDir, Collection<BioAssay> bioAssays, BioAssayMapper bioAssayMapper, @Nullable Path cellRangerPrefix, @Nullable GeoSeries geoSeries ) {
        super( cellRangerPrefix );
        this.bioAssayMapper = bioAssayMapper;
        this.geoSeries = geoSeries;
        this.sampleNames = new ArrayList<>();
        this.bioAssayBySampleName = new HashMap<>();
        this.sampleDirs = new ArrayList<>();
        for ( BioAssay ba : bioAssays ) {
            Path sampleDir;
            if ( ba.getAccession() != null ) {
                sampleDir = mexDir.resolve( ba.getAccession().getAccession() );
            } else {
                sampleDir = mexDir.resolve( ba.getName() );
            }
            sampleNames.add( ba.getName() );
            bioAssayBySampleName.put( ba.getName(), ba );
            sampleDirs.add( sampleDir );
        }
    }

    @Override
    public MexSingleCellDataLoader configureLoader( SingleCellDataLoaderConfig config ) {
        MexSingleCellDataLoader loader = super.configureLoader( config );
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

    @Override
    protected boolean detect10x( String sampleName, Path sampleDir ) {
        BioAssay assay = requireNonNull( bioAssayBySampleName.get( sampleName ),
                "No BioAssay with name " + sampleName + " found." );
        return super.detect10x( sampleName, sampleDir )
                || getGeoSample( assay ).map( TenXCellRangerUtils::detect10x ).orElse( false );
    }

    @Override
    protected boolean detectUnfiltered( String sampleName, Path sampleDir ) {
        BioAssay assay = requireNonNull( bioAssayBySampleName.get( sampleName ),
                "No BioAssay with name " + sampleName + " found." );
        return super.detectUnfiltered( sampleName, sampleDir )
                || getGeoSample( assay ).map( TenXCellRangerUtils::detect10xUnfiltered ).orElse( false );
    }

    @Override
    protected String detect10xGenome( String sampleName, Path sampleDir ) {
        BioAssay assay = requireNonNull( bioAssayBySampleName.get( sampleName ),
                "No BioAssay with name " + sampleName + " found." );
        return assay.getSampleUsed().getSourceTaxon().getCommonName();
    }

    @Nullable
    @Override
    protected String detect10xChemistry( String sampleName, Path sampleDir ) {
        BioAssay assay = requireNonNull( bioAssayBySampleName.get( sampleName ),
                "No BioAssay with name " + sampleName + " found." );
        if ( geoSeries != null && assay.getAccession() != null && Objects.equals( assay.getAccession().getExternalDatabase().getName(), ExternalDatabases.GEO ) ) {
            log.info( assay + " appears to originate from GEO, will use its GEO sample metadata to infer the chemistry." );
            return getGeoSample( assay )
                    .map( TenXCellRangerUtils::detect10xChemistry )
                    .orElse( null );
        } else {
            return null;
        }
    }

    private Optional<GeoSample> getGeoSample( BioAssay assay ) {
        if ( geoSeries == null ) {
            return Optional.empty();
        }
        if ( assay.getAccession() == null || !assay.getAccession().getExternalDatabase().getName().equals( ExternalDatabases.GEO ) ) {
            // sample has no accession or is not from GEO
            return Optional.empty();
        }
        return geoSeries.getSamples().stream()
                .filter( s -> s.getGeoAccession() != null && s.getGeoAccession().equals( assay.getAccession().getAccession() ) )
                .findFirst();
    }
}
