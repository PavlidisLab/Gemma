package ubic.gemma.ontology;

import java.io.Serializable;

public class OntologyDataList implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -2232618638114399153L;
    private Object[] data;
    private int totalSize;

    public Object[] getData() {
        return data;
    }

    /**
     * @return the totalSize
     */
    public int getTotalSize() {
        return totalSize;
    }

    /**
     * @param data the data to set
     */
    public void setData( Object[] data ) {
        this.data = data;
    }

    /**
     * @param totalSize the totalSize to set
     */
    public void setTotalSize( int totalSize ) {
        this.totalSize = totalSize;
    }
}
