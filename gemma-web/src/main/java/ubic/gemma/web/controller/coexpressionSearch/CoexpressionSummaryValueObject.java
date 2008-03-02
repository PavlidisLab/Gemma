package ubic.gemma.web.controller.coexpressionSearch;

/**
 * @author luke
 */
public class CoexpressionSummaryValueObject {

    int datasetsAvailable;
    int datasetsTested;
    int linksFound;
    int linksMetPositiveStringency;
    int linksMetNegativeStringency;
    
    public int getDatasetsAvailable() {
        return datasetsAvailable;
    }
    
    public void setDatasetsAvailable( int datasetsAvailable ) {
        this.datasetsAvailable = datasetsAvailable;
    }
    
    public int getDatasetsTested() {
        return datasetsTested;
    }
    
    public void setDatasetsTested( int datasetsTested ) {
        this.datasetsTested = datasetsTested;
    }
    
    public int getLinksFound() {
        return linksFound;
    }
    
    public void setLinksFound( int linksFound ) {
        this.linksFound = linksFound;
    }
    
    public int getLinksMetNegativeStringency() {
        return linksMetNegativeStringency;
    }
    
    public void setLinksMetNegativeStringency( int linksMetNegativeStringency ) {
        this.linksMetNegativeStringency = linksMetNegativeStringency;
    }
    
    public int getLinksMetPositiveStringency() {
        return linksMetPositiveStringency;
    }
    
    public void setLinksMetPositiveStringency( int linksMetPositiveStringency ) {
        this.linksMetPositiveStringency = linksMetPositiveStringency;
    }
    
    
    
}
