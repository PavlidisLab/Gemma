<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="experimentalDesign" scope="request"
    class="ubic.gemma.model.expression.experiment.ExperimentalDesignImpl" />
<jsp:useBean id="expressionExperiment" scope="request"
	class="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<head>
	<title><fmt:message key="experimentalDesign.details" /></title>

	<script src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all.js'/>" type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/RowExpander.js'/>" type="text/javascript"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
	<script type="text/javascript" src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/BioMaterialController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ExpressionExperimentController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/ExperimentalDesignController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/CharacteristicBrowserController.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/interface/MgedOntologyService.js'></script>
	<script type="text/javascript" src='/Gemma/dwr/interface/OntologyService.js'></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/CharacteristicCombo.js'/>"></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/MGEDCombo.js'/>"></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/AnnotationToolBar.js'/>"></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/util/GemmaGridPanel.js'/>"></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/ExperimentalFactorEditor.js'/>"></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/ExperimentalFactorCombo.js'/>"></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/FactorValueCombo.js'/>"></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/FactorValueEditor.js'/>"></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/BioMaterialEditor.js'/>"></script>
	<script type='text/javascript' src="<c:url value='/scripts/ajax/annotation/ExperimentalDesign.js'/>"></script>

</head>

<authz:authorize ifAnyGranted="admin">
<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</authz:authorize>
<authz:authorize ifNotGranted="admin">
<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</authz:authorize>

<input type="hidden" name="expressionExperimentID" value="${expressionExperiment.id}" />
<input type="hidden" name="experimentalDesignID" value="${experimentalDesign.id}" />

<div style="padding: 2px;" onclick="Effect.toggle('edDetail', 'blind', {duration:0.1})">
	<h2>
		<img src="/Gemma/images/plus.gif" />
		<fmt:message key="experimentalDesign.details" /> for <a href='<c:out value="${expressionExperimentUrl}" />'><jsp:getProperty name="expressionExperiment" property="shortName" /></a>
	</h2>
</div>
<div id="edDetail" style="display: none">
	<div>
		<%-- inner div needed for effect  --%>
<table cellspacing="10">
	<tr>
    	<td class="label">
        	<b><fmt:message key="experimentalDesign.name" /></b>
        </td>
		<td>
        <%
        if ( experimentalDesign.getName() != null ) {
        %>
        	<jsp:getProperty name="experimentalDesign" property="name" />
        <%
                        } else {
                        out.print( "Experimental Design Name unavailable" );
                    }
        %>
    	</td>
    </tr>    
    <tr>
       <td class="label">
                    <b>
                        <fmt:message key="experimentalDesign.description" />
                    </b>
                </td>
                <td>
                	<%
                	if ( experimentalDesign.getDescription() != null ) {
                	%>
						<jsp:getProperty name="experimentalDesign" property="description" />
                    <%
                                    } else {
                                    out.print( "Description unavailable" );
                                }
                    %>
                </td>
            </tr>
         
            <tr>
                <td class="label">
                    <b>
                        <fmt:message key="experimentalDesign.replicateDescription" />
                    </b>
                </td>
                <td>
                	<%
                	if ( experimentalDesign.getReplicateDescription() != null ) {
                	%>
                    	<jsp:getProperty name="experimentalDesign" property="replicateDescription" />
                    <%
                                    } else {
                                    out.print( "Replicate description unavailable" );
                                }
                    %>
                </td>
            </tr>    
      
            <tr>
                <td class="label">
                    <b>
                        <fmt:message key="experimentalDesign.qualityControlDescription" />
                    </b>
                </td>
                <td>
                	<%
                	if ( experimentalDesign.getQualityControlDescription() != null ) {
                	%>
                    	<jsp:getProperty name="experimentalDesign" property="qualityControlDescription" />
                    <%
                                    } else {
                                    out.print( "Quality control description unavailable" );
                                }
                    %>
                </td>
            </tr>
            
            <tr>
                <td class="label">
                    <b>
                        <fmt:message key="experimentalDesign.normalizationDescription" />
                    </b>
                </td>
                <td>
                	<%
                	if ( experimentalDesign.getNormalizationDescription() != null ) {
                	%>
                    	<jsp:getProperty name="experimentalDesign" property="normalizationDescription" />
                    <%
                                    } else {
                                    out.print( "Normalization description unavailable" );
                                }
                    %>
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
	</div>
</div>

<!-- Expression Experiment Details  -->	

