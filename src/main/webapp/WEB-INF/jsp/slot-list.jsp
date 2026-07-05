<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Заезды — Апекс</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        .slot-card { transition: transform 0.1s; }
        .slot-card:hover { transform: scale(1.01); }
    </style>
</head>
<body class="bg-gray-50 min-h-screen flex flex-col">
    <header class="bg-white shadow-sm py-3 px-4 flex justify-between items-center sticky top-0 z-10">
        <h1 class="text-xl font-bold text-blue-600">Апекс</h1>
        <div class="flex items-center gap-3">
            <span class="text-sm text-gray-600">
                ${sessionScope.client.name != null ? sessionScope.client.name : sessionScope.client.phone}
            </span>
            <form action="${pageContext.request.contextPath}/logout" method="post" class="inline">
                <button type="submit" class="text-sm text-gray-500 hover:text-gray-700">Выйти</button>
            </form>
        </div>
    </header>

    <main class="flex-1 container mx-auto px-4 py-6 max-w-3xl w-full">
        <div class="flex justify-between items-center mb-4">
            <h2 class="text-xl font-semibold">Заезды</h2>
            <button onclick="toggleFilters()" class="px-3 py-1 border rounded hover:bg-gray-100 text-sm">
                Фильтры
            </button>
        </div>

        <!-- Блок фильтров -->
        <div id="filtersPanel" class="hidden bg-white p-4 rounded-lg shadow mb-4">
            <form action="${pageContext.request.contextPath}/slots" method="get" class="space-y-3">
                <div class="grid grid-cols-2 gap-3">
                    <div>
                        <label class="block text-sm font-medium">С</label>
                        <input type="date" name="dateFrom" value="${filterDateFrom != null ? filterDateFrom.toLocalDate() : ''}" class="w-full border rounded px-2 py-1">
                    </div>
                    <div>
                        <label class="block text-sm font-medium">По</label>
                        <input type="date" name="dateTo" value="${filterDateTo != null ? filterDateTo.toLocalDate() : ''}" class="w-full border rounded px-2 py-1">
                    </div>
                </div>
                <div>
                    <label class="block text-sm font-medium">Трасса</label>
                    <div class="flex gap-2">
                        <label><input type="checkbox" name="trackConfig" value="novice" ${fn:contains(filterTrackConfig, 'novice') ? 'checked' : ''}> Короткая</label>
                        <label><input type="checkbox" name="trackConfig" value="experienced" ${fn:contains(filterTrackConfig, 'experienced') ? 'checked' : ''}> Длинная</label>
                    </div>
                </div>
                <div>
                    <label class="flex items-center gap-2">
                        <input type="checkbox" name="onlyAvailable" value="true" ${filterOnlyAvailable ? 'checked' : ''}>
                        Только со свободными местами
                    </label>
                </div>
                <button type="submit" class="bg-blue-600 text-white px-4 py-1 rounded">Применить</button>
                <a href="${pageContext.request.contextPath}/slots" class="ml-2 text-sm text-gray-500">Сбросить</a>
            </form>
        </div>

        <!-- Список слотов -->
        <c:choose>
            <c:when test="${not empty slots}">
                <div class="space-y-3">
                    <c:forEach var="slot" items="${slots}">
                        <div class="slot-card bg-white rounded-lg shadow p-4 cursor-pointer hover:shadow-md transition"
                             onclick="location.href = '${pageContext.request.contextPath}/slots/${slot.id}'">
                            <div class="flex justify-between items-start">
                                <div>
                                    <p class="font-medium">
                                        <fmt:formatDate value="${slot.startAt.toGregorianCalendar().time}" pattern="dd MMM, HH:mm"/>
                                    </p>
                                    <p class="text-sm text-gray-600">${slot.trackConfig.name}</p>
                                    <p class="text-sm">${slot.marshal.name} ★ <fmt:formatNumber value="${slot.marshal.ratingAvg}" pattern="#0.0"/></p>
                                </div>
                                <div class="text-right">
                                    <p class="font-bold text-lg">${slot.priceKart} ₽</p>
                                    <p class="text-sm ${slot.freeKarts > 0 ? 'text-green-600' : 'text-red-500'}">
                                        ${slot.freeKarts > 0 ? 'Свободно ' .concat(slot.freeKarts).concat(' из ').concat(slot.totalKarts) : 'Мест нет'}
                                    </p>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-10 text-gray-500">
                    <p>Пока нет доступных заездов</p>
                </div>
            </c:otherwise>
        </c:choose>
    </main>

    <script>
        function toggleFilters() {
            document.getElementById('filtersPanel').classList.toggle('hidden');
        }
    </script>
</body>
</html>