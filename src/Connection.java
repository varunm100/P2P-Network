import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

class Connection {
    private Socket socket;
    String IP = null;
    private int port = -1;
    private ObjectOutputStream outStream;

    Connection() {}

    void establishConnection(String _IP, int _port) {
        IP = _IP;
        port = _port;
        try {
            socket = new Socket(IP, port);
            outStream = new ObjectOutputStream(socket.getOutputStream());
            System.out.println("Connection Established with " + IP + ":" + port);
        } catch (IOException e) {
            System.out.println("Error while trying to establish a connection with " + IP + ":" + port);
            e.printStackTrace();
        }
    }

    void disconnect() {
        if (socket.isConnected()) {
            try {
                outStream.close();
                socket.close();
                IP = null;
                port = -1;
            } catch (IOException e) {
                System.out.println("Error while disconnecting socket.");
                e.printStackTrace();
            }
        } else {
            System.out.println("Socket is connected, therefore could not disconnect.");
        }
    }

    void sendObject(Object o) {
        try {
            outStream.writeObject(o);
        } catch (IOException e) {
            System.out.println("Error while sending object data");
            e.printStackTrace();
        }
    }
}
