/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.persistence.service.genome;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
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
    private boolean isOnSameStrand = false;

    private long range = 0L;

    public RelativeLocationData( PhysicalLocation physicalLocation, Gene nearestGene, GeneProduct nearestGeneProduct,
            PhysicalLocation nearestGeneProductPhysicalLocation ) {
        super();
        this.physicalLocation = physicalLocation;
        this.nearestGene = nearestGene;
        this.nearestGeneProduct = nearestGeneProduct;
        this.nearestGeneProductPhysicalLocation = nearestGeneProductPhysicalLocation;
    }

    /**
     * @return the nearestGene
     */
    public Gene getNearestGene() {
        return nearestGene;
    }

    /**
     * @return the nearestGeneProduct
     */
    public GeneProduct getNearestGeneProduct() {
        return nearestGeneProduct;
    }

    /**
     * @return the nearestGeneProductPhysicalLocation
     */
    public PhysicalLocation getNearestGeneProductPhysicalLocation() {
        return nearestGeneProductPhysicalLocation;
    }

    /**
     * @return the physicalLocation
     */
    public PhysicalLocation getPhysicalLocation() {
        return physicalLocation;
    }

    /**
     * @return the range
     */
    public long getRange() {
        return range;
    }

    /**
     * @return the isContainedWithinGene
     */
    public boolean isContainedWithinGene() {
        return isContainedWithinGene;
    }

    public boolean isOnSameStrand() {
        return isOnSameStrand;
    }

    /**
     * @return the overlapsGene
     */
    public boolean isOverlapsGene() {
        return overlapsGene;
    }

    /**
     * @param isContainedWithinGene the isContainedWithinGene to set
     */
    public void setContainedWithinGene( boolean isContainedWithinGene ) {
        this.isContainedWithinGene = isContainedWithinGene;
    }

    /**
     * @param nearestGene the nearestGene to set
     */
    public void setNearestGene( Gene nearestGene ) {
        this.nearestGene = nearestGene;
    }

    /**
     * @param nearestGeneProduct the nearestGeneProduct to set
     */
    public void setNearestGeneProduct( GeneProduct nearestGeneProduct ) {
        this.nearestGeneProduct = nearestGeneProduct;
    }

    /**
     * @param nearestGeneProductPhysicalLocation the nearestGeneProductPhysicalLocation to set
     */
    public void setNearestGeneProductPhysicalLocation( PhysicalLocation nearestGeneProductPhysicalLocation ) {
        this.nearestGeneProductPhysicalLocation = nearestGeneProductPhysicalLocation;
    }

    public void setOnSameStrand( boolean isOnSameStrand ) {
        this.isOnSameStrand = isOnSameStrand;
    }

    /**
     * @param overlapsGene the overlapsGene to set
     */
    public void setOverlapsGene( boolean overlapsGene ) {
        this.overlapsGene = overlapsGene;
    }

    /**
     * @param physicalLocation the physicalLocation to set
     */
    public void setPhysicalLocation( PhysicalLocation physicalLocation ) {
        this.physicalLocation = physicalLocation;
    }

    /**
     * @param range the range to set
     */
    public void setRange( long range ) {
        this.range = range;
    }

}
