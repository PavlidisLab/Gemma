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

import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;

import javax.persistence.Transient;

/**
 * A subset of samples from an ExpressionExperiment
 */
public class ExpressionExperimentSubSet extends BioAssaySet implements SecuredChild {

    public static final int MAX_NAME_LENGTH = 255;


    private ExpressionExperiment sourceExperiment;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public ExpressionExperimentSubSet() {
    }

    @Transient
    @Override
    public Securable getSecurityOwner() {
        return sourceExperiment;
    }

    public ExpressionExperiment getSourceExperiment() {
        return this.sourceExperiment;
    }

    public void setSourceExperiment( ExpressionExperiment sourceExperiment ) {
        this.sourceExperiment = sourceExperiment;
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

        public static ExpressionExperimentSubSet newInstance( String name, ExpressionExperiment sourceExperiment ) {
            ExpressionExperimentSubSet subset = new ExpressionExperimentSubSet();
            subset.setName( name );
            subset.setSourceExperiment( sourceExperiment );
            return subset;
        }

    }

}