<div style="padding: 2px;" onclick="Effect.toggle('eeDetail', 'blind', {duration:0.1})">
	<h2>
		<img src="/Gemma/images/plus.gif" />
		<fmt:message key="expressionExperiment.details" />
	</h2>
</div>
<div id="eeDetail" style="display: none">
	<div>
		<%-- inner div needed for effect  --%>
<table cellspacing="10">		
			    <tr>
    	<td class="label">
        	<b><fmt:message key="expressionExperiment.name" /></b>
        </td>
		<td>
        <%
        if ( expressionExperiment.getName() != null ) {
        %>
        	<jsp:getProperty name="expressionExperiment" property="name" />
        <%
                        } else {
                        out.print( "Expression Experiment Name unavailable" );
                    }
        %>
    	</td>
    </tr>
    <tr>
		<td class="label">
			<fmt:message key="expressionExperiment.description" />
		</td>
		<td>
			<%
			if ( expressionExperiment.getDescription() != null ) {
			%>
			<div class="clob" style="width: 40%;">
				<jsp:getProperty name="expressionExperiment" property="description" />
			</div>

			<%
			                } else {
			                out.print( "Description unavailable" );
			            }
			%>
		</td>
	</tr>
		<tr>
		<td class="label">
			<fmt:message key="databaseEntry.title" />
		</td>
		<td>
			<Gemma:databaseEntry databaseEntry="${expressionExperiment.accession}" />
		</td>
	</tr>
	<tr>
		<td class="label">
			<fmt:message key="pubMed.publication" />
		</td>
		<td>
			<%
			if ( expressionExperiment.getPrimaryPublication() != null ) {
			%>
			<Gemma:citation citation="${expressionExperiment.primaryPublication }" />
			<%
			                } else {
			                out.print( "Primary publication unavailable" );
			            }
			%>
		</td>
	</tr>
</table>
	</div>
</div>

<!-- Experimental Factors -->

<!-- 
<div id="tabPanel">
	<div id="experimentalFactorPanel" class="x-hide-display"></div>
	<div id="factorValuePanel" class="x-hide-display"></div>
</div>
-->
<div id="experimentalFactorPanel" style="margin-bottom: 1em;"></div>
<form name="factorValueForm">
<div id="factorValuePanel" style="margin-bottom: 1em;"></div>
</form>
<div id="bioMaterialsPanel"></div>

<!-- 
<script type="text/javascript" src="<c:url value='/scripts/ajax/ExperimentalDesign.js'/>" type="text/javascript"></script>
<authz:authorize ifAllGranted="admin">
	<input type="hidden" name="experimentalDesignAdmin" value="true" />
</authz:authorize>
<authz:authorize ifNotGranted="admin">
<input type="hidden" name="experimentalDesignAdmin" value="" />
</authz:authorize>

<table>
	<tr>
		<td style="vertical-align: top">
        	<h3><fmt:message key="experimentalFactors.title" /></h3>
        	<authz:authorize ifAllGranted="admin">
        		<div id="factorGridTB" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width:400px; height:30px;"></div>
	        </authz:authorize>
    	    <div id="factorGrid" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width:400px; height:150px;"></div>
	     </td>
    	 <td style="vertical-align: top">
        	<h3><fmt:message key="experimentalDesign.factorValues" />  for selected factor</h3>
	        <div id="factorValueGrid" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width:400px; height:180px;"></div>
	  	</td>
	</tr>
 	<authz:authorize ifAllGranted="admin">  
  	<tr>
  		<td colspan=2>
			<h3>BioMaterial to Factor Value Association</h3>
	    	<div id="eDesign" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width:800px; height:30px;"></div>      
			<div id="bmGrid" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width:800px; height:400px;"></div>
		</td>
	</tr>
	</authz:authorize>
</table>
-->

<!-- Doesn't work so comment out.
<table>
	<tr>
		<td COLSPAN="2">
			<div align="left">
				<input type="button"
					onclick="location.href='/Gemma/expressionExperiment/showAllExpressionExperiments.html'"
					value="Back">
			</div>
		</td>
		<authz:acl domainObject="${experimentalDesign}" hasPermission="1,6">
		<td COLSPAN="2">
			<div align="left">
				<input type="button"
					onclick="location.href='editExperimentalDesign.html?id=<%=request.getAttribute( "id" )%>'"
					value="Edit">
			</div>
		</td>
		</authz:acl>
	</tr>
</table>
-->
