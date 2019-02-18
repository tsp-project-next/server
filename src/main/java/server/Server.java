package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Server {

    private int clientNumber = 0;

    public static void main(String args[]) {
        new Server();
    }

    public Server() {
        try {
            //Create server socket
            ServerSocket serverSocket = new ServerSocket(9000);
            System.out.println("Server started...");

            while(true) {
                //Listen for new connection request
                Socket socket = serverSocket.accept();

                //increment client number
                clientNumber++;

                InetAddress address = socket.getInetAddress();
                System.out.println("Client " + clientNumber + " connected with host name " + address.getHostName());

                new Thread(new HandleClient(socket)).start();
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    class HandleClient implements Runnable {

        private ObjectInputStream inputFromClient;
        private ObjectOutputStream outputToClient;

        //The connected socket
        private Socket socket;

        public HandleClient(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                System.out.println("New thread created for socket: " + socket.getInetAddress().getHostName());

                //Create output stream from socket
                outputToClient = new ObjectOutputStream(socket.getOutputStream());

                //Create input stream from socket
                inputFromClient = new ObjectInputStream(socket.getInputStream());

                while(true){
                    //Read from input
                    Object obj = inputFromClient.readObject();

                    //return information to client.
                    outputToClient.writeObject(obj);
                    System.out.println("Sending information back to client...");
                }
            } catch(ClassNotFoundException ex) {
                ex.printStackTrace();
            } catch(SocketException ex) {
                System.out.println("Connection reset/closed for client: " + socket.getInetAddress().getHostName());
                return;
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
