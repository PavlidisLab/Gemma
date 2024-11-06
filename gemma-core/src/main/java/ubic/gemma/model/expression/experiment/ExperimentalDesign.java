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

import org.hibernate.search.annotations.*;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.common.description.Characteristic;

import javax.persistence.Transient;
import java.util.Set;

@Indexed
public class ExperimentalDesign extends AbstractDescribable implements SecuredChild {

    private String replicateDescription;
    private String qualityControlDescription;
    private String normalizationDescription;
    private Set<ExperimentalFactor> experimentalFactors = new java.util.HashSet<>();
    private Set<Characteristic> types = new java.util.HashSet<>();

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    @Override
    @Field
    public String getName() {
        return super.getName();
    }

    @Override
    @Field(store = Store.YES)
    public String getDescription() {
        return super.getDescription();
    }

    /**
     * @return The description of the factors (TimeCourse, Dosage, etc.) that group the BioAssays.
     */
    @IndexedEmbedded
    public Set<ExperimentalFactor> getExperimentalFactors() {
        return this.experimentalFactors;
    }

    public void setExperimentalFactors( Set<ExperimentalFactor> experimentalFactors ) {
        this.experimentalFactors = experimentalFactors;
    }

    public String getNormalizationDescription() {
        return this.normalizationDescription;
    }

    public void setNormalizationDescription( String normalizationDescription ) {
        this.normalizationDescription = normalizationDescription;
    }

    public String getQualityControlDescription() {
        return this.qualityControlDescription;
    }

    public void setQualityControlDescription( String qualityControlDescription ) {
        this.qualityControlDescription = qualityControlDescription;
    }

    public String getReplicateDescription() {
        return this.replicateDescription;
    }

    public void setReplicateDescription( String replicateDescription ) {
        this.replicateDescription = replicateDescription;
    }

    @Transient
    @Override
    public Securable getSecurityOwner() {
        return null;
    }

    public Set<Characteristic> getTypes() {
        return this.types;
    }

    public void setTypes( Set<Characteristic> types ) {
        this.types = types;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof ExperimentalDesign ) )
            return false;
        ExperimentalDesign that = ( ExperimentalDesign ) object;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return false;
        }
    }

    public static final class Factory {

        public static ExperimentalDesign newInstance() {
            return new ExperimentalDesign();
        }

    }

}