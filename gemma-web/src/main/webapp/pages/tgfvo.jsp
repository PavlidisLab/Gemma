<%@ include file="/common/taglibs.jsp" %>

<title>The Gemma Factor Value Ontology</title>

<div class="padded">
    <h1>The Gemma Factor Value Ontology</h1>
    <p><a href="${pageContext.request.contextPath}/ont/TGFVO.OWL?download=true">Download TGFVO</a></p>
    <p>The Gemma Factor Value Ontology provides a structured view of all the annotations used in Gemma.</p>
    <p style="margin-bottom: 0;">Retrieve TGFVO in RDF/XML:</p>
    <pre class="mb-3">curl -H Accept:application/rdf+xml --compressed ${fn:escapeXml(hostUrl)}/ont/TGFVO.OWL</pre>
    <p class="flex justify-space-around">
        <c:choose>
            <c:when test="${offset > 0}">
                <a href="?offset=0&limit=${limit}">First Page</a>
            </c:when>
            <c:otherwise>
                <span>First Page</span>
            </c:otherwise>
        </c:choose>
        <c:choose>
            <c:when test="${offset > 0}">
                <a href="?offset=${offset - (offset % limit)}&limit=${limit}">Previous Page</a>
            </c:when>
            <c:otherwise>
                <span>Previous Page</span>
            </c:otherwise>
        </c:choose>
        <c:choose>
            <c:when test="${offset + limit < count}">
                <a href="?offset=${offset - (offset % limit)}&limit=${limit}">Next Page</a>
            </c:when>
            <c:otherwise>
                <span>Next Page</span>
            </c:otherwise>
        </c:choose>
        <c:choose>
            <c:when test="${offset + limit < count}">
                <a href="?offset=${count - (count % limit)}&limit=${limit}">Last Page</a>
            </c:when>
            <c:otherwise>
                <span>Last Page</span>
            </c:otherwise>
        </c:choose>
    </p>
    <ul class="mb-3">
        <c:forEach var="oi" items="${individuals}">
            <li><Gemma:ontologyResource resource="${oi}" /></li>
        </c:forEach>
    </ul>
    <div class="mb-3">
        <p style="margin-bottom: 0;">Retrieve this page in RDF/XML:</p>
        <pre>curl -H Accept:application/rdf+xml ${fn:escapeXml(hostUrl)}/ont/TGFVO?offset=${offset}&limit=${limit}</pre>
    </div>
    <p style="margin-bottom: 0"> Other ontologies used by Gemma:</p>
    <ul>
        <li><a href="${pageContext.request.contextPath}/ont/TGEMO">Temporary Gemma Ontology</a></li>
    </ul>
</div>

