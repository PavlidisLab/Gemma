package ubic.gemma.core.util.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author poirigui
 */
class LinuxRuntime extends ExtendedRuntime {

    private static final Path PROC_SELF_FILE = Paths.get( "/proc/self" );

    private static final Path PROCS_CPU_INFO = Paths.get( "/proc/cpuinfo" );
    private static final Pattern PROCS_CPU_INFO_FLAGS_PATTERN = Pattern.compile( "^flags\\s*:\\s*(.+)$" );

    private static final Path PROC_LOCKS_FILE = Paths.get( "/proc/locks" );
    // we only care about POSIX locks
    private static final Pattern PROC_LOCKS_PATTERN = Pattern.compile( "^(.+): POSIX {2}(ADVISORY|MANDATORY) {2}(READ|WRITE) (\\d+) (.+):(.+):(\\d+) (\\d+) (\\d+|EOF)$" );

    private static final Path PROC_MEMINFO_FILE = Paths.get( "/proc/meminfo" );

    @Override
    public int getPid() throws IOException {
        return Integer.parseInt( Files.readSymbolicLink( PROC_SELF_FILE ).getFileName().toString() );
    }

    @Override
    public CpuInfo[] getCpuInfo() throws IOException {
        List<CpuInfo> list = new ArrayList<>();
        String[] flags = null;
        for ( String line : Files.readAllLines( PROCS_CPU_INFO ) ) {
            Matcher match = PROCS_CPU_INFO_FLAGS_PATTERN.matcher( line );
            if ( match.matches() ) {
                flags = match.group( 1 ).split( " " );
            }
            if ( line.isEmpty() ) {
                list.add( new CpuInfo( Objects.requireNonNull( flags ) ) );
            }
        }
        return list.toArray( new CpuInfo[0] );
    }

    /**
     * Read system-wide lock metadata from /proc/locks.
     * <p>
     * This implementation has been tested on Fedora 41 and Rocky Linux 9.
     * <p>
     * FIXME: locks held through NFS do not show up under /proc/locks.
     */
    @Override
    public FileLockInfo[] getFileLockInfo() throws IOException {
        int myPid = getPid();
        List<FileLockInfo> result = new ArrayList<>();
        for ( String line : Files.readAllLines( PROC_LOCKS_FILE ) ) {
            Matcher matcher = PROC_LOCKS_PATTERN.matcher( line );
            if ( !matcher.matches() )
                continue;
            String id = matcher.group( 1 );
            boolean mandatory = "MANDATORY".equals( matcher.group( 2 ) );
            boolean exclusive = "WRITE".equals( matcher.group( 3 ) );
            int pid = Integer.parseInt( matcher.group( 4 ) );
            String majorDevice = matcher.group( 5 );
            String minorDevice = matcher.group( 6 );
            long inode = Long.parseLong( matcher.group( 7 ) );
            long start = Long.parseLong( matcher.group( 8 ) );
            long length = "EOF".equals( matcher.group( 9 ) ) ? Long.MAX_VALUE : ( Long.parseLong( matcher.group( 9 ) ) - start + 1 );
            result.add( new FileLockInfo( id, mandatory, exclusive, pid, pid == myPid, majorDevice, minorDevice, inode, start, length ) );
        }
        return result.toArray( new FileLockInfo[0] );
    }

    @Override
    public MemInfo getMemInfo() throws IOException {
        try ( BufferedReader br = Files.newBufferedReader( PROC_MEMINFO_FILE ) ) {
            return br
                    .lines()
                    .filter( l -> l.startsWith( "MemAvailable:" ) )
                    .map( l -> {
                        String[] pieces = l.split( "\\s+" );
                        long m = Long.parseLong( pieces[1] );
                        String unit = pieces[2];
                        switch ( unit.charAt( 0 ) ) {
                            case 'B':
                                return new MemInfo( m );
                            case 'k':
                                assert unit.charAt( 1 ) == 'B';
                                return new MemInfo( 1000 * m );
                            case 'm':
                                assert unit.charAt( 1 ) == 'B';
                                return new MemInfo( 1000000 * m );
                            default:
                                throw new RuntimeException();
                        }
                    } )
                    .findFirst()
                    .orElse( null );
        }
    }
}
