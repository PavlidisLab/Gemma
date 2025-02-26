<%@ include file="/common/taglibs.jsp" %>

<head>
<title>${fn:escapeXml(bioMaterial.name)}</title>
<c:choose>
    <c:when test="${not empty bioMaterial.description}">
        <meta name="description" content="${fn:escapeXml(fn:trim(bioMaterial.description))}" />
    </c:when>
    <c:when test="${not empty bioMaterial.sourceBioMaterial.description}">
        <meta name="description" content="${fn:escapeXml(fn:trim(bioMaterial.sourceBioMaterial.description))}" />
    </c:when>
    <c:otherwise><i>Description not available</i></c:otherwise>
</c:choose>
<Gemma:script src='/scripts/app/bmFactorValues.js' />
</head>

<div class="padded">

    <h2>${fn:escapeXml(bioMaterial.name)}</h2>

    <table>

        <tr>
            <td class="label"><fmt:message key="bioMaterial.description" />:</td>
            <td style="max-width: 800px;">
                <c:choose>
                    <c:when test="${not empty bioMaterial.description}">
                        <div style="white-space: pre-wrap;">${fn:escapeXml(fn:trim(bioMaterial.description))}</div>
                    </c:when>
                    <c:when test="${not empty bioMaterial.sourceBioMaterial.description}">
                        <div style="white-space: pre-wrap;">${fn:escapeXml(fn:trim(bioMaterial.sourceBioMaterial.description))}&nbsp;<b>(inherited)</b></div>
                    </c:when>
                    <c:otherwise><i>Description not available</i></c:otherwise>
                </c:choose>
            </td>
        </tr>

        <tr>
            <td class="label"><fmt:message key="taxon.title" />:</td>
            <td>
                <c:choose>
                    <c:when test="${not empty bioMaterial.sourceTaxon}">
                        <Gemma:entityLink
                                entity="${bioMaterial.sourceTaxon}">${bioMaterial.sourceTaxon.scientificName}</Gemma:entityLink>
                    </c:when>
                    <c:otherwise><i>Taxon not available</i></c:otherwise>
                </c:choose>
            </td>
        </tr>

        <tr>
            <td class="label"><fmt:message key="databaseEntry.title" />:</td>

            <td>
                <c:choose>
                    <c:when test="${not empty bioMaterial.externalAccession}">
                        <c:out value="${bioMaterial.externalAccession.accession}" />
                    </c:when>
                    <c:when test="${not empty bioMaterial.sourceBioMaterial.externalAccession}">
                        <c:out value="${bioMaterial.sourceBioMaterial.externalAccession}" /> <b>(inherited)</b>
                    </c:when>
                    <c:otherwise><i>No external identifier available</i></c:otherwise>
                </c:choose>
            </td>
        </tr>

        <tr>
            <td class="label">Assays used in:</td>
            <td>
                <ul>
                    <c:forEach items="${bioMaterial.bioAssaysUsedIn}" var="assay">
                        <li>
                            <Gemma:entityLink entity="${assay}"
                                    dimension="${dimension}">${fn:escapeXml(assay.name)}</Gemma:entityLink>
                        </li>
                    </c:forEach>
                </ul>
            </td>
        </tr>

        <tr>
            <td class="label">Experiments used in</td>
            <td>
                <c:forEach items="${expressionExperiments.entrySet()}" var="e">
                    <c:forEach items="${e.value.entrySet()}" var="ba2bm">
                        <Gemma:entityLink
                                entity="${ba2bm.value}">${fn:escapeXml(ba2bm.value.shortName)}</Gemma:entityLink>
                        <c:if test="${not (e.key eq bioMaterial)}">
                            (via&nbsp;<Gemma:entityLink entity="${e.key}"
                                dimension="${dimension}">${fn:escapeXml(e.key.name)}</Gemma:entityLink>&nbsp;&rarr;&nbsp;<Gemma:entityLink
                                entity="${ba2bm.key}"
                                dimension="${dimension}">${fn:escapeXml(ba2bm.key.name)}</Gemma:entityLink>)
                        </c:if>
                    </c:forEach>
                </c:forEach>
            </td>
        </tr>

    </table>

    <c:if test="${not empty parent || not empty siblings || not empty children}">

        <hr class="normal">

        <h3>Hierarchy for dimension ${fn:escapeXml(dimension.id)}</h3>
        <ul>
            <li>
                <c:if test="${parent != null}">
                    <h4>Parent:</h4>
                    <Gemma:entityLink entity="${parent}"
                            dimension="${dimension}">${fn:escapeXml(parent.name)}</Gemma:entityLink>
                    <br>
                </c:if>
                <c:if test="${not empty siblings}">
                    <h4>Siblings:</h4>
                    <ul style="max-height: 300px; overflow-y: scroll;">
                        <c:forEach items="${siblings}" var="sibling">
                            <li>
                                <Gemma:entityLink entity="${sibling}"
                                        dimension="${dimension}">${fn:escapeXml(sibling.name)}</Gemma:entityLink>
                            </li>
                        </c:forEach>
                    </ul>
                </c:if>
                <c:if test="${not empty children}">
                    <h4>Children:</h4>
                    <ul style="max-height: 300px; overflow-y: scroll;">
                        <c:forEach items="${children}" var="child">
                            <li>
                                <Gemma:entityLink entity="${child}"
                                        dimension="${dimension}">${fn:escapeXml(child.name)}</Gemma:entityLink>
                            </li>
                        </c:forEach>
                    </ul>
                </c:if>
            </li>
        </ul>
    </c:if>

    <c:if test="${not empty bioMaterial.treatments}">
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
                    <td style="white-space: pre-wrap;">${fn:escapeXml(fn:trim(treatment.description))}</td>
                    <td style="text-align: center;">${treatment.orderApplied}</td>
                </tr>
            </c:forEach>
        </table>
    </c:if>

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

    <security:authorize access="hasAuthority('GROUP_ADMIN') || hasPermission(bioMaterial, 'WRITE,ADMINISTRATION')">
        <td colspan="2">
            <div>
                <input type="button"
                        onclick="location.href='${pageContext.request.contextPath}/bioMaterial/editBioMaterial.html?id=${bioMaterial.id}'"
                        value="Edit">
            </div>
        </td>
    </security:authorize>

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
