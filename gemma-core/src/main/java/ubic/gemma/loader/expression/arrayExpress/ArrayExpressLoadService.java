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
package ubic.gemma.loader.expression.arrayExpress;

import java.io.InputStream;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
public interface ArrayExpressLoadService {

    /**
     * Load an AE dataset into the db. NOTE this currently will not handle data sets that have multiple array designs.
     * 
     * @param accession e.g. E-AFMX-
     * @return
     */
    public abstract ExpressionExperiment load( String accession );

    /**
     * Load an AE dataset into the db. NOTE this currently will not handle data sets that have multiple array designs.
     * 
     * @param accession e.g. E-AFMX-4
     * @param adAccession accession for the array design, either short name or name.
     * @param allowArrayExpressDesign if true, the array design will be loaded from ArrayExpress. Should only be used if
     *        the array design isn't in Gemma or GEO.
     * @return
     */
    public abstract ExpressionExperiment load( String accession, String adAccession, boolean allowArrayExpressDesign );

    /**
     * Process an AE dataset and optinally load it into the database. NOTE this currently will not handle data sets that
     * have multiple array designs.
     * 
     * @param accession e.g. E-AFMX-4
     * @param adAccession accession for the array design, either short name or name, as represented in Gemma. This
     *        should be filled in if we are associating with the array design from Gemma.
     * @param allowArrayExpressDesign if true, the array design will be loaded from ArrayExpress. Should only be used if
     *        the array design isn't in Gemma or GEO.
     * @param useDb save to the database; otherwise run in 'test mode'.
     * @return
     */
    public abstract ExpressionExperiment load( String accession, String adAccession, boolean allowArrayExpressDesign,
            boolean useDb );

    /**
     * Designed for test purposes, not likely to be used otherwise. Writes to the database.
     * 
     * @param mageMlStream
     * @param processedDataStream
     * @param accession In ArrayExpress, e.g. E-TAMB-1
     * @param adAccession
     * @return
     */
    public abstract ExpressionExperiment load( InputStream mageMlStream, InputStream processedDataStream,
            String accession, String adAccession );

}