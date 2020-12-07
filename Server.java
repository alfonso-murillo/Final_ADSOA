package server;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import static org.apache.commons.io.FileUtils.copyDirectory;


public class Server {

    static int my_port;
    static int node_port;
    static int node_handler_port;
    
    static Socket s;
    static ServerSocket ss;
    
    static DataInputStream din;
    static DataOutputStream dout;
    
    static String message;
    
    static String[] accepted_codes = {"500", "1", "2", "3", "4",
                                        "501", "502", "503", "504", 
                                        "511", "512", "513", "514"};
    static Boolean[] accept_this_code = {true, false, false, false, false,
                                            true, true, true, true, 
                                            true, true, true, true};
    
    static String footprint = "";
    
    static String my_path = "/Users/alfonsomurillo/Documents/CompDistr/";
    static String general_folder = "Server";
    static String my_folder = "Server";
    
    public static void main(String[] args) {
        
        //Create a random port for the server to use
        Random rand = new Random();
        my_port = rand.nextInt(1000) + 8000;
        
        footprint = StringToSHA_1(GetDate());        
        
        //Search a random node to connect to
        SearchNode();
        
        if (node_port == -1) return;
        System.out.println("SERVER [" + my_port + "] CONNECTED TO NODE AT PORT " 
                + node_handler_port + " IN PORT " + node_port);
        System.out.println("FOOTPRINT: " + footprint);
        
        Boolean all_false = true;
        //JOptionPane.showMessageDialog(null, "HOLA num args: " + args.length, "InfoBox: ", JOptionPane.INFORMATION_MESSAGE);
        if (args.length > 1) {
            for (int ind = 0; ind < args.length; ind++) {
                //System.out.println(args[ind] +"!!!!");
                //JOptionPane.showMessageDialog(null, "HOLA etraaa ARGS" + args[ind], "InfoBox: ", JOptionPane.INFORMATION_MESSAGE);
                if (args[ind].trim().startsWith("Server"))
                    my_folder = args[ind];
                else 
                    switch (args[ind].trim()) {
                        case "1":
                            all_false = false;
                            accept_this_code[1] = true;
                            break;
                        case "2":
                            all_false = false;
                            accept_this_code[2] = true;
                            break;
                        case "3":
                            all_false = false;
                            accept_this_code[3] = true;
                            break;
                        case "4":
                            all_false = false;
                            accept_this_code[4] = true;
                            break;
                    }
            }
        }
        if (all_false)
            for (int ind = 1; ind <= 4; ind++)
                accept_this_code[ind] = false;
        
        
        
        String argumentos = "null";
            for (int ind = 1; ind <= 4; ind++)
                if (accept_this_code[ind])
                    argumentos+=ind;
        //JOptionPane.showMessageDialog(null, "HOLA" + argumentos, "InfoBox: ", JOptionPane.INFORMATION_MESSAGE);
        
        //Listen to my_port
        try {
            ss = new ServerSocket(my_port);
            while (true) {
                s = ss.accept();
                
                //Read a message arrived at the server
                din = new DataInputStream(s.getInputStream());
                message = din.readUTF();
                
                //Manage the received message
                ManageReceivedMessage();
                
                din.close();
                s.close();
            } 
                
        } catch (IOException ex) {
            System.out.println("Error at main: " + ex);
        }
        
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
    
    static void ManageReceivedMessage() {
        float num1, num2;
        String receipt_message = "";
        String result_message = "";
        String r_message;
        
        //System.out.println("------------------------------ SERVER RECEIVING MESSAGE ------------------------------");
        r_message = message;
        System.out.println("MESSAGE RECEIVED: " + r_message);
        if (Arrays.stream(accepted_codes).anyMatch(r_message.split("#")[0]::equals)) {
            
            //System.out.println("Acepta mensaje");
            
            if (r_message.split("#")[0].equals("500")) {
                //Duplicar servidor
                if (r_message.split("#")[1].equals(footprint)) {
                    System.out.println("DUPLICATING THIS SERVER [" + footprint + "]");
                    DuplicateServer();
                }
            } else if (Integer.parseInt(r_message.split("#")[0]) < 10
                    && accept_this_code[Integer.parseInt(r_message.split("#")[0])]) {
                try {
                    //Send receipt message
                    receipt_message = String.valueOf(
                            Integer.parseInt(r_message.split("#")[0]) + 100) + "#";
                    receipt_message += r_message.split("#")[1] + "#" + footprint;
                    
                    //System.out.println("Mandando recibo: " + receipt_message);
                    s = new Socket("localhost", node_port);

                    dout = new DataOutputStream(s.getOutputStream());

                    dout.writeUTF(receipt_message);

                    dout.close();
                    s.close();
                    try {                    
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    //Send result
                    SendResult(r_message);

                } catch (IOException ex) {
                    System.out.println("Error at OperationThread" + ex);
                }
            } else if (Integer.parseInt(r_message.split("#")[0]) > 500 && 
                    Integer.parseInt(r_message.split("#")[0]) < 505 &&
                    !accept_this_code[Integer.parseInt(r_message.split("#")[0]) - 500]) {
                //Accepts 501-504 codes
                System.out.println("ACCEPTING REQUEST TO RECEIVE OPERATION "+ r_message.split("#")[1] +" | SENDING FOOTPRINT:"); 
                try {
                    //Send receipt message
                    receipt_message = String.valueOf(
                            Integer.parseInt(r_message.split("#")[0]) + 100) + "#";
                    receipt_message += footprint;
                    
                    //System.out.println("Mandando recibo: " + receipt_message);
                    s = new Socket("localhost", node_port);

                    dout = new DataOutputStream(s.getOutputStream());

                    dout.writeUTF(receipt_message);

                    dout.close();
                    s.close();
                    System.out.println(receipt_message);
                    System.out.println(receipt_message);
                    try {                    
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                } catch (IOException ex) {
                    System.out.println("Error at OperationThread" + ex);
                }
            } else if (Integer.parseInt(r_message.split("#")[0]) > 510 && 
                    Integer.parseInt(r_message.split("#")[0]) < 520 &&
                    !accept_this_code[Integer.parseInt(r_message.split("#")[0]) - 510]) {
                String op_received = "";
                int index_operation = 0;
                if (r_message.split("#")[1].equals(footprint)) {
                    
                    switch (r_message.split("#")[0]) {
                        case "511":
                            op_received = "Suma";
                            index_operation = 1;
                            break;
                        case "512":
                            op_received = "Resta";
                            index_operation = 2;
                            break;
                        case "513":
                            op_received = "Mult";
                            index_operation = 3;
                            break;
                        case "514":
                            op_received = "Div";
                            index_operation = 4;
                            break;
                    }
                    
                    System.out.println("Accepting operation...");
                    //Obtaining file and saving it
                    byte[] decode = Base64.getDecoder().decode(r_message.split("#")[2]);
                    File someFile = new File(my_path + my_folder + "/" + op_received + ".jar");
                    FileOutputStream fos;
                    try {
                        fos = new FileOutputStream(someFile);
                        fos.write(decode);
                        fos.flush();
                        fos.close();
                        System.out.println("OPERATION SUCCESSFULLY RECEIVED");
                        accept_this_code[index_operation] = true;
                    } catch (FileNotFoundException ex) {
                        System.out.println("Error when saving file: FileNotFound: " + ex);
                    } catch (IOException ex) {
                        System.out.println("Error when writing file: " + ex);
                    }
                    
                }
                
            }
        
        }
        else {
            System.out.println("Mensaje invalido");
        }
        
        //System.out.println("--------------------------------------------------------------------------------------");
      
    }
    
    static void SendResult(String m) {
        String ans = ""; 
        float num1 = Float.parseFloat(m.split("#")[3]), num2 = Float.parseFloat(m.split("#")[4]); 
        URL[] classLoaderUrls;
        URLClassLoader urlClassLoader;
        Class<?> clazz;
        String class_name = "";
            switch (m.split("#")[0]) {
                case "1":
                    class_name = "Suma";
                    break;
                case "2":
                    class_name = "Resta";
                    break;
                case "3":
                    class_name = "Mult";
                    break;
                case "4":
                    class_name = "Div";
                    break;
            }   
	
        //Operation classes constructor's param String event, String footprint, float n1, float n2, int port
        try {
            System.out.println("CALLING " + class_name + ".jar");
            classLoaderUrls = new URL[]{new URL("file:////Users/alfonsomurillo/Documents/CompDistr/" + my_folder + "/" + class_name + ".jar")};
            urlClassLoader = new URLClassLoader(classLoaderUrls);
            clazz = urlClassLoader.loadClass(class_name.toLowerCase()+ "." +class_name);
            Constructor<?> ct = clazz.getConstructor(String.class, String.class, float.class, float.class, int.class);
            ct.newInstance( m.split("#")[1], m.split("#")[2], Float.parseFloat(m.split("#")[3]), 
                    Float.parseFloat(m.split("#")[4]), node_port);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
	} catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
	} catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
	} catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
	} catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
	} catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
    
