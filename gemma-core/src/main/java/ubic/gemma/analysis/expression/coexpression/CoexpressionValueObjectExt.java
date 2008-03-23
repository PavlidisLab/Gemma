/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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

import java.util.Collection;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.genome.Gene;

/**
 * Implementation note: This has very abbreviated field names to reduce the size of strings sent to browsers. Some
 * browsers have a problem.
 * 
 * @author luke
 * @version $Id$
 */
public class CoexpressionValueObjectExt {

    private Gene queryGene;
    private Gene foundGene;
    private String sortKey;
    private Integer supportKey;
    private Integer posLinks;
    private Integer negLinks;
    private Integer nonSpecPosLinks;
    private Integer nonSpecNegLinks;
    private Boolean hybWQuery;
    private Integer numTestedIn;
    private Integer goSim;
    private Integer maxGoSim;
    private String datasetVector;
    private Collection<Long> supportingExperiments;

    public Gene getQueryGene() {
        return queryGene;
    }

    public void setQueryGene( Gene queryGene ) {
        this.queryGene = queryGene;
    }

    public Gene getFoundGene() {
        return foundGene;
    }

    public void setFoundGene( Gene foundGene ) {
        this.foundGene = foundGene;
    }

    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey() {
        this.sortKey = String.format( "%06f%s", 1.0 / Math.abs( getSupportKey() ), getFoundGene().getOfficialSymbol() );
    }

    public Integer getSupportKey() {
        return supportKey;
    }

    public void setSupportKey( Integer supportKey ) {
        this.supportKey = supportKey;
    }

    public String getDatasetVector() {
        return datasetVector;
    }

    public void setDatasetVector( String datasetVector ) {
        this.datasetVector = datasetVector;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        if ( getPosLinks() > 0 ) {
            buf.append( getSupportRow( getPosLinks(), "+" ) );
        }
        if ( getNegLinks() > 0 ) {
            if ( buf.length() > 0 ) buf.append( "\n" );
            buf.append( getSupportRow( getNegLinks(), "-" ) );
        }
        return buf.toString();
    }

    private String getSupportRow( Integer links, String sign ) {
        String[] fields = new String[] { queryGene.getOfficialSymbol(), foundGene.getOfficialSymbol(),
                links.toString(), sign };
        return StringUtils.join( fields, "\t" );
    }

    public Integer getPosLinks() {
        return posLinks;
    }

    public void setPosLinks( Integer posLinks ) {
        this.posLinks = posLinks;
    }

    public Integer getNegLinks() {
        return negLinks;
    }

    public void setNegLinks( Integer negLinks ) {
        this.negLinks = negLinks;
    }

    public Integer getNonSpecPosLinks() {
        return nonSpecPosLinks;
    }

    public void setNonSpecPosLinks( Integer nonSpecPosLinks ) {
        this.nonSpecPosLinks = nonSpecPosLinks;
    }

    public Integer getNonSpecNegLinks() {
        return nonSpecNegLinks;
    }

    public void setNonSpecNegLinks( Integer nonSpecNegLinks ) {
        this.nonSpecNegLinks = nonSpecNegLinks;
    }

    public Boolean getHybWQuery() {
        return hybWQuery;
    }

    public void setHybWQuery( Boolean hybWQuery ) {
        this.hybWQuery = hybWQuery;
    }

    public Integer getNumTestedIn() {
        return numTestedIn;
    }

    public void setNumTestedIn( Integer numTestedIn ) {
        this.numTestedIn = numTestedIn;
    }

    public Integer getGoSim() {
        return goSim;
    }

    public void setGoSim( Integer goSim ) {
        this.goSim = goSim;
    }

    public Integer getMaxGoSim() {
        return maxGoSim;
    }

    public void setMaxGoSim( Integer maxGoSim ) {
        this.maxGoSim = maxGoSim;
    }

    public void setSortKey( String sortKey ) {
        this.sortKey = sortKey;
    }

    public Collection<Long> getSupportingExperiments() {
        return supportingExperiments;
    }

    public void setSupportingExperiments( Collection<Long> supportingExperiments ) {
        this.supportingExperiments = supportingExperiments;
    }

}
