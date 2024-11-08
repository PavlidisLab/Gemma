<%@ include file="/common/taglibs.jsp" %>

<head>
<title>${fn:escapeXml(bioAssay.name)}</title>
<c:choose>
    <c:when test="${not empty bioAssay.description}">
        <meta name="description" content="${fn:escapeXml(bioAssay.description)}" />
    </c:when>
    <c:when test="${not empty singleParent.description}">
        <meta name="description" content="${fn:escapeXml(singleParent.description)}" />
    </c:when>
</c:choose>
</head>

<div class="padded">
    <h2>${fn:escapeXml(bioAssay.name)}</h2>
    <table>
        <tr>
            <td class="label"><fmt:message key="databaseEntry.title" />:</td>
            <td>
                <c:choose>
                    <c:when test="${bioAssay.accession != null}">
                        <Gemma:databaseEntry databaseEntry="${bioAssay.accession}" />
                    </c:when>
                    <c:when test="${singleParent.accession != null}">
                        <Gemma:databaseEntry databaseEntry="${singleParent.accession}" /> <b>(inherited)</b>
                    </c:when>
                    <c:otherwise>
                        <i>No external identifier available</i>
                    </c:otherwise>
                </c:choose>
            </td>
        </tr>

        <tr>
            <td class="label"><fmt:message key="bioAssay.description" />:</td>
            <td>
                <c:choose>
                    <c:when test="${not empty bioAssay.description}">
                        ${fn:escapeXml(bioAssay.description)}
                    </c:when>
                    <c:when test="${not empty singleParent.description}">
                        ${fn:escapeXml(singleParent.description)} <b>(inherited)</b>
                    </c:when>
                    <c:otherwise><i>No description available</i></c:otherwise>
                </c:choose>
            </td>
        </tr>

        <c:if test="${not empty bioAssaySets}">
            <tr>
                <td class="label">Experiments used in:</td>
                <td>
                    <c:forEach items="${bioAssaySets}" var="bioAssaySet">
                        <Gemma:entityLink entity="${bioAssaySet}">
                            ${fn:escapeXml(bioAssaySet.name)}
                        </Gemma:entityLink>
                    </c:forEach>
                </td>
            </tr>
        </c:if>

        <tr>
            <td class="label">Sample:</td>
            <td>
                <a href="${pageContext.request.contextPath}/bioMaterial/showBioMaterial.html?id=${bioAssay.sampleUsed.id}&dimension=${dimension.id}">
                    ${fn:escapeXml(bioAssay.sampleUsed.name)}
                </a>
            </td>
        </tr>
        <tr>
            <td class="label">Platform:</td>
            <td>
                <Gemma:entityLink entity="${bioAssay.arrayDesignUsed}">
                    ${fn:escapeXml(bioAssay.arrayDesignUsed.shortName)}
                </Gemma:entityLink>
            </td>
        </tr>

        <c:choose>
            <c:when test="${bioAssay.originalPlatform != null}">
                <tr>
                    <td class="label">Original Platform:</td>
                    <td>
                        <Gemma:entityLink
                                entity="${bioAssay.originalPlatform}">${bioAssay.originalPlatform.shortName}</Gemma:entityLink>
                            ${fn:escapeXml(bioAssay.originalPlatform.name)}
                    </td>
                </tr>
            </c:when>
            <c:when test="${singleParent.originalPlatform != null}">
                <tr>
                    <td class="label">Original Platform:</td>
                    <td>
                        <Gemma:entityLink
                                entity="${singleParent.originalPlatform}">${singleParent.originalPlatform.shortName}</Gemma:entityLink>
                            ${fn:escapeXml(singleParent.originalPlatform.name)} <b>(inherited)</b>
                    </td>
                </tr>
            </c:when>
        </c:choose>

    </table>

    <c:if test="${not empty parents || not empty siblings || not empty children}">

        <hr class="normal">

        <h3>Hierarchy for ${fn:escapeXml(dimension.name)}</h3>
        <c:if test="${not empty parents}">
            <%-- Having more than one parent would be very unusual, but the data model allows it --%>
            <h4>${parents.size() > 1 ? 'Parents' : 'Parent'}:</h4>
            <ul style="max-height: 300px; overflow-y: scroll;">
                <c:forEach items="${parents}" var="parent">
                    <li>
                        <a href="${pageContext.request.contextPath}/bioAssay/showBioAssay.html?id=${parent.id}&dimension=${dimension.id}">${parent.name}</a>
                        (via <a
                            href="${pageContext.request.contextPath}/bioMaterial/showBioMaterial.html?id=${parent.sampleUsed.id}&dimension=${dimension.id}">${parent.sampleUsed.name}</a>)
                    </li>
                </c:forEach>
            </ul>
        </c:if>
        <c:if test="${not empty siblings}">
            <h4>Siblings:</h4>
            <ul style="max-height: 300px; overflow-y: scroll;">
                <c:forEach items="${siblings}" var="sibling">
                    <li>
                        <a href="${pageContext.request.contextPath}/bioAssay/showBioAssay.html?id=${sibling.id}&dimension=${dimension.id}">${sibling.name}</a>
                        (via <a
                            href="${pageContext.request.contextPath}/bioMaterial/showBioMaterial.html?id=${sibling.sampleUsed.id}&dimension=${dimension.id}">${sibling.sampleUsed.name}</a>)
                    </li>
                </c:forEach>
            </ul>
        </c:if>
        <c:if test="${not empty children}">
            <h4>Children:</h4>
            <ul style="max-height: 300px; overflow-y: scroll;">
                <c:forEach items="${children}" var="child">
                    <li>
                        <a href="${pageContext.request.contextPath}/bioAssay/showBioAssay.html?id=${child.id}&dimension=${dimension.id}">${child.name}</a>
                        (via <a
                            href="${pageContext.request.contextPath}/bioMaterial/showBioMaterial.html?id=${child.sampleUsed.id}&dimension=${dimension.id}">${child.sampleUsed.name}</a>)
                    </li>
                </c:forEach>
            </ul>
        </c:if>
    </c:if>
</div>