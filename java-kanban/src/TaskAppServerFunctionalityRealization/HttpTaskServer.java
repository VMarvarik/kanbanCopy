package TaskAppServerFunctionalityRealization;

import TaskAppClasses.Epic;
import TaskAppEnums.Endpoint;
import TaskAppClasses.Subtask;
import TaskAppClasses.Task;
import TaskAppEnums.Status;
import TaskAppManagers.InMemoryTaskManager;
import TaskAppServerFunctionalityRealization.CustomJson.EpicSerializer;
import TaskAppServerFunctionalityRealization.CustomJson.SubtaskSerializer;
import TaskAppServerFunctionalityRealization.CustomJson.TaskSerializer;
import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class HttpTaskServer {
    private final HttpServer server;
    private static final int PORT = 6080;
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private final Gson gson;
    private final InMemoryTaskManager manager;

    public HttpTaskServer(InMemoryTaskManager manager) throws IOException {
        this.manager = manager;

        server = HttpServer.create();

        server.bind(new InetSocketAddress(PORT), 0);

        server.createContext("/tasks/task/", new taskManagerHandler());

        server.createContext("/tasks", new taskManagerHandler());
        server.createContext("/tasks/task", new taskManagerHandler());
        server.createContext("/tasks/subtask/", new taskManagerHandler());
        gson = new GsonBuilder().
                registerTypeAdapter(Task.class, new TaskSerializer()).
                registerTypeAdapter(Epic.class, new EpicSerializer()).
                registerTypeAdapter(Subtask.class, new SubtaskSerializer()).
                create();
    }

    public void start() {
        server.start();
        System.out.println("HTTP-сервер запущен на " + PORT + " порту!");
    }

    public void stop() {
        server.stop(0);
        System.out.println("HTTP-сервер остановлен на " + PORT + " порту!");
    }

    class taskManagerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Endpoint endpoint = getEndpoint(exchange.getRequestURI().getPath(), exchange.getRequestMethod(), exchange.getRequestURI().getQuery());
            switch (endpoint) {
                case GET_TASK -> handleGetTask(exchange);
                case GET_EPIC -> handleGetEpic(exchange);
                case GET_SUBTASK -> handleGetSubtask(exchange);
                case GET_TASKS -> handleGetTasks(exchange);
                case GET_EPICS -> handleGetEpics(exchange);
                case GET_SUBTASKS -> handleGetSubtasks(exchange);
                case GET_EPIC_SUBTASKS -> handleGetEpicSubtasks(exchange);
                case GET_HISTORY -> handleGetHistory(exchange);
                case GET_PRIORITIZED -> handleGetPrioritized(exchange);
                case POST_UPDATE_TASK -> handlePostOrUpdateTask(exchange);
                case POST_UPDATE_EPIC -> handlePostOrUpdateEpic(exchange);
                case POST_UPDATE_SUBTASK -> handlePostOrUpdateSubtask(exchange);
                case DELETE_TASK -> handleDeleteTask(exchange);
                case DELETE_EPIC -> handleDeleteEpic(exchange);
                case DELETE_SUBTASK -> handleDeleteSubtask(exchange);
                case DELETE_TASKS -> handleDeleteTasks(exchange);
                case DELETE_EPICS -> handleDeleteEpics(exchange);
                case DELETE_SUBTASKS -> handleDeleteSubtasks(exchange);
                case UNKNOWN -> writeResponse(exchange, "Такого эндпоинта не существует", 404);
            }
        }

        private Endpoint getEndpoint(String requestPath, String requestMethod, String query) {
            String[] pathParts = requestPath.split("/");
            switch (requestMethod) {
                case "GET":
                    switch (pathParts[2]) {
                        case "task":
                            if (pathParts.length == 3 && query == null) {
                                return Endpoint.GET_TASKS;
                            }
                            return Endpoint.GET_TASK;
                        case "epic":
                            if (pathParts.length == 3 && query == null) {
                                return Endpoint.GET_EPICS;
                            }
                            return Endpoint.GET_EPIC;
                        case "subtask":
                            if (pathParts.length == 3 && query == null) {
                                return Endpoint.GET_SUBTASKS;
                            } else if (pathParts.length == 3) {
                                return Endpoint.GET_SUBTASK;
                            } else if (pathParts[3].equals("epic")) {
                                return Endpoint.GET_EPIC_SUBTASKS;
                            }
                        case "history":
                            return Endpoint.GET_HISTORY;
                        case "prioritized":
                            return Endpoint.GET_PRIORITIZED;
                    }
                case "POST":
                    switch (pathParts[2]) {
                        case "task" -> {
                            return Endpoint.POST_UPDATE_TASK;
                        }
                        case "epic" -> {
                            return Endpoint.POST_UPDATE_EPIC;
                        }
                        case "subtask" -> {
                            return Endpoint.POST_UPDATE_SUBTASK;
                        }
                    }
                case "DELETE":
                    switch (pathParts[2]) {
                        case "task" -> {
                            if (pathParts.length == 3 && query == null) {
                                return Endpoint.DELETE_TASKS;
                            }
                            return Endpoint.DELETE_TASK;
                        }
                        case "epic" -> {
                            if (pathParts.length == 3 && query == null) {
                                return Endpoint.DELETE_EPICS;
                            }
                            return Endpoint.DELETE_EPIC;
                        }
                        case "subtask" -> {
                            if (pathParts.length == 3 && query == null) {
                                return Endpoint.DELETE_SUBTASKS;
                            }
                            return Endpoint.DELETE_SUBTASK;
                        }
                    }
            }
            return Endpoint.UNKNOWN;
        }

        private void writeResponse(HttpExchange exchange, String responseString, int responseCode) throws IOException {
            if (responseString.isBlank()) {
                exchange.sendResponseHeaders(responseCode, 0);
            } else {
                byte[] bytes = responseString.getBytes(DEFAULT_CHARSET);
                exchange.sendResponseHeaders(responseCode, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
            exchange.close();
        }

        private Optional<Integer> getId(HttpExchange exchange) {
            String query = exchange.getRequestURI().getQuery();
            String id = query.substring(3);
            try {
                return Optional.of(Integer.parseInt(id));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }

        private void handleGetTask(HttpExchange exchange) throws IOException {
            Optional<Integer> optionalInteger = getId(exchange);
            if (optionalInteger.isEmpty()) {
                writeResponse(exchange, "Некорректный идентификатор задачи", 400);
                return;
            }
            int id = optionalInteger.get();
            if (manager.getTaskByID(id) != null) {
                writeResponse(exchange, gson.toJson(manager.getTaskByID(id)), 200);
                return;
            }
            writeResponse(exchange, "Задача с идентификатором " + id + " не найдена", 404);
        }

        private void handleGetEpic(HttpExchange exchange) throws IOException {
            Optional<Integer> optionalInteger = getId(exchange);
            if (optionalInteger.isEmpty()) {
                writeResponse(exchange, "Некорректный идентификатор эпика", 400);
                return;
            }
            int id = optionalInteger.get();
            if (manager.getEpicByID(id) != null) {
                writeResponse(exchange, gson.toJson(manager.getEpicByID(id)), 200);
                return;
            }
            writeResponse(exchange, "Эпик с идентификатором " + id + " не найден", 404);
        }

        private void handleGetSubtask(HttpExchange exchange) throws IOException {
            Optional<Integer> optionalInteger = getId(exchange);
            if (optionalInteger.isEmpty()) {
                writeResponse(exchange, "Некорректный идентификатор подзадачи", 400);
                return;
            }
            int id = optionalInteger.get();
            if (manager.getSubtaskByID(id) != null) {
                writeResponse(exchange, gson.toJson(manager.getSubtaskByID(id)), 200);
                return;
            }
            writeResponse(exchange, "Подзадача с идентификатором " + id + " не найдена", 404);
        }

        private void handleGetTasks(HttpExchange exchange) throws IOException {
            if (!manager.getTaskHashMap().isEmpty()) {
                writeResponse(exchange, gson.toJson(manager.getAllTasks().toString()), 200);
                return;
            }
            writeResponse(exchange, "Задачи отсутствуют.", 404);
        }

        private void handleGetEpics(HttpExchange exchange) throws IOException {
            if (!manager.getEpicHashMap().isEmpty()) {
                writeResponse(exchange, gson.toJson(manager.getAllEpics().toString()), 200);
                return;
            }
            writeResponse(exchange, "Эпики отсутствуют.", 404);
        }

        private void handleGetSubtasks(HttpExchange exchange) throws IOException {
            if (!manager.getSubtaskHashMap().isEmpty()) {
                writeResponse(exchange, gson.toJson(manager.getAllSubtasks().toString()), 200);
                return;
            }
            writeResponse(exchange, "Подзадачи отсутствуют.", 404);
        }

        private void handleGetEpicSubtasks(HttpExchange exchange) throws IOException {
            Optional<Integer> optionalInteger = getId(exchange);
            if (optionalInteger.isEmpty()) {
                writeResponse(exchange, "Некорректный идентификатор эпика", 400);
                return;
            }
            int id = optionalInteger.get();
            if (manager.getEpicByID(id) != null) {
                if (manager.getEpicSubtasks(id) != null) {
                    writeResponse(exchange, gson.toJson(manager.getEpicSubtasks(id).toString()), 200);
                    return;
                }
                writeResponse(exchange, "У эпика с идентификатором " + id + " нет подзадач.", 404);
                return;
            }
            writeResponse(exchange, "Эпик с идентификатором " + id + " не найден, нельзя вывести подзадачи", 404);
        }

        private void handleGetHistory(HttpExchange exchange) throws IOException {
            if (manager.getHistory() != null) {
                writeResponse(exchange, gson.toJson(manager.getHistory().toString()), 200);
                return;
            }
            writeResponse(exchange, "Истории нет", 404);
        }

        private void handleGetPrioritized(HttpExchange exchange) throws IOException {
            if (manager.getPrioritizedTasks() != null) {
                writeResponse(exchange, gson.toJson(manager.getPrioritizedTasks().toString()), 200);
                return;
            }
            writeResponse(exchange, "Нет задач, чтобы вывести по приоритету", 404);
        }

        private void handlePostOrUpdateTask(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), DEFAULT_CHARSET);
            try {
                JsonElement jsonElement = JsonParser.parseString(body);
                Task newTask = deserializeTask(jsonElement);
                if (newTask.getId() != 0 && manager.getTaskByID(newTask.getId()) != null) {
                    manager.updateTask(newTask);
                    writeResponse(exchange, "Задача " + newTask.getId() + " обновлена", 201);
                    return;
                } else if (newTask.getId() == 0){
                    manager.saveTask(newTask);
                    writeResponse(exchange, "Задача " + newTask.getId() + " сохранена", 201);
                    return;
                }
                writeResponse(exchange,
                        "Задача не была добавлена, так как ее время пересекается с другой задачей",
                        400);
            } catch (JsonSyntaxException ex) {
                writeResponse(exchange, "Получен некорректный JSON", 400);
            }
        }

        private void handlePostOrUpdateEpic(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), DEFAULT_CHARSET);
            try {
                JsonElement jsonElement = JsonParser.parseString(body);
                Epic newEpic = deserializeEpic(jsonElement);
                if (newEpic.getId() != 0 && manager.getEpicByID(newEpic.getId()) != null) {
                    manager.updateEpic(newEpic);
                    writeResponse(exchange, "Эпик " + newEpic.getId() + " обновлен", 201);
                    return;
                } else if (newEpic.getId() == 0){
                    manager.saveEpic(newEpic);
                    writeResponse(exchange, "Эпик " + newEpic.getId() + " сохранен", 201);
                    return;
                }
                writeResponse(exchange,
                        "Эпик не был добавлен.",
                        400);
            } catch (JsonSyntaxException ex) {
                writeResponse(exchange, "Получен некорректный JSON", 400);
            }
        }

        private void handlePostOrUpdateSubtask(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), DEFAULT_CHARSET);
            try {
                JsonElement jsonElement = JsonParser.parseString(body);
                Subtask newSubtask = deserializeSubtask(jsonElement);
                if (newSubtask.getId() != 0 && manager.getSubtaskByID(newSubtask.getId()) != null) {
                    manager.updateSubtask(newSubtask);
                    writeResponse(exchange, "Подзадача " + newSubtask.getId() + " обновлена", 201);
                    return;
                } else if (newSubtask.getId() == 0) {
                    int id = manager.saveSubtask(newSubtask);
                    writeResponse(exchange, "Подзадача " + id + " сохранена", 201);
                    return;
                }
                writeResponse(exchange,
                        "Подзадача не была добавлена, так как ее время пересекается с другой задачей",
                        400);
            } catch (JsonSyntaxException ex) {
                writeResponse(exchange, "Получен некорректный JSON", 400);
            }
        }

        private void handleDeleteTask(HttpExchange exchange) throws IOException {
            Optional<Integer> optionalInteger = getId(exchange);
            if (optionalInteger.isEmpty()) {
                writeResponse(exchange, "Некорректный идентификатор задачи", 400);
                return;
            }
            int id = optionalInteger.get();
            writeResponse(exchange, gson.toJson(manager.removeTaskByID(id)), 200);
        }

        private void handleDeleteEpic(HttpExchange exchange) throws IOException {
            Optional<Integer> optionalInteger = getId(exchange);
            if (optionalInteger.isEmpty()) {
                writeResponse(exchange, "Некорректный идентификатор задачи", 400);
                return;
            }
            int id = optionalInteger.get();
            writeResponse(exchange, gson.toJson(manager.removeEpicByID(id)), 200);
        }

        private void handleDeleteSubtask(HttpExchange exchange) throws IOException {
            Optional<Integer> optionalInteger = getId(exchange);
            if (optionalInteger.isEmpty()) {
                writeResponse(exchange, "Некорректный идентификатор задачи", 400);
                return;
            }
            int id = optionalInteger.get();
            writeResponse(exchange, gson.toJson(manager.removeSubtaskByID(id)), 200);
        }

        private void handleDeleteTasks(HttpExchange exchange) throws IOException {
            writeResponse(exchange, gson.toJson(manager.deleteAllTasks()), 200);
        }

        private void handleDeleteEpics(HttpExchange exchange) throws IOException {
            writeResponse(exchange, gson.toJson(manager.deleteAllEpics()), 200);
        }

        private void handleDeleteSubtasks(HttpExchange exchange) throws IOException {
            writeResponse(exchange, gson.toJson(manager.deleteAllSubtasks()), 200);
        }

        private Task deserializeTask(JsonElement jsonElement){
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            int id = jsonObject.get("Id").getAsInt();
            String name = jsonObject.get("Name").getAsString();
            String description = jsonObject.get("Description").getAsString();
            long duration = jsonObject.get("Duration").getAsLong();
            String startTime = jsonObject.get("StartTime").getAsString();
            Task task = new Task(name, description,
                    Status.valueOf(jsonObject.get("Status").getAsString()), duration, startTime);
            task.setId(id);
            return task;
        }

        private Subtask deserializeSubtask(JsonElement jsonElement){
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            int id = jsonObject.get("Id").getAsInt();
            String name = jsonObject.get("Name").getAsString();
            String description = jsonObject.get("Description").getAsString();
            int epicId = jsonObject.get("EpicId").getAsInt();
            long duration = jsonObject.get("Duration").getAsLong();
            String startTime = jsonObject.get("StartTime").getAsString();
            Subtask subtask = new Subtask(name, description,
                    Status.valueOf(jsonObject.get("Status").getAsString()), epicId, duration, startTime);
            subtask.setId(id);
            return subtask;
        }

        private Epic deserializeEpic(JsonElement jsonElement){
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            int id = jsonObject.get("Id").getAsInt();
            String name = jsonObject.get("Name").getAsString();
            String description = jsonObject.get("Description").getAsString();
            Epic epic = new Epic(name, description,
                    Status.valueOf(jsonObject.get("Status").getAsString()));
            epic.setId(id);
            return epic;
        }
    }
}