/*
 * The Gemma project
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
package ubic.gemma.loader.util.converter;

import java.util.Collection;

/**
 * Defines a class that can convert objects from one type to another.
 * 
 * @author pavlidis
 * @version $Id$
 */
public interface Converter<S, T> {

    /**
     * Given a collection of source domain objects, conver them into Gemma domain objects.
     * 
     * @param sourceDomainObjects
     * @return
     */
    public Collection<T> convert( Collection<? extends S> sourceDomainObjects );

    /**
     * Convert a single object.
     * 
     * @param sourceDomainObject
     * @return
     */
    public T convert( S sourceDomainObject );

}