    static void DuplicateServer() {
        String program_to_run = "";
        String parameters = "";
        File temp = new File(my_path + general_folder);
	int i = 1;
	while(temp.exists()) {
            temp = new File(my_path + general_folder + i);
            i++;
        }
        i--;
        try {
            copyDirectory(new File(my_path + my_folder), temp, false);
            program_to_run = my_path + general_folder + i + "/Server.jar";
            parameters = "";
            List<String> commands_list = new ArrayList<String>();
            commands_list.add("java");
            commands_list.add("-jar");
            commands_list.add(program_to_run);
            commands_list.add(general_folder + i);
            
            for (int ind = 1; ind <= 4; ind++)
                if (accept_this_code[ind]) {
                    parameters += " " + accepted_codes[ind];
                    commands_list.add(String.valueOf(ind));
                }
            System.out.println("PROGRAM TO RUN: " + program_to_run);
            //Runtime.getRuntime().exec(new String[] {"java", "-jar", program_to_run, general_folder + i, parameters});
            String[] itemsArray = new String[commands_list.size()];
            itemsArray = commands_list.toArray(itemsArray);
            Runtime.getRuntime().exec(itemsArray);
        } catch (IOException ex) {
            System.out.println("ERROR DUPLICATING SERVER");
        }
    }
    
}

