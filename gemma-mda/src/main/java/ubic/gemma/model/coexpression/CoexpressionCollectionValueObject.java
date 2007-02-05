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
package ubic.gemma.model.coexpression;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

/**
 * @author pavlidis
 * @version $Id$
 */
public class CoexpressionCollectionValueObject {
    private int linkCount;
    private int stringencyLinkCount;
    private Collection<ExpressionExperimentValueObject> expressionExperiments;
    private Collection<CoexpressionValueObject> coexpressionData;
    private double firstQuerySeconds;
    private double secondQuerySeconds;
    private double postProcessSeconds;

    public CoexpressionCollectionValueObject() {
        linkCount = 0;
        stringencyLinkCount = 0;
        coexpressionData = new HashSet<CoexpressionValueObject>();
        expressionExperiments = new HashSet<ExpressionExperimentValueObject>();
    }

    /**
     * @return the coexpressionData
     */
    public Collection<CoexpressionValueObject> getCoexpressionData() {
        return coexpressionData;
    }

    /**
     * @param coexpressionData the coexpressionData to set
     */
    public void setCoexpressionData( Collection<CoexpressionValueObject> coexpressionData ) {
        this.coexpressionData = coexpressionData;
    }

    /**
     * @return the linkCount
     */
    public int getLinkCount() {
        return linkCount;
    }

    /**
     * @param linkCount the linkCount to set
     */
    public void setLinkCount( int linkCount ) {
        this.linkCount = linkCount;
    }

    /**
     * @return the stringencyLinkCount
     */
    public int getStringencyLinkCount() {
        return stringencyLinkCount;
    }

    /**
     * @param stringencyLinkCount the stringencyLinkCount to set
     */
    public void setStringencyLinkCount( int stringencyLinkCount ) {
        this.stringencyLinkCount = stringencyLinkCount;
    }

    /**
     * @return the expressionExperiments
     */
    public Collection<ExpressionExperimentValueObject> getExpressionExperiments() {
        return expressionExperiments;
    }

    /**
     * @param expressionExperiments the expressionExperiments to set
     */
    public void setExpressionExperiments( Collection<ExpressionExperimentValueObject> expressionExperiments ) {
        this.expressionExperiments = expressionExperiments;
    }

    /**
     * Add an expression experiment to the list
     * 
     * @param vo
     */
    public void addExpressionExperiment( ExpressionExperimentValueObject vo ) {
        this.expressionExperiments.add( vo );
    }

    /**
     * Add a collection of expression experiment to the list
     * 
     * @param vo
     */
    public void addExpressionExperiment( Collection<ExpressionExperimentValueObject> vos ) {
        this.expressionExperiments.addAll( vos );
    }

    public void setFirstQueryElapsedTime( Long elapsed ) {
        this.firstQuerySeconds = elapsed / 1000.0;

    }

    public void setSecondQueryElapsedTime( Long elapsed ) {
        this.secondQuerySeconds = elapsed / 1000.0;

    }

    public void setPostProcessTime( Long elapsed ) {
        this.postProcessSeconds = elapsed / 1000.0;

    }

    public double getFirstQuerySeconds() {
        return firstQuerySeconds;
    }

    public double getPostProcessSeconds() {
        return postProcessSeconds;
    }

    public double getSecondQuerySeconds() {
        return secondQuerySeconds;
    }

}
