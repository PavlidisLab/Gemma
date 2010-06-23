<%@ include file="/common/taglibs.jsp"%>
<head>
	<title>Samples</title>
</head>
<!--  Summary of bioMaterials -->
<h1>
	Samples
</h1>


<h3>
	Displaying
	<c:out value="${numBioMaterials}" />
	Samples
</h3>


<display:table name="bioMaterials" sort="list" class="list" requestURI="" id="bioMaterialList" pagesize="50"
	decorator="ubic.gemma.web.taglib.displaytag.expression.biomaterial.BioMaterialWrapper">
	<display:column property="nameLink" sortable="true" title="Name" />
	<display:column property="sourceTaxon.commonName" sortable="true" title="Taxon" />
	<display:column property="description" sortable="true" maxLength="60" titleKey="description" />

	<display:column property="factorList" sortable="true" maxLength="60" title="Factors" />
	<display:setProperty name="basic.empty.showtable" value="true" />

</display:table>






