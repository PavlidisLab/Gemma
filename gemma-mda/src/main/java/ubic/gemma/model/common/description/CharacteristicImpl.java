/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.common.description;

/**
 * @see ubic.gemma.model.common.description.Characteristic
 * @author pavlidis
 * @version $Id$
 */
public class CharacteristicImpl extends ubic.gemma.model.common.description.Characteristic {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 163962374233046021L;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.Characteristic#toString()
     */
    @Override
    public String toString() {
        return "Category = " + this.getCategory() + " Value = " + this.getValue();
    }

}