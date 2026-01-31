<%@ include file="/common/taglibs.jsp" %>
<head>
<title>Taxa</title>
</head>

<div class="padded">
    <h1>Taxa</h1>
    <ul>
        <c:forEach var="taxon" items="${taxa}">
            <li>
                <Gemma:entityLink entity="${taxon}">${taxon.commonName}</Gemma:entityLink>
            </li>
        </c:forEach>
    </ul>
</div>