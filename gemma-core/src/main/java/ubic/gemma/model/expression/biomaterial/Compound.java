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
package ubic.gemma.model.expression.biomaterial;

/**
 * 
 */
public abstract class Compound extends ubic.gemma.model.common.Describable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.expression.biomaterial.Compound}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.expression.biomaterial.Compound}.
         */
        public static ubic.gemma.model.expression.biomaterial.Compound newInstance() {
            return new ubic.gemma.model.expression.biomaterial.CompoundImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.expression.biomaterial.Compound}, taking all possible
         * properties (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.expression.biomaterial.Compound newInstance( String name, String description,
                Boolean isSolvent, String registryNumber,
                ubic.gemma.model.common.description.DatabaseEntry externalLIMS,
                ubic.gemma.model.common.description.Characteristic compoundIndices ) {
            final ubic.gemma.model.expression.biomaterial.Compound entity = new ubic.gemma.model.expression.biomaterial.CompoundImpl();
            entity.setName( name );
            entity.setDescription( description );
            entity.setIsSolvent( isSolvent );
            entity.setRegistryNumber( registryNumber );
            entity.setExternalLIMS( externalLIMS );
            entity.setCompoundIndices( compoundIndices );
            return entity;
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 8136911145519622152L;
    private Boolean isSolvent;

    private String registryNumber;

    private ubic.gemma.model.common.description.DatabaseEntry externalLIMS;

    private ubic.gemma.model.common.description.Characteristic compoundIndices;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public Compound() {
    }

    /**
     * 
     */
    public ubic.gemma.model.common.description.Characteristic getCompoundIndices() {
        return this.compoundIndices;
    }

    /**
     * 
     */
    public ubic.gemma.model.common.description.DatabaseEntry getExternalLIMS() {
        return this.externalLIMS;
    }

    /**
     * 
     */
    public Boolean getIsSolvent() {
        return this.isSolvent;
    }

    /**
     * CAS registry number (see http://www.cas.org/)
     */
    public String getRegistryNumber() {
        return this.registryNumber;
    }

    public void setCompoundIndices( ubic.gemma.model.common.description.Characteristic compoundIndices ) {
        this.compoundIndices = compoundIndices;
    }

    public void setExternalLIMS( ubic.gemma.model.common.description.DatabaseEntry externalLIMS ) {
        this.externalLIMS = externalLIMS;
    }

    public void setIsSolvent( Boolean isSolvent ) {
        this.isSolvent = isSolvent;
    }

    public void setRegistryNumber( String registryNumber ) {
        this.registryNumber = registryNumber;
    }

}