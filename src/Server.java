/*
 * @author Varun on 6/17/2018
 * @project P2P-Network
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.stream.IntStream;

class Server {
    private ServerSocket serverSocket;
    private LinkedList<Socket> socketList = new LinkedList<>();
    private LinkedList<ObjectInputStream> objInputStreams = new LinkedList<>();
    private Consumer<Object> handleDataReceived = null;

    /**
     * Server Class.
     */
    Server() {
    }

    /**
     * Formats the IP address of a socket.
     *
     * @param socket Socket Object
     * @return Returns formatted IP address.
     */
    private String formatIP(Socket socket) {
        return socket != null ? socket.getInetAddress().toString().replace("/", "") : "";
    }

    /**
     * Finds a suitable client for the server. (Only allows certain IP addresses to connect to it)
     *
     * @param whiteListIP The list of allowed IP addresses to connect to the server.
     * @return Returns a socket connection to the selected client.
     */
    private Socket findSuitableClient(LinkedList<String> whiteListIP) {
        Socket tempSocket = null;
        while (Peer.Shared.running) {
            try {
                tempSocket = serverSocket.accept();
                if (whiteListIP.contains(formatIP(tempSocket))) {
                    System.out.println("Accepted Client " + formatIP(tempSocket) + ":" + tempSocket.getPort());
                    break;
                } else {
                    tempSocket.close();
                    System.out.println("Rejected Client " + formatIP(tempSocket) + ":" + tempSocket.getPort());
                }
            } catch (IOException e) {
                System.out.println("Error occurred while trying to accept client connection.");
                e.printStackTrace();
            }
        }
        return tempSocket;
    }

    /**
     * Listens to any updates in the InputStream of a Socket.
     *
     * @param inStream ObjectInputStream
     */
    private void inputStreamListener(ObjectInputStream inStream) {
        while (Peer.Shared.running) {
            try {
                Object o = inStream.readObject();
                Peer.Shared.threadManager.submit(() -> handleDataReceived.accept(o));
            } catch (IOException e) {
                System.out.println("Error occurred while receiving data.");
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.out.println("Received data that does not contain a recognizable object.");
                e.printStackTrace();
            }
        }
    }

    /**
     * Listens and establishes a connection to all whitelisted peers.
     *
     * @param adjPeerIP The ip addresses of all the adjacent peers.
     * @param port      The port number at which the server initializes.
     */
    void startServer(LinkedList<String> adjPeerIP, int port, Consumer<Object> handleSocketInputStream) {
        handleDataReceived = handleSocketInputStream;
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(0);
        } catch (IOException e) {
            System.out.println("Could not create a ServerSocket.");
            e.printStackTrace();
        }

        adjPeerIP.forEach(peer -> {
            try {
                socketList.add(findSuitableClient(adjPeerIP));
                objInputStreams.add(new ObjectInputStream(socketList.getLast().getInputStream()));
                Peer.Shared.threadManager.submit(() -> inputStreamListener(objInputStreams.getLast()));
                System.out.println("Server connected to " + formatIP(socketList.getLast()) + ":" + socketList.getLast().getPort() + " (" + (adjPeerIP.indexOf(peer) + 1) + "/" + adjPeerIP.size() + ")");
            } catch (IOException e) {
                System.out.println("Error occurred while finding a suitable client.");
                e.printStackTrace();
            }
        });
        System.out.println("Setup connections to ALL clients successfully!");
    }

    /**
     * Safely ends the server.
     */
    void closeServer() {

        IntStream.range(0, socketList.size()).forEach(i -> {
            try {
                socketList.get(i).close();
                objInputStreams.get(i).close();
            } catch (IOException e) {
                System.out.println("Error while closing a socket.");
                e.printStackTrace();
            }
        });
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("Error while closing server socket.");
            e.printStackTrace();
        }
    }
}
