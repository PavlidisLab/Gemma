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
package ubic.gemma.tasks.visualization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ubic.gemma.model.analysis.Direction;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.HitListSize;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Represents a complete set of data for a differential expression query over a set of genes x conditions (resultSets x
 * contrasts).
 * 
 * @author Anton
 * @version $Id$
 */
public class DifferentialExpressionGenesConditionsValueObject {
    // The details of the result for a gene x condition combination.
    public class Cell {
        private boolean isProbeMissing;
        private Double visualizationValue;
        private Double pValue = null; // important!
        private Double logFoldChange = 0.0;
        private Integer direction = 0;
        private Integer numberOfProbes = 0;
        private Integer numberOfProbesDiffExpressed = 0;
        private Double correctedPValue = null; // important

        public Double getCorrectedPValue() {
            return correctedPValue;
        }

        public int getDirection() {
            return direction;
        }

        public Boolean getIsProbeMissing() {
            return isProbeMissing;
        }

        public Double getLogFoldChange() {
            return logFoldChange;
        }

        public Integer getNumberOfProbes() {
            return numberOfProbes;
        }

        public Integer getNumberOfProbesDiffExpressed() {
            return numberOfProbesDiffExpressed;
        }

        public Double getpValue() {
            return pValue;
        }

        public Double getVisualizationValue() {
            return visualizationValue;
        }

    }

    // Represents one column in the differential expression view; one contrast in a resultset in an experiment.
    public class Condition {
        private String baselineFactorValue;
        private String contrastFactorValue;
        private Long resultSetId;
        private Long analysisId;
        private String analysisType;
        private Boolean isSubset = false;

        private String experimentId;

        private String experimentName;
        private String datasetName;
        private Long datasetId;
        private Integer numberDiffExpressedProbes = -1;
        private Integer numberDiffExpressedProbesUp = -1;
        private Integer numberDiffExpressedProbesDown = -1;
        private Long baselineFactorValueId;
        private String factorName;
        private String factorDescription;
        private Long factorId;
        private String factorCategory;
        private String experimentGroupName;
        private int experimentGroupIndex;
        private String datasetShortName;
        private Integer numberOfProbesOnArray;
        private String id;

        private boolean isSelected = false;

        private Integer numberOfGenesTested;
        private Integer numberOfGenesDiffExpressed;
        
        private Long factorValueId;

        public Condition( ExpressionExperiment experiment,
                          DifferentialExpressionAnalysis analysis,
                          ExpressionAnalysisResultSet resultSet,
                          FactorValue factorValue ) {
            this( resultSet.getId(), factorValue.getId() );
            numberOfProbesOnArray = resultSet.getNumberOfProbesTested();
            numberOfGenesTested = resultSet.getNumberOfGenesTested();
            ExperimentalFactor factor = factorValue.getExperimentalFactor();
            datasetShortName = experiment.getShortName();
            datasetName = experiment.getName();
            datasetId = experiment.getId();
            analysisId = analysis.getId();
            baselineFactorValueId = resultSet.getBaselineGroup().getId();
            factorName = factor.getName();
            contrastFactorValue = getFactorValueString( factorValue );
            baselineFactorValue = getFactorValueString( resultSet.getBaselineGroup() );
            factorDescription = factor.getDescription();
            factorId = factor.getId();

            factorCategory = ( factor.getCategory() == null ) ? "[No category]" : factor.getCategory().getCategory();

            this.isSubset = analysis.getSubsetFactorValue() != null;

            for ( HitListSize h : resultSet.getHitListSizes() ) {
                if ( h.getThresholdQvalue() == THRESHOLD_QVALUE_FOR_HITLISTS ) {
                    if ( h.getDirection().equals( Direction.DOWN ) ) {
                        numberDiffExpressedProbesDown = h.getNumberOfProbes();
                    } else if ( h.getDirection().equals( Direction.UP ) ) {
                        numberDiffExpressedProbesUp = h.getNumberOfProbes();
                    } else if ( h.getDirection().equals( Direction.EITHER ) ) {
                        numberDiffExpressedProbes = h.getNumberOfProbes();
                        numberOfGenesDiffExpressed = h.getNumberOfGenes();
                    }
                }
            }
        }

        public Condition( Long resultSetId, Long factorValueId ) {
            this.resultSetId = resultSetId;
            this.factorValueId = factorValueId;
            this.id = constructConditionId( resultSetId, factorValueId );
        }

        @Override
        public boolean equals( Object obj ) {
            if ( this == obj ) return true;
            if ( obj == null ) return false;
            if ( getClass() != obj.getClass() ) return false;
            Condition other = ( Condition ) obj;
            if ( !getOuterType().equals( other.getOuterType() ) ) return false;
            if ( id == null ) {
                if ( other.id != null ) return false;
            } else if ( !id.equals( other.id ) ) return false;
            return true;
        }

