<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="bioMaterial" scope="request" class="ubic.gemma.model.expression.biomaterial.BioMaterialImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<head>
	<title><fmt:message key="bioMaterial.details" /></title>
	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/bmFactorValues.js' />


	<script type='text/javascript'>
		Ext.namespace('Gemma');
	Ext.onReady(function() {
		Ext.QuickTips.init();
	
	var bmId = dwr.util.getValue("bmId");
	var bmClass = dwr.util.getValue("bmClass");
	var admin = dwr.util.getValue("hasAdmin");
	var grid = new Gemma.AnnotationGrid( { renderTo : "bmAnnotations",
				readMethod :BioMaterialController.getAnnotation,
				readParams : [{
					id : bmId,
					classDelegatingFor : bmClass
				}],
				writeMethod : OntologyService.saveBioMaterialStatement,
				removeMethod : OntologyService.removeBioMaterialStatement,
				entId : bmId,
				
				editable : admin,
				mgedTermKey : "experiment"
			});
}); 
	  </script>


</head>

<security:authorize ifAnyGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</security:authorize>
<security:authorize ifNotGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</security:authorize>

<h2>
	<fmt:message key="bioMaterial.details" />
</h2>
<table width="50%" cellspacing="5">
	<tr>
		<td valign="top">
			<b> <fmt:message key="bioMaterial.name" /> </b>
		</td>
		<td>
			<%
			if ( bioMaterial.getName() != null ) {
			%>
			<jsp:getProperty name="bioMaterial" property="name" />
			<%
			                } else {
			                out.print( "No name available" );
			            }
			%>
		</td>
	</tr>

	<tr>
		<td valign="top">
			<b> <fmt:message key="bioMaterial.description" /> </b>
		</td>
		<td>
			<%
			if ( bioMaterial.getDescription() != null ) {
			%>
			<jsp:getProperty name="bioMaterial" property="description" />
			<%
			                } else {
			                out.print( "Description unavailable" );
			            }
			%>
		</td>
	</tr>

	<tr>
		<td valign="top">
			<b> <fmt:message key="taxon.title" /> </b>
		</td>
		<td>
			<%
			                if ( bioMaterial.getSourceTaxon() != null ) {
			                out.print( bioMaterial.getSourceTaxon().getScientificName() );
			            } else {
			                out.print( "Taxon unavailable" );
			            }
			%>
		</td>
	</tr>

	<tr>
		<td valign="top">
			<b> <fmt:message key="databaseEntry.title" /> </b>
		</td>
		<td>
			<%
			                if ( bioMaterial.getExternalAccession() != null ) {
			                out.print( bioMaterial.getExternalAccession().getAccession() + "."
			                        + bioMaterial.getExternalAccession().getAccessionVersion() );
			            } else {
			                out.print( "No accession" );
			            }
			%>
		</td>
	</tr>

</table>

<h3>
	<fmt:message key="treatments.title" />
</h3>
<display:table name="bioMaterial.treatments" defaultsort="1" class="list" requestURI="" id="treatmentList" pagesize="30"
	decorator="ubic.gemma.web.taglib.displaytag.expression.biomaterial.BioMaterialWrapper">
	<display:column sortable="true" property="name" maxWords="20" />
	<display:column sortable="true" property="description" maxWords="100" />
	<display:column sortable="true" property="orderApplied" maxWords="100" />
</display:table>


<h3>
	<fmt:message key="experimentalDesign.factorValues" />
</h3>

<div id="bmFactorValues" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width: 650px;"></div>

<h3>
	Annotations
</h3>

<div id="bmAnnotations"></div>
<input type="hidden" name="bmId" id="bmId" value="${bioMaterial.id}" />
<input type="hidden" name="bmClass" id="bmClass" value="${bioMaterial.class.name}" />



<table>
	<tr>
		<td COLSPAN="2">
			<DIV align="left">
				<input type="button" onclick="location.href='/Gemma/expressionExperiment/showAllExpressionExperiments.html'" value="Back">
			</DIV>
		</TD>
		<security:acl domainObject="${bioMaterial}" hasPermission="1,6">
			<TD COLSPAN="2">
				<DIV align="left">
					<input type="button" onclick="location.href='/Gemma/bioMaterial/editBioMaterial.html?id=<%=bioMaterial.getId()%>'"
						value="Edit">
				</DIV>
			</td>
		</security:acl>
	</tr>
</table>
