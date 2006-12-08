<%@ include file="/common/taglibs.jsp"%>
<jsp:directive.page import="org.apache.commons.lang.StringUtils" />
<jsp:useBean id="expressionExperiment" scope="request"
    class="ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<spring:bind path="expressionExperiment.*">
	<c:if test="${not empty status.errorMessages}">
		<div class="error"><c:forEach var="error"
			items="${status.errorMessages}">
			<img src="<c:url value="/images/iconWarning.gif"/>"
				alt="<fmt:message key="icon.warning"/>" class="icon" />
			<c:out value="${error}" escapeXml="false" />
			<br />
		</c:forEach></div>
	</c:if>
</spring:bind>

<title> <fmt:message key="expressionExperiment.title" /> </title>
<form method="post" action="<c:url value="/expressionExperiment/editExpressionExperiment.html"/>">


<h2>
	<fmt:message key="expressionExperiment.title" />
</h2>
	
<table cellspacing="10">
    <tr>
       <td valign="top">
        	<b>
        	<fmt:message key="expressionExperiment.name" />
            </b>
        </td>
        <td>
        	<spring:bind path="expressionExperiment.name">
	        <input type="text" name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/>
	        </spring:bind>
	    </td>
    </tr>
    
    <tr>    
	    <td valign="top">
        	<b>
        		<fmt:message key="expressionExperiment.description" />
            </b>
        </td>
        <td>
        	<spring:bind path="expressionExperiment.description">
	        <input type="text" name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/>
	        <%--<textarea rows=8 cols=30 name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"></textarea>--%>
	        </spring:bind>
	    </td>
	</tr>
	
	<tr>    
	    <td valign="top">
        	<b>
        	<fmt:message key="expressionExperiment.source" />
            </b>
        </td>
        <td>
        	<spring:bind path="expressionExperiment.source">
	        <input type="text" name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/>
	        </spring:bind>
	    </td>
	</tr>	
                <tr>
       <td valign="top">
        	<b>
        	<fmt:message key="databaseEntry.title" />
            </b>
        </td>
        <td>
            <spring:bind path="expressionExperiment.accession">
        	<c:choose>
            <c:when test="${expressionExperiment.accession == null}">
                <input type="text" name="expressionExperiment.accession.accession" value="<c:out value="Accession unavailable"/>"/>
            </c:when>
            <c:otherwise>
                <input type="text" name="expressionExperiment.accession.accession" value="<c:out value="${expressionExperiment.accession.accession}"/>"/>
            </c:otherwise>
        	</c:choose>      
	        </spring:bind>
	      
	    </td>
    </tr>  
    
        <tr>
       <td valign="top">
        	<b>
        	<fmt:message key="externalDatabase.title" />
            </b>
        </td>
        <td>
                <c:if test="${expressionExperiment.accession != null}">
       		<spring:bind path="expressionExperiment.accession.externalDatabase.name">
       			<select name="${status.expression}">
          			<c:forEach items="${externalDatabases}" var="externalDatabase">
            			<option value="${externalDatabase.name}" <c:if test="${status.value == externalDatabase.name}">selected="selected"</c:if>>
                			${externalDatabase.name}
            			</option>
          			</c:forEach>
        		</select>
        	<span class="fieldError">${status.errorMessage}</span>
       		</spring:bind>
       	</c:if>
	    </td>
    </tr>
    
   
        

       	
       	
    
       
    	            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="expressionExperiment.owner" />
                    </b>
                </td>
                <td>
                	<%if (expressionExperiment.getOwner() != null){%>
                    	<jsp:getProperty name="expressionExperiment" property="owner" />
                    <%}else{
                    	out.print("Owner unavailable");
                    }%>
                </td>
            </tr>       
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="pubMed.publication" />
                    </b>
                </td>
                <td>
                	<%if (expressionExperiment.getPrimaryPublication() != null){%>
                    	<jsp:getProperty name="expressionExperiment" property="primaryPublication" />
                    <%}else{
                    	out.print("Primary publication unavailable");
                    }%>
                </td>
            </tr>   
            
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="auditTrail.date" />
                    </b>
                </td>
                <td>
                	<%if (expressionExperiment.getAuditTrail() != null){
                		out.print(expressionExperiment.getAuditTrail().getCreationEvent().getDate());
                    }else{
                    	out.print("Create date unavailable");
                    }%>
                </td>
            </tr>                
            
        
      	

	        
	        
	               
