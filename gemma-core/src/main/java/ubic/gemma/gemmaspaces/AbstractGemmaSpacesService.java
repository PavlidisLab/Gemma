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
package ubic.gemma.gemmaspaces;

import org.springframework.context.ApplicationContext;

import ubic.gemma.util.gemmaspaces.GemmaSpacesEnum;
import ubic.gemma.util.gemmaspaces.GemmaSpacesUtil;

/**
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractGemmaSpacesService {

    protected GemmaSpacesUtil gigaSpacesUtil = null;

    protected ApplicationContext updatedContext = null;

    /**
     * @return ApplicationContext
     */
    public ApplicationContext addGigaspacesToApplicationContext() {
        if ( gigaSpacesUtil == null ) gigaSpacesUtil = new GemmaSpacesUtil();

        return gigaSpacesUtil.addGemmaSpacesToApplicationContext( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() );
    }

    /**
     * Controllers extending this class must implement this method. The implementation should call
     * injectGigaspacesUtil(GigaSpacesUtil gigaSpacesUtil) to "inject" a spring loaded GigaSpacesUtil into this abstract
     * class.
     * 
     * @param gigaSpacesUtil
     */
    abstract protected void setGigaSpacesUtil( GemmaSpacesUtil gigaSpacesUtil );

    /**
     * @param gigaSpacesUtil
     */
    protected void injectGigaspacesUtil( GemmaSpacesUtil gigaspacesUtil ) {
        this.gigaSpacesUtil = gigaspacesUtil;
    }

}
