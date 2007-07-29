/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.web.controller.security;

import java.io.Serializable;

/**
 * Security command object that wraps security preferences.
 * 
 * @author keshav
 * @version $Id$
 */
public class SecurityCommand implements Serializable {

    private static final long serialVersionUID = 2166768356457316142L;

    private String securableType = null;

    private String shortName = null;

    private String mask = null;

    /**
     * @return the securableType
     */
    public String getSecurableType() {
        return securableType;
    }

    /**
     * @param securableType the securableType to set
     */
    public void setSecurableType( String securableType ) {
        this.securableType = securableType;
    }

    /**
     * @return the shortName
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * @param shortName the shortName to set
     */
    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    /**
     * @return the mask
     */
    public String getMask() {
        return mask;
    }

    /**
     * @param mask the mask to set
     */
    public void setMask( String mask ) {
        this.mask = mask;
    }

}
