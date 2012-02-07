package polly.porat.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import org.apache.log4j.Logger;


import polly.network.Connection;
import polly.network.events.ConnectionListener;
import polly.network.events.NetworkEvent;
import polly.network.events.ObjectReceivedEvent;
import polly.network.events.ObjectReceivedListener;
import polly.network.protocol.Constants;
import polly.network.protocol.ErrorResponse;
import polly.network.protocol.Ping;
import polly.network.protocol.Pong;
import polly.network.protocol.ProtocolObject;
import polly.network.protocol.Request;
import polly.network.protocol.Response;
import polly.network.protocol.Constants.RequestType;
import polly.network.protocol.Constants.ResponseType;


public class ClientConnection implements Connection, Runnable {
    
    
    
    public static void main(String[] args) throws NoSuchAlgorithmException, UnknownHostException, IOException {

        
        ClientConnection con = new ClientConnection(InetAddress.getByName("localhost"), 
            24500);
        
        Request login = new Request(RequestType.LOGIN);
        login.getPayload().put(Constants.USER_NAME, "C0mb4t");
        login.getPayload().put(Constants.PASSWORD, "nichtpenner");
        
        con.send(login);
    }
    
    
    
    
    private final static Logger logger = Logger.getLogger(
            ClientConnection.class.getName());
    

    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private ExecutorService connectionThread;
    private AtomicBoolean shutdownFlag;
    
    
    
    public ClientConnection(InetAddress serverHost, int port) 
                throws NoSuchAlgorithmException, IOException {
        
        SSLContext context = SSLContext.getDefault();
        SocketFactory sf = context.getSocketFactory();
        
        try {
            this.socket = sf.createSocket(serverHost, port);
            this.input = new ObjectInputStream(this.socket.getInputStream());
            this.output = new ObjectOutputStream(this.socket.getOutputStream());
        
            Object in = null;
            try {
                in = this.input.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
    
            if (!(in instanceof Response)) {
                throw new IOException("invalid server answer");
            }
            Response response = (Response) in;
            if (response.is(ResponseType.ACCEPTED)) {
                this.shutdownFlag = new AtomicBoolean(false);
                this.connectionThread = Executors.newSingleThreadExecutor();
                this.connectionThread.execute(this);
            } else if (response.is(ResponseType.ERROR)) {
                throw new IOException("connection rejected. Error code: " + 
                    ((ErrorResponse) response).getErrorType());
            }
        } catch (IOException e) {
            this.close();
            throw e;
        }
    }
    
    
    
    @Override
    public void send(ProtocolObject message) {
        if (this.shutdownFlag.get() || this.output == null) {
            return;
            // throw new IOException("Trying to send over closed connection");
        }
        
        try {
            synchronized (this.output) {
                this.output.writeObject(message);
                this.output.flush();
                this.output.reset();
            }
        } catch (IOException e) {
            logger.error("Error while sending", e);
            this.close();
        }
    }



    @Override
    public void run() {
        try {
            while (!this.shutdownFlag.get()) {
                Object incoming = this.input.readObject();
                
                if (incoming instanceof Ping) {
                    // XXX:
                    Ping ping = (Ping) incoming;
                    System.out.println("Ping: " + (System.currentTimeMillis() - ping.getTimestamp()));
                    this.send(new Pong());
                } else if (incoming instanceof ProtocolObject) {
                    ProtocolObject po = (ProtocolObject) incoming;
                    po.setReceivedAt(System.currentTimeMillis());
                    
                    this.fireObjectReceived(new ObjectReceivedEvent(this, po));
                }
            }
        } catch (ClassNotFoundException e) {
            logger.error("Received invalid class", e);
        } catch (IOException e) {
            if (!this.shutdownFlag.get()) {
                logger.error("IO Error", e);
            }
            
        } finally {
            this.close();
        }
    }
    
    
    
    public void fireObjectReceived(ObjectReceivedEvent e) {
        
    }
    
    
    public void fireConnectionClosed(NetworkEvent e) {
        
    }
    
    
    public void addObjectReceivedListener(ObjectReceivedListener listener) {
        
    }
    
    
    public void removeObjectReceivedListener(ObjectReceivedListener listener) {
        
    }
    
    
    public void addConnectionListener(ConnectionListener listener) {
        
    }
    
    
    public void removeConnectionListener(ConnectionListener listener) {
        
    }
    
    
    @Override
    public boolean isConnected() {
        return !this.shutdownFlag.get() && this.socket.isConnected();
    }
    
    

    @Override
    public boolean isAuthenticated() {
        return false;
    }
    
    
    
    public void close() {
        if (this.shutdownFlag.get()) {
            return;
        }
        
        this.shutdownFlag.set(true);
        try {
            if (this.socket != null) {
                this.socket.close();
            }
        } catch (IOException e) {
            logger.error("Error while closing socket", e);
        }
        
        this.connectionThread.shutdown();
        this.fireConnectionClosed(new NetworkEvent(this));
    }

}
