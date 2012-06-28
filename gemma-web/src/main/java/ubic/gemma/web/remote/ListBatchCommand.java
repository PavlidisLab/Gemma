/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.web.remote;

/**
 * Encapsulates information needed for generic list browsing.
 * 
 * @author paul
 * @version $Id$
 */
public class ListBatchCommand {

    private String dir;

    private Integer limit;

    // FIXME rather not use a string here.
    private String sort = "modDate"; // which column

    private Integer start;

    /**
     * @return the dir
     */
    public String getDir() {
        return dir;
    }

    public Integer getLimit() {
        return limit;
    }

    /**
     * @return the sort
     */
    public String getSort() {
        return sort;
    }

    public Integer getStart() {
        return start;
    }

    /**
     * @param dir the dir to set
     */
    public void setDir( String dir ) {
        this.dir = dir;
    }

    public void setLimit( Integer limit ) {
        this.limit = limit;
    }

    /**
     * @param sort the sort to set
     */
    public void setSort( String sort ) {
        this.sort = sort;
    }

    public void setStart( Integer start ) {
        this.start = start;
    }

}
