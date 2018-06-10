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
    Connection() {}

    /**
     * Connects client socket to a serverSocket on another peer.
     * @param _ip Ip address of desired peer.
     * @param _port Port number of desired peer.
     */
    void establishConnection(String _ip, int _port) {
        ip = _ip;
        port = _port;
        try {
            socket = new Socket(ip, port);
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
     * Sends an object over the currently binded socket.
     * @param _o Object to be sent.
     */
    void sendObject(Object _o) {
        try {
            outStream.writeObject(_o);
        } catch (IOException e) {
            System.out.println("Error while sending object data.");
            e.printStackTrace();
        }
    }
}
