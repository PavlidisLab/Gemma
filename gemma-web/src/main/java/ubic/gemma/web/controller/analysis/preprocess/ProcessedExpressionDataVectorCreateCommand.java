/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.controller.analysis.preprocess;

import ubic.gemma.web.controller.BaseCommand;

/**
 * @author keshav
 * @version $Id$
 */
public class ProcessedExpressionDataVectorCreateCommand extends BaseCommand {

    private static final long serialVersionUID = 1L;

    private String accession = null;

    /**
     * @return
     */
    public String getAccession() {
        return accession;
    }

    /**
     * @param accession
     */
    public void setAccession( String accession ) {
        this.accession = accession;
    }

}
