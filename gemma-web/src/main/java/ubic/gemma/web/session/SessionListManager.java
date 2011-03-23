package ubic.gemma.web.session;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.persistence.GemmaSessionBackedValueObject;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentSetValueObject;


@Service
public class SessionListManager{
	
	@Autowired
	GeneSetListContainer geneSetList;
	
	@Autowired
	ExperimentSetListContainer experimentSetList;
	
	public Collection<GeneSetValueObject> getRecentGeneSets(){
		
		//We know that geneSetList will only contain GeneSetValueObjects (via SessionListManager.addGeneSet(GeneSetValueObject) so this cast is okay
		@SuppressWarnings("unchecked")
		List<GeneSetValueObject> castedCollection = (List)geneSetList.getRecentSets();
				
		return castedCollection;
	}
	
	public GeneSetValueObject addGeneSet(GeneSetValueObject gsvo){
		
		return (GeneSetValueObject)geneSetList.addSet(gsvo);
		
	}
	
	public void removeGeneSet(GeneSetValueObject gsvo){
		
		geneSetList.removeSet(gsvo);
		
	}
	
	public void updateGeneSet(GeneSetValueObject gsvo){
		
		geneSetList.updateSet(gsvo);
		
	}
	
	//this gives result(from the DB) unique Session Ids(used by the front end store) if it doesn't already have one
	public void setUniqueGeneSetStoreIds(Collection<GeneSetValueObject> result){
		
		//this cast is safe because we know that we are getting a Collection of GeneSetValueObjects(which implements GemmaSessionBackedValueObject
		@SuppressWarnings("unchecked")
		Collection<GemmaSessionBackedValueObject> castedCollection = (Collection)result;
		
		geneSetList.setUniqueSetStoreIds(castedCollection);		
        
	}
	
	public boolean isDbBackedGeneSetSessionId(Long sessionId){
		
		return geneSetList.isDbBackedSessionId(sessionId);
		
	}
	
	public Long getDbGeneSetIdBySessionId(Long sessionId){
		return geneSetList.getDbIdFromSessionId(sessionId);
	}
	
	public Long incrementAndGetLargestGeneSetSessionId(){
		return geneSetList.incrementAndGetLargestSessionId();
	}
	
	
	public Collection<ExpressionExperimentSetValueObject> getRecentExperimentSets(){		
		
		@SuppressWarnings("unchecked")
		List<ExpressionExperimentSetValueObject> castedCollection = (List)experimentSetList.getRecentSets();
				
		return castedCollection;		
		
	}	
	
	public ExpressionExperimentSetValueObject addExperimentSet(ExpressionExperimentSetValueObject eesvo){
		
		return (ExpressionExperimentSetValueObject)experimentSetList.addSet(eesvo);
		
	}
	
	public void removeExperimentSet(ExpressionExperimentSetValueObject eesvo){
		
		experimentSetList.removeSet(eesvo);
		
	}
	
	public void updateExperimentSet(ExpressionExperimentSetValueObject eesvo){
		
		experimentSetList.updateSet(eesvo);
		
	}
	
	public void setUniqueExperimentSetStoreIds(Collection<ExpressionExperimentSetValueObject> result){
		
		//this cast is safe because we know that we are getting a Collection of ExpressionExperimentSetValueObjects(which implements GemmaSessionBackedValueObject
		@SuppressWarnings("unchecked")
		Collection<GemmaSessionBackedValueObject> castedCollection = (Collection)result;
		
		experimentSetList.setUniqueSetStoreIds(castedCollection);		
        
	}
	
	public Long incrementAndGetLargestExperimentSetSessionId(){
		return experimentSetList.incrementAndGetLargestSessionId();
	}
	
	public boolean isDbBackedExperimentSetSessionId(Long sessionId){
		
		return experimentSetList.isDbBackedSessionId(sessionId);
		
	}
	
	public Long getDbExperimentSetIdBySessionId(Long sessionId){
		return experimentSetList.getDbIdFromSessionId(sessionId);
	}	
	

}
