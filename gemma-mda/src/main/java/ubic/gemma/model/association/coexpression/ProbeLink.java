/*
 * The Gemma_sec1 project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.model.association.coexpression;

import ubic.gemma.model.analysis.expression.coexpression.Link;

/**
 * Helper class.
 */
public class ProbeLink implements Link {
    private Long firstDesignElementId = 0L;
    private Long secondDesignElementId = 0L;
    private Double score = 0.0;
    private Long eeId = null;

    public ProbeLink() {
    }

    public Long getEeId() {
        return eeId;
    }

    public Long getFirstDesignElementId() {
        return firstDesignElementId;
    }

    @Override
    public Double getScore() {
        return score;
    }

    public Long getSecondDesignElementId() {
        return secondDesignElementId;
    }

    public void setEeId( Long eeId ) {
        this.eeId = eeId;
    }

    public void setFirstDesignElementId( Long first_design_element_fk ) {
        this.firstDesignElementId = first_design_element_fk;
    }

    @Override
    public void setScore( Double score ) {
        this.score = score;
    }

    public void setSecondDesignElementId( Long second_design_element_fk ) {
        this.secondDesignElementId = second_design_element_fk;
    }

    public String toSqlString() {
        return "(" + firstDesignElementId + ", " + secondDesignElementId + ",  " + score + ", " + eeId + ")";
    }

    @Override
    public String toString() {
        return "DE1=" + firstDesignElementId + ", DE2=" + secondDesignElementId + ", SCORE=s" + score + ", EE=" + eeId;
    }
}