        public long getAnalysisId() {
            return analysisId;
        }

        public String getAnalysisType() {
            return analysisType;
        }

        public String getBaselineFactorValue() {
            return baselineFactorValue;
        }

        public Long getBaselineFactorValueId() {
            return baselineFactorValueId;
        }

        public String getContrastFactorValue() {
            return contrastFactorValue;
        }

        public Long getDatasetId() {
            return datasetId;
        }

        public String getDatasetName() {
            return datasetName;
        }

        public String getDatasetShortName() {
            return datasetShortName;
        }

        public int getExperimentGroupIndex() {
            return experimentGroupIndex;
        }

        public String getExperimentGroupName() {
            return experimentGroupName;
        }

        public String getExperimentId() {
            return experimentId;
        }

        public String getExperimentName() {
            return experimentName;
        }

        public String getFactorCategory() {
            return factorCategory;
        }

        public String getFactorDescription() {
            return factorDescription;
        }

        public Long getFactorId() {
            return factorId;
        }

        public String getFactorName() {
            return factorName;
        }

        public Long getFactorValueId() {
            return factorValueId;
        }

        public String getId() {
            return id;
        }

        public boolean getIsSelected() {
            return this.isSelected;
        }

        public Boolean getIsSubset() {
            return isSubset;
        }

        public Integer getNumberDiffExpressedProbes() {
            return numberDiffExpressedProbes;
        }

        public Integer getNumberOfGenesDiffExpressed() {
            return numberOfGenesDiffExpressed;
        }

        public Integer getNumberDiffExpressedProbesDown() {
            return numberDiffExpressedProbesDown;
        }

        public Integer getNumberDiffExpressedProbesUp() {
            return numberDiffExpressedProbesUp;
        }

        public Integer getNumberOfGenesTested() {
            return numberOfGenesTested;
        }

        public Integer getNumberOfProbesOnArray() {
            return numberOfProbesOnArray;
        }

        public long getResultSetId() {
            return resultSetId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
            return result;
        }

        public void setExperimentGroupIndex( int experimentGroupIndex ) {
            this.experimentGroupIndex = experimentGroupIndex;
        }

        void setExperimentGroupName( String experimentGroupName ) {
            this.experimentGroupName = experimentGroupName;
        }

        /*
         * Helper method to get factor values. TODO: Fix FactorValue class to return correct factor value in the first
         * place.
         */
        private String getFactorValueString( FactorValue fv ) {
            if ( fv == null ) return "[No value]";

            if ( fv.getCharacteristics() != null && fv.getCharacteristics().size() > 0 ) {
                String fvString = "";
                for ( Characteristic c : fv.getCharacteristics() ) {
                    fvString += c.getValue() + " ";
                }
                return fvString;
            } else if ( fv.getMeasurement() != null ) {
                return fv.getMeasurement().getValue();
            } else if ( fv.getValue() != null && !fv.getValue().isEmpty() ) {
                return fv.getValue();
            } else {
                return "[Missing]";
            }
        }

        private DifferentialExpressionGenesConditionsValueObject getOuterType() {
            return DifferentialExpressionGenesConditionsValueObject.this;
        }

    }

    // A Gene Value object specialized to hold differential expression results.
    public class Gene {
        private Long id;
        private String name;
        private String fullName;
        private double specificityScore;
        private Integer groupIndex;
        private String groupName;
        private boolean isSelected = false;

        public Gene( long id, String name, String fullName ) {
            this.id = id;
            this.name = name;
            this.fullName = fullName;
        }

        @Override
        public boolean equals( Object obj ) {
            if ( this == obj ) return true;
            if ( obj == null ) return false;
            if ( getClass() != obj.getClass() ) return false;
            Gene other = ( Gene ) obj;
            if ( !getOuterType().equals( other.getOuterType() ) ) return false;
            if ( id != other.id ) return false;
            return true;
        }

        public String getFullName() {
            return fullName;
        }

        public int getGroupIndex() {
            return groupIndex;
        }

        public String getGroupName() {
            return groupName;
        }

        public long getId() {
            return id;
        }

        public boolean getIsSelected() {
            return this.isSelected;
        }

        public String getName() {
            return name;
        }

        public double getSpecificityScore() {
            return specificityScore;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ( int ) ( id ^ ( id >>> 32 ) );
            return result;
        }

        public void setFullName( String fullName ) {
            this.fullName = fullName;
        }

        public void setGroupIndex( int groupIndex ) {
            this.groupIndex = groupIndex;
        }

        public void setGroupName( String groupName ) {
            this.groupName = groupName;
        }

