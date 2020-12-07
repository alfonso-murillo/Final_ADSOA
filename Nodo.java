package nodo;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Vector;

public class Nodo {
    
    enum connection_types {
        CONNECTION_HANDLER,
        NODE,
        CLIENT,
        SERVER,
        NONE
    }
    
    //Number of reserved ports
    static final int RESERVED_PORTS = 50;
    
    //Array to store what type of connection is en each port
    static connection_types[] connections = 
            new connection_types[RESERVED_PORTS];
    
    //Array to store the ports to communicate as client
    static int[] servers_ports =
            new int[RESERVED_PORTS];
    
    //Connection handler port
    static int connection_handler_port;
    
    //
    static Socket s;
    static DataInputStream data_input; 
    static DataOutputStream data_output;
    static String connection_message;
    
    static int returned_port;
    
    public static void main(String[] args) {
        for (int i = 0; i < RESERVED_PORTS; i++) 
            connections[i] = connection_types.NONE;  
        
        ServerSocket conn_handler_socket = obtainAvailableSocket(6000, 7000);
        System.out.println("Connection handler port set at: " 
                + connection_handler_port);
        connections[0] = connection_types.CONNECTION_HANDLER;
        
        AskForConnections(6000);
        
        //Handle the conection to the connection handler port
        while(true) {
            try {
                //Detecting a connection attempt
                s = conn_handler_socket.accept();
                System.out.println("Connection attempt from: " + s);
                
                //Indicating that this is a node
                data_output = new DataOutputStream(s.getOutputStream());
                
                //Getting message from connection attempt
                data_input = new DataInputStream(s.getInputStream());
                connection_message = data_input.readUTF();
                System.out.println("Message from connection attempt: " 
                        + connection_message);
                
                //Mannage this connection attempt based on connection_message
                ManageConnectionAttempt(connection_message);
                
                
                data_output.close();
                data_input.close();
                s.close();
            
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }
        
    }
    
    //Obtain available port and return ServerSocket
    static ServerSocket obtainAvailableSocket(int first_port, int last_port) {
        ServerSocket ss = null;
        
        //Try in all possible ports
        for (int i = first_port; i < last_port; i = i + 50) {
            try {
                //Create socket
                Socket s = new Socket("localhost", i);
                
                //Indicate that this is a node searching for a free port
                DataOutputStream dout=new DataOutputStream(s.getOutputStream());  
                dout.writeUTF("NODE_SEARCHING_FREE");  
                dout.flush(); 
                
                DataInputStream din = new DataInputStream(s.getInputStream());
                System.out.println("PORT " + i + " OCCUPIED. MESSAGE: " 
                        + din.readUTF());
                
                dout.close();
                din.close();
                s.close();  
            } catch(ConnectException e) {
                //If connection was not possible the port is available
                System.out.println("AVAILABLE PORT FOUND AT: " + i);
                try {
                    //Create a server socket to listen this port
                    ss = new ServerSocket(i);
                    //Update the connection handler port value
                    connection_handler_port = i;
                    break;
                } catch (IOException ex) {
                    System.out.println(ex);
                }
                break;
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        
        return ss;
    }
    
    //Ask each existing node for a socket to communicate to
    //and give them a socket to communicate with me
    static void AskForConnections(int first_port) {
        int server_port;
        int obtained_port;
        
        //Ask to all existing nodes
        for (int i = first_port; i < connection_handler_port; i = i + 50) {
            try {
                Socket s = new Socket("localhost", i);
                
                //Search for availabe port in this node
                server_port = 0;
                while(connections[server_port] != connection_types.NONE && 
                        server_port < RESERVED_PORTS)
                    server_port++;

                if (server_port == RESERVED_PORTS) {
                    System.out.println("AskForConnections: " +
                            "UNABLE TO ASIGN PORTS.");
                    return;
                }
                
                //Adjust the value of the available port to work as Server Socket
                server_port += connection_handler_port;
            
                //Give the other node a server socket port to communicate
                DataOutputStream dout=new DataOutputStream(s.getOutputStream());  
                dout.writeUTF("NODE_ASKING_FOR_PORTS#" + server_port);  
                dout.flush(); 
                
                //Obtain the available port to communicate with that node
                DataInputStream din = new DataInputStream(s.getInputStream());
                obtained_port = Integer.parseInt(din.readUTF());
                System.out.println("PORT OBTAINED FROM [" + i + "]: "
                        + obtained_port);
                
                //Store the obtained port to communicate later
                servers_ports[server_port - connection_handler_port] = 
                        obtained_port;
                
                //Create temporal vector for each type of ports
                Vector<Integer> temp_clients = new Vector<Integer>();
                Vector<Integer> temp_servers = new Vector<Integer>();
                Vector<Integer> temp_nodes = new Vector<Integer>();
                
                for (int h = 0; h < connections.length; h++) {
                    switch (connections[h]) {
                        case CLIENT:
                            temp_clients.add(servers_ports[h]);
                            break;
                        case SERVER:
                            temp_servers.add(servers_ports[h]);
                            break;
                        case NODE:
                            temp_nodes.add(servers_ports[h]);
                            break;
                    }
                }
                
                //Send the new node's ServerSocket port to my ServerSocket  
                //threads for them to update their routing table
                SendNewConnections(obtained_port, connection_types.NODE); //Ver si es necesario!!!
                
                //Add this new connection to this node's connections array
                connections[server_port - connection_handler_port] = connection_types.NODE;
                
                //Create thread to listen incoming communications from the
                //node in this iteration
                ServerThread st = new ServerThread(server_port, obtained_port, 
                        temp_clients, temp_servers, temp_nodes);
                Thread t = new Thread(st);
                t.start();
                
                /*System.out.println("Connections: " + Arrays.toString(connections));
                System.out.println("Servers_ports" + Arrays.toString(servers_ports));*/
                
                dout.close();
                din.close();
                s.close();  
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
    
    
    //Handle the incoming connections attempts
    static void ManageConnectionAttempt(String message) {
        
        int server_port;
        int obtained_port;
        
        try {
            //If the connection was from a node searching a free space
            //I just tell him this is a node
            if (message.startsWith("NODE_SEARCHING_FREE")) {
                data_output.writeUTF("THIS_IS_NODE");  
                data_output.flush();
            }
            //If the connection is from a node asking for ports
            else if (message.startsWith("NODE_ASKING_FOR_PORTS")) {
                obtained_port = Integer.parseInt(message.split("#")[1]);
                System.out.println("NEW NODE GAVE PORT: " + obtained_port);
                
                //Search a free port to give to this new node
                server_port = 0;
                while(connections[server_port] != connection_types.NONE && 
                        server_port < RESERVED_PORTS)
                    server_port++;

                if (server_port == RESERVED_PORTS) {
                    System.out.println("ManageConnectionAttempt: "
                            + "Unable to assign server ports.");
                    return;
                }
                
                //Send this port to the new node
                server_port += connection_handler_port;
                data_output.writeUTF(String.valueOf(server_port));  
                
                //Register the new server port
                servers_ports[server_port - connection_handler_port] = 
                        obtained_port;
                
                data_output.flush();   
                
                //Create temporal vectors for ports of each type
                Vector<Integer> temp_clients = new Vector<Integer>();
                Vector<Integer> temp_servers = new Vector<Integer>();
                Vector<Integer> temp_nodes = new Vector<Integer>();
                
                for (int h = 0; h < connections.length; h++) {
                    switch (connections[h]) {
                        case CLIENT:
                            temp_clients.add(servers_ports[h]);
                            break;
                        case SERVER:
                            temp_servers.add(servers_ports[h]);
                            break;
                        case NODE:
                            temp_nodes.add(servers_ports[h]);
                            break;
                    }
                }
                
                //Send the new node's ServerSocket port to my ServerSocket  
                //threads for them to update their routing table
                SendNewConnections(obtained_port, connection_types.NODE);
                
                //Register this new connection
                connections[server_port - connection_handler_port] = connection_types.NODE;
                
                //Create ServerSocket thread listening to this new port
                ServerThread st = new ServerThread(server_port, obtained_port,
                        temp_clients, temp_servers, temp_nodes);
                Thread t = new Thread(st);
                t.start();
                
                /*System.out.println("Connections: " + Arrays.toString(connections));
                System.out.println("Servers_ports" + Arrays.toString(servers_ports));*/
            }
            //If the connection is from a client asking for ports
            else if (message.startsWith("CLIENT_ASKING_FOR_PORTS")) {
                obtained_port = Integer.parseInt(message.split("#")[1]);
                System.out.println("NEW CLIENT GAVE PORT: " + obtained_port);
                
                //Search a free port in this node to give to the client
                //to communicate with me
                server_port = 0;
                while(connections[server_port] != connection_types.NONE && 
                        server_port < RESERVED_PORTS)
                    server_port++;

                if (server_port == RESERVED_PORTS) {
                    System.out.println("ManageConnectionAttempt: "
                            + "Unable to assign server ports.");
                    return;
                }
                
                //Send the assigned server port to the client
                server_port += connection_handler_port;
                data_output.writeUTF(String.valueOf(server_port));  
                
                //Register the new ServerSocket port
                servers_ports[server_port - connection_handler_port] = 
                        obtained_port;
                
                data_output.flush();   
                
                //Create temporal vector with each type of ports
                Vector<Integer> temp_clients = new Vector<Integer>();
                Vector<Integer> temp_servers = new Vector<Integer>();
                Vector<Integer> temp_nodes = new Vector<Integer>();
                
                for (int h = 0; h < connections.length; h++) {
                    switch (connections[h]) {
                        case CLIENT:
                            temp_clients.add(servers_ports[h]);
                            break;
                        case SERVER:
                            temp_servers.add(servers_ports[h]);
                            break;
                        case NODE:
                            temp_nodes.add(servers_ports[h]);
                            break;
                    }
                }
                
                //Send the new client's ServerSocket port to my threads
                //for them to update their routing table
                SendNewConnections(obtained_port, connection_types.CLIENT);
                
                //Register this new client port
                connections[server_port - connection_handler_port] = connection_types.CLIENT;
                
                //Create thread to listen to the new client in server_port
                ServerThread st = new ServerThread(server_port, obtained_port,
                        temp_clients, temp_servers, temp_nodes);
                Thread t = new Thread(st);
                t.start();
                
                /*System.out.println("Connections: " + Arrays.toString(connections));
                System.out.println("Servers_ports" + Arrays.toString(servers_ports));*/
            }
            //If the connection is from a server asking for ports
            else if (message.startsWith("SERVER_ASKING_FOR_PORTS")) {
                obtained_port = Integer.parseInt(message.split("#")[1]);
                System.out.println("NEW SERVER GAVE PORT: " + obtained_port);
                
                //Search a free port in this node to give to the new
                //server to communicate with me
                server_port = 0;
                while(connections[server_port] != connection_types.NONE && 
                        server_port < RESERVED_PORTS)
                    server_port++;

                if (server_port == RESERVED_PORTS) {
                    System.out.println("ManageConnectionAttempt: "
                            + "Unable to assign server ports.");
                    return;
                }
                
                //Send this ServerSocket port to the new server
                server_port += connection_handler_port;
                data_output.writeUTF(String.valueOf(server_port));  
                
                //Register the server's port to communicate with it
                servers_ports[server_port - connection_handler_port] = 
                        obtained_port;
                
                data_output.flush();   
                
                //Create temporal array of each kind of port
                Vector<Integer> temp_clients = new Vector<Integer>();
                Vector<Integer> temp_servers = new Vector<Integer>();
                Vector<Integer> temp_nodes = new Vector<Integer>();
                
                for (int h = 0; h < connections.length; h++) {
                    switch (connections[h]) {
                        case CLIENT:
                            temp_clients.add(servers_ports[h]);
                            break;
                        case SERVER:
                            temp_servers.add(servers_ports[h]);
                            break;
                        case NODE:
                            temp_nodes.add(servers_ports[h]);
                            break;
                    }
                }
                
                //Send this new server's ServerSocket port to all my
                //ServerSocket threads for them to update their routing table
                SendNewConnections(obtained_port, connection_types.SERVER);
                
                //Register this new server connection
                connections[server_port - connection_handler_port] = connection_types.SERVER;
                
                //Create a thread to for listening to this new server
                ServerThread st = new ServerThread(server_port, obtained_port,
                        temp_clients, temp_servers, temp_nodes);
                Thread t = new Thread(st);
                t.start();
                
                /*System.out.println("Connections: " + Arrays.toString(connections));
                System.out.println("Servers_ports" + Arrays.toString(servers_ports));*/
            }
        } catch (Exception ex) {
            System.out.println("Error at MannageConnectionAttempt: " + ex);
        }
    }
    
    //This function is invoked every time a new ServerSocket port is 
    //given to the node to communicate with another one so it can tell
    //all the threads (each listening to one specific port) to update
    //their routing table
    static void SendNewConnections(int port, connection_types con_t) {
        Socket s1;
        DataOutputStream dout_new_conn;
        
        
        for (int i = 0; i < connections.length; i++) {
            if (connections[i] != connection_types.CONNECTION_HANDLER
                    && connections[i] != connection_types.NONE) {
                
                try {
                    //Send a different package depending on the connection type
                    s1 = new Socket("localhost", connection_handler_port + i);
                    dout_new_conn = new DataOutputStream(s1.getOutputStream());
                    if (null != con_t) switch (con_t) {
                        case CLIENT:
                            dout_new_conn.writeUTF("NEW_CLIENT#" + port);
                            break;
                        case SERVER:
                            dout_new_conn.writeUTF("NEW_SERVER#" + port);
                            break;
                        case NODE:
                            dout_new_conn.writeUTF("NEW_NODE#" + port);
                            break;
                        default:
                            break;
                    }
                    dout_new_conn.close();
                    s1.close();
                } catch (IOException ex) {
                    System.out.println("Problem at SendNewConnections. " +
                            ex);
                }
            }
        }
    }
    
}

//This class is in charge of listening to the ServerSockets of the node
class ServerThread implements Runnable {
    static Vector<Integer> clients_ports;
    static Vector<Integer> servers_ports;
    static Vector<Integer> nodes_ports;
    
    int port_number;
    ServerSocket ss;
    Socket s;
    DataOutputStream data_output;
    DataInputStream data_input;
    String message;
    int my_client_comm_port;
    
    /*
    port - port to which the server is going to listen to the client
    my_client_comm_port - port with which I can communicate with my client
    clients_ports - vector with ports through which I can communicate with clients
    servers_ports - vector with ports through which I can communicate with servers
    nodes_ports - vector with ports through which I can communicate with nodes
    */
    
    ServerThread(int port, int my_client_comm_port, Vector<Integer> clients_ports, Vector<Integer> servers_ports,
                    Vector<Integer> nodes_ports) {
        port_number = port;
        this.my_client_comm_port = my_client_comm_port;
        /*this.clients_ports.add(clients_ports);
        this.servers_ports.add(servers_ports);
        this.nodes_ports.add(nodes_ports);*/
        this.clients_ports = new Vector<Integer>(clients_ports);
        this.servers_ports = new Vector<Integer>(servers_ports);
        this.nodes_ports = new Vector<Integer>(nodes_ports);
        
        System.out.println("THREAD [" + port_number + "] CREATED FOR CLIENT AT: " 
                + this.my_client_comm_port);
    }
    
    @Override
    public void run() {
        
        try {
            ss = new ServerSocket(port_number);
            
            while(true) {
                System.out.println("THREAD LISTENING AT PORT " + port_number);
                s = ss.accept();
                
                System.out.println("CONNECTION ATTEMPT [" + 
                        port_number + "] FROM: " + s);
                
                //Indicating that this is a node
                data_output = new DataOutputStream(s.getOutputStream());
                
                //Getting message from connection attempt
                data_input = new DataInputStream(s.getInputStream());
                message = data_input.readUTF();
                System.out.println("MESSAGE RECEIVED AT [" + port_number +
                        "]: " + message);
                
                //If message received is to indicate a new connection from
                //the SendNewConnections method
                if (message.startsWith("NEW")) {
                    switch (message.split("#")[0]) {
                        case "NEW_NODE":
                            nodes_ports.add(Integer.parseInt(message.split("#")[1]));
                            break;
                        case "NEW_CLIENT":
                            clients_ports.add(Integer.parseInt(message.split("#")[1]));
                            break;
                        case "NEW_SERVER":
                            servers_ports.add(Integer.parseInt(message.split("#")[1]));
                            break;
                    }
                    /*System.out.println("Clients vector: " + port_number + " " + clients_ports);
                    System.out.println("Servers vector: " + port_number + " " +  servers_ports);
                    System.out.println("Nodes vector: " + port_number + " " +  nodes_ports);*/
                }
                //If this thread is listening from a node
                else if (my_client_comm_port >= 6000 
                        && my_client_comm_port < 7000) {
                    
                    //Send message to all clients
                    for (int c = 0; c < clients_ports.size(); c++) {
                        SendMessageToPort smtp = 
                                new SendMessageToPort(clients_ports.get(c), 
                                        message);
                        Thread t = new Thread(smtp);
                        t.start();
                    }
                    //Send message to all servers
                    for (int s = 0; s < servers_ports.size(); s++) {
                        SendMessageToPort smtp = 
                                new SendMessageToPort(servers_ports.get(s), 
                                        message);
                        Thread t = new Thread(smtp);
                        t.start();
                    }
                }
                //If this thread is listening from a No-node
                else {
                    //Send message to all clients
                    for (int c = 0; c < clients_ports.size(); c++) {
                        SendMessageToPort smtp = 
                                new SendMessageToPort(clients_ports.get(c), 
                                        message);
                        Thread t = new Thread(smtp);
                        t.start();
                    }
                    //Send message to all servers
                    for (int s = 0; s < servers_ports.size(); s++) {
                        SendMessageToPort smtp = 
                                new SendMessageToPort(servers_ports.get(s), 
                                        message);
                        Thread t = new Thread(smtp);
                        t.start();
                    }
                    //Send message to all nodes
                    for (int n = 0; n < nodes_ports.size(); n++) {
                        SendMessageToPort smtp = 
                                new SendMessageToPort(nodes_ports.get(n), 
                                        message);
                        Thread t = new Thread(smtp);
                        t.start();
                    }
                }
                
                data_output.close();
                data_input.close();
                s.close();
            
            }
            
        } catch (IOException ex) {
            System.out.println("Error at ServerThread: " + ex);
        }
    }
    
}

//This class is in charge to communicate to one specific port and send a message
class SendMessageToPort implements Runnable {

    int port;
    String message;
    DataOutputStream dout_message;
    
    SendMessageToPort(int port, String message) {
        this.port = port;
        this.message = message;
    }
    
    @Override
    public void run() {
        Socket sending_socket;
        
        try {
            sending_socket = new Socket("localhost", port);
            dout_message = 
                    new DataOutputStream(sending_socket.getOutputStream());
            dout_message.writeUTF(message);
            dout_message.flush();
            dout_message.close();
            
            sending_socket.close();
            
        } catch (IOException ex) {
            System.out.println("Error at SendMessageToPort: " + ex);
        }
    }
}

