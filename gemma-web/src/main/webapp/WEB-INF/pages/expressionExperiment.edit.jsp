<%@ include file="/common/taglibs.jsp"%>

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
</table>
		<h3>
            <fmt:message key="bioAssays.title" />
        </h3>
        <display:table name="expressionExperiment.bioAssays" class="list" requestURI="" id="bioAssayList" export="true" pagesize="10">
         	<display:column property="id" sortable="true" href="/Gemma/bioAssay/showBioAssay.html" paramId="id" paramProperty="id"/>
            <display:column property="name" maxWords="20" />
            <display:column property="description" maxWords="100" />
            <display:setProperty name="export.pdf" value="true" />
        </display:table>

        <h3>
            <fmt:message key="experimentalDesigns.title" />
        </h3>
        <display:table name="expressionExperiment.experimentalDesigns" class="list" requestURI="" id="experimentalDesignList"
            export="true" pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
            <display:column property="name" sortable="true" maxWords="20" href="/Gemma/experimentalDesign/showExperimentalDesign.html" paramId="name" paramProperty="name"/>
            <display:column property="description" sortable="true" maxWords="100"  />
            <display:column property="factorsLink" sortable="true" maxWords="100"/>
            <display:setProperty name="basic.empty.showtable" value="false" />
        </display:table>	
        
        <h3>
            <fmt:message key="investigators.title" />
        </h3>
        <display:table name="expressionExperiment.investigators" class="list" requestURI="" id="contactList"
            export="true" pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
            <display:column property="name" sortable="true" maxWords="20" href="/Gemma/experimentalDesign/showExperimentalDesign.html" paramId="name" paramProperty="name"/>
            <display:column property="phone" sortable="true" maxWords="100"  />
            <display:column property="fax" sortable="true" maxWords="100"  />
            <display:column property="email" sortable="true" maxWords="100"  />
            <display:setProperty name="basic.empty.showtable" value="false" />
        </display:table>
        
        <h3>
            <fmt:message key="analyses.title" />
        </h3>
        <display:table name="expressionExperiment.analyses" class="list" requestURI="" id="analysisList"
            export="true" pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
            <display:column property="name" sortable="true" maxWords="20" href="/Gemma/experimentalDesign/showExperimentalDesign.html" paramId="name" paramProperty="name"/>
            <display:column property="description" sortable="true" maxWords="100"  />
            <display:setProperty name="basic.empty.showtable" value="false" />
        </display:table>
        
        <h3>
            <fmt:message key="expressionExperimentSubsets.title" />
        </h3>
        <display:table name="expressionExperiment.subsets" class="list" requestURI="" id="subsetList"
            export="true" pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
            <display:setProperty name="basic.empty.showtable" value="false" />
        </display:table>
        
        <h3>
            <fmt:message key="designElementDataVectors.title" />
        </h3>
        <display:table name="expressionExperiment.designElementDataVectors" class="list" requestURI="" id="dataVectorList"
            export="true" pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.experiment.ExpressionExperimentWrapper">
            <display:setProperty name="basic.empty.showtable" value="false" />
        </display:table>

	<tr>
		<td>	
		<h3>
            <fmt:message key="expressionExperiment.owner" />
        </h3>
        <Gemma:contact
            contact="<%=expressionExperiment.getOwner()%>" />
        </td>
    </tr>        
        <br/>
   
        <h3>
            <fmt:message key="databaseEntry.title" />
        </h3>
        
        <h5>
            <fmt:message key="databaseEntry.accession.title" />
        </h5>  
      	
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
	   
	
        <h5>
        	<b>
        		<fmt:message key="externalDatabase.title" />
            </b>
        </h5>
        
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
   	<%--
            <input type="text" name="<c:out value="${status.expression}"/>" value="<Gemma:databaseEntry
            databaseEntry="<%=expressionExperiment.getAccession()%>" />"/>
        <br/>
        --%>
   		<br/>
   		
        <h3>
            <fmt:message key="pubMed.publication" />
        </h3>
        <Gemma:bibref bibliographicReference="<%=expressionExperiment.getPrimaryPublication() %>" />
        
        
        <br />
		
        <h3>
            <fmt:message key="auditTrail.title" />
        </h3>
        <Gemma:auditTrail
            auditTrail="<%=expressionExperiment.getAuditTrail()%>" />
            
        <br />
			
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
