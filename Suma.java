/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package suma;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author alfonsomurillo
 */
public class Suma {
    String msg = "5#";
    Socket s;
    DataOutputStream dout;
    
    public Suma(String event, String footprint, float n1, float n2, int port) {
        msg += event + "#" + footprint + "#" + String.valueOf(n1) + "#" + String.valueOf(n2) + "#";
        msg += String.valueOf(n1 + n2);
        
        try {
            //Send receipt message
            System.out.println("******** Suma.jar SENDING: " + msg + " TO PORT [" + port + "] ********");
            s = new Socket("localhost", port);

            dout = new DataOutputStream(s.getOutputStream());

            dout.writeUTF(msg);

            dout.close();
            s.close();
           
        } catch (IOException ex) {
            System.out.println("Error at Suma.jar: " + ex);
        }
    }
}
