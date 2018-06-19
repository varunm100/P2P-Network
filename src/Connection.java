/*
 * @author Varun on 6/17/2018
 * @project P2P-Network
 */

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

class Connection {
    private Socket socket;
    String ip = null;
    private int port = -1;
    private ObjectOutputStream outStream;

    /**
     * Connects client socket with server socket.
     */
    Connection() {
    }

    /**
     * Connects client socket to a serverSocket on another peer.
     *
     * @param _ip   Ip address of desired peer.
     * @param _port Port number of desired peer.
     */
    void establishConnection(String _ip, int _port) {
        ip = _ip;
        port = _port;
        try {
            socket = new Socket(_ip, _port);
            outStream = new ObjectOutputStream(socket.getOutputStream());
            System.out.println("Connection Established with " + ip + ":" + port);
        } catch (IOException e) {
            System.out.println("Error while trying to establish a connection with " + ip + ":" + port);
            e.printStackTrace();
        }
    }

    /**
     * Disconnects socket.
     */
    void disconnect() {
        if (socket.isConnected()) {
            try {
                outStream.close();
                socket.close();
                ip = null;
                port = -1;
            } catch (IOException e) {
                System.out.println("Error while disconnecting socket.");
                e.printStackTrace();
            }
        } else {
            System.out.println("Socket is connected, therefore could not disconnect.");
        }
    }

    /**
     * Sends an object via this socket.
     *
     * @param o Object to be sent.
     */
    void sendObject(Object o) {
        try {
            outStream.writeObject(o);
            outStream.flush();
        } catch (IOException e) {
            System.out.println("Error while sending object data.");
            e.printStackTrace();
        }
    }
}
