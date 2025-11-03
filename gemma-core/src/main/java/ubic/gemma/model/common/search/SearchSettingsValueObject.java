/*
 * The gemma project
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
package ubic.gemma.model.common.search;

import lombok.Data;
import ubic.gemma.model.common.ValueObject;

import java.io.Serializable;

/**
 * author: anton date: 18/03/13
 */
@Data
@ValueObject
public class SearchSettingsValueObject implements Serializable {

    private static final long serialVersionUID = -934534534L;

    private String query;
    private String termUri;

    private String datasetConstraint;
    private String platformConstraint;
    private String taxonConstraint;

    private boolean searchBibrefs = Boolean.TRUE;
    private boolean searchBioSequences = Boolean.TRUE;
    private boolean searchExperiments = Boolean.TRUE;
    private boolean searchExperimentSets = Boolean.TRUE;
    private boolean searchGenes = Boolean.TRUE;
    private boolean searchGeneSets = Boolean.TRUE;
    private boolean searchPlatforms = Boolean.TRUE;
    private boolean searchProbes = Boolean.TRUE;

    private boolean useCharacteristics = Boolean.TRUE;
    private boolean useDatabase = Boolean.TRUE;
    private boolean useGo = Boolean.TRUE;
    private boolean useIndices = Boolean.TRUE;

    private Integer maxResults = null;
}
