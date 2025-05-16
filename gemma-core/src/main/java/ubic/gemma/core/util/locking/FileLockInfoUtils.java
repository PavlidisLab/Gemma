package ubic.gemma.core.util.locking;

public class FileLockInfoUtils {

    public static String format( FileLockInfo lockInfo ) {
        StringBuilder message = new StringBuilder();
        message.append( "Lock status for " ).append( lockInfo.getPath() ).append( "\n" )
                .append( "Lockfile:\t" ).append( lockInfo.getLockfilePath() ).append( "\n" )
                .append( "Number of readers:\t" ).append( lockInfo.getReadLockCount() ).append( "\n" )
                .append( "Write locked:\t" ).append( lockInfo.isWriteLocked() ? "yes" : "no" );
        if ( !lockInfo.getProcInfo().isEmpty() ) {
            message.append( "\nProcess info:" );
            for ( FileLockInfo.ProcessInfo processInfo : lockInfo.getProcInfo() ) {
                message.append( "\n\t" )
                        .append( "PID: " ).append( processInfo.getPid() ).append( processInfo.isSelf() ? " (self)" : "" ).append( ", " )
                        .append( "Mandatory: " ).append( processInfo.isMandatory() ? "yes" : "no" ).append( ", " )
                        .append( "Exclusive: " ).append( processInfo.isExclusive() ? "yes" : "no" ).append( ", " )
                        .append( "Start: " ).append( processInfo.getStart() ).append( ", " )
                        .append( "Length: " ).append( processInfo.getLength() );
            }
        } else {
            message.append( "\nNo process is holding this lock." );
        }

        return message.toString();
    }
}
