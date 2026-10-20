package ubic.gemma.core.util.runtime;

/**
 * @author poirigui
 */
class DummyRuntime extends ExtendedRuntime {

    @Override
    public int getPid() {
        return -1;
    }

    @Override
    public FileLockInfo[] getFileLockInfo() {
        return new FileLockInfo[0];
    }

    @Override
    public CpuInfo[] getCpuInfo() {
        return new CpuInfo[0];
    }

    @Override
    public MemInfo getMemInfo() {
        return new MemInfo( 0 );
    }
}
