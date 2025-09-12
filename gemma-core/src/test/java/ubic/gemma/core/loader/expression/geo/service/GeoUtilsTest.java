package ubic.gemma.core.loader.expression.geo.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ubic.gemma.core.loader.expression.geo.service.GeoUtils.*;

public class GeoUtilsTest {

    @Test
    public void test() {
        assertEquals( "https://ftp.ncbi.nlm.nih.gov/geo/series/GSEnnn/GSE1/miniml/GSE1_family.xml.tgz", getUrlForSeriesFamily( "GSE1", GeoSource.FTP_VIA_HTTPS, GeoFormat.MINIML ).toString() );
        assertEquals( "https://ftp.ncbi.nlm.nih.gov/geo/series/GSEnnn/GSE100/miniml/GSE100_family.xml.tgz", getUrlForSeriesFamily( "GSE100", GeoSource.FTP_VIA_HTTPS, GeoFormat.MINIML ).toString() );
        assertEquals( "https://ftp.ncbi.nlm.nih.gov/geo/series/GSE100nnn/GSE100000/miniml/GSE100000_family.xml.tgz", getUrlForSeriesFamily( "GSE100000", GeoSource.FTP_VIA_HTTPS, GeoFormat.MINIML ).toString() );
        assertEquals( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE100000&targ=all&form=xml&view=brief", getUrlForSeriesFamily( "GSE100000", GeoSource.DIRECT, GeoFormat.MINIML ).toString() );
        assertEquals( "ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSE100nnn/GSE100000/miniml/GSE100000_family.xml.tgz", getUrlForSeriesFamily( "GSE100000", GeoSource.FTP, GeoFormat.MINIML ).toString() );
        assertEquals( "https://www.ncbi.nlm.nih.gov/geo/browse/?view=series&zsort=date&mode=csv&page=1&display=20", getUrlForBrowsing( GeoRecordType.SERIES, 1, 20, GeoFormat.CSV ).toString() );
        assertEquals( "https://www.ncbi.nlm.nih.gov/geo/download/?acc=GSE1&format=file&file=GSE1_test.txt", getUrlForSupplementaryMaterial( GeoRecordType.SERIES, "GSE1", "GSE1_test.txt", GeoSource.DIRECT ).toString() );
        assertEquals( "ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSEnnn/GSE1/suppl/GSE1_test.txt", getUrlForSupplementaryMaterial( GeoRecordType.SERIES, "GSE1", "GSE1_test.txt", GeoSource.FTP ).toString() );
        assertEquals( "https://ftp.ncbi.nlm.nih.gov/geo/series/GSEnnn/GSE1/suppl/GSE1_test.txt", getUrlForSupplementaryMaterial( GeoRecordType.SERIES, "GSE1", "GSE1_test.txt", GeoSource.FTP_VIA_HTTPS ).toString() );
    }
}