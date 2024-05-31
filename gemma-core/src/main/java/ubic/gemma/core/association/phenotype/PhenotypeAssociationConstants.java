/*
 * The Gemma project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.association.phenotype;

import ubic.gemma.core.config.Settings;

import java.io.File;

@SuppressWarnings({ "WeakerAccess", "unused" }) // Possible external use (ermineJ)
@Deprecated
public class PhenotypeAssociationConstants {

    public final static String PHENOTYPE = "Phenotype";
    public final static String PHENOTYPE_CATEGORY_URI = "http://www.ebi.ac.uk/efo/EFO_0000651";
    public final static String TAXON_HUMAN = "human";
    public final static String TAXON_MOUSE = "mouse";
    public final static String TAXON_RAT = "rat";
    public final static String[] TAXA_IN_USE = { PhenotypeAssociationConstants.TAXON_HUMAN,
            PhenotypeAssociationConstants.TAXON_MOUSE, PhenotypeAssociationConstants.TAXON_RAT };

    // those are used to write files the PhenocartaExport

    public final static String DISEASE_ONTOLOGY_ROOT = "MONDO_0000001";
    public final static String PHENOCARTA_EXPORT = "PhenocartaExport";
    public final static String MANUAL_CURATION = "Manual Curation";

    // path to where to place the files on the isntance running it
    public final static String PHENOCARTA_HOME_FOLDER_PATH = Settings.getString( "gemma.appdata.home" ) + File.separator
            + PhenotypeAssociationConstants.PHENOCARTA_EXPORT + File.separator;

    public final static String PHENOCARTA_NAME = "phenocarta";
    public final static String LATEST_EVIDENCE_EXPORT = "LatestEvidenceExport";
    // names of folder and files where things are kept
    public final static String FILE_ALL_PHENOCARTA_ANNOTATIONS = "AllPhenocartaAnnotations.tsv";
    public final static String FILE_MANUAL_CURATION = "ManualCuration.tsv";
    public final static String DATASET_FOLDER_NAME = "AnnotationsByDataset";
    public final static String ERMINEJ_FOLDER_NAME = "ErmineJ";

    // path to the final files on production
    public final static String GEMMA_PHENOCARTA_HOST_URL =
            Settings.getHostUrl() + "/" + PhenotypeAssociationConstants.PHENOCARTA_NAME + File.separator
                    + PhenotypeAssociationConstants.LATEST_EVIDENCE_EXPORT + File.separator;
    // those are folders
    public final static String GEMMA_PHENOCARTA_HOST_URL_DATASETS =
            PhenotypeAssociationConstants.GEMMA_PHENOCARTA_HOST_URL + PhenotypeAssociationConstants.DATASET_FOLDER_NAME
                    + File.separator;
    public final static String GEMMA_PHENOCARTA_HOST_URL_ERMINEJ =
            PhenotypeAssociationConstants.GEMMA_PHENOCARTA_HOST_URL + PhenotypeAssociationConstants.ERMINEJ_FOLDER_NAME
                    + File.separator;
    // those are files
    public final static String ALL_PHENOCARTA_ANNOTATIONS_FILE_LOCATION =
            PhenotypeAssociationConstants.GEMMA_PHENOCARTA_HOST_URL
                    + PhenotypeAssociationConstants.FILE_ALL_PHENOCARTA_ANNOTATIONS;
    public final static String MANUAL_CURATION_FILE_LOCATION =
            PhenotypeAssociationConstants.GEMMA_PHENOCARTA_HOST_URL_DATASETS
                    + PhenotypeAssociationConstants.FILE_MANUAL_CURATION;

}
