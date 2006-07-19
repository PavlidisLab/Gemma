<%@ include file="/common/taglibs.jsp"%>

<jsp:useBean id="command" scope="request"
    class="ubic.gemma.web.controller.expression.experiment.ExpressionExperimentSearchCommand" />

<spring:bind path="command.*">
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

<form method="post" action="<c:url value="/expressionExperiment/searchExpressionExperiment.html"/>">

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
                	<%if (command.getName() != null){%>
                    <jsp:getProperty name="command" property="name"/>                   	
                    <%}else{
                    	out.print("Name unavailable");
                    }%>
                </td>
            </tr>
      
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="expressionExperiment.description" />
                    </b>
                </td>	
                <td>
                	<%if (command.getDescription() != null){%>
                    	<jsp:getProperty name="command" property="description" />
                    <%}else{
                    	out.print("Description unavailable");
                    }%>
                </td>
            </tr>
		</table>
		<hr/>
		
		<table>		
        <tr>
            <td valign="top">
        	  <b>
        		<fmt:message key="label.search" />
              </b>
            </td>
        
            <td>  
       		<spring:bind path="command.searchCriteria">
       			<select name="${status.expression}">
          			<c:forEach items="${searchCategories}" var="searchCategory">
            			<option value="${searchCategory}" <c:if test="${status.value == searchCategory}">selected="selected" </c:if>>
                			${searchCategory}
            			</option>
          			</c:forEach>
        		</select>       		
        	<span class="fieldError">${status.errorMessage}</span>
       		</spring:bind>      	
   	        </td>
   	        
   	        <td valign="top">
                    <b>
                        <fmt:message key="label.suppressVisualizations" />
                    </b>
                </td>
                <td>
        		<spring:bind path="command.suppressVisualizations">
        			<input type="hidden" name="_${status.expression}"/>
	        		<input type="checkbox" name="${status.expression}" value="true"/>
	        			<c:if test="${status.value}">checked="checked"</c:if>
	        	<span class="fieldError">${status.errorMessage}</span>		
	        	</spring:bind>
	    	</td>                   	
        </tr>
        
        <tr>
           <td valign="top">
        		<b>
        			<fmt:message key="label.searchString" />
        			<br/>
        			(comma sep.)      	
            	</b>
        	</td>
        	<td>
        		<spring:bind path="command.searchString">
	        		<input type="text" size=10 name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/>
	        	</spring:bind>
	    	</td>     
        </tr>
        
        <tr>
                <td valign="top">
        		<b>
        			<fmt:message key="label.stringency" />
            	</b>
        	</td>
        	<td>
        		<spring:bind path="command.stringency">
	        		<input "type="text" size=1 name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/>
	        	</spring:bind>
	    	</td>
            </tr>
            
            <tr>
           <td valign="top">
        		<b>
        			<fmt:message key="label.imageWidth" />
        			<br/>
            	</b>
        	</td>
        	<td>
        		<spring:bind path="command.imageWidth">
	        		<input type="text" size=1 name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/>
	        	</spring:bind>
	    	</td>     
        </tr>
        
        <tr>
           <td valign="top">
        		<b>
        			<fmt:message key="label.imageHeight" />
        			<br/>
            	</b>
        	</td>
        	<td>
        		<spring:bind path="command.imageHeight">
	        		<input type="text" size=1 name="<c:out value="${status.expression}"/>" value="<c:out value="${status.value}"/>"/>
	        	</spring:bind>
	    	</td>     
        </tr>
		</table>        
        <br />
        		
		<table>
		<tr>
        <td>
	    	<input type="submit" class="button" name="submit" value="<fmt:message key="button.submit"/>" />
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