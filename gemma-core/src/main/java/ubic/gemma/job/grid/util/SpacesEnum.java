/*
 * The Gemma project
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
package ubic.gemma.job.grid.util;

import ubic.gemma.util.ConfigUtils;

/**
 * An enumeration of java spaces used by Gemma.
 * 
 * @author keshav
 * @version $Id$
 */
public enum SpacesEnum {

    DEFAULT_SPACE();

    private String spaceUrl = null;

    /**
     * @param url
     */
    private SpacesEnum() {
        this.spaceUrl = ConfigUtils.getString( "gemma.spaces.url.0" );
    }

    /**
     * @return String
     */
    public String getSpaceUrl() {
        return this.spaceUrl;
    }

}
