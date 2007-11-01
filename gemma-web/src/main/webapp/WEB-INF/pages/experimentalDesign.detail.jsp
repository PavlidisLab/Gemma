<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="experimentalDesign" scope="request"
    class="ubic.gemma.model.expression.experiment.ExperimentalDesignImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<title>  <fmt:message key="experimentalDesign.details" />  </title>


	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all-debug.js'/>" type="text/javascript"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ExpressionExperimentController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/BioMaterialController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/MgedOntologyService.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ExperimentalDesignController.js'></script>
	 <script type="text/javascript" src='/Gemma/dwr/interface/OntologyService.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/CharacteristicCombo.js'/>"></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/MGEDCombo.js'/>"></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/AnnotationToolBar.js'/>"></script>
	
	
	

<h2>
	<fmt:message key="experimentalDesign.details" />
</h2>
<table cellspacing="10">
	<tr>
    	<td class="label">
        	<b><fmt:message key="experimentalDesign.name" /></b>
        </td>
		<td>
        <%if (experimentalDesign.getName() != null){%>
        	<jsp:getProperty name="experimentalDesign" property="name" />
        <%}else{
        	out.print("Name unavailable");
        }%>
    	</td>
    </tr>
        
            <tr>
                <td class="label">
                    <b>
                        <fmt:message key="experimentalDesign.description" />
                    </b>
                </td>
                <td>
                	<%if (experimentalDesign.getDescription() != null){%>
						<jsp:getProperty name="experimentalDesign" property="description" />
                    <%}else{
                    	out.print("Description unavailable");
                    }%>
                </td>
            </tr>
         
            <tr>
                <td class="label">
                    <b>
                        <fmt:message key="experimentalDesign.replicateDescription" />
                    </b>
                </td>
                <td>
                	<%if (experimentalDesign.getReplicateDescription() != null){%>
                    	<jsp:getProperty name="experimentalDesign" property="replicateDescription" />
                    <%}else{
                    	out.print("Replicate description unavailable");
                    }%>
                </td>
            </tr>    
      
            <tr>
                <td class="label">
                    <b>
                        <fmt:message key="experimentalDesign.qualityControlDescription" />
                    </b>
                </td>
                <td>
                	<%if (experimentalDesign.getQualityControlDescription() != null){%>
                    	<jsp:getProperty name="experimentalDesign" property="qualityControlDescription" />
                    <%}else{
                    	out.print("Quality control description unavailable");
                    }%>
                </td>
            </tr>
            
            <tr>
                <td class="label">
                    <b>
                        <fmt:message key="experimentalDesign.normalizationDescription" />
                    </b>
                </td>
                <td>
                	<%if (experimentalDesign.getNormalizationDescription() != null){%>
                    	<jsp:getProperty name="experimentalDesign" property="normalizationDescription" />
                    <%}else{
                    	out.print("Normalization description unavailable");
                    }%>
                </td>
            </tr>
			<authz:authorize ifAllGranted="admin">
				<tr>
					<td class="label">
						<fmt:message key="auditTrail.date" />
					</td>
					<td>
						<%
						                    if ( experimentalDesign.getAuditTrail() != null ) {
						                    out.print( experimentalDesign.getAuditTrail().getCreationEvent().getDate() );
						                } else {
						                    out.print( "Create date unavailable" );
						                }
						%>
					</td>
				</tr>
			</authz:authorize>
        </table>

<table>
<tr> <td>
        <h3>
            <fmt:message key="experimentalFactors.title" />
        </h3>
        
        <authz:authorize ifAllGranted="admin">
        	<div id="factorGridTB" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width:350px; height:30px;"></div>
        </authz:authorize>
        <div id="factorGrid" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width:350px; height:250px;"></div>
        <br />
     </td>
     <td>
        
        <h3>    <fmt:message key="experimentalDesign.factorValues" />  for factor selected above   </h3>
        <authz:authorize ifAllGranted="admin">
	        <div id="factorValueTB" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width:350px; height:30px;"></div>    	
        </authz:authorize>
        <div id="factorValueGrid" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width:350px; height:250px;"></div>
  
        <br />
        <hr />
        <hr />
  	</td>
  </tr>  
  <tr><td colspan=2>
    <h3>
    	
   	</h3>
   	 
 	<script type="text/javascript" src="<c:url value='/scripts/ajax/ExperimentalDesign.js'/>" type="text/javascript"></script>
    
    <input type="hidden" name="expressionExperimentID" id="expressionExperimentID" value="${expressionExperiment.id}"
    <input type="hidden" name="experimentalDesignID" id="experimentalDesignID" value="${experimentalDesign.id}"
 
    <authz:authorize ifAllGranted="admin">
         <h3>  BioMaterials to Factor Value Association     </h3>   
	    <div id="eDesign" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width:700px; height:30px;"></div>      
		<div id="bmGrid" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width:700px; height:400px;"></div>
	 </authz:authorize>
	 </td>
     </tr>
     </table>   
    <table>
    <tr>
    <td COLSPAN="2">    
            <div align="left"><input type="button"
            onclick="location.href='/Gemma/expressionExperiment/showAllExpressionExperiments.html'"
            value="Back"></div>
            </td>
        <authz:acl domainObject="${experimentalDesign}" hasPermission="1,6">
            <td COLSPAN="2">    
            <div align="left"><input type="button"
            onclick="location.href='editExperimentalDesign.html?id=<%=request.getAttribute("id")%>'"
            value="Edit"></div>
            </td>
        </authz:acl>
    </tr>
    </table>
