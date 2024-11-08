<%@ include file="/common/taglibs.jsp" %>
<jsp:useBean id="bioMaterial" scope="request"
        class="ubic.gemma.model.expression.biomaterial.BioMaterial" />

<head>
<title><fmt:message key="bioMaterial.details" /></title>
<jwr:script src='/scripts/app/bmFactorValues.js' />
</head>

<div class="padded">

    <h2>
        ${fn:escapeXml(bioMaterial.name)} from
        <a title="${fn:escapeXml(expressionExperiment.name)}"
                href="${pageContext.request.contextPath}/expressionExperiment/showExpressionExperiment.html?id=${expressionExperiment.id}">${fn:escapeXml(expressionExperiment.shortName)}</a>
    </h2>

    <table style="width: 650px" class="row-separated pad-cols v-padded">

        <tr>
            <td><b><fmt:message key="bioMaterial.description" /></b>
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
            <td>
                <b><fmt:message key="taxon.title" /></b>
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
            <td><b><fmt:message key="databaseEntry.title" /></b></td>

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
            <td>
                <b>Assays used in</b>
            </td>
            <td>
                <ul>
                    <c:forEach items="${bioMaterial.bioAssaysUsedIn}" var="assay">
                        <li><a
                                href="${pageContext.request.contextPath}/bioAssay/showBioAssay.html?id=${assay.id}">${assay.name}</a>
                        </li>
                    </c:forEach>
                </ul>
            </td>
        </tr>

    </table>

    <hr class="normal">

    <h3><fmt:message key="treatments.title" /></h3>
    <table>
        <thead>
        <tr>
            <th>Name</th>
            <th>Description</th>
            <th>Order Applied</th>
        </tr>
        </thead>
        <c:forEach var="treatment" items="${bioMaterial.treatments}">
            <tr>
                <td>${fn:escapeXml(treatment.name)}</td>
                <td>${fn:escapeXml(treatment.description)}</td>
                <td style="text-align: center;">${treatment.orderApplied}</td>
            </tr>
        </c:forEach>
    </table>

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
    <input type="hidden" name="canEdit" id="canEdit" value="true" />

    <br>

    <security:accesscontrollist domainObject="${bioMaterial}"
            hasPermission="WRITE,ADMINISTRATION">
        <td colspan="2">
            <div>
                <input type="button"
                        onclick="location.href='${pageContext.request.contextPath}/bioMaterial/editBioMaterial.html?id=${bioMaterial.id}'"
                        value="Edit">
            </div>
        </td>
    </security:accesscontrollist>

</div>

<script type='text/javascript'>
Ext.namespace( 'Gemma' );
Ext.onReady( function() {
   Ext.QuickTips.init();
   const bmId = Ext.get( "bmId" ).getValue();
   const bmClass = Ext.get( "bmClass" ).getValue();
   const canEdit = Ext.get( 'canEdit' ) === null ? false : Ext.get( 'canEdit' ).getValue();
   const grid = new Gemma.AnnotationGrid( {
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
