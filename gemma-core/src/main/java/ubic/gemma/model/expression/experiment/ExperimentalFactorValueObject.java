/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.common.description.Characteristic;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author luke
 * @author keshav
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
@Data
@EqualsAndHashCode(callSuper = true)
public class ExperimentalFactorValueObject extends IdentifiableValueObject<ExperimentalFactor> implements Serializable {

    private static final long serialVersionUID = -2615804031123874251L;

    private String category;
    private String categoryUri;

    private String description;

    private String factorValues;

    private String name;
    private String type = "categorical"; // continuous or categorical.
    private Collection<FactorValueValueObject> values;

    /**
     * Required when using the class as a spring bean.
     */
    public ExperimentalFactorValueObject() {
    }

    public ExperimentalFactorValueObject( Long id ) {
        super( id );
    }

    public ExperimentalFactorValueObject( ExperimentalFactor factor ) {
        super( factor.getId() );
        this.name = factor.getName();
        this.description = factor.getDescription();

        if ( factor.getCategory() != null ) {
            this.category = factor.getCategory().getCategory();
            this.categoryUri = factor.getCategory().getCategoryUri();
        }

        /*
         * Note: this code copied from the ExperimentalDesignController.
         */
        Collection<FactorValueValueObject> vals = new HashSet<>();

        if ( factor.getType() != null ) {
            this.type = factor.getType().equals( FactorType.CATEGORICAL ) ? "categorical" : "continuous";
        } else {
            // Backwards compatibility: for old entries created prior to introduction of 'type' field in
            // ExperimentalFactor entity.
            // We have to take a guess.
            if ( factor.getFactorValues().isEmpty() ) {
                this.type = "categorical";
            } else {
                // Just use first factor value to make our guess.
                if ( factor.getFactorValues().iterator().next().getMeasurement() != null ) {
                    this.type = "continuous";
                } else {
                    this.type = "categorical";
                }
            }
        }

        if ( factor.getFactorValues() == null || factor.getFactorValues().isEmpty() ) {
            return;
        }

        Collection<FactorValue> fvs = factor.getFactorValues();
        StringBuilder factorValuesAsString = new StringBuilder( StringUtils.EMPTY );
        for ( FactorValue fv : fvs ) {
            String fvName = fv.toString();
            if ( StringUtils.isNotBlank( fvName ) ) {
                factorValuesAsString.append( fvName ).append( ", " );
            }
        }
        /* clean up the start and end of the string */
        factorValuesAsString = new StringBuilder(
                StringUtils.remove( factorValuesAsString.toString(), factor.getName() + ":" ) );
        factorValuesAsString = new StringBuilder( StringUtils.removeEnd( factorValuesAsString.toString(), ", " ) );

        this.factorValues = factorValuesAsString.toString();

        Characteristic c = factor.getCategory();
        /*
         * NOTE this replaces code that previously made no sense. PP
         */
        for ( FactorValue value : factor.getFactorValues() ) {
            vals.add( new FactorValueValueObject( value, c ) );
        }

        this.values = vals;
    }

    /**
     * Number of factor values.
     */
    @GemmaWebOnly
    public int getNumValues() {
        return this.values == null ? 0 : this.values.size();
    }
}
