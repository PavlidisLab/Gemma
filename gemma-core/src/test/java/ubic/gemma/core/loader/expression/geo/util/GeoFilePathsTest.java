package ubic.gemma.core.loader.expression.geo.util;

import org.junit.Test;
import ubic.gemma.core.loader.expression.geo.GeoMetadataFormat;

import static org.assertj.core.api.Assertions.assertThat;

public class GeoFilePathsTest {

    @Test
    public void testShortenAccession() {
        assertThat( GeoFilePaths.getSeriesFamilyFilePath( "GSE1", GeoMetadataFormat.SOFT ) )
                .isEqualTo( "geo/series/GSEnnn/GSE1/soft/GSE1_family.soft.gz" );
        assertThat( GeoFilePaths.getSeriesFamilyFilePath( "GSE1000", GeoMetadataFormat.SOFT ) )
                .isEqualTo( "geo/series/GSE1nnn/GSE1000/soft/GSE1000_family.soft.gz" );
    }
}