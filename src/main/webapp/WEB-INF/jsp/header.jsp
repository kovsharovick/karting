<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<header class="bg-white shadow-sm py-3 px-4 flex justify-between items-center sticky top-0 z-10">
    <div class="flex items-center gap-4">
        <a href="${pageContext.request.contextPath}/slots" class="text-xl font-bold text-blue-600 hover:underline">
            Апекс
        </a>
    </div>

    <div class="flex items-center gap-4">
        <!-- Всегда показываем "Мои брони", если пользователь авторизован -->
        <c:if test="${not empty sessionScope.clientId}">
            <a href="${pageContext.request.contextPath}/bookings" class="text-sm text-gray-700 hover:text-blue-600">
                Мои брони
            </a>
        </c:if>

        <span class="text-sm text-gray-600">
            ${sessionScope.client != null ? (sessionScope.client.name != null ? sessionScope.client.name : sessionScope.client.phone) : ''}
        </span>

        <c:if test="${not empty sessionScope.clientId}">
            <form action="${pageContext.request.contextPath}/logout" method="post" class="inline">
                <button type="submit" class="text-sm text-gray-500 hover:text-gray-700">
                    Выйти
                </button>
            </form>
        </c:if>
    </div>
</header>