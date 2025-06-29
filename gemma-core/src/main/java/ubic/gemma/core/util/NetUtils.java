package ubic.gemma.core.util;

public class NetUtils {

    public static String bytePerSecondToDisplaySize( double bytesPerSecond ) {
        if ( bytesPerSecond < 1e3 ) {
            return String.format( "%.2f B/s", bytesPerSecond );
        } else if ( bytesPerSecond < 1e6 ) {
            return String.format( "%.2f KB/s", ( bytesPerSecond / 1e3 ) );
        } else if ( bytesPerSecond < 1e9 ) {
            return String.format( "%.2f MB/s", ( bytesPerSecond / 1e6 ) );
        } else {
            return String.format( "%.2f GB/s", ( bytesPerSecond / 1e9 ) );
        }
    }
}
