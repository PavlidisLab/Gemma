/*
 * The gemma-model project
 * 
 * Copyright (c) 2015 University of British Columbia
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

package ubic.gemma.model.expression.experiment;

/**
 * Represents quality information about a data set. The class name comes from the research project name, GEEQ.
 * <p>
 * There are two components to the quality score. First, a "suitability" score that attempts to measure how appropriate
 * the data set is for inclusion in Gemma. Data sets that have low suitability scores would get a lower priority for
 * curation and analysis.
 * <p>
 * Second, a quality score captures information about the data itself. Thus data sets that have large batch effects,
 * outliers or other QC flags get lower quality scores.
 * <p>
 * At the moment (May 2015) only the suitability score is fully implemented.
 * 
 * @author paul
 * @version $Id$
 */
public abstract class Geeq {

    private Double distExpSize;
    private Double distPlatCount;
    private Double distPlatCoverage;
    private Double distPlatMissingValues;
    private Double distPlatPopularity;
    private Long id;
    private Double quality;
    private Double scoreExpBatchInfo;
    private Double scoreExpPublication;
    private Double scoreExpQT;
    private Double scoreExpSize;
    private Double scorePlatCount;
    private Double scorePlatCoverage;
    private Double scorePlatMissingValues;
    private Double scorePlatPopularity;
    private Double scorePlatTroubled;
    private Double suitability;

    public Double getDistExpSize() {
        return distExpSize;
    }

    public Double getDistPlatCount() {
        return distPlatCount;
    }

    public Double getDistPlatCoverage() {
        return distPlatCoverage;
    }

    public Double getDistPlatMissingValues() {
        return distPlatMissingValues;
    }

    public Double getDistPlatPopularity() {
        return distPlatPopularity;
    }

    public Long getId() {
        return id;
    }

    public Double getQuality() {
        return quality;
    }

    public Double getScoreExpBatchInfo() {
        return scoreExpBatchInfo;
    }

    public Double getScoreExpPublication() {
        return scoreExpPublication;
    }

    public Double getScoreExpQT() {
        return scoreExpQT;
    }

    public Double getScoreExpSize() {
        return scoreExpSize;
    }

    public Double getScorePlatCount() {
        return scorePlatCount;
    }

    public Double getScorePlatCoverage() {
        return scorePlatCoverage;
    }

    public Double getScorePlatMissingValues() {
        return scorePlatMissingValues;
    }

    public Double getScorePlatPopularity() {
        return scorePlatPopularity;
    }

    public Double getScorePlatTroubled() {
        return scorePlatTroubled;
    }

    public Double getSuitability() {
        return suitability;
    }

    public void setDistExpSize( Double distExpSize ) {
        this.distExpSize = distExpSize;
    }

    public void setDistPlatCount( Double distPlatCount ) {
        this.distPlatCount = distPlatCount;
    }

    public void setDistPlatCoverage( Double distPlatCoverage ) {
        this.distPlatCoverage = distPlatCoverage;
    }

    public void setDistPlatMissingValues( Double distPlatMissingValues ) {
        this.distPlatMissingValues = distPlatMissingValues;
    }

    public void setDistPlatPopularity( Double distPlatPopularity ) {
        this.distPlatPopularity = distPlatPopularity;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setQuality( Double quality ) {
        this.quality = quality;
    }

    public void setScoreExpBatchInfo( Double scoreExpBatchInfo ) {
        this.scoreExpBatchInfo = scoreExpBatchInfo;
    }

    public void setScoreExpPublication( Double scoreExpPublication ) {
        this.scoreExpPublication = scoreExpPublication;
    }

    public void setScoreExpQT( Double scoreExpQT ) {
        this.scoreExpQT = scoreExpQT;
    }

    public void setScoreExpSize( Double scoreExpSize ) {
        this.scoreExpSize = scoreExpSize;
    }

    public void setScorePlatCount( Double scorePlatCount ) {
        this.scorePlatCount = scorePlatCount;
    }

    public void setScorePlatCoverage( Double scorePlatCoverage ) {
        this.scorePlatCoverage = scorePlatCoverage;
    }

    public void setScorePlatMissingValues( Double scorePlatMissingValues ) {
        this.scorePlatMissingValues = scorePlatMissingValues;
    }

    public void setScorePlatPopularity( Double scorePlatPopularity ) {
        this.scorePlatPopularity = scorePlatPopularity;
    }

    public void setScorePlatTroubled( Double scorePlatTroubled ) {
        this.scorePlatTroubled = scorePlatTroubled;
    }

    public void setSuitability( Double suitability ) {
        this.suitability = suitability;
    }
}
