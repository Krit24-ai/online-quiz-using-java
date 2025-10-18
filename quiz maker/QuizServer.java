import java.io.*;
import java.net.*;
import java.util.*;
import com.sun.net.httpserver.*;

public class QuizServer {

    private static final String[] QUESTIONS = {
        "Who is known as the Father of the Indian Nation?",
        "What is the capital of India?",
        "In which year did India gain independence?",
        "Which river is considered the holiest in India?",
        "Who was the first Prime Minister of India?",
        "Which is the national animal of India?",
        "What is the national currency of India?",
        "Which Indian city is called the Pink City?",
        "Who wrote the Indian National Anthem?",
        "Which state is known as the Land of Five Rivers?",
        "What is the national fruit of India?",
        "Who was India’s first woman Prime Minister?",
        "Which festival is called the festival of lights?",
        "Where is the Taj Mahal located?",
        "Which is the largest Indian state by area?"
    };

    private static final String[][] OPTIONS = {
        {"Subhas Chandra Bose", "Mahatma Gandhi", "Bhagat Singh", "Jawaharlal Nehru"},
        {"Mumbai", "New Delhi", "Kolkata", "Chennai"},
        {"1945", "1947", "1950", "1962"},
        {"Yamuna", "Ganga", "Godavari", "Krishna"},
        {"Jawaharlal Nehru", "Sardar Patel", "Indira Gandhi", "Rajendra Prasad"},
        {"Elephant", "Tiger", "Lion", "Peacock"},
        {"Dollar", "Rupee", "Yen", "Pound"},
        {"Jaipur", "Chennai", "Lucknow", "Kolkata"},
        {"Rabindranath Tagore", "Bankim Chandra", "Sarojini Naidu", "Mahatma Gandhi"},
        {"Punjab", "Kerala", "Haryana", "Bihar"},
        {"Banana", "Apple", "Mango", "Orange"},
        {"Indira Gandhi", "Sarojini Naidu", "Pratibha Patil", "Sonia Gandhi"},
        {"Holi", "Diwali", "Eid", "Navratri"},
        {"Agra", "Delhi", "Jaipur", "Mumbai"},
        {"Rajasthan", "Uttar Pradesh", "Madhya Pradesh", "Gujarat"}
    };

