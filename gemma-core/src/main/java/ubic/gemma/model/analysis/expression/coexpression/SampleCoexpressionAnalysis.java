/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.analysis.expression.coexpression;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import ubic.gemma.model.analysis.SingleExperimentAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import org.springframework.lang.Nullable;

/**
 * The 'analysis' in the name is a bit of a stretch here, as this object servers purely as an aggregator
 * of all the sample coexpression matrices.
 */
@Entity
@DiscriminatorValue("SampleCoexpressionAnalysis")
public class SampleCoexpressionAnalysis extends SingleExperimentAnalysis<ExpressionExperiment> {

    /*
     * 🛑 LAZY, and it has to be: these carry an n^2 LONGBLOB each and almost nobody who loads an analysis
     * wants the bytes.
     *
     * They were EAGER with select fetching, which meant every query returning analyses batch-initialized
     * both matrix proxies through EntityBatchLoaderInPredicate -- including findByExperimentAnalyzed, whose
     * only caller is removeForExperiment and which therefore read ~19 MB of blob per analysis purely to
     * find rows to delete. On GSE260875 (eid 35280) that read is where corrMat died, with 17.0 GB of
     * connector packet buffers live across thirteen consecutive GCs (frb, 2026-09-16, JFR
     * ObjectAllocationSample at stackdepth 256).
     *
     * This is an OWNING @ManyToOne -- the FK is on ANALYSIS -- so Hibernate can hand back a real proxy and
     * LAZY works without bytecode enhancement. That is not true of an inverse @OneToOne, which is why the
     * numberOfCells association on the vectors cannot be fixed the same way.
     *
     * Every reader is inside SampleCoexpressionAnalysisServiceImpl, which is @Transactional, so the session
     * is open when the bytes are genuinely wanted. cascade = ALL still deletes them: Hibernate initializes
     * the proxy to cascade the remove, which is one row.
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "SAMPLE_COEXPRESSION_MATRIX_RAW_FK", unique = true, columnDefinition = "BIGINT")
    private SampleCoexpressionMatrix fullCoexpressionMatrix;

    @Nullable
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "SAMPLE_COEXPRESSION_MATRIX_REG_FK", unique = true, columnDefinition = "BIGINT")
    private SampleCoexpressionMatrix regressedCoexpressionMatrix;

    /**
     * Note that since you get a full square matrix, all correlations are represented twice, and values on the main
     * diagonal will always be 1.
     *
     * @return a coexpression matrix with all factors (none regressed out), and including outliers.
     */
    public SampleCoexpressionMatrix getFullCoexpressionMatrix() {
        return fullCoexpressionMatrix;
    }

    public void setFullCoexpressionMatrix( SampleCoexpressionMatrix rawFullCoexpressionMatrix ) {
        this.fullCoexpressionMatrix = rawFullCoexpressionMatrix;
    }

    /**
     * Note that since you get a full square matrix, all correlations are represented twice, and values on the main
     * diagonal will always be 1.
     *
     * @return a coexpression matrix with regressed out major factors.
     */
    @Nullable
    public SampleCoexpressionMatrix getRegressedCoexpressionMatrix() {
        return regressedCoexpressionMatrix;
    }

    public void setRegressedCoexpressionMatrix( @Nullable SampleCoexpressionMatrix regressedCoexpressionMatrix ) {
        this.regressedCoexpressionMatrix = regressedCoexpressionMatrix;
    }

    @Transient
    public SampleCoexpressionMatrix getBestCoexpressionMatrix() {
        return regressedCoexpressionMatrix != null ? regressedCoexpressionMatrix : fullCoexpressionMatrix;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof SampleCoexpressionAnalysis ) )
            return false;
        SampleCoexpressionAnalysis that = ( SampleCoexpressionAnalysis ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        } else {
            return false;
        }
    }

    public static class Factory {

        public static SampleCoexpressionAnalysis newInstance( ExpressionExperiment experimentAnalyzed, SampleCoexpressionMatrix fullCoexpressionMatrix,
                @Nullable SampleCoexpressionMatrix regressedCoexpressionMatrix ) {
            SampleCoexpressionAnalysis analysis = new SampleCoexpressionAnalysis();
            analysis.setExperimentAnalyzed( experimentAnalyzed );
            analysis.setFullCoexpressionMatrix( fullCoexpressionMatrix );
            analysis.setRegressedCoexpressionMatrix( regressedCoexpressionMatrix );
            return analysis;
        }
    }
}
