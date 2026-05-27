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
package ubic.gemma.model.expression.experiment;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static ubic.gemma.core.util.StringUtils.abbreviateWithSuffix;

/**
 * A subset of assays (or derived assays) from an {@link ExpressionExperiment}.
 * <p>
 * In the case of a "derived" assay, it is possible to walk up to the samples of the assays of the source experiment via
 * {@link BioMaterial#getSourceBioMaterial()}.
 * <p>
 * This is used for single-cell datasets to represent aggregated pseudo-bulks.
 *
 * @author Paul
 */
@Entity
@DiscriminatorValue("ExpressionExperimentSubSet")
public class ExpressionExperimentSubSet extends BioAssaySet implements SecuredChild<ExpressionExperiment> {

    /**
     * Maximum length of the name of a subset.
     */
    public static final int MAX_NAME_LENGTH = 255;

    /**
     * Delimiter used to separate the source experiment name from the subset name.
     */
    public static final String NAME_DELIMITER = " - ";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "SOURCE_EXPERIMENT_FK", columnDefinition = "BIGINT")
    private ExpressionExperiment sourceExperiment;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "BIO_ASSAYS2EXPRESSION_EXPERIMENT_SUB_SET",
            joinColumns = @JoinColumn(name = "EXPRESSION_EXPERIMENT_SUB_SET_FK", columnDefinition = "BIGINT"),
            inverseJoinColumns = @JoinColumn(name = "BIO_ASSAYS_FK", columnDefinition = "BIGINT"),
            foreignKey = @ForeignKey(name = "BIO_ASSAY_EXPRESSION_EXPERIMENT_SUB_SET_FKC"))
    private Set<BioAssay> bioAssays = new HashSet<>();

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public ExpressionExperimentSubSet() {
    }
    public ExpressionExperiment getSourceExperiment() {
        return this.sourceExperiment;
    }

    public void setSourceExperiment( ExpressionExperiment sourceExperiment ) {
        this.sourceExperiment = sourceExperiment;
    }

    @Override
    public Set<BioAssay> getBioAssays() {
        return bioAssays;
    }

    @Override
    public void setBioAssays( Set<BioAssay> bioAssays ) {
        this.bioAssays = bioAssays;
    }

    @Transient
    @Override
    public ExpressionExperiment getSecurityOwner() {
        return sourceExperiment;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof ExpressionExperimentSubSet ) )
            return false;
        ExpressionExperimentSubSet that = ( ExpressionExperimentSubSet ) object;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return false;
        }
    }

    public static final class Factory {

        /**
         * Subsets are named by appending the name to the source experiment name, separated by {@link #NAME_DELIMITER}.
         */
        public static ExpressionExperimentSubSet newInstance( String name, ExpressionExperiment sourceExperiment ) {
            ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
            subset.setName( abbreviateWithSuffix( sourceExperiment.getName(), " - " + name, "…", ExpressionExperimentSubSet.MAX_NAME_LENGTH, true, StandardCharsets.UTF_8 ) );
            subset.setSourceExperiment( sourceExperiment );
            return subset;
        }

    }

}
