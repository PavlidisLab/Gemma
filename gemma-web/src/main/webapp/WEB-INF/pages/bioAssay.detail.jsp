<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="bioAssay" scope="request"
	class="ubic.gemma.model.expression.bioAssay.BioAssayImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<title><fmt:message key="bioAssay.details" />
</title>

<h2>
	<fmt:message key="bioAssay.details" />
</h2>
<table width="100%" cellspacing="10">
	<tr>
		<td class="label">
			<b> <fmt:message key="bioAssay.name" /> </b>
		</td>
		<td>
			<%
			    if ( bioAssay.getName() != null ) {
			%>
			<jsp:getProperty name="bioAssay" property="name" />
			<%
			    } else {
			        out.print( "No name available" );
			    }
			%>
		</td>
	</tr>
	<tr>
		<td class="label">
			<fmt:message key="databaseEntry.title" />
		</td>
		<td>
			<Gemma:databaseEntry databaseEntry="${bioAssay.accession}" />
		</td>
	</tr>

	<tr>
		<td class="label">
			<b> <fmt:message key="bioAssay.description" /> </b>
		</td>
		<td>
			<%
			    if ( bioAssay.getDescription() != null ) {
			%>
			<div class="clob"><jsp:getProperty name="bioAssay"
					property="description" /></div>
			<%
			    } else {
			        out.print( "Description unavailable" );
			    }
			%>
		</td>
	</tr>
	<tr>
		<td class="label">
			<b>Batch date</b>
		</td>
		<td>
			<%
			    if ( bioAssay.getProcessingDate() != null ) {
			%>
			${bioAssay.processingDate}
			<%
			    } else {
			        out.print( "Batch date not available" );
			    }
			%>
		</td>
	</tr>
</table>
<h3>
	<fmt:message key="bioMaterials.title" />
</h3>
<display:table name="bioAssay.samplesUsed" class="list" requestURI=""
	id="bioMaterialList" pagesize="10"
	decorator="ubic.gemma.web.taglib.displaytag.expression.bioAssay.BioAssayWrapper">
	<display:column property="name" maxWords="20"
		href="/Gemma/bioMaterial/showBioMaterial.html" paramId="id"
		paramProperty="id" />
	<display:column property="description" maxWords="100" />
</display:table>

<h3>
	<fmt:message key="bioAssay.arrayDesigns" />
</h3>
<display:table name="bioAssay.arrayDesignUsed" class="list"
	requestURI="" id="arrayDesignList" pagesize="10">
	<display:column property="name" maxWords="20" sortable="true"
		href="/Gemma/arrays/showArrayDesign.html" paramId="id"
		paramProperty="id" />
	<display:column property="description" maxWords="100" />
</display:table>

<br />

<br />

<hr />
<hr />

<table>
	<TR>
		<TD COLSPAN="2">
			<DIV align="left">
				<input type="button"
					onclick="location.href='/Gemma/expressionExperiment/showAllExpressionExperiments.html'"
					value="Back">
			</DIV>
		</TD>
		<security:accesscontrollist domainObject="${bioAssay}"
			hasPermission="WRITE,ADMINISTRATION">
			<TD COLSPAN="2">
				<DIV align="left">
					<input type="button"
						onclick="location.href='/Gemma/bioAssay/editBioAssay.html?id=<%=bioAssay.getId()%>'"
						value="Edit">
				</DIV>
			</TD>
		</security:accesscontrollist>
	</TR>
</table>