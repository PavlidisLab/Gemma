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
package ubic.gemma.web.controller.visualization;

import ubic.gemma.model.common.quantitationtype.QuantitationType;

import java.io.Serializable;

/**
 * Expression experiment command object that wraps expression experiment visualization preferences.
 *
 * @author keshav
 * @author pavlidis
 */
@SuppressWarnings("WeakerAccess") // Used in front end
public class ExpressionExperimentVisualizationCommand implements Serializable {

    private static final long serialVersionUID = 2166768356457316142L;
    private String searchCriteria = null;
    private String name = null;
    private Long expressionExperimentId = null;
    private String searchString = null;
    private boolean viewSampling;
    private boolean maskMissing = false;
    private QuantitationType quantitationType;

    public ExpressionExperimentVisualizationCommand() {
        this.quantitationType = QuantitationType.Factory.newInstance();
    }

    public Long getExpressionExperimentId() {
        return expressionExperimentId;
    }

    public void setExpressionExperimentId( Long id ) {
        this.expressionExperimentId = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public QuantitationType getQuantitationType() {
        return quantitationType;
    }

    public void setQuantitationType( QuantitationType quantitationType ) {
        this.quantitationType = quantitationType;
    }

    public String getSearchCriteria() {
        return searchCriteria;
    }

    public void setSearchCriteria( String searchCriteria ) {
        this.searchCriteria = searchCriteria;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString( String searchString ) {
        this.searchString = searchString;
    }

    public boolean isMaskMissing() {
        return maskMissing;
    }

    public void setMaskMissing( boolean maskMissing ) {
        this.maskMissing = maskMissing;
    }

    public boolean isViewSampling() {
        return viewSampling;
    }

    public void setViewSampling( boolean viewSampling ) {
        this.viewSampling = viewSampling;
    }

}
