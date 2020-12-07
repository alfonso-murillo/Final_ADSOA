package calculadoracliente;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.TextField;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.*;


public class CalculadoraCliente extends Application {

    static enum operations {
        NONE,
        SUM,
        SUBS,
        MULT,
        DIV
    }
    
    static int node_port;
    static int node_handler_port;
    static Socket s;
    static ServerSocket ss;
    static int my_port;
    static DataInputStream din;
    static DataOutputStream dout;
    static String message;
    
    static operations actual_operation;
    
    static String[] accepted_codes = {"5", "6", "7", "8", 
                                        "101", "102", "103", "104"};
    
    static String screen_text = "";
    static String num1 = "";
    static String num2 = "";
    static boolean is_num1 = true;
    
    static final String[][] button_labels = {
        { "7", "8", "9", "/", "", ""},
        { "4", "5", "6", "*", "", ""},
        { "1", "2", "3", "-", "", ""},
        { "0", "C", ".", "+", "", "SEND"},
    };
    
    static TextField screen;
    static TilePane buttons;
    static TextArea sum, subs, mult, div;
    static Label sum_t, subs_t, mult_t, div_t;
    
    static String footprint = "";
    
    static Vector<OperationTableElement> sum_table = 
            new Vector<OperationTableElement>();
    static Vector<OperationTableElement> subs_table = 
            new Vector<OperationTableElement>();
    static Vector<OperationTableElement> mult_table = 
            new Vector<OperationTableElement>();
    static Vector<OperationTableElement> div_table = 
            new Vector<OperationTableElement>();
    
    //Queues to store the operations
    static BlockingQueue<String> sum_queue =
            new ArrayBlockingQueue(1024);
    static BlockingQueue<String> subs_queue =
            new ArrayBlockingQueue(1024);
    static BlockingQueue<String> mult_queue =
            new ArrayBlockingQueue(1024);
    static BlockingQueue<String> div_queue =
            new ArrayBlockingQueue(1024);
    
    //Queues to store the operations' receipts
    static BlockingQueue<String> sum_receipts_queue =
            new ArrayBlockingQueue(1024);
    static BlockingQueue<String> subs_receipts_queue =
            new ArrayBlockingQueue(1024);
    static BlockingQueue<String> mult_receipts_queue =
            new ArrayBlockingQueue(1024);
    static BlockingQueue<String> div_receipts_queue =
            new ArrayBlockingQueue(1024);
    
    //Queues to store the received results
    static BlockingQueue<String> sum_results_queue =
            new ArrayBlockingQueue(1024);
    static BlockingQueue<String> subs_results_queue =
            new ArrayBlockingQueue(1024);
    static BlockingQueue<String> mult_results_queue =
            new ArrayBlockingQueue(1024);
    static BlockingQueue<String> div_results_queue =
            new ArrayBlockingQueue(1024);
    
    static int sum_folio = 0, subs_folio = 0, mult_folio = 0, div_folio = 0;
    
