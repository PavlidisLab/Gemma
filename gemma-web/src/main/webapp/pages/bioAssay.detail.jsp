<%@ include file="/common/taglibs.jsp" %>

<head>
<title>${fn:escapeXml(bioAssay.name)}</title>
<c:choose>
    <c:when test="${not empty bioAssay.description}">
        <meta name="description" content="${fn:escapeXml(fn:trim(bioAssay.description))}" />
    </c:when>
    <c:when test="${not empty singleParent.description}">
        <meta name="description" content="${fn:escapeXml(fn:trim(singleParent.description))}" />
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
            <td style="max-width: 800px;">
                <c:choose>
                    <c:when test="${not empty bioAssay.description}">
                        <div style="white-space: pre-wrap;">${fn:escapeXml(fn:trim(bioAssay.description))}</div>
                    </c:when>
                    <c:when test="${not empty singleParent.description}">
                        <div style="white-space: pre-wrap;">${fn:escapeXml(fn:trim(singleParent.description))}&nbsp;<b>(inherited)</b>
                        </div>
                    </c:when>
                    <c:otherwise><i>No description available</i></c:otherwise>
                </c:choose>
            </td>
        </tr>

        <c:if test="${not empty bioAssaySets}">
            <tr>
                <td class="label">Experiments used in:</td>
                <td>
                    <c:forEach items="${bioAssaySets}" var="bioAssaySet" varStatus="i">
                        <c:if test="${!i.first}">, </c:if>
                        <Gemma:entityLink entity="${bioAssaySet}" dimension="${dimension}">
                            ${fn:escapeXml(bioAssaySet.name)}
                        </Gemma:entityLink>
                    </c:forEach>
                </td>
            </tr>
        </c:if>

        <tr>
            <td class="label">Sample:</td>
            <td>
                <Gemma:entityLink entity="${bioAssay.sampleUsed}" dimension="${dimension}">
                    ${fn:escapeXml(bioAssay.sampleUsed.name)}
                </Gemma:entityLink>
            </td>
        </tr>
        <tr>
            <td class="label">Platform:</td>
            <td>
                <Gemma:entityLink entity="${bioAssay.arrayDesignUsed}">
                    ${fn:escapeXml(bioAssay.arrayDesignUsed.shortName)}
                </Gemma:entityLink>
                - ${fn:escapeXml(bioAssay.arrayDesignUsed.name)}
            </td>
        </tr>

        <c:choose>
            <c:when test="${bioAssay.originalPlatform != null}">
                <tr>
                    <td class="label">Original Platform:</td>
                    <td>
                        <Gemma:entityLink
                                entity="${bioAssay.originalPlatform}">${bioAssay.originalPlatform.shortName}</Gemma:entityLink>
                        - ${fn:escapeXml(bioAssay.originalPlatform.name)}
                    </td>
                </tr>
            </c:when>
            <c:when test="${singleParent.originalPlatform != null}">
                <tr>
                    <td class="label">Original Platform:</td>
                    <td>
                        <Gemma:entityLink
                                entity="${singleParent.originalPlatform}">${singleParent.originalPlatform.shortName}</Gemma:entityLink>
                        - ${fn:escapeXml(singleParent.originalPlatform.name)}&nbsp;<b>(inherited)</b>
                    </td>
                </tr>
            </c:when>
        </c:choose>

        <c:if test="${bioAssay.numberOfCells != null}">
            <tr>
                <td class="label">Number of cells:</td>
                <td>${bioAssay.numberOfCells}</td>
            </tr>
        </c:if>

        <c:if test="${bioAssay.numberOfDesignElements != null}">
            <tr>
                <td class="label">Number of design elements:</td>
                <td>${bioAssay.numberOfDesignElements}</td>
            </tr>
        </c:if>

        <c:if test="${bioAssay.numberOfCellsByDesignElements != null}">
            <tr>
                <td class="label">Number of cells &times; design elements:</td>
                <td>${bioAssay.numberOfCellsByDesignElements}</td>
            </tr>
        </c:if>

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
                        <Gemma:entityLink entity="${parent}"
                                dimension="${dimension}">${fn:escapeXml(parent.name)}</Gemma:entityLink>
                        (via&nbsp;<Gemma:entityLink entity="${parent.sampleUsed}"
                            dimension="${dimension}">${fn:escapeXml(parent.sampleUsed.name)}</Gemma:entityLink>)
                    </li>
                </c:forEach>
            </ul>
        </c:if>
        <c:if test="${not empty siblings}">
            <h4>Siblings:</h4>
            <ul style="max-height: 300px; overflow-y: scroll;">
                <c:forEach items="${siblings}" var="sibling">
                    <li>
                        <Gemma:entityLink entity="${sibling}"
                                dimension="${dimension}">${fn:escapeXml(sibling.name)}</Gemma:entityLink>
                        (via&nbsp;<Gemma:entityLink entity="${sibling.sampleUsed}"
                            dimension="${dimension}">${fn:escapeXml(sibling.sampleUsed.name)}</Gemma:entityLink>)
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
                        (via&nbsp;<Gemma:entityLink entity="${child.sampleUsed}"
                            dimension="${dimension}">${fn:escapeXml(child.sampleUsed.name)}</Gemma:entityLink>)
                    </li>
                </c:forEach>
            </ul>
        </c:if>
    </c:if>
</div>