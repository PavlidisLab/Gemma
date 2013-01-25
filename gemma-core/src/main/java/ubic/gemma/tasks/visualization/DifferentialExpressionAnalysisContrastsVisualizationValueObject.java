package ubic.gemma.tasks.visualization;

import java.util.HashMap;
import java.util.Map;

public class DifferentialExpressionAnalysisContrastsVisualizationValueObject {
    
    public class ContrastVisualizationValueObject {
        public double visualizationValue;
        public double foldChangeValue;
        public double contrastPvalue;
        public boolean isBaseline;
        public long id;
        public String factorValueName;
       
        public ContrastVisualizationValueObject( Long id, double visualizationValue, double foldChangeValue, double pValue, String name ) {
            this.factorValueName = name;
            this.foldChangeValue = foldChangeValue;
            this.id = id;
            this.visualizationValue = visualizationValue;
            this.contrastPvalue = pValue;
        }
        
        public double getVisualizationValue() {
            return this.visualizationValue;
        }
        
        public double getFoldChangeValue() {
            return this.foldChangeValue;
        }
        
        public double getContrastPvalue() {
            return this.contrastPvalue;
        }
    }
    
    private Map<Long,Map<Long,ContrastVisualizationValueObject>> contrasts;                     
    
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
