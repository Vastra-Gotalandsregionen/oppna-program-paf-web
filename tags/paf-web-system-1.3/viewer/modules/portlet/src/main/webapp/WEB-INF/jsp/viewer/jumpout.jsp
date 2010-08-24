<%--

    Copyright 2010 Västra Götalandsregionen

      This library is free software; you can redistribute it and/or modify
      it under the terms of version 2.1 of the GNU Lesser General Public
      License as published by the Free Software Foundation.

      This library is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU Lesser General Public License for more details.

      You should have received a copy of the GNU Lesser General Public
      License along with this library; if not, write to the
      Free Software Foundation, Inc., 59 Temple Place, Suite 330,
      Boston, MA 02111-1307  USA


--%>

<%--
  Created by IntelliJ IDEA.
  User: david
  Date: May 12, 2010
  Time: 4:46:44 PM
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="portlet" uri="http://java.sun.com/portlet_2_0" %>

<html>
<head>
    <script type="text/javascript">
        <!--
        function redirecter() {
            var accessMode = "${accessMode}";

            if (document.getElementById("jump").value == "open") {
                document.getElementById("jump").value = "";
                if (accessMode == "post") {
                    document.forms["paf"].submit();
                } else { // default get
                    window.location = "${jumpout}";
                }
            } else {
                window.location = document.getElementById("jumpback").value;
            }
        }
        //-->
    </script>
</head>
<body onload="redirecter()">

<portlet:renderURL var="jumpback"/>

<div class="module-content" style="width: 100%">
    <div style="text-align: center;">
        Om inte PafWeb öppnas automatiskt.
        <br/><br/>

        <form id="paf" method="post" action="${url}">
            <input type="hidden" value="${user}" name="USER" /><br />
            <input type="hidden" value="${mode}" name="MODE" /><br />
            <input type="hidden" value="${pid}" name="PID" /><br />
            <a href="javascript: redirecter()">Öppna</a>
            <a href="${jumpback}">Stäng</a>
        </form>
    </div>

    <input type="hidden" id="jump" name="jump" value="${jump}"/>
    <input type="hidden" id="jumpback" name="jumpback" value="${jumpback}"/>
</div>
</body>
</html>
