package ubic.gemma.analysis.service;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

import ubic.gemma.analysis.service.ArrayDesignAnnotationServiceImpl.OutputType;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.ConfigUtils;

public interface ArrayDesignAnnotationService {

    public static final String ANNOTATION_FILE_SUFFIX = ".an.txt.gz";
    public static final String BIO_PROCESS_FILE_SUFFIX = "_bioProcess";
    public static final String NO_PARENTS_FILE_SUFFIX = "_noParents";
    /**
     * String included in file names for standard (default) annotation files. These include GO terms and all parents.
     */
    public static final String STANDARD_FILE_SUFFIX = "";
    public static final String ANNOT_DATA_DIR = ConfigUtils.getString( "gemma.appdata.home" ) + File.separatorChar
            + "microAnnots" + File.separatorChar;

    /**
     * Format details:
     * <p>
     * There is a one-line header. The columns are:
     * <ol>
     * <li>Probe name
     * <li>Gene symbol. Genes located at different genome locations are delimited by "|"; multiple genes at the same
     * location are delimited by ",". Both can happen simultaneously.
     * <li>Gene name, delimited as for the symbol except '$' is used instead of ','.
     * <li>GO terms, delimited by '|'; multiple genes are not handled specially (for compatibility with ermineJ)
     * <li>Gemma's gene ids, delimited by '|'
     * <li>NCBI gene ids, delimited by '|'
     * </ol>
     * 
     * @param writer
     * @param genesWithSpecificity map of cs ->* physical location ->* ( blat association ->* gene product -> gene)
     * @param ty whether to include parents (OutputType.LONG); only use biological process (OutputType.BIOPROCESS) or
     *        'standard' output (OutputType.SHORT).
     * @param knownGenesOnly Whether output should include PARs and predicted genes. If true, they will be excluded.
     * @return number processed.
     * @throws IOException
     */
    public abstract int generateAnnotationFile( Writer writer,
            Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity, OutputType ty,
            boolean knownGenesOnly ) throws IOException;

    /**
     * Generate an annotation for a list of genes, instead of probes. The second column will contain the NCBI id, if
     * available.
     * 
     * @param writer
     * @param genes
     * @param type
     * @return
     */
    public abstract int generateAnnotationFile( Writer writer, Collection<Gene> genes, OutputType type );

    /**
     * Opens a file for writing and adds the header.
     * 
     * @param arrayDesign
     * @param fileBaseName if Null, output will be written to standard output.
     * @param overWrite clobber existing file. Otherwise returns null.
     * @return writer to use
     * @throws IOException
     */
    public abstract Writer initOutputFile( ArrayDesign arrayDesign, String fileBaseName, boolean overWrite )
            throws IOException;

}