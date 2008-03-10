<%-- 
author: keshav
version: $Id$
--%>


<%@ include file="/common/taglibs.jsp"%>

<jsp:useBean id="diffExpressionSearchCommand" scope="request"
	class="ubic.gemma.web.controller.diff.DiffExpressionSearchCommand" />

<script
	src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>"
	type="text/javascript"></script>
<script src="<c:url value='/scripts/ext/ext-all.js'/>"
	type="text/javascript"></script>
<script src="<c:url value='/scripts/ext/RowExpander.js'/>"
	type="text/javascript"></script>
<script type="text/javascript"
	src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>
<script type="text/javascript"
	src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>
<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
<script type='text/javascript' src='/Gemma/dwr/util.js'></script>
<script type='text/javascript'
	src='/Gemma/dwr/interface/GenePickerController.js'></script>
<script type='text/javascript'
	src='/Gemma/scripts/ajax/coexpression/GeneCombo.js'></script>

<title><fmt:message key="diffExpressionSearch.title" /></title>


<spring:bind path="diffExpressionSearchCommand.*">
	<c:if test="${not empty status.errorMessages}">
		<div class="error">
			<c:forEach var="error" items="${status.errorMessages}">
				<img src="<c:url value="/images/iconWarning.gif"/>"
					alt="<fmt:message key="icon.warning"/>" class="icon" />
				<c:out value="${error}" escapeXml="false" />
				<br />
			</c:forEach>
		</div>
	</c:if>
</spring:bind>


<form method="post"
	action="<c:url value="/diff/diffExpressionSearch.html"/>">

	<h2>
		<fmt:message key="diffExpressionSearch.title" />
	</h2>

	<hr />

	<table>

		<tr>
			<td valign="top">
				<b> <fmt:message key="gene.query" /> <br /> </b>
			</td>
			<td>
				<spring:bind path="diffExpressionSearchCommand.geneOfficialSymbol">
					<input type="text" size=10 id="geneCombo"
						name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
				</spring:bind>
			</td>
		</tr>

		<script language='javascript'>
	Ext.onReady( function() {
		var geneCombo = new Ext.Gemma.GeneCombo( {
			emptyText : 'search for a gene',
			transform : 'geneCombo'
		} )
	} );
</script>

		<tr>
			<td valign="top">
				<b> <fmt:message key="diff.threshold" /> <br /> </b>
			</td>
			<td>
				<spring:bind path="diffExpressionSearchCommand.threshold">
					<input type="text" size=5
						name="<c:out value="${status.expression}"/>"
						value="<c:out value="${status.value}"/>" />
				</spring:bind>
			</td>
		</tr>

	</table>
	<br />

	<table>
		<tr>
			<td>
				<input type="submit" class="button" name="submit"
					value="<fmt:message key="button.submit"/>" />
			</td>
		</tr>
	</table>


</form>
<%-- TODO
<validate:javascript formName="expressionExperimentForm" staticJavascript="false"/>
<script type="text/javascript"
      src="<c:url value="/scripts/validator.jsp"/>"></script>
--%>