        public void setId( long id ) {
            this.id = id;
        }

        public void setName( String name ) {
            this.name = name;
        }

        public void setSpecificityScore( double specificityScore ) {
            this.specificityScore = specificityScore;
        }

        private DifferentialExpressionGenesConditionsValueObject getOuterType() {
            return DifferentialExpressionGenesConditionsValueObject.this;
        }

    }

    public static final double THRESHOLD_QVALUE_FOR_HITLISTS = 0.05;

    public static String constructConditionId( long resultSetId, long factorValueId ) {
        return "rs:" + resultSetId + "fv:" + factorValueId;
    }

    /*
     * Map of Condition IDs to map of Genes to the Cell holding the information for the results for that Gene x
     * Condition combination.
     */
    private Map<String, Map<Long, Cell>> cellData;

    /*
     * The Condition dimension
     */
    private List<Condition> conditions;

    /*
     * The Gene dimension
     */
    private List<Gene> genes;

    /**
     * 
     */
    public DifferentialExpressionGenesConditionsValueObject() {
        cellData = new HashMap<String, Map<Long, Cell>>();
        conditions = new ArrayList<Condition>();
        genes = new ArrayList<Gene>();
    }

    /**
     * @param geneId
     * @param conditionId
     * @param correctedPValue
     * @param pValue
     * @param numProbes
     * @param numProbesDiffExpressed
     */
    public void addBlackCell( Long geneId, String conditionId, double correctedPValue, double pValue, int numProbes,
            int numProbesDiffExpressed ) {
        Cell cell = new Cell();
        cell.isProbeMissing = false;
        cell.correctedPValue = correctedPValue;
        cell.pValue = pValue;
        cell.visualizationValue = 0.0;
        cell.logFoldChange = 0.0;
        cell.numberOfProbes = numProbes;
        cell.numberOfProbesDiffExpressed = numProbesDiffExpressed;

        addCell( geneId, conditionId, cell );
    }

    /**
     * Set the details of the data for one Cell.
     * 
     * @param geneId
     * @param conditionId
     * @param pValue
     * @param foldChange
     * @param numProbes
     * @param numProbesDiffExpressed
     * @param correctedPvalue
     */
    public void addCell( Long geneId, String conditionId, double correctedPValue, double foldChange, int numProbes,
            int numProbesDiffExpressed, Double uncorrectedPvalue ) {
        Cell cell = new Cell();
        cell.isProbeMissing = false;
        cell.correctedPValue = correctedPValue;
        cell.pValue = uncorrectedPvalue;
        cell.logFoldChange = foldChange;
        cell.numberOfProbes = numProbes;
        cell.numberOfProbesDiffExpressed = numProbesDiffExpressed;

        addCell( geneId, conditionId, cell );
    }

    /**
     * Initialize the column of cells for the given Condition, treating them as missing values.
     * 
     * @param condition
     */
    public void addCondition( Condition condition ) {
        conditions.add( condition );
        // // Start with a column of missing values.
        for ( Gene gene : this.genes ) {
            this.addProbeMissingCell( gene.getId(), condition.getId() );
        }
    }

    /**
     * @param gene
     */
    public void addGene( Gene gene ) {
        genes.add( gene );
    }

    /**
     * @return
     */
    public Map<String, Map<Long, Cell>> getCellData() {
        return cellData;
    }

    /**
     * @return
     */
    public List<Condition> getConditions() {
        return conditions;
    }

    /**
     * @return
     */
    public List<Gene> getGenes() {
        return genes;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append( "\nCorner" );

        for ( Condition c : conditions ) {
            buf.append( "\t" + c.getId() );
        }
        buf.append( "\n" );

        for ( Gene g : genes ) {
            buf.append( g.getName() );
            for ( Condition c : conditions ) {
                buf.append( String.format( "\t%.2f", cellData.get( c.getId() ).get( g.getId() ).getpValue() ) );
            }
            buf.append( "\n" );
        }

        return buf.toString();
    }

    /**
     * @param geneId
     * @param conditionId
     * @param cell
     */
    private void addCell( Long geneId, String conditionId, Cell cell ) {
        Map<Long, Cell> geneToCellMap = cellData.get( conditionId );
        if ( geneToCellMap == null ) {
            geneToCellMap = new HashMap<Long, Cell>();
        }
        geneToCellMap.put( geneId, cell );
        cellData.put( conditionId, geneToCellMap );
    }

    /**
     * @param geneId
     * @param conditionId
     */
    private void addProbeMissingCell( Long geneId, String conditionId ) {
        Cell cell = new Cell();
        cell.isProbeMissing = true;
        addCell( geneId, conditionId, cell );
    }

}
