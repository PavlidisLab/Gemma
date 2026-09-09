package ubic.gemma.core.loader.expression.geo.model;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * SRA's {@code library_strategy} controlled vocabulary, as GEO reports it in
 * {@code !Sample_library_strategy}.
 * <p>
 * 🛑 <b>Completeness is the point, not tidiness.</b> {@code GeoFamilyParser} THROWS on a value it does not
 * recognize, and it does so deliberately: the alternative — folding the unknown into {@link #OTHER} — would
 * be unsafe, because {@code OTHER} is one of the three strategies {@code GeoConverterImpl} ALLOWS through as
 * expression data. An unrecognized chromatin or genomic assay would then be imported as if it were RNA. So a
 * missing constant costs an import failure, and the fix for that is to list the value, never to widen the
 * fallback.
 * <p>
 * The vocabulary below is SRA's, plus the two single-cell spellings GEO adds on its own
 * ({@code scRNA-Seq}, {@code snRNA-Seq}).
 * <p>
 * ⚠️ Naming an assay here does not admit it. {@code GeoConverterImpl} accepts only {@code RNA_SEQ},
 * {@code SSRNA_SEQ} and {@code OTHER} (with {@code libSource == TRANSCRIPTOMIC}); everything else is
 * recognized in order to be REFUSED with a reason rather than to crash.
 * <p>
 * ⚠️ {@link #RIBO_SEQ} will match fewer samples than its name suggests. Ribosome profiling is usually
 * submitted as {@code OTHER} — GSE288755's samples are titled {@code … Ribo-seq replicate #1} and
 * {@code … TCP-seq replicate 1} while their {@code library_strategy} reads {@code OTHER} — so the constant is
 * here for series that DO declare it, not to reclassify the ones that do not.
 *
 * @author gembro
 */
public enum GeoLibraryStrategy {

    // ---- transcriptomic ----
    RNA_SEQ( "RNA-Seq" ),
    SSRNA_SEQ( "ssRNA-seq" ),
    MIRNA_SEQ( "miRNA-Seq" ),
    NCRNA_SEQ( "ncRNA-Seq" ),
    RIBO_SEQ( "Ribo-Seq" ),
    FL_CDNA( "FL-cDNA" ),
    EST( "EST" ),
    RIP_SEQ( "RIP-Seq" ),
    /** GEO's own, not SRA's. */
    SCRNA_SEQ( "scRNA-Seq" ),
    /** GEO's own, not SRA's. */
    SNRNA_SEQ( "snRNA-Seq" ),

    // ---- chromatin / epigenomic ----
    ATAC_SEQ( "ATAC-seq" ),
    BISULFITE_SEQ( "Bisulfite-Seq" ),
    CHIA_PET( "ChIA-PET" ),
    CHIP_SEQ( "ChIP-Seq" ),
    CHM_SEQ( "ChM-Seq" ),
    DNASE_HYPERSENSITIVITY( "DNase-Hypersensitivity" ),
    FAIRE_SEQ( "FAIRE-seq" ),
    HI_C( "Hi-C" ),
    /** 🛑 Spelled MDB rather than MBD; the constant predates this class and is PERSISTED in {@code BIO_ASSAY.LIBRARY_STRATEGY}, so renaming it would orphan stored rows. */
    MDB_SEQ( "MBD-Seq" ),
    MEDIP_SEQ( "MeDIP-Seq" ),
    MNASE_SEQ( "MNase-Seq" ),
    MRE_SEQ( "MRE-Seq" ),
    NOME_SEQ( "NOMe-Seq" ),
    TETHERED_CHROMATIN_CONFORMATION_CAPTURE( "Tethered Chromatin Conformation Capture" ),

    // ---- genomic ----
    WGS( "WGS" ),
    WGA( "WGA" ),
    WXS( "WXS" ),
    WCS( "WCS" ),
    AMPLICON( "AMPLICON" ),
    TARGETED_CAPTURE( "Targeted-Capture" ),
    RAD_SEQ( "RAD-Seq" ),
    GBS( "GBS" ),
    CLONE( "CLONE" ),
    CLONEEND( "CLONEEND" ),
    POOLCLONE( "POOLCLONE" ),
    FINISHING( "FINISHING" ),
    SYNTHETIC_LONG_READ( "Synthetic-Long-Read" ),
    CTS( "CTS" ),
    TN_SEQ( "Tn-Seq" ),
    SELEX( "SELEX" ),
    VALIDATION( "VALIDATION" ),

    OTHER( "OTHER" );

    private static final Map<String, GeoLibraryStrategy> BY_GEO_STRING = new HashMap<>();

    static {
        for ( GeoLibraryStrategy s : values() ) {
            BY_GEO_STRING.put( s.geoString.toLowerCase( Locale.ROOT ), s );
        }
    }

    private final String geoString;

    GeoLibraryStrategy( String geoString ) {
        this.geoString = geoString;
    }

    /**
     * @return the spelling GEO uses in {@code !Sample_library_strategy}
     */
    public String getGeoString() {
        return geoString;
    }

    /**
     * Resolve GEO's spelling, case-insensitively.
     *
     * @return the matching constant, or {@code null} when the vocabulary does not contain it — the caller
     *         decides what an unknown value means, and {@code GeoFamilyParser} treats it as fatal
     */
    @Nullable
    public static GeoLibraryStrategy fromGeoString( String s ) {
        return BY_GEO_STRING.get( s.trim().toLowerCase( Locale.ROOT ) );
    }
}
