package ubic.gemma.core.loader.expression.geo.singleCell;

/**
 * Interface implemented by {@link SingleCellDetector} have the capabilities of looking-up archives.
 * @author poirigui
 */
public interface ArchiveBasedSingleCellDetector extends SingleCellDetector {

    long DEFAULT_MAX_ENTRY_SIZE_IN_ARCHIVE_TO_SKIP = 25_000_000L;
    long DEFAULT_MAX_NUMBER_OF_ENTRIES_TO_SKIP = 10;

    /**
     * Set the maximum size of an archive entry to skip the supplementary file altogether.
     * <p>
     * Use -1 to indicate no limit.
     * <p>
     * Note that if a relevant file was previously found in the archive, it will not be skipped.
     */
    void setMaxEntrySizeInArchiveToSkip( long maxEntrySizeInArchiveToSkip );

    /**
     * Set the maximum number of archive entries to skip in order to ignore the supplementary file altogether.
     * <p>
     * Use -1 to indicate no limit.
     * <p>
     * Note that if a relevant file was previously found in the archive, it will not be ignored.
     */
    void setMaxNumberOfEntriesToSkip( long maxNumberOfEntriesToSkip );
}
