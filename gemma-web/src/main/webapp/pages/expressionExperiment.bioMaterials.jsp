<%@ include file="/common/taglibs.jsp" %>

<head>
<title>Biomaterials from ${fn:escapeXml(expressionExperiment.shortName)} - ${fn:escapeXml(expressionExperiment.name)}</title>
<meta name="description" content="${fn:escapeXml(fn:trim(expressionExperiment.description))}" />
<meta name="keywords" content="${fn:escapeXml(keywords)}" />
</head>

<div class="padded">
    <h2>
        Biomaterials from
        <Gemma:entityLink
                entity="${expressionExperiment}">${fn:escapeXml(expressionExperiment.shortName)}</Gemma:entityLink>
        - ${fn:escapeXml(expressionExperiment.name)}
    </h2>

    <p>
        <a href="${pageContext.request.contextPath}/experimentalDesign/showExperimentalDesign.html?eeid=${expressionExperiment.id}">
            View the experimental design
        </a>
    </p>

    <table>
        <tr>
            <th>Name</th>
            <th>Description</th>
        </tr>
        <c:forEach var="bioMaterial" items="${bioMaterials}">
            <tr>
                <td>
                    <Gemma:entityLink entity="${bioMaterial}">
                        ${fn:escapeXml(bioMaterial.name)}
                    </Gemma:entityLink>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${not empty bioMaterial.description}">
                            <div style="white-space: pre-wrap;">${fn:escapeXml(fn:trim(bioMaterial.description))}</div>
                        </c:when>
                        <c:otherwise>
                            <i>No description available</i>
                        </c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:forEach>
    </table>
</div>