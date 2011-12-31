/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.web.visualization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DifferentialExpressionGenesConditionsValueObject {

    public class Condition {
        public long contrastId;
        public String baselineFactorValue;
        public String contrastFactorValue;
        public long resultSetId;
        public long analysisId;
        public String analysisType;
        public String experimentId;
        public String experimentName;
        public String datasetName;
        public Long datasetId;
        public Integer numberDiffExpressedProbes;
        public Integer numberDiffExpressedProbesUp;
        public Integer numberDiffExpressedProbesDown;
        public Long baselineFactorValueId;
        public String factorName;
        public String factorDescription;
        public Long factorId;
        public String factorCategory;
        public String experimentGroupName;
        public int experimentGroupIndex;
        public String datasetShortName;
        public Integer numberOfProbesOnArray;
        public String id;
        public boolean isSelected = false;

        public boolean getIsSelected() {
            return this.isSelected;
        }

        public long getContrastId() {
            return contrastId;
        }

        public String getBaselineFactorValue() {
            return baselineFactorValue;
        }

        public String getContrastFactorValue() {
            return contrastFactorValue;
        }

        public long getResultSetId() {
            return resultSetId;
        }

        public long getAnalysisId() {
            return analysisId;
        }

        public String getAnalysisType() {
            return analysisType;
        }

        public String getExperimentId() {
            return experimentId;
        }

        public String getExperimentName() {
            return experimentName;
        }

        public String getDatasetName() {
            return datasetName;
        }

        public Long getDatasetId() {
            return datasetId;
        }

        public Integer getNumberDiffExpressedProbes() {
            return numberDiffExpressedProbes;
        }

        public Integer getNumberDiffExpressedProbesUp() {
            return numberDiffExpressedProbesUp;
        }

        public Integer getNumberDiffExpressedProbesDown() {
            return numberDiffExpressedProbesDown;
        }

        public Long getBaselineFactorValueId() {
            return baselineFactorValueId;
        }

        public String getFactorName() {
            return factorName;
        }

        public String getFactorDescription() {
            return factorDescription;
        }

        public Long getFactorId() {
            return factorId;
        }

        public String getFactorCategory() {
            return factorCategory;
        }

        public String getExperimentGroupName() {
            return experimentGroupName;
        }

        public int getExperimentGroupIndex() {
            return experimentGroupIndex;
        }

        public String getDatasetShortName() {
            return datasetShortName;
        }

        public Integer getNumberOfProbesOnArray() {
            return numberOfProbesOnArray;
        }

        public String getId() {
            return id;
        }

    }

    public class Gene {
        public long id;
        public String name;
        public String fullName;
        public double specificityScore;
        private int groupIndex;
        private String groupName;
        public boolean isSelected = false;

        public boolean getIsSelected() {
            return this.isSelected;
        }

        public Gene( long id, String name, String fullName ) {
            this.id = id;
            this.name = name;
            this.fullName = fullName;
        }

        public long getId() {
            return id;
        }

        public void setId( long id ) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName( String name ) {
            this.name = name;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName( String fullName ) {
            this.fullName = fullName;
        }

        public double getSpecificityScore() {
            return specificityScore;
        }

        public void setSpecificityScore( double specificityScore ) {
            this.specificityScore = specificityScore;
        }

        public int getGroupIndex() {
            return groupIndex;
        }

        public void setGroupIndex( int groupIndex ) {
            this.groupIndex = groupIndex;
        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName( String groupName ) {
            this.groupName = groupName;
        }

    }

    public class Cell {
        public boolean isProbeMissing;
        public double visualizationValue;
        public double pValue;
        public double logFoldChange;
        public int direction;
        public int numberOfProbes;
        public int numberOfProbesDiffExpressed;
        

        public int getNumberOfProbes() {
            return numberOfProbes;
        }

        public int getNumberOfProbesDiffExpressed() {
            return numberOfProbesDiffExpressed;
        }

        public boolean getIsProbeMissing() {
            return isProbeMissing;
        }

        public double getVisualizationValue() {
            return visualizationValue;
        }

        public double getpValue() {
            return pValue;
        }

        public double getLogFoldChange() {
            return logFoldChange;
        }

        public int getDirection() {
            return direction;
        }

    }

    private Map<String, Map<Long, Cell>> cellData;
    private List<Condition> conditions;
    private List<Gene> genes;

    public DifferentialExpressionGenesConditionsValueObject() {
        cellData = new HashMap<String, Map<Long, Cell>>();
        conditions = new ArrayList<Condition>();
        genes = new ArrayList<Gene>();
    }

    public Map<String, Map<Long, Cell>> getCellData() {
        return cellData;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public List<Gene> getGenes() {
        return genes;
    }

    public void addGene( Gene gene ) {
        genes.add( gene );
    }

    public void addCondition( Condition condition ) {
        conditions.add( condition );
        // // Start with a column of missing values.
        for ( Gene gene : this.genes ) {
            this.addProbeMissingCell( gene.getId(), condition.getId() );
        }
    }

    public void addCell( Long geneId, String conditionId, double pValue, double foldChange, int numProbes, int numProbesDiffExpressed ) {
        Cell cell = new Cell();
        cell.isProbeMissing = false;
        cell.pValue = pValue;
        cell.logFoldChange = foldChange;
        cell.numberOfProbes = numProbes;
        cell.numberOfProbesDiffExpressed = numProbesDiffExpressed;
        
        addCell( geneId, conditionId, cell );
    }

    public void addProbeMissingCell( Long geneId, String conditionId ) {
        Cell cell = new Cell();
        cell.isProbeMissing = true;

        addCell( geneId, conditionId, cell );
    }

    public void addBlackCell( Long geneId, String conditionId ) {
        Cell cell = new Cell();
        cell.isProbeMissing = false;
        cell.pValue = 1;
        cell.visualizationValue = 0;
        cell.logFoldChange = 0;

        addCell( geneId, conditionId, cell );
    }

    private void addCell( Long geneId, String conditionId, Cell cell ) {
        Map<Long, Cell> geneToCellMap = cellData.get( conditionId );
        if ( geneToCellMap == null ) {
            geneToCellMap = new HashMap<Long, Cell>();
        }
        geneToCellMap.put( geneId, cell );
        cellData.put( conditionId, geneToCellMap );
    }

}
