package server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import client.Packet;

public class Server {

    private ServerSocket serverSocket;
    private Database database;

    //client number serves no purpose other than to display how many users are connected
    private int clientNumber = 0;
    private List<String> user_ids = new ArrayList<>();
    private List<String> lobby_ids = new ArrayList<>();
    private ConcurrentHashMap<String,HandleClient> clientMap = new ConcurrentHashMap<>();

    public static void main(String args[]) {
        new Server();
    }

    public Server() {
        try {
            database = new Database();
            boolean connected = database.connectDB();
            if(connected == false) {
                System.out.println("Failed to connect to database.");
                System.out.println("Server stopping...");
                System.exit(0);
            }

            //Create server socket
            serverSocket = new ServerSocket(9000);
            System.out.println("Server started...");

            while(true) {
                //Listen for new connection request
                Socket socket = serverSocket.accept();

                InetAddress address = socket.getInetAddress();
                System.out.println("Client " + clientNumber + " connected with host name " + address.getHostName());

                String user_id;
                do {
                    user_id = Utilities.generateCode();
                } while (user_ids.contains(user_id));

                HandleClient handleClient;
                new Thread(handleClient = new HandleClient(socket, user_id)).start();

                //add the socket to the client map
                clientMap.put(user_id, handleClient);

                //increment client number
                clientNumber++;
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    //might need to throw the actual object writing on a separate thread.
    //might not need to if the class instance itself is run on a separate thread.
    public void sendPacketToAllClients(String packetID, int packetType) {
        try {
            //Create a packet to send
            Packet packet = new Packet(packetID, packetType);
            for (String user_id : clientMap.keySet()){
                //send packet to every currently registered client
                clientMap.get(user_id).outputToClient.writeObject(packet);
                System.out.println("Object sent to user_id: " + user_id);
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    //Use this method to switch through and handle different received packets
    public void handleReceivedPacket(Packet packet, ObjectOutputStream outputToClient) {
        try {
            switch(packet.getPacketType()) {
                case 0:
                    Packet returnPacket = new Packet(packet.getPacketIdentifier(), 0);
                    outputToClient.writeObject(packet);
                    break;
                case 1:
                    break;
                default:
                    System.out.println("Packet Type Mismatch...");
                    break;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    class HandleClient implements Runnable {

        private ObjectInputStream inputFromClient;
        private ObjectOutputStream outputToClient;
        private String user_id;

        //The connected socket
        private Socket socket;

        public HandleClient(Socket socket, String user_id) {
            this.socket = socket;
            this.user_id = user_id;
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
                    Packet packetReceived = (Packet) inputFromClient.readObject();

                    handleReceivedPacket(packetReceived, outputToClient);
                }
            } catch(ClassNotFoundException ex) {
                ex.printStackTrace();
            } catch(SocketException ex) {
                System.out.println("Connection reset/closed for client: " + socket.getInetAddress().getHostName());
                return;
            } catch(EOFException ex) {
                // System.out.println("We're catching this in the final block....");
            } catch(IOException ex) {
                ex.printStackTrace();
            } finally {
                clientMap.remove(user_id);
            }
        }
    }
}
