package ubic.gemma.core.loader.expression.geo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ubic.gemma.core.loader.expression.geo.service.GeoUtils.getUrlForSupplementaryMaterial;

public class GeoUtilsTest {

    @Test
    public void test() {
        assertEquals( "https://ftp.ncbi.nlm.nih.gov/geo/series/GSEnnn/GSE1/miniml/GSE1_family.xml.tgz", GeoUtils.getUrl( "GSE1", GeoSource.FTP_VIA_HTTPS, GeoFormat.MINIML, GeoScope.FAMILY, GeoAmount.FULL ).toString() );
        assertEquals( "https://ftp.ncbi.nlm.nih.gov/geo/series/GSEnnn/GSE100/miniml/GSE100_family.xml.tgz", GeoUtils.getUrl( "GSE100", GeoSource.FTP_VIA_HTTPS, GeoFormat.MINIML, GeoScope.FAMILY, GeoAmount.FULL ).toString() );
        assertEquals( "https://ftp.ncbi.nlm.nih.gov/geo/series/GSE100nnn/GSE100000/miniml/GSE100000_family.xml.tgz", GeoUtils.getUrl( "GSE100000", GeoSource.FTP_VIA_HTTPS, GeoFormat.MINIML, GeoScope.FAMILY, GeoAmount.FULL ).toString() );
        assertEquals( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE100000&targ=all&form=xml&view=brief", GeoUtils.getUrl( "GSE100000", GeoSource.DIRECT, GeoFormat.MINIML, GeoScope.FAMILY, GeoAmount.BRIEF ).toString() );
        assertEquals( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE100000", GeoUtils.getUrl( "GSE100000", GeoSource.DIRECT, GeoFormat.HTML, GeoScope.SELF, GeoAmount.BRIEF ).toString() );
        assertEquals( "ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSE100nnn/GSE100000/miniml/GSE100000_family.xml.tgz", GeoUtils.getUrl( "GSE100000", GeoSource.FTP, GeoFormat.MINIML, GeoScope.FAMILY, GeoAmount.FULL ).toString() );
        assertEquals( "https://www.ncbi.nlm.nih.gov/geo/download/?acc=GSE1&format=file&file=GSE1_test.txt", getUrlForSupplementaryMaterial( GeoRecordType.SERIES, "GSE1", "GSE1_test.txt", GeoSource.DIRECT ).toString() );
        assertEquals( "ftp://ftp.ncbi.nlm.nih.gov/geo/series/GSEnnn/GSE1/suppl/GSE1_test.txt", getUrlForSupplementaryMaterial( GeoRecordType.SERIES, "GSE1", "GSE1_test.txt", GeoSource.FTP ).toString() );
        assertEquals( "https://ftp.ncbi.nlm.nih.gov/geo/series/GSEnnn/GSE1/suppl/GSE1_test.txt", getUrlForSupplementaryMaterial( GeoRecordType.SERIES, "GSE1", "GSE1_test.txt", GeoSource.FTP_VIA_HTTPS ).toString() );
    }

    /**
     * 🛑 acc.cgi's targets are self / gsm / gpl / gse / all. A target it does not recognize is not an
     * error — it answers with the accession's own record — so {@code targ=samples} returned the 2 KB
     * series record where sample records were wanted, and nothing failed to say so. Measured against
     * GSE1024 on 2026-08-29: targ=gsm 57 KB / 36 sample records, targ=samples 2 KB / none.
     */
    @Test
    public void testTheDirectTargetsAreTheOnesGeoKnows() {
        assertEquals( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE1024&targ=gsm&form=text&view=brief",
                GeoUtils.getUrl( "GSE1024", GeoSource.DIRECT, GeoFormat.SOFT, GeoScope.SAMPLES, GeoAmount.BRIEF ).toString() );
        assertEquals( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE1024&targ=gpl&form=text&view=brief",
                GeoUtils.getUrl( "GSE1024", GeoSource.DIRECT, GeoFormat.SOFT, GeoScope.PLATFORM, GeoAmount.BRIEF ).toString() );
        assertEquals( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE1024&targ=gse&form=text&view=brief",
                GeoUtils.getUrl( "GSE1024", GeoSource.DIRECT, GeoFormat.SOFT, GeoScope.SERIES, GeoAmount.BRIEF ).toString() );
        assertEquals( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE1024&targ=self&form=text&view=brief",
                GeoUtils.getUrl( "GSE1024", GeoSource.DIRECT, GeoFormat.SOFT, GeoScope.SELF, GeoAmount.BRIEF ).toString() );
    }
}
