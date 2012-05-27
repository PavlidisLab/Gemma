/*
 * The Gemma  project
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

package ubic.gemma.web.taglib.displaytag;

import java.util.Comparator;

import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;

/**
 * @author klc Compares
 * @author $Id$
 */
public class ArrayDesignValueObjectComparator implements Comparator<ArrayDesignValueObject> {

    @Override
    public int compare( ArrayDesignValueObject arg0, ArrayDesignValueObject arg1 ) {

        if ( arg0.getDateCreated() == null || arg1.getDateCreated() == null ) return 0;

        return arg0.getDateCreated().compareTo( arg1.getDateCreated() ) * -1;
    }

}
