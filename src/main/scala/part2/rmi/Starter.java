package part2.rmi;


import part2.rmi.controller.Controller;
import part2.rmi.puzzle.SerializableTile;
import part2.rmi.remotes.*;

import java.util.List;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Starts a new game.
 *
 */
public class Starter {

    public static final int REGISTRY_PORT = 1099;
    public static final int ROWS = 3;
    public static final int COLS = 5;
    public static final String IMG = "res/numbers-smaller.png";

    public static void main(String[] args) {
        try {
            //CREATE LOCAL REGISTRY
            LocateRegistry.createRegistry(REGISTRY_PORT);

            //CREATE THE MODEL
            List<SerializableTile> emptyList = Stream.generate(() -> new SerializableTile(0,0, 0)).limit(ROWS*COLS).collect(Collectors.toList());
            RemoteBoard board = new RemoteBoardImpl(emptyList);
            RemoteBoard boardStub = (RemoteBoard) UnicastRemoteObject.exportObject(board, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(REGISTRY_PORT);
            registry.rebind("boardStatus", boardStub);

            new Controller(1, board);
        } catch (RemoteException e) {
            e.printStackTrace();
        }


    }
}
