<%@ include file="/common/taglibs.jsp" %>

<head>
<title>Biomaterials for ${fn:escapeXml(expressionExperiment.shortName)}</title>
<meta name="description" content="${fn:escapeXml(expressionExperiment.description)}" />
<meta name="keywords" content="${fn:escapeXml(keywords)}" />
</head>

<div class="padded">
    <h2>
        Biomaterials for
        <Gemma:entityLink
                entity="${expressionExperiment}">${fn:escapeXml(expressionExperiment.shortName)}</Gemma:entityLink>
    </h2>
    <p>${fn:escapeXml(expressionExperiment.name)}</p>

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
                    <a href="${pageContext.request.contextPath}/bioMaterial/showBioMaterial.html?id=${bioMaterial.id}">${bioMaterial.name}</a>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${not empty bioMaterial.description}">
                            ${fn:escapeXml(bioMaterial.description)}
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