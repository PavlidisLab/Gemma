<%@ include file="/common/taglibs.jsp" %>

<head>
<title>Expression data for ${gene != null ? gene.name : designElement.name}
    in ${fn:escapeXml(expressionExperiment.shortName)} - ${fn:escapeXml(expressionExperiment.name)}</title>
<meta name=ubic.gemma.model.expression.bioAssayData.CellTypeAssignment"description"
        content="${fn:escapeXml(expressionExperiment.description)}" />
<meta name="keywords" content="${fn:escapeXml(keywords)}" />
</head>

<div class="padded">
    <h2>Single-cell expression data for
        <c:choose>
            <c:when test="${gene != null}">
                <Gemma:entityLink entity="${gene}">${gene.name}</Gemma:entityLink>
                (<Gemma:entityLink entity="${designElement}">${fn:escapeXml(designElement.name)}</Gemma:entityLink>)
            </c:when>
            <c:otherwise>
                <Gemma:entityLink entity="${designElement}">${fn:escapeXml(designElement.name)}</Gemma:entityLink>
            </c:otherwise>
        </c:choose>
        in
        <c:choose>
            <c:when test="${focusedCharacteristic != null}">
                ${focusedCharacteristic.value} of
                <Gemma:entityLink
                        entity="${expressionExperiment}">${fn:escapeXml(expressionExperiment.shortName)}</Gemma:entityLink>
            </c:when>
            <c:otherwise>
                <Gemma:entityLink
                        entity="${expressionExperiment}">${fn:escapeXml(expressionExperiment.shortName)}</Gemma:entityLink>
            </c:otherwise>

        </c:choose>
    </h2>
    <p>${fn:escapeXml(expressionExperiment.name)}</p>
    <c:choose>
        <c:when test="${cellTypeAssignment != null && focusedCharacteristic == null}">
            <p>Results are grouped by <b>cell type assignment ${fn:escapeXml(cellTypeAssignment.name)}</b>.</p>
        </c:when>
        <c:when test="${cellLevelCharacteristics != null && focusedCharacteristic == null}">
            <p>Results are grouped by <b>cell-level characteristics ${cellLevelCharacteristics.name}</b>.</p>
        </c:when>
        <c:when test="${focusedCharacteristic != null}">
            <p>Only cells from <b>${focusedCharacteristic.value}</b> are displayed.</p>
        </c:when>
        <c:otherwise>
            <p>All cells are shown.</p>
        </c:otherwise>
    </c:choose>
    <div style="overflow-x: scroll" class="mb-3">
        <img src="${pageContext.request.contextPath}/expressionExperiment/visualizeSingleCellDataBoxplot.html?id=${expressionExperiment.id}&quantitationType=${quantitationType.id}&designElement=${designElement.id}${cellTypeAssignment != null ? '&cellTypeAssignment='.concat(cellTypeAssignment.id) : ''}${cellLevelCharacteristics != null ? '&cellLevelCharacteristics='.concat(cellLevelCharacteristics.id) : ''}${focusedCharacteristic != null ? '&focusedCharacteristic='.concat(focusedCharacteristic.id) : ''}&showTitle=false${font != null ? '&font=' + font : ''}"
                height="400"
                alt="Single-cell box for ..."
        />
    </div>
    <ul>
        <c:set var="singleCellDataUrl"
                value="${pageContext.request.contextPath}/expressionExperiment/showSingleCellExpressionData.html?id=${expressionExperiment.id}&quantitationType=${quantitationType.id}&designElement=${designElement.id}" />
        <c:if test="${cellTypeAssignment != null || cellLevelCharacteristics != null}">
            <li>
                <h3>Ungrouped</h3>
                <a href="${singleCellDataUrl}">All Cells</a>
            </li>
        </c:if>
        <c:forEach items="${cellTypeAssignments}" var="cta">
            <li>
                <h3>Grouped by cell type assignment ${cta.name}</h3>
                <c:set var="ctaUrl" value="${singleCellDataUrl}&cellTypeAssignment=${cta.id}" />
                <c:if test="${cellTypeAssignment == null || focusedCharacteristic != null || cellTypeAssignment.id != cta.id}">
                    <a href="${ctaUrl}">
                        All Cells
                    </a>
                </c:if>
                <ul>
                    <c:forEach var="ct" items="${cta.cellTypes}">
                        <c:if test="${focusedCharacteristic == null || focusedCharacteristic.id != ct.id}">
                            <li>
                                <a href="${ctaUrl}&focusedCharacteristic=${ct.id}">${ct.value}</a>
                            </li>
                        </c:if>
                    </c:forEach>
                </ul>
            </li>
        </c:forEach>
        <c:forEach items="${allCellLevelCharacteristics}" var="clc">
            <li>
                <h3>Grouped by cell-level characteristics ${clc.name}</h3>
                <c:set var="clcUrl" value="${singleCellDataUrl}&cellLevelCharacteristics=${clc.id}" />
                <c:if test="${cellLevelCharacteristics == null || focusedCharacteristic != null || cellLevelCharacteristics.id != clc.id}">
                    <a href="${clcUrl}">All Cells</a>
                </c:if>
                <ul>
                    <c:forEach var="ct" items="${clc.characteristics}">
                        <c:if test="${focusedCharacteristic == null || focusedCharacteristic.id != ct.id}">
                            <li>
                                <a href="${clcUrl}&focusedCharacteristic=${ct.id}">${ct.value}</a>
                            </li>
                        </c:if>
                    </c:forEach>
                </ul>
            </li>
        </c:forEach>
    </ul>
</div>