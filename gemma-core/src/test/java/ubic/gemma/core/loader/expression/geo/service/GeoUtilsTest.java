package ubic.gemma.core.loader.expression.geo.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ubic.gemma.core.loader.expression.geo.service.GeoUtils.getUrlForSeriesFamily;

public class GeoUtilsTest {

    @Test
    public void test() {
        assertEquals( "https://ftp.ncbi.nlm.nih.gov/geo/series/GSEnnn/GSE1/miniml/GSE1_family.xml.tgz", getUrlForSeriesFamily( "GSE1", GeoSource.FTP_VIA_HTTPS, GeoFormat.MINIML ).toString() );
        assertEquals( "https://ftp.ncbi.nlm.nih.gov/geo/series/GSEnnn/GSE100/miniml/GSE100_family.xml.tgz", getUrlForSeriesFamily( "GSE100", GeoSource.FTP_VIA_HTTPS, GeoFormat.MINIML ).toString() );
        assertEquals( "https://ftp.ncbi.nlm.nih.gov/geo/series/GSE100nnn/GSE100000/miniml/GSE100000_family.xml.tgz", getUrlForSeriesFamily( "GSE100000", GeoSource.FTP_VIA_HTTPS, GeoFormat.MINIML ).toString() );
        assertEquals( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE100000&targ=all&form=xml&view=brief", getUrlForSeriesFamily( "GSE100000", GeoSource.QUERY, GeoFormat.MINIML ).toString() );
        assertEquals( "ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSE100nnn/GSE100000/miniml/GSE100000_family.xml.tgz", getUrlForSeriesFamily( "GSE100000", GeoSource.FTP, GeoFormat.MINIML ).toString() );
    }
}