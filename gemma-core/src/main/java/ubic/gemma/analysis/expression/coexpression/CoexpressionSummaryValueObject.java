/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.expression.coexpression;

/**
 * @author luke
 * @version $Id$
 */
public class CoexpressionSummaryValueObject {

    int datasetsAvailable;
    int datasetsTested;
    int datasetsWithSpecificProbes;

    public int getDatasetsWithSpecificProbes() {
        return datasetsWithSpecificProbes;
    }

    public void setDatasetsWithSpecificProbes( int datasetsWithSpecificProbes ) {
        this.datasetsWithSpecificProbes = datasetsWithSpecificProbes;
    }

    int linksFound;
    int linksMetPositiveStringency;
    int linksMetNegativeStringency;

    public int getDatasetsAvailable() {
        return datasetsAvailable;
    }

    public void setDatasetsAvailable( int datasetsAvailable ) {
        this.datasetsAvailable = datasetsAvailable;
    }

    public int getDatasetsTested() {
        return datasetsTested;
    }

    public void setDatasetsTested( int datasetsTested ) {
        this.datasetsTested = datasetsTested;
    }

    public int getLinksFound() {
        return linksFound;
    }

    public void setLinksFound( int linksFound ) {
        this.linksFound = linksFound;
    }

    public int getLinksMetNegativeStringency() {
        return linksMetNegativeStringency;
    }

    public void setLinksMetNegativeStringency( int linksMetNegativeStringency ) {
        this.linksMetNegativeStringency = linksMetNegativeStringency;
    }

    public int getLinksMetPositiveStringency() {
        return linksMetPositiveStringency;
    }

    public void setLinksMetPositiveStringency( int linksMetPositiveStringency ) {
        this.linksMetPositiveStringency = linksMetPositiveStringency;
    }

}
