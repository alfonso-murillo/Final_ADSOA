package monitor;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Monitor {

    static String my_path = "/Users/alfonsomurillo/Documents/CompDistr/";
    static String general_folder = "Operations";
    static String file_name = "";    
    
    static Socket s;
    static DataOutputStream dout;
    
    static ServerSocket ss;
    static DataInputStream din;
            
    /*public static void main(String[] args) {
        String a = FileToBase64(my_path + file_name);
        System.out.println("ARCHIVO GENERADO: " + a);
        SendMessage(9000, a);
        
    }  */
    
    static int my_port;
    static int node_port;
    static int node_handler_port;
    
    
    
    
    static String[] accepted_codes = {"601", "602", "603", "604"};
    
    static String footprint = "";
    
    static BlockingQueue<String> footprints_queue =
            new ArrayBlockingQueue(1024);
    
    static Boolean continue_reading = true;

    //sum_queue.add(send);
    //sum_results_queue.poll(2, TimeUnit.SECONDS);
    
    public static void main(String[] args) {
        
        //Create a random port for the server to use
        Random rand = new Random();
        my_port = rand.nextInt(1000) + 8000;
        
        footprint = StringToSHA_1(GetDate());        
        
        //Search a random node to connect to
        SearchNode();
        
        if (node_port == -1) return;
        System.out.println("MONITOR [" + my_port + "] CONNECTED TO NODE AT PORT " 
                + node_handler_port + " IN PORT " + node_port);
        System.out.println("FOOTPRINT: " + footprint);
        
        ListeningThread lt = new ListeningThread();
        Thread t_lt = new Thread(lt);
        t_lt.start();
        
        while (true) {
            if (continue_reading) {
                continue_reading = false;
                ReadFromConsole();
            }
            try {
                Thread.sleep(500)                    ;
            } catch (InterruptedException ex) {
                Logger.getLogger(Monitor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static void ReadFromConsole() {
        //Enter data using BufferReader 
        BufferedReader reader =  
                   new BufferedReader(new InputStreamReader(System.in)); 
         
        // Reading data using readLine 
        String operation = ""; 
        System.out.println("ELEGIR SUMA/RESTA/MULT/DIV: ");
        try {
            operation = reader.readLine().toUpperCase();
        } catch (IOException ex) {
            System.out.println("Lectura fallida");
        }
        
        footprints_queue = new ArrayBlockingQueue(1024);
        String content_code = "";
        switch (operation) {
            case "SUMA":
                SendMessage(node_port, "501#SUMA");
                operation = "Suma";
                content_code = "511";
                break;
            case "RESTA":
                SendMessage(node_port, "502#RESTA");
                operation = "Resta";
                content_code = "512";
                break;
            case "MULT":
                SendMessage(node_port, "503#MULT");
                operation = "Mult";
                content_code = "513";
                break;
            case "DIV":
                SendMessage(node_port, "504#DIV");
                operation = "Div";
                content_code = "514";
                break;
        }
        //Initalize a JarSender for sending that operation
        JarSender js = new JarSender(operation, content_code);
        Thread t_js = new Thread(js);
        t_js.start();
    }
    
    public static String GetDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date now = new Date();
        String strDate = sdf.format(now);
        return strDate;
    }
    
    public static String StringToSHA_1(String input) 
    { 
        try { 
            // getInstance() method is called with algorithm SHA-1 
            MessageDigest md = MessageDigest.getInstance("SHA-1"); 
  
            // digest() method is called 
            // to calculate message digest of the input string 
            // returned as array of byte 
            byte[] messageDigest = md.digest(input.getBytes()); 
  
            // Convert byte array into signum representation 
            BigInteger no = new BigInteger(1, messageDigest); 
  
            // Convert message digest into hex value 
            String hashtext = no.toString(16); 
  
            // Add preceding 0s to make it 32 bit 
            while (hashtext.length() < 32) { 
                hashtext = "0" + hashtext; 
            } 
  
            // return the HashText 
            return hashtext; 
        } 
        // For specifying wrong message digest algorithms 
        catch (NoSuchAlgorithmException e) { 
            throw new RuntimeException(e); 
        } 
    } 
    
    public static void SearchNode() {
        //Create a vector of all the possible node ports
        Vector<Integer> nodes_possible_ports = new Vector<Integer>();
        for (int i = 6000; i < 7000; i = i + 50) 
            nodes_possible_ports.add(i);
        
        Random r = new Random();
        int try_at;
        int random_i;
        
        while (true) {
            if (nodes_possible_ports.size() < 1) {
                System.out.println("No available ports");
                node_port = -1;
                break;
            }
            //Select a random node port to connect to
            random_i = r.nextInt(nodes_possible_ports.size());
            try_at = nodes_possible_ports.get(random_i);
            
            try {
                s = new Socket("localhost", try_at);
               
                node_handler_port = try_at;
                
                //Send a port to obtain communication
                dout = new DataOutputStream(s.getOutputStream());  
                dout.writeUTF("SERVER_ASKING_FOR_PORTS#" + my_port);  
                dout.flush(); 
                
                //Obtain the available port to communicate with that node
                DataInputStream din = new DataInputStream(s.getInputStream());
                node_port = Integer.parseInt(din.readUTF());
                System.out.println("Port obtained from " + try_at + ": "
                        + node_port);
                dout.close();
                s.close();
                
                break;
                
            } catch(ConnectException e) {
                //There is no node in this port
                //Delete from vector
                nodes_possible_ports.remove(random_i);
            } catch (IOException ex) {
                nodes_possible_ports.remove(random_i);
                System.out.println("Error at SearchNode: " + ex);
            }
        }
    }
    
    public static class ListeningThread implements Runnable {

        String message;
        
        @Override
        public void run() {
            try {
                ss = new ServerSocket(my_port);
                while (true) {
                    s = ss.accept();

                    //Read a message arrived at the server
                    din = new DataInputStream(s.getInputStream());
                    message = din.readUTF();

                    //Manage the received message
                    ManageReceivedMessage(message);

                    din.close();
                    s.close();
                } 

            } catch (IOException ex) {
                System.out.println("Error at main: " + ex);
            }
        }
        
    }
    
    static void ManageReceivedMessage(String m) {
       //System.out.println(m);
       if (Arrays.stream(accepted_codes).anyMatch(m.split("#")[0]::equals)) {
           footprints_queue.add(m.split("#")[1]);
       }
    }
    
    public static class JarSender implements Runnable {

        Boolean try_again;
        List<String> fp_list = new ArrayList<String>();
        String jar_name = "";
        String temp = "";
        String content_code = "";
        
        public JarSender(String jar_name, String content_code) {
            this.jar_name = jar_name;
            this.content_code = content_code;
            try_again = true;
        }
        
        @Override
        public void run() {
            try {
                System.out.println("Obtaining servers footprints...");
                while (try_again) {
                    temp = footprints_queue.poll(3, TimeUnit.SECONDS);
                    if (temp != null) {
                        fp_list.add(temp);
                    } else {
                        try_again = false;
                    }
                }
                if (fp_list.size() != 0) {
                    Collections.sort(fp_list, String.CASE_INSENSITIVE_ORDER);
                    String server_to_send = fp_list.get(0);
                    String path_to_jar = my_path + general_folder + "/" + jar_name + ".jar";
                    System.out.println("Sending file: " + path_to_jar + " to server [" + server_to_send + "]");
                    String message = content_code + "#" + server_to_send + "#" + FileToBase64(path_to_jar);
                    SendMessage(node_port, message);
                    System.out.println("File sent :)");
                    continue_reading = true;
                } else {
                    System.out.println("No servers available");
                    continue_reading = true;
                }
                
            } catch (InterruptedException ex) {
                System.out.println("Error in JarSender: " + ex);
            }
        }
        
    }
    
    static String FileToBase64(String path_to_file) {
        String file_base64 = "";
        FileInputStream fis = null;
        try {
            File file = new File(path_to_file);
            fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024 * 19];
            file_base64 = "";
            try {
                for (int readNum; (readNum = fis.read(buf)) != -1;) {
                    file_base64 += Base64.getEncoder().encodeToString(Arrays.copyOfRange(buf, 0, readNum));
                    //Writes len bytes from the specified byte array starting at offset off to this byte array output stream.
                    System.out.println("Reading " + readNum + " bytes from file");
                }
            } catch (IOException ex) {
                System.out.println("Error 1");
            }
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Monitor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Monitor.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(Monitor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return file_base64;
    }
    
    static void SendMessage(int port, String message_to_send) {
        try {
            s = new Socket("localhost", port);
            dout = new DataOutputStream(s.getOutputStream());

            dout.writeUTF(message_to_send);

            dout.close();
            s.close();
        } catch (IOException ex) {
            Logger.getLogger(Monitor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
