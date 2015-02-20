/*
 * The gemma-model project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.genome.gene.phenotype.valueObject;

/**
 * TODO Document Me
 * 
 * @author jleong
 * @version $Id$
 */
public class DumpsValueObject {
    
    private String name;
    private String url;
    private String lastModified;
    
    public DumpsValueObject(String paramName, String paramUrl, String paramLastModified)
    {
        name = paramName;
        url = paramUrl;
        lastModified = paramLastModified;
    }
    
    public void setName(String paramName)
    {
        name = paramName;
    }
    
    public void setUrl(String paramUrl)
    {
        url = paramUrl;
    }    
}
