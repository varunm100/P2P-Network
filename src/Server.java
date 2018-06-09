import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class Server {
    class Shared {

    }

    ServerSocket serverSocket;
    LinkedList<Socket> socketList = new LinkedList<>();
    LinkedList<ObjectInputStream> objInputStreams = new LinkedList<>();

    public Server() {}

    public String formatIP(Socket socket) { if (socket != null) { return socket.getInetAddress().toString().replace("/", ""); } return ""; }

    public Socket findValidClient(LinkedList<String> _whiteListIP) {
        Socket tempSocket;
        while(true) {
            try {
                tempSocket = serverSocket.accept();
                if (_whiteListIP.contains(formatIP(tempSocket))) {
                    System.out.println("Accepted Client with IP " + formatIP(tempSocket));
                    return tempSocket;
                } else {
                    tempSocket.close();
                    System.out.println("Rejected Client with IP " + formatIP(tempSocket));
                }
            } catch (IOException e) {
                System.out.println("Error occurred while trying to accept client connection.");
                e.printStackTrace();
            }
        }
    }

    public void checkUpdate(int i) {
        try {
            Object o = objInputStreams.get(i).readObject();

        } catch (IOException e) {
            System.out.println("Error occurred while receiving data.");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("Received data does not contain a recognizable object.");
            e.printStackTrace();
        }
    }

    public void startServer(LinkedList<String> _adjPeerIP, int _port) {
        try {
            serverSocket = new ServerSocket(_port);
            serverSocket.setSoTimeout(0);
        } catch (IOException e) {
            System.out.println("Could not create ServerSocket.");
            e.printStackTrace();
        }

        for (String peer : _adjPeerIP) {
            try {
                socketList.add(findValidClient(_adjPeerIP));
                objInputStreams.add(new ObjectInputStream(socketList.getLast().getInputStream()));
                new Thread(() -> checkUpdate(socketList.size()-1)).start();
                System.out.println("Server connected to " + formatIP(socketList.getLast()) + ":" + socketList.getLast().getPort() + " (" + (_adjPeerIP.indexOf(peer)+1) + "/" + _adjPeerIP.size() + ")");
            } catch (IOException e) {
                System.out.println("Error occurred while finding suitable a client.");
                e.printStackTrace();
            }
        }
        System.out.println("Setup Connections to ALL Clients Successfully!");
    }

    public void closeServer() {
        for (int i = 0; i < socketList.size(); i++) {
            try {
                socketList.get(i).close();
                objInputStreams.get(i).close();
            } catch (IOException e) {
                System.out.println("Error while closing a socket");
                e.printStackTrace();
            }
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("Error while closing server socket");
            e.printStackTrace();
        }
    }

    public void handleObjData(Object o) {
        /* TODO */
    }
}
