/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.service;

import ubic.gemma.core.config.Settings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * Methods to generate annotations for array designs, based on information already in the database. This can be used to
 * generate annotation files used for ermineJ, for example. The file format:
 * <ul>
 * <li>The file is tab-delimited text. Comma-delimited files or Excel spreadsheets (for example) are not supported.</li>
 * <li>There is a one-line header included in the file for readability.</li>
 * <li>The first column contains the probe identifier</li>
 * <li>The second column contains a gene symbol(s). Clusters are delimited by '|' and genes within clusters are
 * delimited by ','</li>
 * <li>The third column contains the gene names (or description). Clusters are delimited by '|' and names within
 * clusters are delimited by '$'</li>
 * <li>The fourth column contains a delimited list of GO identifiers. These include the "GO:" prefix. Thus they read
 * "GO:00494494" and not "494494". Delimited by '|'.</li>
 * </ul>
 * <p>
 * Note that for backwards compatibility, GO terms are not segregated by gene cluster.
 * </p>
 *
 * @author paul
 */
public interface ArrayDesignAnnotationService {
    public enum OutputType {
        BIOPROCESS, LONG, SHORT
    }

    String ANNOTATION_FILE_SUFFIX = ".an.txt.gz";
    String BIO_PROCESS_FILE_SUFFIX = "_bioProcess";
    String NO_PARENTS_FILE_SUFFIX = "_noParents";
    /**
     * String included in file names for standard (default) annotation files. These include GO terms and all parents.
     */
    String STANDARD_FILE_SUFFIX = "";
    String ANNOTATION_FILE_DIRECTORY_NAME = "microAnnots";
    String ANNOT_DATA_DIR = Settings.getString( "gemma.appdata.home" ) + File.separatorChar + ANNOTATION_FILE_DIRECTORY_NAME + File.separatorChar;

    void deleteExistingFiles( ArrayDesign arrayDesign );

    /**
     * This tries to read one of the annotation files (noparents, bioprocess or regular) to get the gene information -
     * GO annotations are not part of the result.
     *
     * @param  arrayDesign array design
     * @return Map of composite sequence ids to an array of delimited strings: [probe name,genes symbol,
     *                     gene Name,
     *                     gemma gene id, ncbi id] for a given probe id. format of string is geneSymbol then geneNames
     *                     same as found
     *                     in annotation file.
     */
    Map<CompositeSequence, String[]> readAnnotationFile( ArrayDesign arrayDesign ) throws IOException;

    /**
     * Create (or update) all the annotation files for the given platform. Side effect: any expression experiment data
     * files that use this platform will be deleted.
     *
     * Format details:
     * There is a one-line header. The columns are:
     * <ol>
     * <li>Probe name
     * <li>Gene symbol. Genes located at different genome locations are delimited by "|"; multiple genes at the same
     * location are delimited by ",". Both can happen simultaneously.
     * <li>Gene name, delimited as for the symbol except '$' is used instead of ','.
     * <li>GO terms, delimited by '|'; multiple genes are not handled specially (for compatibility with ermineJ) -- unless useGO is false
     * <li>Gemma's gene ids, delimited by '|'
     * <li>NCBI gene ids, delimited by '|'
     * <li>Ensembl gene ids, delimited by '|'</li>
     * </ol>
     *  @param  inputAd     platform to process
     *
     * @param useGO            if true, GO terms will be included
     * @param deleteOtherFiles if true, other files conaining the annotations for this platform will be deleted, such as DEA results and data flat files.
     */
    void create( ArrayDesign inputAd,  Boolean useGO, boolean deleteOtherFiles ) throws IOException;

    /**
     * Generate an annotation for a list of genes, instead of probes. The second column will contain the NCBI id, if
     * available. Will generate the 'short' version.
     *
     * @param  writer the writer
     * @param  genes  genes
     * @param  useGO  if true, GO terms will be included
     * @return code
     */
    int generateAnnotationFile( Writer writer, Collection<Gene> genes, Boolean useGO );

}