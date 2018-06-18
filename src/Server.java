/*
 * @author Varun on 6/17/2018
 * @project P2P-Network
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

class Server {
    private ServerSocket serverSocket;
    private LinkedList<Socket> socketList = new LinkedList<>();
    private LinkedList<ObjectInputStream> objInputStreams = new LinkedList<>();

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
        if (socket != null) return socket.getInetAddress().toString().replace("/", "");
        return "";
    }

    /**
     * Finds a suitable client for the server. (Only allows certain IP addresses to connect to it)
     *
     * @param whiteListIP The list of allowed IP addresses to connect to the server.
     * @return Returns a socket connection to the selected client.
     */
    private Socket findSuitableClient(LinkedList<String> whiteListIP) {
        Socket tempSocket;
        while (true) {
            try {
                tempSocket = serverSocket.accept();
                if (whiteListIP.contains(formatIP(tempSocket))) {
                    System.out.println("Accepted Client with ip " + formatIP(tempSocket));
                    return tempSocket;
                } else {
                    tempSocket.close();
                    System.out.println("Rejected Client with ip " + formatIP(tempSocket));
                }
            } catch (IOException e) {
                System.out.println("Error occurred while trying to accept client connection.");
                e.printStackTrace();
            }
        }
    }

    /**
     * Listens to any updates in the InputStream of a Socket.
     *
     * @param inStream ObjectInputStream
     */
    private void listenToInputStream(ObjectInputStream inStream) {
        while (Peer.Shared.running) {
            try {
                Object o = inStream.readObject();
                Peer.Shared.threads.add(new Thread(() -> handleObjData(o)));
                Peer.Shared.threads.getLast().start();
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
    void startServer(LinkedList<String> adjPeerIP, int port) {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(0);
        } catch (IOException e) {
            System.out.println("Could not create a ServerSocket.");
            e.printStackTrace();
        }

        for (String peer : adjPeerIP) {
            try {
                socketList.add(findSuitableClient(adjPeerIP));
                objInputStreams.add(new ObjectInputStream(socketList.getLast().getInputStream()));
                Peer.Shared.threads.add(new Thread(() -> listenToInputStream(objInputStreams.getLast())));
                Peer.Shared.threads.getLast().start();
                System.out.println("Server connected to " + formatIP(socketList.getLast()) + ":" + socketList.getLast().getPort() + " (" + (adjPeerIP.indexOf(peer) + 1) + "/" + adjPeerIP.size() + ")");
            } catch (IOException e) {
                System.out.println("Error occurred while finding a suitable client.");
                e.printStackTrace();
            }
        }
        System.out.println("Setup connections to ALL clients successfully!");
    }

    /**
     * Safely ends the server.
     */
    void closeServer() {
        for (int i = 0; i < socketList.size(); i++) {
            try {
                socketList.get(i).close();
                objInputStreams.get(i).close();
            } catch (IOException e) {
                System.out.println("Error while closing a socket.");
                e.printStackTrace();
            }
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("Error while closing server socket.");
            e.printStackTrace();
        }
    }

    /**
     * Handles received data.
     *
     * @param o Object received.
     */
    void handleObjData(Object o) {
        if (o instanceof SerializableText) {
            handleTextData((SerializableText) o);
        } else if (o instanceof TraversalObj) {
            recursiveTraversal((TraversalObj) o);
        } else {
            System.out.println("Received an unrecognizable object.");
        }
    }

    /**
     * Handles received text data. (Log the message)
     *
     * @param text The text received.
     */
    private void handleTextData(SerializableText text) {
        System.out.println(text.text + " (" + text.source + ")" + "(" + text.timeStamp.toString() + ")");
    }

    /**
     * Implementation of a peer traversal algorithm.
     *
     * @param traversalObj An object which contains all required data to continue recursion.
     */
    private void recursiveTraversal(TraversalObj traversalObj) {
        if (checkBaseCase(traversalObj)) return;
        handleObjData(traversalObj.data);
        traversalObj.visited.add(Peer.Ipv4Local);
        String timeStamp = traversalObj.timeStamp.toString();

        Peer.intitializeCounterAtomically(Peer.Shared.callBackCounter, timeStamp);

        TraversalObj sendingData = new TraversalObj();
        sendingData.equals(traversalObj);
        sendingData.callbackSubject = Peer.Ipv4Local;

        for (Connection connection : Peer.connections.values()) {
            if (!traversalObj.visited.contains(connection.ip)) {
                Peer.iterateCounterAtomically(Peer.Shared.callBackCounter, timeStamp, 1, 0);
                Peer.Shared.threads.add(new Thread(() -> Peer.sendObject(sendingData, connection.ip)));
                Peer.Shared.threads.getLast().start();
            }
        }

        System.out.println("Expected Callbacks: " + Peer.Shared.callBackCounter.get().get(timeStamp).get(0));
        while (Peer.Shared.callBackCounter.get().get(timeStamp).get(1) < Peer.Shared.callBackCounter.get().get(timeStamp).get(0)) {

        }
        System.out.println("GOT ALL CALLBACKS! " + Peer.Shared.callBackCounter.get().get(timeStamp).get(1));
        if (traversalObj.globalSource.equals(Peer.Ipv4Local)) {
            System.out.println("CONFIRMATION: DATA REACHED ALL NODES");
            return;
        }
        traversalObj.type = "CALLBACK";
        Peer.sendObject(traversalObj, traversalObj.callbackSubject);
    }

    /**
     * Used to check base case for recursion.
     *
     * @param data An object which contains all required data to continue recursion.
     * @return Returns whether the Peer should continue recursion.
     */
    private boolean checkBaseCase(TraversalObj data) {
        if (Peer.Shared.callBackCounter.get().containsKey(data.timeStamp.toString())) {
            if (data.type.contains("CALLBACK")) {
                Peer.iterateCounterAtomically(Peer.Shared.callBackCounter, data.timeStamp.toString(), 1, 1);
                return true;
            }
            data.visited.add(Peer.Ipv4Local);
            data.type = "CALLBACKINVALID";
            Peer.sendObject(data, data.callbackSubject);
            return true;
        }
        return false;
    }
}
