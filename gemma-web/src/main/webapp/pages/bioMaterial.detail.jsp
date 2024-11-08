<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="bioMaterial" scope="request"
	class="ubic.gemma.model.expression.biomaterial.BioMaterial" />

<head>
<title><fmt:message key="bioMaterial.details" /></title>
<Gemma:script src='/scripts/api/ext/data/DwrProxy.js' />
<Gemma:script src='/scripts/app/bmFactorValues.js' />

<script type='text/javascript'>
   Ext.namespace( 'Gemma' );
   Ext.onReady( function() {
      Ext.QuickTips.init();

      var bmId = Ext.get( "bmId" ).getValue();
      var bmClass = Ext.get( "bmClass" ).getValue();
      var canEdit = Ext.get( 'canEdit' ) === null ? false : Ext.get( 'canEdit' ).getValue();
      var grid = new Gemma.AnnotationGrid( {
         renderTo : "bmAnnotations",
         readMethod : BioMaterialController.getAnnotation,
         readParams : [ {
            id : bmId,
            classDelegatingFor : bmClass
         } ],
         writeMethod : AnnotationController.createBioMaterialTag,
         removeMethod : AnnotationController.removeBioMaterialTag,
         entId : bmId,
         editable : canEdit,
         mgedTermKey : "experiment"
      } );
   } );
</script>


</head>

<div class="padded">

	<h2>
		BioMaterial: ${bioMaterial.name} from
		<a title="${expressionExperiment.name}"
			href="${pageContext.request.contextPath}/expressionExperiment/showExpressionExperiment.html?id=${expressionExperiment.id}">${expressionExperiment.shortName}</a>
	</h2>
	
	<table style="width: 650px" class="row-separated pad-cols v-padded">

		<tr>
			<td valign="top">
				<b> <fmt:message key="bioMaterial.description" />
				</b>
			</td>
			<td>
				<c:choose>
					<c:when test="${not empty bioMaterial.description}">
						<c:out value="${bioMaterial.description}" />
					</c:when>
					<c:otherwise>Description not available</c:otherwise>
				</c:choose>
			</td>
		</tr>

		<tr>
			<td valign="top">
				<b> <fmt:message key="taxon.title" />
				</b>
			</td>
			<td>
				<c:choose>
					<c:when test="${not empty bioMaterial.sourceTaxon}">
						<c:out value="${bioMaterial.sourceTaxon.scientificName}" />
					</c:when>
					<c:otherwise>Taxon not available</c:otherwise>
				</c:choose>
			</td>
		</tr>

		<tr>
			<td valign="top">
				<b> <fmt:message key="databaseEntry.title" />
				</b>
			</td>

			<td>
				<c:choose>
					<c:when test="${not empty bioMaterial.externalAccession}">
						<c:out value="${bioMaterial.externalAccession.accession}" />
					</c:when>
					<c:otherwise>No external identifier</c:otherwise>
				</c:choose>
			</td>
		</tr>

		<tr>
			<td valign="top">
				<b>Assays used in</b>
			</td>
			<td>
				<ul>
					<c:forEach items="${bioMaterial.bioAssaysUsedIn}" var="assay">
						<li><a
								href="${pageContext.request.contextPath}/bioAssay/showBioAssay.html?id=${assay.id}">${assay.name}</a></li>
					</c:forEach>
				</ul>
			</td>
		</tr>

	</table>
	
	<hr class="normal">

	<h3>
		<fmt:message key="treatments.title" />
	</h3>
	<display:table name="bioMaterial.treatments" defaultsort="1"
		class="list" requestURI="" id="treatmentList" pagesize="30"
		decorator="ubic.gemma.web.taglib.displaytag.expression.biomaterial.BioMaterialWrapper">
		<display:column sortable="true" property="name" maxWords="20" />
		<display:column sortable="true" property="description" maxWords="100" />
		<display:column sortable="true" property="orderApplied" maxWords="100" />
	</display:table>

	<hr class="normal">

	<h3>
		<fmt:message key="experimentalDesign.factorValues" />
	</h3>
	<div id="bmFactorValues" class="x-grid-mso" style="overflow: hidden; width: 650px;"></div>
		
	<hr class="normal">

	<h3>Annotations</h3>
	<div id="bmAnnotations" class="x-grid-mso" style="overflow: hidden; width: 650px;"></div>
	
	<input type="hidden" name="bmId" id="bmId" value="${bioMaterial.id}" />
	<input type="hidden" name="bmClass" id="bmClass"
		value="${bioMaterial['class'].name}" />
		
	<br>

	<security:accesscontrollist domainObject="${bioMaterial}"
		hasPermission="WRITE,ADMINISTRATION">
		<input type="hidden" name="canEdit" id="canEdit" value="true" />
		<TD COLSPAN="2">
			<DIV align="left">
				<input type="button"
					onclick="location.href='${pageContext.request.contextPath}/bioMaterial/editBioMaterial.html?id=${bioMaterial.id}'"
					value="Edit">
			</DIV>
		</td>
	</security:accesscontrollist>

</div>