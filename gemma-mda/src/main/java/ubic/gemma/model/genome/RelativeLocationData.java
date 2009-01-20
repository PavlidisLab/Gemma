/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.model.genome;

import ubic.gemma.model.genome.gene.GeneProduct;

/**
 * Simple helper value object. Holds information on what gene is nearest a given physical location.
 * 
 * @author paul
 * @version $Id$
 */
public class RelativeLocationData {

    private PhysicalLocation physicalLocation;

    private Gene nearestGene;

    private GeneProduct nearestGeneProduct;

    private PhysicalLocation nearestGeneProductPhysicalLocation;

    private boolean isContainedWithinGene = false;
    private boolean overlapsGene = false;

    private long range = 0L;

    /**
     * @return the range
     */
    public long getRange() {
        return range;
    }

    /**
     * @param range the range to set
     */
    public void setRange( long range ) {
        this.range = range;
    }

    /**
     * @return the physicalLocation
     */
    public PhysicalLocation getPhysicalLocation() {
        return physicalLocation;
    }

    /**
     * @param physicalLocation the physicalLocation to set
     */
    public void setPhysicalLocation( PhysicalLocation physicalLocation ) {
        this.physicalLocation = physicalLocation;
    }

    /**
     * @return the nearestGene
     */
    public Gene getNearestGene() {
        return nearestGene;
    }

    /**
     * @param nearestGene the nearestGene to set
     */
    public void setNearestGene( Gene nearestGene ) {
        this.nearestGene = nearestGene;
    }

    /**
     * @return the nearestGeneProduct
     */
    public GeneProduct getNearestGeneProduct() {
        return nearestGeneProduct;
    }

    /**
     * @param nearestGeneProduct the nearestGeneProduct to set
     */
    public void setNearestGeneProduct( GeneProduct nearestGeneProduct ) {
        this.nearestGeneProduct = nearestGeneProduct;
    }

    /**
     * @return the nearestGeneProductPhysicalLocation
     */
    public PhysicalLocation getNearestGeneProductPhysicalLocation() {
        return nearestGeneProductPhysicalLocation;
    }

    /**
     * @param nearestGeneProductPhysicalLocation the nearestGeneProductPhysicalLocation to set
     */
    public void setNearestGeneProductPhysicalLocation( PhysicalLocation nearestGeneProductPhysicalLocation ) {
        this.nearestGeneProductPhysicalLocation = nearestGeneProductPhysicalLocation;
    }

    /**
     * @return the isContainedWithinGene
     */
    public boolean isContainedWithinGene() {
        return isContainedWithinGene;
    }

    /**
     * @param isContainedWithinGene the isContainedWithinGene to set
     */
    public void setContainedWithinGene( boolean isContainedWithinGene ) {
        this.isContainedWithinGene = isContainedWithinGene;
    }

    /**
     * @return the overlapsGene
     */
    public boolean isOverlapsGene() {
        return overlapsGene;
    }

    /**
     * @param overlapsGene the overlapsGene to set
     */
    public void setOverlapsGene( boolean overlapsGene ) {
        this.overlapsGene = overlapsGene;
    }

    public RelativeLocationData( PhysicalLocation physicalLocation, Gene nearestGene, GeneProduct nearestGeneProduct,
            PhysicalLocation nearestGeneProductPhysicalLocation ) {
        super();
        this.physicalLocation = physicalLocation;
        this.nearestGene = nearestGene;
        this.nearestGeneProduct = nearestGeneProduct;
        this.nearestGeneProductPhysicalLocation = nearestGeneProductPhysicalLocation;
    }

}
