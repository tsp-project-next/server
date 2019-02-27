package server;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;

import client.Packet;

public class Server {

    private int clientNumber = 0;
    private ConcurrentHashMap<Integer,HandleClient> clientMap = new ConcurrentHashMap<>();

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

                InetAddress address = socket.getInetAddress();
                System.out.println("Client " + clientNumber + " connected with host name " + address.getHostName());

                HandleClient handleClient;
                new Thread(handleClient = new HandleClient(socket, clientNumber)).start();

                //add the socket to the client map
                clientMap.put(clientNumber, handleClient);

                //increment client number
                clientNumber++;
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    //might need to throw the actual object writing on a separate thread.
    //might not need to if the class instance itself is run on a separate thread.
    public void sendPacketToAllClients(String data, String cid) {
        try {
            //Create a packet to send
            Packet packet = new Packet(data, cid);
            for (int clientNumber : clientMap.keySet()){
                //send packet to every currently registered client
                clientMap.get(clientNumber).outputToClient.writeObject(packet);
                System.out.println("Object sent to client number: " + clientNumber);
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    //Use this method to switch through and handle different received packets
    public void handleReceivedPacket(Packet packet, ObjectOutputStream outputToClient) {
        try {
            outputToClient.writeObject(packet);
            System.out.println("information sent back to client...");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    class HandleClient implements Runnable {

        private ObjectInputStream inputFromClient;
        private ObjectOutputStream outputToClient;
        private int clientNumber;

        //The connected socket
        private Socket socket;

        public HandleClient(Socket socket, int clientNumber) {
            this.socket = socket;
            this.clientNumber = clientNumber;
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
                clientMap.remove(clientNumber);
            }
        }
    }
}
