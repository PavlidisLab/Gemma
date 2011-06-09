package ubic.gemma.web.visualization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DifferentialExpressionAnalysisContrastsVisualizationValueObject {
    
    public class ContrastVisualizationValueObject {
        public double visualizationValue;
        public double foldChangeValue;
        public boolean isBaseline;
        public long id;
        public String factorValueName;
       
        public ContrastVisualizationValueObject( Long id, double visualizationValue, double foldChangeValue, String name ) {
            this.factorValueName = name;
            this.foldChangeValue = foldChangeValue;
            this.id = id;
            this.visualizationValue = visualizationValue;
        }
        
        public double getVisualizationValue() {
            return this.visualizationValue;
        }
        
        public double getFoldChangeValue() {
            return this.foldChangeValue;
        }
    }
    
    private Map<Long,Map<Long,ContrastVisualizationValueObject>> contrasts;                     
    private List<Long> factorValueIds;
    private List<String> factorValueNames;
    
    public DifferentialExpressionAnalysisContrastsVisualizationValueObject() {
        contrasts = new HashMap<Long,Map<Long,ContrastVisualizationValueObject>>();
    }
    
    public Map<Long,Map<Long,ContrastVisualizationValueObject>> getContrasts() {
        return contrasts;
    }
    
    public void add (Long geneId, Map<Long,ContrastVisualizationValueObject> contrastsForGene ) {        
        contrasts.put( geneId, contrastsForGene );                
    }
    
    public void addNoContrastsFound (Long geneId) {
        contrasts.put( geneId, null );                
    }
        
}