    public static void main(String[] args) {
        Random rand = new Random();
        //Obtain a random port for this client
        my_port = rand.nextInt(1000) + 7000;
        System.out.println("CLIENT CREATED AT PORT: " + my_port);
        
        footprint = StringToSHA_1(GetDate());
        System.out.println("FOOTPRINT: " + footprint);
        
        //Search a node to connect to
        SearchNode();
        
        System.out.println("CLIENT [" + my_port + "] CONNECTED TO NODE AT " 
                + node_handler_port + " IN PORT " + node_port);
        
        //Start a thread to listen to my_port
        ServerThread st = new ServerThread();
        Thread t = new Thread(st);
        t.start();
        
        //Create a thread for managing sums
        OperationThread sum_ot = new OperationThread("SUM");
        Thread t_sum = new Thread(sum_ot);
        t_sum.start();
        
        //Create a thread for managing subs
        OperationThread subs_ot = new OperationThread("SUBS");
        Thread t_subs = new Thread(subs_ot);
        t_subs.start();
        
        //Create a thread for managing mult
        OperationThread mult_ot = new OperationThread("MULT");
        Thread t_mult = new Thread(mult_ot);
        t_mult.start();
        
        //Create a thread for managing div
        OperationThread div_ot = new OperationThread("DIV");
        Thread t_div = new Thread(div_ot);
        t_div.start();
        
        actual_operation = operations.NONE;
        
        launch(args);
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
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Calculadora Cliente");
        
        CreateScreen();
        CreateButtons();
        sum = CreateTextArea();
        subs = CreateTextArea();
        mult = CreateTextArea(); 
        div = CreateTextArea();
        sum_t = CreateLabel("ADDITIONS");
        subs_t = CreateLabel("SUBSTRACTIONS");
        mult_t = CreateLabel("MULTIPLICATIONS"); 
        div_t = CreateLabel("DIVISIONS");
         
        
        primaryStage.initStyle(StageStyle.DECORATED);
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(CreateMyScene()));
        primaryStage.show();
    }
    
    VBox CreateMyScene() {
        final VBox this_scene = new VBox(20);
        this_scene.setAlignment(Pos.CENTER);
        this_scene.setStyle(
                "-fx-background-color: #d5dce0; "
                + "-fx-padding: 20; "
                + "-fx-font-size: 20;");
        this_scene.getChildren().setAll(screen, buttons, sum_t, sum, 
                subs_t, subs, mult_t, mult, div_t, div);
        screen.prefWidthProperty().bind(buttons.widthProperty());
        return this_scene;
    }
    
    void CreateScreen() {
        screen = new TextField();
        screen.setStyle("-fx-background-color: #bdcfd9;");
        screen.setAlignment(Pos.CENTER_RIGHT);
        screen.setEditable(false);
        screen.setText(screen_text);
        if (node_port == -1) screen.setText("UNABLE TO CONNECT");
    }
    
    void CreateButtons() {
        buttons = new TilePane();
        buttons.setVgap(7);
        buttons.setHgap(7);
        buttons.setPrefColumns(button_labels[0].length);
        for (String[] r: button_labels) 
            for (String s: r)
                buttons.getChildren().add(CreateButton(s));
    }
    
    Button CreateButton(final String s) {
        Button b = new Button(s);
        b.setStyle("-fx-base: #9db4c2;");
        b.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        b.setOnAction(event);
        return b;
    }
    
    TextArea CreateTextArea() {
        TextArea ta = new TextArea();
        ta.setStyle("-fx-font-size: 0.5em;");
        ta.setPrefHeight(50);
        return ta;
    }
    
    Label CreateLabel(String label_text) {
        Label label = new Label(label_text);
        label.setStyle("-fx-font-size: 0.5em;");
        return label;
    }
    
    EventHandler<ActionEvent> event = new EventHandler<ActionEvent>() { 
        public void handle(ActionEvent e)
        {
            String buttons_text = ((Button)e.getSource()).getText();
            
            switch (buttons_text) {
                case "C":
                    screen_text = "";
                    screen.setText(screen_text);
                    actual_operation = operations.NONE;
                    num1 = "";
                    num2 = "";
                    is_num1 = true;
                    break;
                case "SEND":
                    //Send expression to the node
                    SendOperation();
                    screen_text = "";
                    screen.setText(screen_text);
                    actual_operation = operations.NONE;
                    num1 = "";
                    num2 = "";
                    is_num1 = true;
                    //TODO:
                    break;
                case "+":
                    if (is_num1) {
                        screen_text += buttons_text;
                        screen.setText(screen_text);
                        actual_operation = operations.SUM;
                        is_num1 = false;
                    }
                    break;
                case "-":
                    if (is_num1) {
                        screen_text += buttons_text;
                        screen.setText(screen_text);
                        actual_operation = operations.SUBS;
                        is_num1 = false;
                    }
                    break;
                case "*":
                    if (is_num1) {
                        screen_text += buttons_text;
                        screen.setText(screen_text);
                        actual_operation = operations.MULT;
                        is_num1 = false;
                    }
                    break;
                case "/":
                    if (is_num1) {
                        screen_text += buttons_text;
                        screen.setText(screen_text);
                        actual_operation = operations.DIV;
                        is_num1 = false;
                    }
                    break;
                default:
                    screen_text += buttons_text;
                    screen.setText(screen_text);
                    if (is_num1)
                        num1 += buttons_text;
                    else
                        num2 += buttons_text;
                    break;
            }
        }
    };
 
    //This method searches a node to connect to
    static void SearchNode() {
        //Generate a vector with all available ports
        Vector<Integer> nodes_possible_ports = new Vector<Integer>();
        for (int i = 6000; i < 7000; i = i + 50) 
            nodes_possible_ports.add(i);
        
        //Try to connect to a random one
        Random r = new Random();
        int try_at;
        int random_i;
        
        while (true) {
            if (nodes_possible_ports.size() < 1) {
                System.out.println("No available ports");
                node_port = -1;
                break;
            }
            random_i = r.nextInt(nodes_possible_ports.size());
            try_at = nodes_possible_ports.get(random_i);
            
            try {
                s = new Socket("localhost", try_at);
                
                node_handler_port = try_at;
                
                //Send a port to obtain communication
                dout = new DataOutputStream(s.getOutputStream());  
                dout.writeUTF("CLIENT_ASKING_FOR_PORTS#" + my_port);  
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
    
    //This class listens to the client's port
    static class ServerThread implements Runnable {

        @Override
        public void run() {
            try {
                ss = new ServerSocket(my_port);
                while (true) {
                    s = ss.accept();

                    //
                    din = new DataInputStream(s.getInputStream());
                    message = din.readUTF();

                    //
                    ManageReceivedMessage();

                    din.close();
                    s.close();
                } 

            } catch (IOException ex) {
                System.out.println("Error at main: " + ex);
            }
        }
        
    }
    
    //This method manages the receives messages to see it it is
    //useful for the client or not
    static void ManageReceivedMessage() {
        float num1, num2, result;
        System.out.println("------------------------------ RECEIVING MESSAGE ------------------------------");
        
        System.out.println("MESSAGE: " + message);
        if (message.startsWith(accepted_codes[0] + "#")) {
            //5# - Sum result
            if (message.split("#")[2].equals(footprint)) {
                sum_results_queue.add(message);
            }
        }
        else if (message.startsWith(accepted_codes[1] + "#")) {
            //6# - Substraction result
            if (message.split("#")[2].equals(footprint)) {
                subs_results_queue.add(message);
            }
        }
        else if (message.startsWith(accepted_codes[2] + "#")) {
            //7# - Multiplication result
            if (message.split("#")[2].equals(footprint)) {
                mult_results_queue.add(message);
            }
        }
        else if (message.startsWith(accepted_codes[3] + "#")) {
            //8# - Division result
            if (message.split("#")[2].equals(footprint)) {
                div_results_queue.add(message);
            }
        }
        else if (message.startsWith(accepted_codes[4] + "#")) {
            //101# Receipt sum
            sum_receipts_queue.add(message);
        }
        else if (message.startsWith(accepted_codes[5] + "#")) {
            //102# Receipt subs
            subs_receipts_queue.add(message);
        }
        else if (message.startsWith(accepted_codes[6] + "#")) {
            //103# Receipt mult
            mult_receipts_queue.add(message);
        }
        else if (message.startsWith(accepted_codes[7] + "#")) {
            //104# Receipt div
            div_receipts_queue.add(message);
        }
        else {
            //Unknown message
            System.out.println("NON-COMPATIBLE MESSAGE.");
        }
        System.out.println("-------------------------------------------------------------------------------\n");
    }
    
    //This method makes the package and sends it to the node
    static void SendOperation() {
        String send;
        
        send = "#" + footprint+ "#" + num1 + "#" + num2;
        
        String event;
        event = footprint;
        
        switch (actual_operation) {
            case SUM:
                sum_folio++;
                event += String.valueOf(sum_folio);
                event += num1 + "+" + num2;
                event = StringToSHA_1(event);  
                sum_table.add(new OperationTableElement(sum_folio,
                        num1 + "+" + num2, event));
                send = event + send;
                send = "1#" + send;
                
                System.out.println("ADDING TO SUM QUEUE: " + send);
                sum_queue.add(send);
                break;
            case SUBS:
                subs_folio++;
                event += String.valueOf(subs_folio);
                event += num1 + "-" + num2;
                event = StringToSHA_1(event);
                subs_table.add(new OperationTableElement(subs_folio,
                        num1 + "-" + num2, event));
                send = event + send;
                send = "2#" + send;
                
                System.out.println("ADDING TO SUBS QUEUE: " + send);
                subs_queue.add(send);
                break;
            case MULT:
                mult_folio++;
                event += String.valueOf(mult_folio);
                event += num1 + "*" + num2;
                event = StringToSHA_1(event);
                mult_table.add(new OperationTableElement(mult_folio,
                        num1 + "*" + num2, event));
                send = event + send;
                send = "3#" + send;
                
                System.out.println("ADDING TO MULT QUEUE: " + send);
                mult_queue.add(send);
                break;
            case DIV:
                div_folio++;
                event += String.valueOf(div_folio);
                event += num1 + "/" + num2;
                event = StringToSHA_1(event);
                div_table.add(new OperationTableElement(div_folio,
                        num1 + "/" + num2, event));
                send = event + send;
                send = "4#" + send;
                
                System.out.println("ADDING TO DIV QUEUE: " + send);
                div_queue.add(send);
                break;
        }
        
    }
    
    
    //This class sends a message to a specific port
    static class SendMessage implements Runnable {
    
        String message;
        int port;
        Socket s;
        DataOutputStream dout;

        SendMessage(String message, int port) {
            this.message = message;
            this.port = port;
        }

        @Override
        public void run() {
            try {
                s = new Socket("localhost", port);

                dout = new DataOutputStream(s.getOutputStream());

                dout.writeUTF(message);
                dout.flush();

                dout.close();
                s.close();

            } catch (IOException ex) {
                Logger.getLogger(SendMessage.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
    
    //
    static class OperationThread implements Runnable {
        String this_operation;
        String received_operation;
        
        int min_receipt = 3; int cont_receipt = 0;
        int max_attempts = 1; int cont_attempts = 0;
        int max_lectures = 2; int cont_lectures = 0;
        int wait_for_next_attempt = 1000;
        Boolean try_again;
        Queue<String> receipt_server_footprint =
                new LinkedList<>();
        
        String event;
        
        String actual_receipt;
        String result_message;
        String r_n1, r_n2, result;
        
        OperationThread(String this_operation) {
            this.this_operation = this_operation;
        }
        
        @Override
        public void run() {
            System.out.println("\n******* CREATING THREAD FOR " + this_operation + " *******\n");
            switch (this_operation) {
                case "SUM":
                    while (true) {
                        try {
                            received_operation = sum_queue.take();
                            
                            //
                            try_again = true;
                            cont_attempts = 0;
                            event = received_operation.split("#")[1];
                            
                            while (try_again) {
                                cont_lectures = 0;
                                receipt_server_footprint = new LinkedList<>();
                                System.out.println("SENDING SUM");
                                
                                //Create a thread for sending the message to the node
                                SendMessage sm = new SendMessage(received_operation, node_port);
                                Thread t = new Thread(sm);
                                t.start();
                            
                                while (cont_lectures < max_lectures && 
                                        receipt_server_footprint.size() < min_receipt) {
                                    System.out.println("INTENTANDO OBTENER RECIBO");
                                    actual_receipt = sum_receipts_queue.poll(2, TimeUnit.SECONDS);
                                    if (actual_receipt != null) {
                                        if (actual_receipt.split("#")[1].equals(event)) {
                                            //Received valid receipt
                                            receipt_server_footprint.add(actual_receipt.split("#")[2]);
                                            System.out.println("AGREGA FOOTPRINT");
                                        } 
                                        cont_lectures--;
                                    }
                                    cont_lectures++;
                                }
                                System.out.println("*********RECIBOS ===== " + receipt_server_footprint.size());
                                if (receipt_server_footprint.size() < min_receipt) {
                                    if (cont_attempts < max_attempts) {
                                        System.out.println("DURMIENDO HILO");
                                        Thread.sleep(wait_for_next_attempt);
                                        System.out.println("DESPERTANDO HILO");
                                        cont_attempts++;
                                    } else {
                                        //Mandar a levantar servidor
                                        System.out.println("LEVANTAR SERVIDOR");
                                        System.out.println("DURMIENDO HILO");
                                        DuplicateServer();
                                        cont_attempts = 0;
                                        Thread.sleep(wait_for_next_attempt);
                                        System.out.println("DESPERTANDO HILO");
                                    }
                                } else {
                                    //Wait answer
                                    System.out.println("GETTING SUM RESULT");
                                    System.out.println("QUEUE: " + sum_results_queue);
                                    while(try_again) {
                                        result_message = sum_results_queue.poll(2, TimeUnit.SECONDS);
                                        if (result_message != null 
                                                && result_message.split("#")[1].equals(event)) {
                                            r_n1 = result_message.split("#")[3];
                                            r_n2 = result_message.split("#")[4];
                                            result = result_message.split("#")[5];
                                            System.out.println("SUM RESULT:");
                                            System.out.println(r_n1 + " + " + r_n2 + " = " + result);
                                            sum.appendText(r_n1 + " + " + r_n2 + " = " + result + "\n");
                                            try_again = false;
                                        }
                                    }
                                    cont_attempts = 0;
                                }
                                
                            }
                        } catch (InterruptedException ex) {
                            System.out.println("ERROR TRYING SUM :(");
                        }
                    }
                case "SUBS":
                    while (true) {
                        try {
                            received_operation = subs_queue.take();
                            
                            //
                            try_again = true;
                            cont_attempts = 0;
                            event = received_operation.split("#")[1];
                            
                            while (try_again) {
                                cont_lectures = 0;
                                receipt_server_footprint = new LinkedList<>();
                                System.out.println("SENDING SUBS");
                                
                                //Create a thread for sending the message to the node
                                SendMessage sm = new SendMessage(received_operation, node_port);
                                Thread t = new Thread(sm);
                                t.start();
                            
                                while (cont_lectures < max_lectures && 
                                        receipt_server_footprint.size() < min_receipt) {
                                    System.out.println("INTENTANDO OBTENER RECIBO");
                                    actual_receipt = subs_receipts_queue.poll(2, TimeUnit.SECONDS);
                                    if (actual_receipt != null) {
                                        if (actual_receipt.split("#")[1].equals(event)) {
                                            //Received valid receipt
                                            receipt_server_footprint.add(actual_receipt.split("#")[2]);
                                            System.out.println("AGREGA FOOTPRINT");
                                        } 
                                        cont_lectures--;
                                    }
                                    cont_lectures++;
                                }
                                System.out.println("*********RECIBOS ===== " + receipt_server_footprint.size());
                                if (receipt_server_footprint.size() < min_receipt) {
                                    if (cont_attempts < max_attempts) {
                                        System.out.println("DURMIENDO HILO");
                                        Thread.sleep(wait_for_next_attempt);
                                        System.out.println("DESPERTANDO HILO");
                                        cont_attempts++;
                                    } else {
                                        //Mandar a levantar servidor
                                        System.out.println("LEVANTAR SERVIDOR");
                                        System.out.println("DURMIENDO HILO");
                                        DuplicateServer();
                                        cont_attempts = 0;
                                        Thread.sleep(wait_for_next_attempt);
                                        System.out.println("DESPERTANDO HILO");
                                    }
                                } else {
                                    //Wait answer
                                    System.out.println("GETTING SUBS RESULT");
                                    System.out.println("QUEUE: " + subs_results_queue);
                                    while(try_again) {
                                        result_message = subs_results_queue.poll(2, TimeUnit.SECONDS);
                                        if (result_message != null 
                                                && result_message.split("#")[1].equals(event)) {
                                            r_n1 = result_message.split("#")[3];
                                            r_n2 = result_message.split("#")[4];
                                            result = result_message.split("#")[5];
                                            System.out.println("SUBS RESULT:");
                                            System.out.println(r_n1 + " - " + r_n2 + " = " + result);
                                            subs.appendText(r_n1 + " - " + r_n2 + " = " + result + "\n");
                                            try_again = false;
                                        }
                                    }
                                    cont_attempts = 0;
                                }
                                
                            }
                        } catch (InterruptedException ex) {
                            System.out.println("ERROR TRYING SUBS :(");
                        }
                    }
                case "MULT":
                    while (true) {
                        try {
                            received_operation = mult_queue.take();
                            
                            //
                            try_again = true;
                            cont_attempts = 0;
                            event = received_operation.split("#")[1];
                            
                            while (try_again) {
                                cont_lectures = 0;
                                receipt_server_footprint = new LinkedList<>();
                                System.out.println("SENDING MULT");
                                
                                //Create a thread for sending the message to the node
                                SendMessage sm = new SendMessage(received_operation, node_port);
                                Thread t = new Thread(sm);
                                t.start();
                            
                                while (cont_lectures < max_lectures && 
                                        receipt_server_footprint.size() < min_receipt) {
                                    System.out.println("INTENTANDO OBTENER RECIBO");
                                    actual_receipt = mult_receipts_queue.poll(2, TimeUnit.SECONDS);
                                    if (actual_receipt != null) {
                                        if (actual_receipt.split("#")[1].equals(event)) {
                                            //Received valid receipt
                                            receipt_server_footprint.add(actual_receipt.split("#")[2]);
                                            System.out.println("AGREGA FOOTPRINT");
                                        } 
                                        cont_lectures--;
                                    }
                                    cont_lectures++;
                                }
                                System.out.println("*********RECIBOS ===== " + receipt_server_footprint.size());
                                if (receipt_server_footprint.size() < min_receipt) {
                                    if (cont_attempts < max_attempts) {
                                        System.out.println("DURMIENDO HILO");
                                        Thread.sleep(wait_for_next_attempt);
                                        System.out.println("DESPERTANDO HILO");
                                        cont_attempts++;
                                    } else {
                                        //Mandar a levantar servidor
                                        System.out.println("LEVANTAR SERVIDOR");
                                        System.out.println("DURMIENDO HILO");
                                        DuplicateServer();
                                        cont_attempts = 0;
                                        Thread.sleep(wait_for_next_attempt);
                                        System.out.println("DESPERTANDO HILO");
                                    }
                                } else {
                                    //Wait answer
                                    System.out.println("GETTING MULT RESULT");
                                    System.out.println("QUEUE: " + mult_results_queue);
                                    while(try_again) {
                                        result_message = mult_results_queue.poll(2, TimeUnit.SECONDS);
                                        if (result_message != null 
                                                && result_message.split("#")[1].equals(event)) {
                                            r_n1 = result_message.split("#")[3];
                                            r_n2 = result_message.split("#")[4];
                                            result = result_message.split("#")[5];
                                            System.out.println("MULT RESULT:");
                                            System.out.println(r_n1 + " * " + r_n2 + " = " + result);
                                            mult.appendText(r_n1 + " * " + r_n2 + " = " + result + "\n");
                                            try_again = false;
                                        }
                                    }
                                    cont_attempts = 0;
                                }
                                
                            }
                        } catch (InterruptedException ex) {
                            System.out.println("ERROR TRYING MULT :(");
                        }
                    }
                case "DIV":
                    while (true) {
                        try {
                            received_operation = div_queue.take();
                            
                            //
                            try_again = true;
                            cont_attempts = 0;
                            event = received_operation.split("#")[1];
                            
                            while (try_again) {
                                cont_lectures = 0;
                                receipt_server_footprint = new LinkedList<>();
                                System.out.println("SENDING DIV");
                                
                                //Create a thread for sending the message to the node
                                SendMessage sm = new SendMessage(received_operation, node_port);
                                Thread t = new Thread(sm);
                                t.start();
                            
                                while (cont_lectures < max_lectures && 
                                        receipt_server_footprint.size() < min_receipt) {
                                    System.out.println("INTENTANDO OBTENER RECIBO");
                                    actual_receipt = div_receipts_queue.poll(2, TimeUnit.SECONDS);
                                    if (actual_receipt != null) {
                                        if (actual_receipt.split("#")[1].equals(event)) {
                                            //Received valid receipt
                                            receipt_server_footprint.add(actual_receipt.split("#")[2]);
                                            System.out.println("AGREGA FOOTPRINT");
                                        } 
                                        cont_lectures--;
                                    }
                                    cont_lectures++;
                                }
                                System.out.println("*********RECIBOS ===== " + receipt_server_footprint.size());
                                if (receipt_server_footprint.size() < min_receipt) {
                                    if (cont_attempts < max_attempts) {
                                        System.out.println("DURMIENDO HILO");
                                        Thread.sleep(wait_for_next_attempt);
                                        System.out.println("DESPERTANDO HILO");
                                        cont_attempts++;
                                    } else {
                                        //Mandar a levantar servidor
                                        System.out.println("LEVANTAR SERVIDOR");
                                        System.out.println("DURMIENDO HILO");
                                        DuplicateServer();
                                        cont_attempts = 0;
                                        Thread.sleep(wait_for_next_attempt);
                                        System.out.println("DESPERTANDO HILO");
                                    }
                                } else {
                                    //Wait answer
                                    System.out.println("GETTING DIV RESULT");
                                    System.out.println("QUEUE: " + div_results_queue);
                                    while(try_again) {
                                        result_message = div_results_queue.poll(2, TimeUnit.SECONDS);
                                        if (result_message != null 
                                                && result_message.split("#")[1].equals(event)) {
                                            r_n1 = result_message.split("#")[3];
                                            r_n2 = result_message.split("#")[4];
                                            result = result_message.split("#")[5];
                                            System.out.println("DIV RESULT:");
                                            if (result.equals("NAN")) result = "UNDEFINED";
                                            System.out.println(r_n1 + " / " + r_n2 + " = " + result);
                                            div.appendText(r_n1 + " / " + r_n2 + " = " + result + "\n");
                                            try_again = false;
                                        }
                                    }
                                    cont_attempts = 0;
                                }
                                
                            }
                        } catch (InterruptedException ex) {
                            System.out.println("ERROR TRYING DIV :(");
                        }
                    }
            }
        }
        
        void DuplicateServer() {
            if (receipt_server_footprint.isEmpty()) {
                System.out.println("NO AVAILABLE SERVERS TO DUPLICATE");
                return;
            }
            List<String> items = new ArrayList<String>();
            while (!receipt_server_footprint.isEmpty()) {
                items.add(receipt_server_footprint.remove());
            }
            Collections.sort(items, String.CASE_INSENSITIVE_ORDER);
            String server_to_duplicate = items.get(0);
            server_to_duplicate = "500#" + server_to_duplicate;
            System.out.println("DUP: " + server_to_duplicate);
            //Create a thread for sending the message to the node
            SendMessage sm = new SendMessage(server_to_duplicate, node_port);
            Thread t_sm = new Thread(sm);
            t_sm.start();
        }
        
    }
}

class OperationTableElement {
    public int folio;
    public String operation;
    public String event;
    
    OperationTableElement(int folio, String operation, String event) {
        this.folio = folio;
        this.operation = operation;
        this.event = event;
    }
}
