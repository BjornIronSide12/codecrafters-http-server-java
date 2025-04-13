import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

    // Uncomment this block to pass the first stage
    // adding test comment 

    try {
      ServerSocket serverSocket = new ServerSocket(4221);
    
      // Since the tester restarts your program quite often, setting SO_REUSEADDR
      // ensures that we don't run into 'Address already in use' errors
      serverSocket.setReuseAddress(true);
    
      Socket socket = serverSocket.accept(); // Wait for connection from client.
      System.out.println("accepted new connection");
      InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      String line = bufferedReader.readLine();
      System.out.println(line);
      String[] request = line.split(" ");
      System.out.println(request[0] +  request[1]);
      switch (request[1]) {
        case "/" :
          socket.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
          break;
        default:
          socket.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());

      }

    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}
