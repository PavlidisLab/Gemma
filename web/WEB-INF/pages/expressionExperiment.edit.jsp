<%@ include file="/common/taglibs.jsp"%>

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
	
<table>

    <tr>
        <th>
            <Gemma:label key="expressionExperiment.name"/>
        </th>
        <td>
        <spring:bind path="expressionExperiment.name">
        <c:choose>
            <c:when test="${empty expressionExperiment.name}">
                <input type="text" name="name" value="<c:out value="${status.value}"/>" id="name"/>
                <span class="fieldError"><c:out value="${status.errorMessage}"/></span>
            </c:when>
            <c:otherwise>
        		<c:out value="${expressionExperiment.name}"/>
                <input type="hidden" name="name" value="<c:out value="${status.value}"/>" id="name"/>
            </c:otherwise>
        </c:choose>
        </spring:bind>
        </td>
    </tr>
  
   <%--
	<tr>
		<th>
            <Gemma:label key="expressionExperiment.description"/>
        </th>
        <td>
        <c:choose>
            <c:when test="${empty expressionExperiment.bioassays}">
                <input type="text" name="${status.expression}" value="<c:out value="${status.value}"/>" id="contact.name"/>
                <span class="fieldError"><c:out value="${status.errorMessage}"/></span>
            </c:when>
            <c:otherwise>
                <c:out value="${expressionExperiment.bioassays}"/>
                <input type="hidden" name="${status.expression}" value="<c:out value="${status.value}"/>" id="contact.name"/>
            </c:otherwise>
        </c:choose>
        </td>
	</tr>
    --%>   		
	
	<tr>
        <th>
            <Gemma:label key="expressionExperiment.description"/>
        </th>
        <td>         
        		<spring:bind path="expressionExperiment.description">
        		<textarea name="description" id="description" rows=8 cols=30><c:out value="${status.value}"/></textarea>
            <span class="fieldError"><c:out value="${status.errorMessage}"/></span>
            </spring:bind>
        </td>
    </tr>
	<%--    
    <tr>
        <th>
            <Gemma:label key="expressionExperiment.advertisedNumberOfDesignElements"/>
	                    </th>
	                    <td>
        		<spring:bind path="expressionExperiment.advertisedNumberOfDesignElements">
        		<input type="text" name="${status.expression}" value="${status.value}"/>
        		<span class="fieldError">${status.errorMessage}</span>
        		</spring:bind>
	                    </td>
	                </tr>
	--%>
    <tr>
    	<td></td>
        <td>
	    	<input type="submit" class="button" name="save" value="<fmt:message key="button.save"/>" />
            <input type="submit" class="button" name="cancel" value="<fmt:message key="button.cancel"/>" />
        </td>
    </tr>

</table>
</form>
<%-- TODO
<validate:javascript formName="expressionExperimentForm" staticJavascript="false"/>
<script type="text/javascript"
      src="<c:url value="/scripts/validator.jsp"/>"></script>
--%>