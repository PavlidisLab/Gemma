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
package ubic.gemma.model.common.description;

/**
 * <p>
 * A Characteristic that uses terms from ontologies or controlled vocabularies. These Characteristics can be chained
 * together in complex ways.
 * </p>
 * <p>
 * A Characteristic can form an RDF-style triple, with a Term (the subject) a CharacteristicProperty (the predicate) and
 * an object (either another Characteristic or a DataProperty to hold a literal value).
 * </p>
 */
public abstract class VocabCharacteristic extends ubic.gemma.model.common.description.CharacteristicImpl {

    /**
     * Constructs new instances of {@link ubic.gemma.model.common.description.VocabCharacteristic}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.description.VocabCharacteristic}.
         */
        public static ubic.gemma.model.common.description.VocabCharacteristic newInstance() {
            return new ubic.gemma.model.common.description.VocabCharacteristicImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -1790433503062970331L;
    private String valueUri;

    private String categoryUri;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public VocabCharacteristic() {
    }

    /**
     * The URI of the class that this is an instance of. Will only be different from the termUri when the class is
     * effectively abstract, and this is a concrete instance. By putting the abstract class URI in the object we can
     * more readily group together Characteristics that are instances of the same class. For example: If the classUri is
     * "Sex", then the termUri might be "male" or "female" for various instances. Otherwise, the classUri and the
     * termUri can be the same; for example, for "Age", if the "Age" is defined through its properties declared as
     * associations with this.
     */
    public String getCategoryUri() {
        return this.categoryUri;
    }

    /**
     * This can be a URI to any resources that describes the characteristic. Often it might be a URI to an OWL ontology
     * term. If the URI is an instance of an abstract class, the classUri should be filled in with the URI for the
     * abstract class.
     */
    public String getValueUri() {
        return this.valueUri;
    }

    public void setCategoryUri( String categoryUri ) {
        this.categoryUri = categoryUri;
    }

    public void setValueUri( String valueUri ) {
        this.valueUri = valueUri;
    }

}