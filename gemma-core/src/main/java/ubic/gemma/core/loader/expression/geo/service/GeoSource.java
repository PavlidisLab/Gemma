package ubic.gemma.core.loader.expression.geo.service;

public enum GeoSource {
    /**
     * Directly access GEO via <a href="https://www.ncbi.nlm.nih.gov/geo">www.ncbi.nlm.nih.gov/geo</a>. This is usually
     * the least efficient way to access GEO data, so use it as a last resort.
     */
    DIRECT,
    FTP_VIA_HTTPS,
    FTP
}
