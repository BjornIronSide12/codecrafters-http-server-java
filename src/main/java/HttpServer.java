import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpServer implements Runnable{

    private final int port;
    private final ExecutorService executorService;
    private String fileDirectory;

    public HttpServer(int port, int concurrencyLevel, String fileDirectory) {
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(concurrencyLevel);
        this.fileDirectory = fileDirectory;
    }
    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            // Since the tester restarts your program quite often, setting SO_REUSEADDR
            // ensures that we don't run into 'Address already in use' errors
            serverSocket.setReuseAddress(true);
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
                System.out.println("accepted new connection");
                executorService.submit(() -> handleRequest(clientSocket));
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }

    private void handleRequest(Socket clientSocket) {
        try(
                InputStreamReader inputStreamReader = new InputStreamReader(clientSocket.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        ) {
            String line;
            String requestLine = null;
            String userAgent = null;
            //Read Request line
            while ((line = bufferedReader.readLine()) != null) {
                if (line.isEmpty()) {
                    // End of headers
                    break;
                }
                if (requestLine == null) {
                    requestLine = line;
                }
                if (line.startsWith("User-Agent:")) {
                    userAgent = line.substring("User-Agent:".length()).trim(); // returns value after "User-Agent" after trimming leading and trailing white spaces
                }
            }
            if (requestLine != null || userAgent != null) {
                Pattern echoPattern = Pattern.compile("GET /echo/([^\\s]+) HTTP/1\\.1");
                Matcher echoMatcher = echoPattern.matcher(requestLine);

                Pattern emptyPattern = Pattern.compile("GET / HTTP/1\\.1");
                Matcher emptyMatcher = emptyPattern.matcher(requestLine);

                String response;
                String[] parts = requestLine.split(" ");
                byte[] fileContent = null;
                if (echoMatcher.find()) {
                    String echoedString = echoMatcher.group(1);
                    response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: " + echoedString.getBytes().length + "\r\n\r\n" +
                            echoedString;
                } else if (emptyMatcher.find()) {
                    response = "HTTP/1.1 200 OK\r\n\r\n";
                } else if(requestLine.contains("files") && parts.length >= 2) {
                        String uriString = parts[1];
                        URI uri = new URI(uriString);
                        String path = uri.getPath();
                        String requestedFileName = path.substring("/files".length()).trim();
                        Path filePath = Paths.get(fileDirectory, requestedFileName);
                        File file = filePath.toFile();
                        if (file.exists() && file.isFile()) {
                            // Serve the file
                            fileContent = Files.readAllBytes(filePath);
                            response = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: application/octet-stream\r\n" +
                                    "Content-Length: " + fileContent.length + "\r\n" +
                                    "\r\n";
                        } else {
                            // File not found
                            response = "HTTP/1.1 404 Not Found\r\n\r\n";

                        }

                } else if (userAgent != null) {
                    response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: " + userAgent.getBytes().length + "\r\n\r\n" +
                            userAgent;
                } else {
                    response = "HTTP/1.1 404 Not Found\r\n\r\n";
                }
                clientSocket.getOutputStream().write(response.getBytes());
                if(fileContent != null) {
                    clientSocket.getOutputStream().write(fileContent);
                }
                clientSocket.getOutputStream().flush();
            }
        }  catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
