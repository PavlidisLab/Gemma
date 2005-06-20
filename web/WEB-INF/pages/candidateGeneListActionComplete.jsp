<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.Map" %>
<%-- 
The purpose of this page is to prevent reloads from re-running a completed action.
--%>

<c:if test="${applicationScope.secureLogin == 'true'}">
    <Gemma:secure/>
</c:if>
<%
Map m = (Map)request.getAttribute("model");
if( m==null){
	response.sendRedirect("candidateGeneList.htm");
}
else{
	String target = (String) m.get("target");
	String listID = request.getParameter("listID");
	String geneID = request.getParameter("geneID");
	if( target == null ){
		response.sendRedirect("candidateGeneList.htm");
	}
	else{
		if(target.compareTo("candidateGeneListDetail")==0){
			response.sendRedirect("candidateGeneListDetail.htm?listID=" + listID);
		}
		else if( target.compareTo("geneDetail")==0){
			response.sendRedirect("geneDetail.htm?geneID=" + geneID );
		}
		else{
			response.sendRedirect("candidateGeneList.htm");
		}
	}
}
%>