package part2.rmi.remote;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class SimpleGUIServer {
                
    public static void main(String args[]) {
        
        try {

            Counter count = new CounterImpl(0);
            Counter countStub = (Counter) UnicastRemoteObject.exportObject(count, 0);
            
            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();

            registry.rebind("countObj", countStub);
            
            System.out.println("Objects registered.");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}