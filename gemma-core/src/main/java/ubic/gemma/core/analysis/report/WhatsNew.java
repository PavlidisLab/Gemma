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
package ubic.gemma.core.analysis.report;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import java.util.*;

/**
 * A value object to hold onto the 'new' objects.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
@Getter
@Setter
public class WhatsNew {

    private Collection<Auditable> newObjects;
    private Collection<Auditable> updatedObjects;
    private Map<Taxon, Long> eeCountPerTaxon;
    private Map<Taxon, Collection<Long>> newEEIdsPerTaxon;
    private Map<Taxon, Collection<Long>> updatedEEIdsPerTaxon;
    private long newBioMaterialCount = 0L;
    private Date date;

    public WhatsNew( Date date ) {
        this.date = date;
        newObjects = new ArrayList<>();
        updatedObjects = new ArrayList<>();
    }

    public WhatsNew() {
        newObjects = new ArrayList<>();
        updatedObjects = new ArrayList<>();
    }

    public Collection<ArrayDesign> getUpdatedArrayDesigns() {
        Collection<ArrayDesign> result = new HashSet<>();
        for ( Auditable auditable : updatedObjects ) {
            if ( auditable instanceof ArrayDesign ) {
                result.add( ( ArrayDesign ) auditable );
            }
        }
        return result;
    }

    /**
     * @return collection of ArrayDesigns that are new since this.Date
     */
    public Collection<ArrayDesign> getNewArrayDesigns() {
        Collection<ArrayDesign> result = new HashSet<>();
        for ( Auditable auditable : newObjects ) {
            if ( auditable instanceof ArrayDesign ) {
                result.add( ( ArrayDesign ) auditable );
            }
        }
        return result;
    }

    /**
     * @return experiments updated, in time span as set when the report was generated
     */
    public Collection<ExpressionExperiment> getUpdatedExpressionExperiments() {
        Collection<ExpressionExperiment> result = new HashSet<>();
        for ( Auditable auditable : updatedObjects ) {
            if ( auditable instanceof ExpressionExperiment ) {
                result.add( ( ExpressionExperiment ) auditable );
            }
        }
        return result;
    }

    /**
     * @return new experiments, in time span as set when the report was generated
     */
    public Collection<ExpressionExperiment> getNewExpressionExperiments() {
        Collection<ExpressionExperiment> result = new HashSet<>();
        for ( Auditable auditable : newObjects ) {
            if ( auditable instanceof ExpressionExperiment ) {
                result.add( ( ExpressionExperiment ) auditable );
            }
        }
        return result;
    }

    /**
     * adds a single auditable to the new object list
     *
     * @param newObject the object to add
     */
    public void addNewObjects( Auditable newObject ) {
        this.newObjects.add( newObject );
    }

    /**
     * adds a collection of auditables to the new object list
     *
     * @param objs the objects to add
     */
    public void addNewObjects( Collection<? extends Auditable> objs ) {
        this.newObjects.addAll( objs );
    }

    /**
     * adds a single auditable to the updated object list
     *
     * @param updatedObject the object that has been updated
     */
    public void addUpdatedObjects( Auditable updatedObject ) {
        this.updatedObjects.add( updatedObject );
    }

    /**
     * adds a collection of auditables to the updated object list
     *
     * @param objs the objects that have been updated
     */
    public void addUpdatedObjects( Collection<? extends Auditable> objs ) {
        this.updatedObjects.addAll( objs );
    }

}
