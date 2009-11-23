/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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

import java.io.Serializable;
import java.util.List;

/**
 * Wraps a chunk of data for display (e.g., in a paging grid)
 * 
 * @author Paul
 * @version $Id$
 */
public class ListRange implements Serializable {

    private static final long serialVersionUID = -1765086079209452654L;
    private Object[] data;
    private int totalSize;

    public ListRange( List<? extends Object> objects ) {
        this.data = objects.toArray();
        this.setTotalSize( data.length );
    }

    public Object[] getData() {
        return data;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setData( Object[] data ) {
        this.data = data;
    }

    public void setTotalSize( int totalSize ) {
        this.totalSize = totalSize;
    }
}
