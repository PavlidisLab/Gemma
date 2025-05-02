package ubic.gemma.core.util;

public class NetUtils {

    public static String bytePerSecondToDisplaySize( double bytesPerSecond ) {
        double bps = 8 * bytesPerSecond;
        if ( bps < 1e3 ) {
            return String.format( "%.2f bps", bps );
        } else if ( bps < 1e6 ) {
            return String.format( "%.2f Kbps", ( bps / 1e3 ) );
        } else if ( bps < 1e9 ) {
            return String.format( "%.2f Mbps", ( bps / 1e6 ) );
        } else {
            return String.format( "%.2f Gbps", ( bps / 1e9 ) );
        }
    }
}
