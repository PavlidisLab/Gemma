package ubic.gemma.core.loader.expression.geo;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import ubic.gemma.core.loader.expression.geo.model.GeoSample;
import ubic.gemma.core.loader.expression.geo.model.GeoSeries;
import ubic.gemma.core.loader.expression.singleCell.AnnDataSingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.MexSingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SeuratDiskSingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoader;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author poirigui
 */
@CommonsLog
public class GeoSingleCellDetector {

    /**
     * Use GEO accession for comparing the sample name.
     */
    private static final SingleCellDataLoader.SampleNameComparator GEO_SAMPLE_NAME_COMPARATOR = ( bm, n ) -> {
        if ( bm.getExternalAccession() != null && bm.getExternalAccession().getExternalDatabase().getName().equals( ExternalDatabases.GEO ) ) {
            return bm.getExternalAccession().getAccession().equals( n );
        } else {
            return bm.getName().equals( n );
        }
    };

    public Optional<SingleCellDataLoader> getSingleCellDataLoader( GeoSeries series, Collection<BioMaterial> samples ) {
        // detect AnnData (preferred over Seurat Disk)
        for ( String file : series.getSupplementaryFiles() ) {
            if ( file.endsWith( "h5ad" ) || file.endsWith( "h5ad.gz" ) ) {
                // TODO: retrieve the file
                AnnDataSingleCellDataLoader loader = new AnnDataSingleCellDataLoader( Paths.get( file ) );
                loader.setSampleNameComparator( ( bm, n ) -> n.equals( bm.getName() ) );
                // TODO: find the column
                loader.setSampleFactorName( "ID" );
                return Optional.of( loader );
            }
        }

        // detect Seurat Disk
        for ( String file : series.getSupplementaryFiles() ) {
            // TODO: retrieve the file
            if ( file.endsWith( "h5Seurat" ) || file.endsWith( "h5Seurat.gz" ) ) {
                SeuratDiskSingleCellDataLoader loader = new SeuratDiskSingleCellDataLoader( Paths.get( file ) );
                loader.setSampleNameComparator( ( bm, n ) -> n.equals( bm.getName() ) );
                return Optional.of( loader );
            }
        }

        Set<String> expectedGeoSampleNames = samples.stream()
                .map( bm -> bm.getExternalAccession() != null && bm.getExternalAccession().getExternalDatabase().getName().equals( ExternalDatabases.GEO ) ?
                        bm.getExternalAccession().getAccession() : bm.getName() )
                .collect( Collectors.toSet() );

        List<String> sampleNames = new ArrayList<>();
        List<Path> barcodesFiles = new ArrayList<>(),
                featuresFiles = new ArrayList<>(),
                matricesFiles = new ArrayList<>();

        for ( GeoSample sample : series.getSamples() ) {
            if ( !expectedGeoSampleNames.contains( sample.getGeoAccession() ) ) {
                continue;
            }

            // detect MEX (3 files per GEO sample)
            String barcodes = null, features = null, matrix = null;
            for ( String file : sample.getSupplementaryFiles() ) {
                if ( file.endsWith( "_barcodes.tsv" ) || file.endsWith( "_barcodes.tsv.gz" ) ) {
                    barcodes = file;
                }
                if ( file.endsWith( "_features.tsv" ) || file.endsWith( "_features.tsv.gz" ) ) {
                    features = file;
                }
                if ( file.endsWith( "_matrix.tsv" ) || file.endsWith( "_matrix.mtx.gz" ) ) {
                    matrix = file;
                }
            }

            // detect MEX (1 TAR archive per GEO sample)
            if ( barcodes == null || features == null || matrix == null ) {
                for ( String file : sample.getSupplementaryFiles() ) {
                    if ( file.endsWith( ".tar" ) || file.endsWith( ".tar.gz" ) ) {
                        try ( TarInputStream tis = new TarInputStream( Files.newInputStream( Paths.get( file ) ) ) ) {
                            TarEntry te = tis.getNextEntry();
                            if ( te.getName().endsWith( "_barcodes.tsv" ) || te.getName().endsWith( "_barcodes.tsv.gz" ) ) {
                                barcodes = te.getName();
                            }
                            if ( te.getName().endsWith( "_features.tsv" ) || te.getName().endsWith( "_features.tsv.gz" ) ) {
                                features = te.getName();
                            }
                            if ( te.getName().endsWith( "_matrix.tsv" ) || te.getName().endsWith( "_matrix.mtx.gz" ) ) {
                                matrix = te.getName();
                            }
                        } catch ( IOException e ) {
                            throw new RuntimeException( e );
                        }
                    }
                }
            }

            if ( barcodes != null && features != null && matrix != null ) {
                sampleNames.add( sample.getGeoAccession() );
                barcodesFiles.add( Paths.get( barcodes ) );
                featuresFiles.add( Paths.get( features ) );
                matricesFiles.add( Paths.get( matrix ) );
            }
        }

        if ( !sampleNames.isEmpty() ) {
            if ( !new HashSet<>( sampleNames ).containsAll( expectedGeoSampleNames ) ) {
                sampleNames.forEach( expectedGeoSampleNames::remove );
                log.warn( "The following samples lack a MEX matrix file: " + String.join( ", ", sampleNames ) );
            }
            MexSingleCellDataLoader loader = new MexSingleCellDataLoader( sampleNames, barcodesFiles, featuresFiles, matricesFiles );
            loader.setSampleNameComparator( GEO_SAMPLE_NAME_COMPARATOR );
            return Optional.of( loader );
        }

        return Optional.empty();
    }
}
