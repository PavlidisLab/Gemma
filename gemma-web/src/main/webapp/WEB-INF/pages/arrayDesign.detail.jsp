<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl"%>
<jsp:useBean id="arrayDesign" scope="request" class="ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl" />

<!DOCTYPE html PUBLIC "-//W3C//Dtd html 4.01 transitional//EN">
<html>

	<head><title>Array Design Details</title><content tag="heading">Array Design Details</content></head>
	<body>
<!--  Summary of array design associations -->
	<table class='datasummary'>
<tr>
<td colspan=2>
<b>Array Design Summary</b>
</td>
</tr>
<tr>
<td>
	Sequence associations
</td>
<td>
	<c:out value="${numBioSequences}" />
</td> 
</tr>
<tr>
<td>
	BLAT associations
</td>
<td>
	<c:out value="${numBlatResults}" />
</td>
</tr>
<tr>
<td>
	Genes associations
</td>
<td>
	<c:out value="${numGenes}" />
</td>
</tr>
</table>

		 
		<table>
			<tr>
				<td class="label">
					Name
				</td>
				<td>
					<jsp:getProperty name="arrayDesign" property="name" />
				</td>
			</tr>
			<tr>
				<td class="label">
					Provider
				</td>
				<td>
					<%
					if ( arrayDesign.getDesignProvider() != null ) {
					%>
					<%=arrayDesign.getDesignProvider().getName()%>
					<%
					} else {
					%>
					(Not listed)
					<%
					}
					%>
				</td>
			</tr>
			<tr>
				<td class="label">
					Number Of Features (according to provider)
				</td>
				<td>
					<jsp:getProperty name="arrayDesign" property="advertisedNumberOfDesignElements" />
				</td>
			</tr>
			<tr>
				<td class="label">
					Description
				</td>
				<td>
					<%
					if ( arrayDesign.getDescription() != null && arrayDesign.getDescription().length() > 0 ) {
					%>
					<textarea name="" rows="5" cols="60" readonly=true><jsp:getProperty name="arrayDesign" property="description" /></textarea>
					<%
					} else {
					%>
					(None provided)
					<%
					}
					%>
				</td>
			</tr>
</table>
			<%-- FIXME - show some of the design elements --%>	
			<table>
			<tr>
				<td colspan="2">
					<hr />
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<div align="left">
						<input type="button" onclick="location.href='showAllArrayDesigns.html'" value="Back">
					</div>
				</td>
				<authz:acl domainObject="${arrayDesign}" hasPermission="1,6">
					<td COLSPAN="2">
						<div align="left">
							<input type="button"
								onclick="location.href='/Gemma/arrayDesign/editArrayDesign.html?id=<%=request.getAttribute( "id" )%>'"
								value="Edit">
						</div>
					</td>
				</authz:acl>
			</tr>
		</table>
	</body>
</html>
