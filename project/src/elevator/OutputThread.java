package elevator;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OutputThread extends Thread {

        protected LinkedBlockingQueue<String> commandsQueue;
        protected Socket s;
        public PrintWriter outStream;
       
        public OutputThread(){
            try {
                //establish connection to send commands
                s = new Socket("localhost", 4711);
                outStream = new PrintWriter(s.getOutputStream(), true);

                commandsQueue = new LinkedBlockingQueue<>();
                
            } catch (UnknownHostException ex) {
                Logger.getLogger(OutputThread.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(OutputThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
       
        @Override
        public void run() {
                while(true){
                        sendCommand();
                }
        }
       //Inserts the command at the tail of  commandsQueue
        public void putOnCommandQue(String comm){
                commandsQueue.offer(comm);
        }
       /*
        * Retrieves and removes command from the head of commandQueue, waiting if necessary until an element becomes available.
        * Then send it through outstream
        */
        protected void sendCommand(){
            try {
                outStream.println(commandsQueue.take());
            } catch (InterruptedException ex) {
                Logger.getLogger(OutputThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
}
