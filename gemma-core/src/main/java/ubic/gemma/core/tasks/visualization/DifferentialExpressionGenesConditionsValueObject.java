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
package ubic.gemma.core.tasks.visualization;

import ubic.gemma.model.analysis.expression.diff.DiffExResultSetSummaryValueObject;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;

import java.util.*;

/**
 * Represents a complete set of data for a differential expression query over a set of genes x conditions (resultSets x
 * contrasts).
 *
 * @author anton
 */
public class DifferentialExpressionGenesConditionsValueObject {
    /**
     * Map of Condition IDs to map of Genes to the Cell holding the information for the results for that Gene x
     * Condition combination.
     */
    private final Map<String, Map<Long, Cell>> cellData;
    private final Map<Long, Collection<Condition>> resultSetConditions = new HashMap<>();
    private final List<Condition> conditions;
    private final List<DiffExGene> genes;

    public DifferentialExpressionGenesConditionsValueObject() {
        cellData = new HashMap<>();
        conditions = new ArrayList<>();
        genes = new ArrayList<>();
    }

    public static String constructConditionId( long resultSetId, long factorValueId ) {
        return "rs:" + resultSetId + "fv:" + factorValueId;
    }

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

    public void addCell( Long geneId, String conditionId, Double correctedPValue, Double foldChange, Integer numProbes,
            Integer numProbesDiffExpressed, Double uncorrectedPvalue ) {

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
     * @param condition condition
     */
    public void addCondition( Condition condition ) {
        conditions.add( condition );

        long resultSetId = condition.getResultSetId();
        if ( !resultSetConditions.containsKey( resultSetId ) ) {
            resultSetConditions.put( resultSetId, new HashSet<Condition>() );
        }

        resultSetConditions.get( resultSetId ).add( condition );
        // // Start with a column of missing values.
        for ( DiffExGene gene : this.genes ) {
            this.addProbeMissingCell( gene.getId(), condition.getId() );
        }
    }

    public void addGene( DiffExGene gene ) {
        genes.add( gene );
    }

    /**
     * @return Map of Condition IDs to map of Genes to the Cell holding the information for the results for that Gene x
     * Condition combination.
     */
    public Map<String, Map<Long, Cell>> getCellData() {
        return cellData;
    }

    /**
     * Mark data as available, but not significant (so details will be missing). This is only used if we are not storing
     * all the results. See bug 3365 for discussion.
     *
     * @param geneId      gene id
     * @param resultSetId result set id
     */
    public void setAsNonSignficant( Long geneId, Long resultSetId ) {
        Collection<Condition> cs = resultSetConditions.get( resultSetId );
        for ( Condition c : cs ) {
            Cell cell = new Cell();
            cell.isProbeMissing = false;
            cell.isProbeOmitted = true;
            cell.correctedPValue = 1.0;
            cell.pValue = 1.0;
            cell.logFoldChange = 0.0;
            cell.numberOfProbes = 0;
            cell.numberOfProbesDiffExpressed = 0;
            cell.direction = 0;
            addCell( geneId, c.getId(), cell );
        }
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public List<DiffExGene> getGenes() {
        return genes;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append( "\nCorner" );

        for ( Condition c : conditions ) {
            buf.append( "\t" ).append( c.getId() );
        }
        buf.append( "\n" );

        for ( DiffExGene g : genes ) {
            buf.append( g.getName() );
            for ( Condition c : conditions ) {
                buf.append( String.format( "\t%.2f", cellData.get( c.getId() ).get( g.getId() ).getpValue() ) );
            }
            buf.append( "\n" );
        }

        return buf.toString();
    }

    private void addCell( Long geneId, String conditionId, Cell cell ) {
        Map<Long, Cell> geneToCellMap = cellData.get( conditionId );
        if ( geneToCellMap == null ) {
            geneToCellMap = new HashMap<>();
        }
        geneToCellMap.put( geneId, cell );
        cellData.put( conditionId, geneToCellMap );
    }

    private void addProbeMissingCell( Long geneId, String conditionId ) {
        Cell cell = new Cell();
        cell.isProbeMissing = true;
        addCell( geneId, conditionId, cell );
    }

    // The details of the result for a gene x condition combination.
    @SuppressWarnings("unused")
    public class Cell {
        private Boolean isProbeMissing;
        private Double visualizationValue;
        private Double pValue = null; // important!
        private Double logFoldChange = 0.0;
        private Integer direction = 0;
        private Integer numberOfProbes = 0;
        private Integer numberOfProbesDiffExpressed = 0;
        private Double correctedPValue = null; // important
        private boolean isProbeOmitted = false;

        public boolean getIsProbeOmitted() {
            return isProbeOmitted;
        }

        public void setIsProbeOmitted( boolean isProbeOmitted ) {
            this.isProbeOmitted = isProbeOmitted;
        }

        public Double getCorrectedPValue() {
            return correctedPValue;
        }

        public Integer getDirection() {
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

    /**
     * Represents one column in the differential expression view; one contrast in a resultset in an experiment.
     */
    @SuppressWarnings("unused")
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
        private String datasetShortName;
        private Integer numberOfProbesOnArray;
        private String id;

        private boolean isSelected = false;

        private Integer numberOfGenesTested;
        private Integer numberOfGenesDiffExpressed;

        private Long factorValueId;

        public Condition( ExpressionExperimentValueObject experiment,
                DifferentialExpressionAnalysisValueObject analysis, DiffExResultSetSummaryValueObject resultSet,
                FactorValueValueObject factorValue ) {
            this( resultSet.getResultSetId(), factorValue.getId() );
            numberOfProbesOnArray = resultSet.getNumberOfProbesAnalyzed();
            numberOfGenesTested = resultSet.getNumberOfGenesAnalyzed();
            datasetShortName = experiment.getShortName();
            datasetName = experiment.getName();
            datasetId = experiment.getId();
            analysisId = analysis.getId();
            baselineFactorValueId = resultSet.getBaselineGroup().getId();
            factorName = factorValue.getFactorValue();
            contrastFactorValue = getFactorValueString( factorValue );
            baselineFactorValue = getFactorValueString( resultSet.getBaselineGroup() );
            factorDescription = resultSet.getExperimentalFactors().iterator().next().getDescription();
            factorId = factorValue.getFactorId();

            factorCategory = ( factorValue.getCategory() == null ) ? "[No category]" : factorValue.getCategory();

            this.isSubset = analysis.getSubsetFactorValue() != null;

            numberDiffExpressedProbesDown = resultSet.getDownregulatedCount();
            numberDiffExpressedProbesUp = resultSet.getUpregulatedCount();
            numberDiffExpressedProbes = resultSet.getNumberOfDiffExpressedProbes();
            // DifferentialExpressionAnalysisValueObject.DEFAULT_THRESHOLD
        }

        public Condition( Long resultSetId, Long factorValueId ) {
            this.resultSetId = resultSetId;
            this.factorValueId = factorValueId;
            this.id = constructConditionId( resultSetId, factorValueId );
        }

        @Override
        public boolean equals( Object obj ) {
            if ( this == obj )
                return true;
            if ( obj == null )
                return false;
            if ( getClass() != obj.getClass() )
                return false;
            Condition other = ( Condition ) obj;
            if ( !getOuterType().equals( other.getOuterType() ) )
                return false;
            if ( id == null ) {
                if ( other.id != null )
                    return false;
            } else if ( !id.equals( other.id ) )
                return false;
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

        public String getExperimentGroupName() {
            return experimentGroupName;
        }

        void setExperimentGroupName( String experimentGroupName ) {
            this.experimentGroupName = experimentGroupName;
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

        /*
         * Helper method to get factor values. TODO: Fix FactorValue class to return correct factor value in the first
         * place.
         */
        private String getFactorValueString( FactorValueValueObject fv ) {
            if ( fv == null )
                return "[No value]";
            if ( fv.getValue() == null )
                return "[Missing]";
            return fv.getValue();
        }

        private DifferentialExpressionGenesConditionsValueObject getOuterType() {
            return DifferentialExpressionGenesConditionsValueObject.this;
        }

    }

    // A Gene Value object specialized to hold differential expression results.
    public class DiffExGene {
        private Long id;
        private String name;
        private String fullName;
        private double specificityScore;
        private String groupName;
        private boolean isSelected = false;

        public DiffExGene( long id, String name, String fullName ) {
            this.id = id;
            this.name = name;
            this.fullName = fullName;
        }

        @Override
        public boolean equals( Object obj ) {
            if ( this == obj )
                return true;
            if ( obj == null )
                return false;
            if ( getClass() != obj.getClass() )
                return false;
            DiffExGene other = ( DiffExGene ) obj;
            if ( !getOuterType().equals( other.getOuterType() ) )
                return false;
            //noinspection RedundantIfStatement // Better readability
            if ( !Objects.equals( id, other.id ) )
                return false;
            return true;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName( String fullName ) {
            this.fullName = fullName;
        }

        public String getGroupName() {
            return groupName;
        }

        public void setGroupName( String groupName ) {
            this.groupName = groupName;
        }

        public long getId() {
            return id;
        }

        public void setId( long id ) {
            this.id = id;
        }

        public boolean getIsSelected() {
            return this.isSelected;
        }

        public String getName() {
            return name;
        }

        public void setName( String name ) {
            this.name = name;
        }

        public double getSpecificityScore() {
            return specificityScore;
        }

        public void setSpecificityScore( double specificityScore ) {
            this.specificityScore = specificityScore;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ( int ) ( id ^ ( id >>> 32 ) );
            return result;
        }

        private DifferentialExpressionGenesConditionsValueObject getOuterType() {
            return DifferentialExpressionGenesConditionsValueObject.this;
        }

    }

}