    private static final String[] ANSWERS = {
        "Mahatma Gandhi", "New Delhi", "1947", "Ganga", "Jawaharlal Nehru",
        "Tiger", "Rupee", "Jaipur", "Rabindranath Tagore", "Punjab",
        "Mango", "Indira Gandhi", "Diwali", "Agra", "Rajasthan"
    };

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new Handler());
        server.setExecutor(null);
        server.start();
        System.out.println("🚀 Digital India Quiz running at http://localhost:8000");
    }

    static class Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();

            if (path.equals("/") || path.equals("/home")) serveFile(ex, "home.html", "text/html");
            else if (path.equals("/style.css")) serveFile(ex, "style.css", "text/css");
            else if (path.startsWith("/question")) serveQuestion(ex);
            else if (path.equals("/submit")) serveResult(ex);
            else sendError(ex, 404, "Page not found");
        }

        private void serveQuestion(HttpExchange ex) throws IOException {
            int num = Integer.parseInt(ex.getRequestURI().getPath().substring(9));
            if (num < 1 || num > 15) { sendError(ex, 404, "Invalid question"); return; }

            Map<String, String> prevAns = queryToMap(ex.getRequestURI().getQuery());
            sendHTML(ex, buildQuestion(num, prevAns));
        }

        private String buildQuestion(int num, Map<String, String> prev) {
            String q = QUESTIONS[num - 1];
            String[] opts = OPTIONS[num - 1];
            String next = num < 15 ? "/question" + (num + 1) : "/submit";
            String prevQ = num > 1 ? "/question" + (num - 1) : null;

            StringBuilder sb = new StringBuilder();
            sb.append("""
                <html><head>
                    <title>Digital India Quiz</title>
                    <link rel='stylesheet' href='style.css'>
                    <link href='https://fonts.googleapis.com/css2?family=Orbitron:wght@500;700&display=swap' rel='stylesheet'>
                </head><body class='digital-bg'>
                    <div class='quiz-container fade'>
                        <div class='quiz-card'>
                            <h1>Question %d / 15</h1>
                            <div class='progress'><div class='bar' style='width:%d%%'></div></div>
                            <p class='question-text'>%s</p>
                            <form action='%s' method='get'>
            """.formatted(num, num * 100 / 15, q, next));

            for (var e : prev.entrySet())
                sb.append("<input type='hidden' name='%s' value='%s'>".formatted(e.getKey(), e.getValue()));

            sb.append("<div class='options-grid'>");
            for (String o : opts)
                sb.append("<label class='option-card'><input type='radio' name='q%d' value='%s' required> <span>%s</span></label>"
                        .formatted(num, o, o));
            sb.append("</div>");

            sb.append("<div class='controls'>");
            sb.append("<a href='/home' class='btn home-btn'> Home</a>");
            if (prevQ != null)
                sb.append("<button type='button' class='btn back' onclick='goBack()'> Previous</button>");
            sb.append("<input type='submit' class='btn next' value='%s'>"
                    .formatted(num < 15 ? "Next " : "Submit Quiz"));
            sb.append("<button type='button' class='btn submit-direct' onclick='submitQuiz()'> Submit Directly</button>");
            sb.append("</div></form></div></div>");

            sb.append("""
                <script>
                    function goBack() {
                        const params = new URLSearchParams(window.location.search);
                        window.location.href = '%s?' + params.toString();
                    }
                    function submitQuiz() {
                        const params = new URLSearchParams(window.location.search);
                        const form = document.querySelector('form');
                        new FormData(form).forEach((v, k) => params.set(k, v));
                        window.location.href = '/submit?' + params.toString();
                    }
                </script></body></html>
                """.formatted(prevQ != null ? prevQ : "/question1"));
            return sb.toString();
        }

        private void serveResult(HttpExchange ex) throws IOException {
            Map<String, String> map = queryToMap(ex.getRequestURI().getQuery());
            int score = 0;
            for (int i = 0; i < 15; i++) {
                String key = "q" + (i + 1);
                if (map.containsKey(key) && map.get(key).equals(ANSWERS[i])) score++;
            }

            String feedback = score >= 12 ? " Excellent knowledge!"
                    : score >= 8 ? " Good job!"
                    : score >= 5 ? " Keep learning!"
                    : " Try again, you can do better!";

            String html = """
                <html><head>
                    <title>Quiz Result</title>
                    <link rel='stylesheet' href='style.css'>
                </head><body class='digital-bg'>
                    <div class='result-container fade'>
                        <h1>Quiz Completed </h1>
                        <h2>Score: %d / 15</h2>
                        <p>%s</p>
                        <a href='/home' class='btn home'> Back to Home</a>
                    </div>
                </body></html>
                """.formatted(score, feedback);

            sendHTML(ex, html);
        }

        private static void serveFile(HttpExchange ex, String file, String type) throws IOException {
            File f = new File(file);
            if (!f.exists()) { sendError(ex, 404, "File not found"); return; }
            byte[] data = java.nio.file.Files.readAllBytes(f.toPath());
            ex.getResponseHeaders().set("Content-Type", type);
            ex.sendResponseHeaders(200, data.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(data); }
        }

        private static void sendHTML(HttpExchange ex, String html) throws IOException {
            byte[] data = html.getBytes();
            ex.getResponseHeaders().set("Content-Type", "text/html");
            ex.sendResponseHeaders(200, data.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(data); }
        }

        private static Map<String, String> queryToMap(String query) {
            Map<String, String> map = new LinkedHashMap<>();
            if (query == null) return map;
            for (String p : query.split("&")) {
                String[] parts = p.split("=");
                if (parts.length == 2)
                    map.put(URLDecoder.decode(parts[0], java.nio.charset.StandardCharsets.UTF_8),
                            URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8));
            }
            return map;
        }

        private static void sendError(HttpExchange ex, int code, String msg) throws IOException {
            byte[] data = msg.getBytes();
            ex.sendResponseHeaders(code, data.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(data); }
        }
    }
}
