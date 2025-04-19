import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
  private static String fileDirectory = "/tmp"; // Default, will be overridden by --directory
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

    for(int i = 0 ; i < args.length; i++) {
      if(args[i].equals("--directory") && i + 1 < args.length) {
        fileDirectory = args[i+1];
        break;
      }
    }
    final HttpServer httpServer = new HttpServer(4221, 10, fileDirectory);
    httpServer.run();
  }
}
