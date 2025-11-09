<%@ include file="/WEB-INF/common/taglibs.jsp" %>
<head>
<title>Taxa</title>
</head>

<div class="padded">
    <h2>Taxa</h2>
    <ul>
        <c:forEach var="taxon" items="${taxa}">
            <li>
                <Gemma:entityLink entity="${taxon}">${taxon.commonName}</Gemma:entityLink>
            </li>
        </c:forEach>
    </ul>
</div>