package TcpServer;

import Data.Packet;

import java.util.List;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.net.SocketException;
import java.io.InputStream;
import java.io.BufferedOutputStream;
import java.util.ArrayList;


public class Server{
    static List<ClientData>_clients = new ArrayList<>();
    static ServerSocket listenerSocket;
    static final int port =4242;

    public static void main(String[] args) throws Exception{
        //pt = new PacketType();
        System.out.println("Starting TcpServer.Server on: " + Packet.getIp4Address());
        listenerSocket = new ServerSocket();
        InetSocketAddress ip = new InetSocketAddress(Packet.getIp4Address(), port);
        listenerSocket.bind(ip);
        ListenThread listen = new ListenThread(listenerSocket);
        listen.start();
    }

    public static void dataManager(Packet p) throws Exception{
        switch(p.packetType){
            case Registeration: {
                System.out.println("A client is connected");
                break;
            }
            case Chat: {
                List<ClientData> _disconnectedClients = new ArrayList<>();
                byte[] buffer;

                BufferedOutputStream out;

                buffer = p.toBytes();
                for(ClientData c : _clients)
                {
                    try{
                        if(c.clientSocket.isConnected())
                        {
                            out = new BufferedOutputStream(c.clientSocket.getOutputStream());
                            out.write(buffer);
                            out.flush();
                        }
                        else
                        {
                            _disconnectedClients.add(c);
                        }

                    }
                    catch(SocketException e)
                    {
                        _disconnectedClients.add(c);
                    }
                }
                for(ClientData dc : _disconnectedClients)
                {
                    _clients.remove(dc);
                }
                _disconnectedClients.clear();
                break;
            }
        }
    }

}

class ClientData{
    public Socket clientSocket;
    public  String id;
    public ClientData(Socket clientSocket){
        this.clientSocket = clientSocket;
        id = UUID.randomUUID().toString();
        ClientThread clientTh = new ClientThread(clientSocket);
        clientTh.start();
    }
    public void sendRegisterationPacket() throws Exception
    {
        BufferedOutputStream out = new BufferedOutputStream(clientSocket.getOutputStream());
        Packet p = new Packet(Data.Packet.PacketType.Registeration,1);
        p.Gdata.add(id);
        out.write(p.toBytes());
        out.flush();
    }
}

class ListenThread extends Thread{
    ServerSocket listenerSocket;
    public ListenThread(ServerSocket listenerSocket){
        this.listenerSocket = listenerSocket;
    }
    public void run(){
        Socket clientSocket;
        ClientData clientData;
        for(;;){
            try{
                clientSocket = listenerSocket.accept();
                clientData = new ClientData(clientSocket);
                clientData.sendRegisterationPacket();
                Server._clients.add(clientData);
            }
            catch(Exception e){
                System.out.println(e.toString());
            }
        }
    }
}

class ClientThread extends Thread{
    Socket clientSocket;
    byte[] buffer;
    int readBytes;
    public ClientThread(Object cSocket){
        clientSocket = (Socket)cSocket;
    }
    public void run(){
        for(;;){
            try{
                buffer = new byte[clientSocket.getReceiveBufferSize()];
                InputStream scktIs = clientSocket.getInputStream();
                scktIs.read(buffer);
                readBytes = buffer.length;
                if(readBytes > 0){
                    Packet packet = new Packet(buffer);
                    Server.dataManager(packet);
                }
            }
            catch(SocketException e){
                System.out.println("A client is disconnected");
                break;
            }
            catch(Exception e){
                System.out.println(e.toString());
            }
        }
    }
}

