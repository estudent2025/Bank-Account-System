package mahabank;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class BankServer {
    private static final int PORT = 8080;
    private static final BankService bank = new BankService();
    private static final Path WEB = Paths.get("web");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/", BankServer::handle);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("====================================");
        System.out.println("          MAHA BANK ONLINE");
        System.out.println("====================================");
        System.out.println("Open http://localhost:" + PORT);
        System.out.println("Press Ctrl+C to stop the server.");
    }

    private static void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        try {
            if ("GET".equalsIgnoreCase(ex.getRequestMethod()) && ("/".equals(path) || "/index.html".equals(path))) {
                sendFile(ex, WEB.resolve("index.html"), "text/html; charset=utf-8");
            } else if ("GET".equalsIgnoreCase(ex.getRequestMethod()) && "/style.css".equals(path)) {
                sendFile(ex, WEB.resolve("style.css"), "text/css; charset=utf-8");
            } else if ("GET".equalsIgnoreCase(ex.getRequestMethod()) && "/app.js".equals(path)) {
                sendFile(ex, WEB.resolve("app.js"), "application/javascript; charset=utf-8");
            } else if ("GET".equalsIgnoreCase(ex.getRequestMethod()) && "/api/accounts".equals(path)) {
                sendJson(ex, 200, accountsJson());
            } else if ("GET".equalsIgnoreCase(ex.getRequestMethod()) && "/api/transactions".equals(path)) {
                sendJson(ex, 200, transactionsJson());
            } else if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
                Map<String, String> p = parseForm(ex);
                String action = p.getOrDefault("action", "");
                switch (action) {
                    case "create":
                        BankAccount a = bank.createAccount(p.get("name"), p.get("type"), number(p, "balance"));
                        sendJson(ex, 200, "{\"message\":\"Account created successfully. Account number: " + a.getAccountNumber() + "\"}");
                        break;
                    case "deposit":
                        bank.deposit(integer(p, "account"), number(p, "amount"));
                        sendJson(ex, 200, "{\"message\":\"Deposit successful.\"}");
                        break;
                    case "withdraw":
                        bank.withdraw(integer(p, "account"), number(p, "amount"));
                        sendJson(ex, 200, "{\"message\":\"Withdrawal successful.\"}");
                        break;
                    case "transfer":
                        bank.transfer(integer(p, "from"), integer(p, "to"), number(p, "amount"));
                        sendJson(ex, 200, "{\"message\":\"Transfer completed successfully.\"}");
                        break;
                    case "interest":
                        double interest = bank.creditMonthlyInterest(integer(p, "account"));
                        sendJson(ex, 200, "{\"message\":\"Monthly interest of ₹" + String.format(Locale.US, "%.2f", interest) + " credited.\"}");
                        break;
                    default:
                        sendJson(ex, 400, "{\"error\":\"Unknown banking action.\"}");
                }
            } else {
                sendJson(ex, 404, "{\"error\":\"Page not found.\"}");
            }
        } catch (BankException e) {
            sendJson(ex, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        } catch (NumberFormatException e) {
            sendJson(ex, 400, "{\"error\":\"Please enter valid numbers.\"}");
        } catch (Exception e) {
            sendJson(ex, 500, "{\"error\":\"Something went wrong. Please check your input and try again.\"}");
            e.printStackTrace();
        } finally {
            ex.close();
        }
    }

    private static String accountsJson() {
        StringBuilder s = new StringBuilder("{\"accounts\":[");
        List<BankAccount> list = bank.getAccounts();
        for (int i = 0; i < list.size(); i++) {
            BankAccount a = list.get(i);
            if (i > 0) s.append(",");
            s.append("{\"number\":").append(a.getAccountNumber())
             .append(",\"name\":\"").append(escape(a.getHolderName())).append("\"")
             .append(",\"type\":\"").append(a.getAccountType()).append("\"")
             .append(",\"balance\":").append(String.format(Locale.US, "%.2f", a.getBalance()))
             .append(",\"interest\":").append(String.format(Locale.US, "%.2f", a.calculateMonthlyInterest())).append("}");
        }
        return s.append("]}").toString();
    }

    private static String transactionsJson() {
        StringBuilder s = new StringBuilder("{\"transactions\":[");
        List<Transaction> list = bank.getAllTransactions();
        for (int i = 0; i < list.size(); i++) {
            Transaction t = list.get(i);
            if (i > 0) s.append(",");
            s.append("{\"time\":\"").append(escape(t.getTime())).append("\"")
             .append(",\"type\":\"").append(escape(t.getType())).append("\"")
             .append(",\"account\":").append(t.getAccountNumber())
             .append(",\"details\":\"").append(escape(t.getDetails())).append("\"")
             .append(",\"amount\":").append(String.format(Locale.US, "%.2f", t.getAmount()))
             .append(",\"balance\":").append(String.format(Locale.US, "%.2f", t.getBalanceAfter())).append("}");
        }
        return s.append("]}").toString();
    }

    private static Map<String, String> parseForm(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> map = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(key, value);
        }
        return map;
    }

    private static int integer(Map<String, String> p, String key) {
        return Integer.parseInt(p.getOrDefault(key, "").trim());
    }

    private static double number(Map<String, String> p, String key) {
        return Double.parseDouble(p.getOrDefault(key, "").trim());
    }

    private static void sendFile(HttpExchange ex, Path file, String contentType) throws IOException {
        if (!Files.exists(file)) {
            sendJson(ex, 404, "{\"error\":\"Website file missing. Make sure you run the server from the MahaBank folder.\"}");
            return;
        }
        byte[] data = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, data.length);
        ex.getResponseBody().write(data);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r")
                .replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
    }
}
