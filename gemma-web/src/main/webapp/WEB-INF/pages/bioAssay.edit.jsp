<%@ include file="/common/taglibs.jsp"%>

<spring:bind path="bioAssayImpl.*">
	<c:if test="${not empty status.errorMessages}">
		<div class="error">
		<c:forEach var="error"
			items="${status.errorMessages}">
			<img src="<c:url value="/images/iconWarning.gif"/>"
				alt="<fmt:message key="icon.warning"/>" class="icon" />
			<c:out value="${error}" escapeXml="false" />
			<br />
		</c:forEach></div>
	</c:if>
</spring:bind>

<form method="post" action="<c:url value="/bioAssay/editBioAssay.html"/>">

<table cellspacing="10">
	<tr>
		<td valign="top">
        	<h2>
        		<fmt:message key="bioAssay.title" />
            </h2>
        </td>
	</tr>
	<tr>
		<td valign="top">
        	<b>
        		<fmt:message key="bioAssay.name" />
            </b>
        </td>
        <td>
        	<spring:bind path="bioAssayImpl.name">
	        <input type="text" name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/>
	        </spring:bind>
	    </td>
	</tr>
	<tr>    
	    <td valign="top">
        	<b>
        		<fmt:message key="bioAssay.description" />
            </b>
        </td>
        <td>
        	<spring:bind path="bioAssayImpl.description">
	        <input type="text" name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/>
	        </spring:bind>
	    </td>
	</tr>
	</table>
	
		<h3>
    		<fmt:message key="bioAssay.bioMaterials" />
    	</h3>
        <display:table name="bioAssayImpl.samplesUsed" class="list" requestURI="" id="bioMaterialList"
        export="true" pagesize="10" decorator="ubic.gemma.web.taglib.displaytag.expression.bioAssay.BioAssayWrapper">
            <display:column property="name" maxWords="20" />
            <display:column property="description" maxWords="100" />
            <display:column property="id" sortable="true" href="/Gemma/bioMaterial/showBioMaterial.html" paramId="id" paramProperty="id"/>
        </display:table>
        
        <h3>
            <fmt:message key="bioAssay.arrayDesigns" />
        </h3>
        <display:table name="bioAssayImpl.arrayDesignUsed" class="list" requestURI="" id="arrayDesignList"
        export="true" pagesize="10" >
            <display:column property="name" maxWords="20" sortable="true" href="/Gemma/arrayDesign/showArrayDesign.html" paramId="name" paramProperty="name"/>
            <display:column property="description" maxWords="100" />
            <display:column property="advertisedNumberOfDesignElements" maxWords="100" />
        </display:table>
            
	    <h3>
        	<fmt:message key="databaseEntry.title" />
        </h3>
        
        <h5>
        	<fmt:message key="databaseEntry.accession.title" />
        </h5>
        	<spring:bind path="bioAssayImpl.accession">
        	<c:choose>
            <c:when test="${bioAssayImpl.accession == null}">
                <input type="text" name="bioAssayImpl.accession.accession" value="<c:out value="Accession unavailable"/>"/>
            </c:when>
            <c:otherwise>
                <input type="text" name="bioAssayImpl.accession.accession" value="<c:out value="${bioAssayImpl.accession.accession}"/>"/>
            </c:otherwise>
        	</c:choose>      
	        </spring:bind>
	        
        <h5>
        	<fmt:message key="externalDatabase.title" />
        </h5>
        <c:if test="${bioAssayImpl.accession != null}">
       		<spring:bind path="bioAssayImpl.accession.externalDatabase.name">
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
        <td>
        	<spring:bind path="bioAssayImpl.accession.accession">
	        <input type="text" name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/>
	        </spring:bind>
	    </td>
        --%>
        
       	<br/><br />
       	
	<table>
	<tr>
        <td>
	    	<input type="submit" class="button" name="save" value="<fmt:message key="button.save"/>" />
            <input type="submit" class="button" name="cancel" value="<fmt:message key="button.cancel"/>" />
       	</td>
    </tr>
    </table>
</form>
