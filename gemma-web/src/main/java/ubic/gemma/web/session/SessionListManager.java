package ubic.gemma.web.session;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.web.controller.expression.experiment.ExpressionExperimentSetValueObject;


@Service
public class SessionListManager{
	
	@Autowired
	GeneSetListContainer geneSetList;
	
	@Autowired
	ExperimentSetListContainer experimentSetList;
	
	public Collection<GeneSetValueObject> getRecentGeneSets(){
		return geneSetList.getRecentGeneSets();
	}
	
	public GeneSetValueObject addGeneSet(GeneSetValueObject gsvo){
		
		return geneSetList.addGeneSet(gsvo);
		
	}
	
	public void removeGeneSet(GeneSetValueObject gsvo){
		
		geneSetList.removeGeneSet(gsvo);
		
	}
	
	public void updateGeneSet(GeneSetValueObject gsvo){
		
		geneSetList.updateGeneSet(gsvo);
		
	}
	
	//this gives result(from the DB) unique Session Ids(used by the front end store)
	//ugly and hackish
	public void setUniqueGeneSetStoreIds(Collection<GeneSetValueObject> result, Collection<GeneSetValueObject> sessionResult){		
        
        //give db genesets a unique sessionId so that the javascript widget plays nice
        for (GeneSetValueObject gsvo: result){      	
        	gsvo.setSessionId(geneSetList.incrementAndGetLargestSessionId());        	
        	
        }
	}
	
	public Integer incrementAndGetLargestGeneSetSessionId(){
		return geneSetList.incrementAndGetLargestSessionId();
	}
	
	
	public Collection<ExpressionExperimentSetValueObject> getRecentExperimentSets(){
		return experimentSetList.getRecentExperimentSets();
	}
	
	public void addExperimentSet(ExpressionExperimentSetValueObject eesvo){
		
		experimentSetList.addExperimentSet(eesvo);
		
	}
	
	

}
