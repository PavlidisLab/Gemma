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
package ubic.gemma.loader.genome.gene;

import java.io.InputStream;

/**
 * @author paul, Louise
 * @version $Id$
 */
public interface ExternalFileGeneLoaderService {

    /**
     * Work flow is: The file is first checked to see if readable, and the taxon checked to see it is in Gemma. If
     * validation passes the file is read line by line. Each line equates to a gene and its gene product, which is
     * created from the file details. The gene is then persisted. If successfully loaded taxon flag isGenesLoaded is set
     * to true to indicate that there are genes loaded for this taxon.
     * 
     * @param geneFile Full path to file containing genes details
     * @param taxon taxonName to be associated to this gene, does not have to be a species.
     * @return number of genes loaded
     * @exception Thrown with a file format error or problem in persisting gene to database.
     */
    public abstract int load( String geneFile, String taxonName ) throws Exception;

    /**
     * @param geneInputStream
     * @param taxonName
     * @return number of genes loaded
     * @throws Exception
     */
    public abstract int load( InputStream geneInputStream, String taxonName ) throws Exception;

}