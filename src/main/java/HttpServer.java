import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
                OutputStream outputStream = clientSocket.getOutputStream();
        ) {
            String header;
            String requestLine = bufferedReader.readLine();
            Map<String, String> requestHeadersMap = new HashMap<>();
            String response = "HTTP/1.1 404 Not Found\r\n\r\n";
            //Read Request line
            while ((header = bufferedReader.readLine()) != null && !header.isEmpty()) {
                  String[] keyVal = header.split(":", 2);
                  if(keyVal.length == 2) {
                      requestHeadersMap.put(keyVal[0], keyVal[1].trim());
                  }
            }
            // Read body
            StringBuffer bodyBuffer = new StringBuffer();
            while (bufferedReader.ready()) {
                bodyBuffer.append((char)bufferedReader.read());
            }
            String body = bodyBuffer.toString();

            // Process
            String[] requestLinePieces = requestLine.split(" ", 3);
            String httpMethod = requestLinePieces[0];
            String requestTarget = requestLinePieces[1];
            String httpVersion = requestLinePieces[2];
            //write
            if(httpMethod.equals("POST")) {
                if(requestTarget.startsWith("/files/")) {
                    File file = new File(fileDirectory + requestTarget.substring(7)); // name comes after /files/ ie. 7 index
                    if(file.createNewFile()) {
                        FileWriter fileWriter = new FileWriter(file);
                        fileWriter.write(body);
                        fileWriter.close();
                    } response = "HTTP/1.1 201 Created\r\n\r\n";
                } else {
                    response = "HTTP/1.1 404 Not Found\r\n\r\n";
                }
            } else {
                if (requestTarget.equals("/")) {
                    response = "HTTP/1.1 200 OK\r\n\r\n";
                }   else if (requestTarget.startsWith("/echo/")) {
                    String echoString = requestTarget.substring(6);
                    Boolean isGzip = false;
                    String contentEncoding = requestHeadersMap.get("Accept-Encoding");
                        if(contentEncoding != null) {
                            String[] encodings = contentEncoding.split(",");

                            for(String encoding: encodings) {
                                System.out.println(encoding + " enc " + "gzip".equalsIgnoreCase(encoding.trim()));

                                if("gzip".equalsIgnoreCase(encoding.trim())) {
                                    isGzip = true;
                                    System.out.println(isGzip + " changes sds ");
                                    break;
                                }
                            }
                        }
                        if(isGzip) {
                            response = "HTTP/1.1 200 OK\r\nContent-Encoding: gzip\r\nContent-Type: text/plain\r\nContent-Length: "
                                     + echoString.length() +
                                    "\r\n\r\n" + echoString;
                        }
                        else {
                                response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " +
                                        echoString.length() +
                                        "\r\n\r\n" + echoString;
                        }
                } else if (requestTarget.equals("/user-agent")) {
                    response =
                            "HTTP/1.1 200 OK\r\n"
                                    + "Content-Type: text/plain\r\n"
                                    + "Content-Length: " + requestHeadersMap.get("User-Agent").length() +
                                    "\r\n"
                                    + "\r\n" + requestHeadersMap.get("User-Agent");
                } else if (requestTarget.startsWith("/files/")) {
                    String fileName = requestTarget.substring(7);
                    FileReader fileReader;
                    try {
                        fileReader = new FileReader(fileDirectory + fileName);
                    } catch (FileNotFoundException e) {
                        outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
                        outputStream.flush();
                        outputStream.close();
                        return;
                    }
                    BufferedReader bufferedFileReader = new BufferedReader(fileReader);
                    StringBuffer stringBuffer = new StringBuffer();
                    String line;
                    while ((line = bufferedFileReader.readLine()) != null) {
                        stringBuffer.append(line);
                    }
                    bufferedFileReader.close();
                    fileReader.close();
                    response = "HTTP/1.1 200 OK\r\n"
                            + "Content-Type: application/octet-stream\r\n"
                            + "Content-Length: " + stringBuffer.length() +
                            "\r\n"
                            + "\r\n" + stringBuffer;
                } else {
                    response = "HTTP/1.1 404 Not Found\r\n\r\n";
                }
            }
            outputStream.write(response.getBytes());
            outputStream.flush();

        }
          catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
