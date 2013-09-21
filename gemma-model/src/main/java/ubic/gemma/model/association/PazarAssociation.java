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
package ubic.gemma.model.association;

/**
 * A TF - target association from Pazar (www.pazar.info)
 */
public abstract class PazarAssociation extends ubic.gemma.model.association.TfGeneAssociationImpl {

    /**
     * Constructs new instances of {@link ubic.gemma.model.association.PazarAssociation}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.association.PazarAssociation}.
         */
        public static ubic.gemma.model.association.PazarAssociation newInstance() {
            return new ubic.gemma.model.association.PazarAssociationImpl();
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -2964682249206683908L;
    private String pazarTfId;

    private String pazarTargetGeneId;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public PazarAssociation() {
    }

    /**
     * 
     */
    public String getPazarTargetGeneId() {
        return this.pazarTargetGeneId;
    }

    /**
     * 
     */
    public String getPazarTfId() {
        return this.pazarTfId;
    }

    public void setPazarTargetGeneId( String pazarTargetGeneId ) {
        this.pazarTargetGeneId = pazarTargetGeneId;
    }

    public void setPazarTfId( String pazarTfId ) {
        this.pazarTfId = pazarTfId;
    }

}