</table>

		<authz:authorize ifAnyGranted="admin">
			<h3>
				<fmt:message key="experimentalDesign.title" />
				:
				<a
					href="/Gemma/experimentalDesign/showExperimentalDesign.html?id=<%out.print(expressionExperiment.getExperimentalDesign().getId());%> ">
					<%
					out.print( expressionExperiment.getExperimentalDesign().getName() );
					%> </a>
			</h3>
			<p>
				<b>Description:</b>
				<%
				                        out
				                        .print( StringUtils.abbreviate( expressionExperiment.getExperimentalDesign().getDescription(),
				                                100 ) );
				%>
				<BR />
				<BR />
				This experimental design has
				<%
				out.print( expressionExperiment.getExperimentalDesign().getExperimentalFactors().size() );
				%>
				experimental factors.
			</p>

			<%
			if ( expressionExperiment.getAnalyses().size() > 0 ) {
			%>
			<h3>
				<fmt:message key="analyses.title" />
			</h3>
			<display:table name="expressionExperiment.analyses" class="list"
				requestURI="" id="analysisList" pagesize="10"
				decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
				<display:column property="name" sortable="true" maxWords="20"
					href="/Gemma/experimentalDesign/showExperimentalDesign.html"
					paramId="name" paramProperty="name" />
				<display:column property="description" sortable="true"
					maxWords="100" />
				<display:setProperty name="basic.empty.showtable" value="false" />
			</display:table>
			<%
			}
			%>
			<%
			if ( expressionExperiment.getSubsets().size() > 0 ) {
			%>
			<script type="text/javascript" src="<c:url value="/scripts/aa.js"/>"></script>
			<h3>
				<fmt:message key="expressionExperimentSubsets.title" />
			</h3>
			<aazone tableId="subsetList" zone="subsetTable" />
			<aa:zone name="subsetTable">
				<display:table name="expressionExperiment.subsets" class="list"
					requestURI="/Gemma/expressionExperiment/showExpressionExperiment.html"
					id="subsetList" pagesize="10"
					decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentSubSetWrapper">
					<display:column property="nameLink" sortable="true" maxWords="20"
						titleKey="expressionExperimentSubsets.name" />
					<display:column property="description" sortable="true"
						maxWords="100" />
					<display:setProperty name="basic.empty.showtable" value="false" />
				</display:table>
			</aa:zone>
			<%
			}
			%>
		</authz:authorize>

		<h3>
			<fmt:message key="designElementDataVectors.title" />
		</h3>
		<p>
			There are
			<b> <c:out value="${designElementDataVectorCount}" /> </b>
			expression profiles for this experiment.
			<BR />
			<BR />
			<b>Details by quantitation type:</b>
		</p>
		<aazone tableId="dataVectorList" zone="dataVectorTable" />
		<aa:zone name="dataVectorTable">
			<display:table name="qtCountSet" class="list"
				requestURI="/Gemma/expressionExperiment/showExpressionExperiment.html"
				id="dataVectorList" pagesize="10"
				decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
				<display:column property="qtName" sortable="true" maxWords="20"
					titleKey="quantitationType.name" />
				<display:column property="qtValue" sortable="true" maxWords="20"
					titleKey="quantitationType.countVectors" />
				<display:setProperty name="basic.empty.showtable" value="false" />
			</display:table>
		</aa:zone>
		
		<ul>
		<li id="foo" class="dragItem"> foo </li>
		<li id="bar" class="dragItem"> bar </li>
		<li id="foo2" class="dragItem"> foo2 </li>
		<li id="bar1" class="dragItem"> bar1 </li>
		</ul>
		
		<script language="JavaScript" type="text/javascript">
        var windowIdArray = new Array('foo','bar','foo2','bar1');
        for(i=0;i<windowIdArray.length;i++)
    {
        var windowId = windowIdArray[i];
        //set to be draggable
        new Draggable(windowId,{revert:true});
        //set to be droppable
        Droppables.add(windowId, {overlap: 'vertical', accept: 'dragItem',hoverclass: 'drophover',onDrop: function(element, droppableElement)
            {
                var content1 = element.innerHTML;
                var content2 = droppableElement.innerHTML;          
                droppableElement.innerHTML = content1;
                element.innerHTML = content2;
            }
            });
    }
		</script>

		<authz:authorize ifAnyGranted="admin">
			<h3>
				Biomaterials and Assays
			</h3>
			<Gemma:assayView expressionExperiment="${expressionExperiment}"></Gemma:assayView>
		</authz:authorize>

		
			
		<table>
		<tr>
        <td>
	    	<input type="submit" class="button" name="save" value="<fmt:message key="button.save"/>" />
            <input type="submit" class="button" name="cancel" value="<fmt:message key="button.cancel"/>" />
       	</td>
       	</tr>
       	</table>
       
   
</form>

<validate:javascript formName="expressionExperiment" staticJavascript="false"/>
<script type="text/javascript"
      src="<c:url value="/scripts/validator.jsp"/>"></script>
<script type="text/javascript"
			src="<c:url value="/scripts/aa-init.js"/>"></script>
