/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.common.protocol;

import edu.columbia.gemma.common.Describable;

/**
 * @see edu.columbia.gemma.common.protocol.Protocol
 */
public class ProtocolDaoImpl extends edu.columbia.gemma.common.protocol.ProtocolDaoBase {

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.DescribableDaoBase#findOrCreate(edu.columbia.gemma.common.Describable)
     */
    @Override
    public Protocol findOrCreate( Describable protocol ) {
        if ( !( protocol instanceof Protocol ) ) throw new IllegalArgumentException( "Must be a Protocol" );
        if ( protocol == null || protocol.getName() == null || protocol.getDescription() == null ) return null;
        Protocol newDescribable = ( Protocol ) find( protocol );
        if ( newDescribable != null ) return newDescribable;
        return ( Protocol ) create( ( Protocol ) protocol );
    }
}