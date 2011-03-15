package ubic.gemma.web.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import ubic.gemma.web.controller.common.auditAndSecurity.GeneSetValueObject;

public class GeneSetListContainer implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2163440335723257830L;

	static final int MAX_SIZE = 100;
	
	Integer largestSessionId=0;
	
	ArrayList<GeneSetValueObject> geneSetList;
	
	public GeneSetListContainer(){
		geneSetList = new ArrayList<GeneSetValueObject>();
		
	}
	
	public Integer incrementAndGetLargestSessionId(){
		largestSessionId = largestSessionId +1;
		
		//unique sessionId for each entry in the user's session(doubt that a user will have over 100000 geneset session entries however
		// large gaps can occur between sessionIds because of 'fake' sessionIds assigned to db backed sets for the front end store-
		// eg. page reloads within a user's session lifecycle will create these gaps
		//still I believe 100000 provides a large enough range to avoid conflicts
		if (largestSessionId>100000){
			largestSessionId = 0;
		}
		
		return largestSessionId;
	}
	
	public GeneSetValueObject addGeneSet(GeneSetValueObject gsvo){		
		
		
		boolean setExists=false;
		
		
		//This case shouldn't happen very often (or at all)
		if (gsvo.getSessionId()!=null){
			for (int i =0 ; i<geneSetList.size(); i++){
			
				Integer sid = geneSetList.get(i).getSessionId();
			
				if (sid!=null && sid.equals(gsvo.getSessionId())){
					geneSetList.remove(i);
					geneSetList.add(i, gsvo);
					setExists=true;
					break;				
				}
			
			}
		}
		
		if (!setExists){			
			
			gsvo.setSessionId(incrementAndGetLargestSessionId());
			gsvo.setSession(true);//this line may be redundant
			
			geneSetList.add(gsvo);
			if (geneSetList.size()>MAX_SIZE){
				geneSetList.remove(0);
			}
		}
		
		return gsvo;
		
	}
	
	public void removeGeneSet(GeneSetValueObject gsvo){		
		
		if (gsvo.getSessionId()!=null){
		
			for (int i =0 ; i<geneSetList.size(); i++){
				
				if (geneSetList.get(i).getSessionId().equals(gsvo.getSessionId())){
					geneSetList.remove(i);
					break;
				}
				
			}
			
		}
		
	}
	
	public void updateGeneSet(GeneSetValueObject gsvo){		
		
		if (gsvo.getSessionId()!=null){
		
			for (int i =0 ; i<geneSetList.size(); i++){
				
				if (geneSetList.get(i).getSessionId().equals(gsvo.getSessionId())){
					geneSetList.remove(i);
					geneSetList.add(i, gsvo);
					break;
				}
				
			}
			
		}
		
	}
	
	public Collection<GeneSetValueObject> getRecentGeneSets(){
		
		return geneSetList;
		
	}
	
	

}
