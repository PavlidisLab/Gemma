/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.loader.expression.smd;

import java.util.Set;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * Defines a class that can retrieve and persist data from the Stanford Microarray Database (SMD).
 * <hr>
 * <p>
 * 
 * @author pavlidis
 * @version $Id$
 */
@Deprecated
public interface SMDManager {

    /**
     * Get all the publications from the SMD site and persist them.
     * <p>
     * Note that creation of a BibliographicReference from SMD will result in the creation of Experiment objects (though
     * not filled-in completely). These objects should be filled in by retrieving the Experiment from SMD.
     * 
     * @return Set of BibliographicReferences
     */
    public Set fetchPublications();

    /**
     * Get all the publications for a particular species from the SMD site and persist them.
     * 
     * @return Set of BibliographicReferences
     */
    public Set fetchPublications( Taxon species );

    /**
     * Get all new publications from the SMD site and persist them.
     * 
     * @return Set of BibliographicReferences
     */
    public Set fetchNewPublications();

    /**
     * Get all new publications for a particular species from the SMD site and persist them.
     * 
     * @return Set of BibliographicReferences
     */
    public Set fetchNewPublications( Taxon species );

    /**
     * Get a particular publication from the SMD site and persist it.
     * 
     * @param accessionNumber
     * @return
     */
    public BibliographicReference fetchPublication( int accessionNumber );

    /**
     * Get all experiments from the SMD site and persist them.
     * <p>
     * Retrieving experiments can result in the creation of new objects of type:
     * <ul>
     * <li>BioAssay
     * <li>DatabaseEntry
     * <li>Person
     * </ul>
     * 
     * @return Set of Experiments.
     */
    public Set fetchExperiments();

    /**
     * Try to fill in details about an SMD Experiment by retrieving the associated "expset_XXX.meta" file from the SMD
     * site, plus a data file (to view the header, which has the contact information).
     * <p>
     * The filled-in Experiment is updated in the database.
     * <p>
     * This may result in the creation of new Person objects.
     * 
     * @param unfinishedExperiment
     * @return
     */
    public ExpressionExperiment fetchExperiment( ExpressionExperiment unfinishedExperiment );

    /**
     * Get all experiments for a particular species from the SMD site and persist them.
     * 
     * @return Set of Experiments.
     */
    public Set fetchExperiments( Taxon species );

    /**
     * Get all new experiments from the SMD site and persist them.
     * 
     * @return Set of Experiments.
     */
    public Set fetchNewExperiments();

    /**
     * Get all new experiments for a particular species from the SMD site and persist them.
     * 
     * @return Set of Experiments.
     */
    public Set fetchNewExperiments( Taxon species );

    /**
     * Get and persist a particular experiment from the SMD site.
     * 
     * @param accessionNumber
     * @return
     */
    public ExpressionExperiment fetchExperiment( int accessionNumber );

    /**
     * Get and persist Experiments associated with a given BibliographicReference.
     * 
     * @param publication
     * @return
     */
    public Set fetchExperiment( BibliographicReference publication );

    /**
     * Get and persist the BioAssays associated with a given Experiment. . The ubic.description.File objects
     * corresponding to it are persisted.
     * <p>
     * Note that creation of a BioAssay may result in the creation of the following additional objects, based on
     * information from SMD files:
     * <ul>
     * <li>Person
     * <li>DatabaseEntry
     * <li>BioMaterial
     * <li>FactorValue
     * <li>Protocol
     * <li>FileFormat
     * <li>File
     * <li>Description
     * <li>ArrayDesign
     * </ul>
     * 
     * @param experiment
     * @return
     */
    public Set fetchBioAssays( ExpressionExperiment experiment );

    /**
     * Get a particular bioassay from the SMD site.
     * 
     * @param accessionNumber
     * @return
     */
    public BioAssay fetchBioAssay( int accessionNumber );

    /**
     * Try to fill in the details about a given BioAssay. This is used to "fill in" BioAssays that were "started" based
     * only on the Experiment information. This requires examination of the header of a data File.
     * 
     * @param unfinishedBioAssay
     * @return
     */
    public BioAssay fetchBioAssay( BioAssay unfinishedBioAssay );

    /**
     * Get the data files for one experiment from the SMD site. The ubic.description.File objects corresponding to it
     * are persisted.
     * 
     * @param experientAccessionNumber
     * @return a Set of ubic.description.File objects representing the
     */
    public Set fetchDataFiles( int experimentAccessionNumber );

    /**
     * Get the data file for one bio assay from the SMD site. The ExternalFile object corresponding to it is persisted.
     * 
     * @param bioAssayAccessionNumber
     * @return
     */
    public ubic.gemma.model.common.description.LocalFile fetchDataFile( int bioAssayAccessionNumber );

}