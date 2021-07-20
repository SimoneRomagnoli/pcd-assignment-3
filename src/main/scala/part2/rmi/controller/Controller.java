package part2.rmi.controller;

import part2.rmi.Starter;
import part2.rmi.puzzle.PuzzleBoard;
import part2.rmi.puzzle.SerializableTile;
import part2.rmi.remotes.*;

import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import java.rmi.RemoteException;
import java.util.Map;

/**
 * Controller of the game:
 * it interacts with the observer of the remote objects and with a local manager.
 *
 */
public class Controller {

    private final RemoteObserver observer;

    private final PuzzleBoard puzzleBoard;

    private RemoteBoard remoteBoard;

    private final OwnershipManager manager;

    private final int id;

    public Controller(int id, RemoteBoard remoteBoard) {
        this.id = id;
        this.remoteBoard = remoteBoard;

        this.puzzleBoard = new PuzzleBoard(this, id == 1);
        this.puzzleBoard.setVisible(true);
        this.observer = new RemoteObserverImpl(this, this.id);

        this.manager = new OwnershipManager(this.id, this);

        try {
            UnicastRemoteObject.exportObject(observer, 0);
            this.remoteBoard.addRemoteObserver(this.observer, this.id);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Communicate a selection to the remote puzzle board.
     * If the remote object's owner has crashed, recreate the object.
     *
     * @param tile, the selected tile
     * @throws RemoteException if the node is unreachable
     * @throws NotBoundException if the registry is unreachable
     */
    public void select(SerializableTile tile) throws RemoteException, NotBoundException {
        try {
            this.remoteBoard.select(tile, this.id);
        } catch (RemoteException starterFailed) {
            RemoteBoard board = new RemoteBoardImpl(this.puzzleBoard.getSerializableTiles());
            RemoteBoard boardStub = (RemoteBoard) UnicastRemoteObject.exportObject(board, 0);
            this.remoteBoard = board;
            this.remoteBoard.setRemoteObservers(this.manager.getObservers());
            Registry registry = LocateRegistry.getRegistry(Starter.REGISTRY_PORT + this.id - 1);
            registry.rebind("boardStatus", boardStub);

            this.manager.newObjectOwner();
            select(tile);
        }
    }

    /**
     * Update the local board with the remote status.
     * The failure handling avoids that the update fails.
     *
     */
    public void update() {
        try {
            this.puzzleBoard.updateBoard();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load the starter board after it has been created.
     *
     * @param tiles, the new random tiles.
     */
    public void loadInitialBoard(List<SerializableTile> tiles) {
        try {
            this.remoteBoard.loadCurrentTiles(tiles);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the remote serializable tiles.
     *
     * @return the tiles of the remote board
     * @throws RemoteException if the board is unreachable
     */
    public List<SerializableTile> getSerializableTiles() throws RemoteException {
        return this.remoteBoard.getTiles();
    }

    /**
     * Update the local map of observers.
     *
     * @param observers, the new map of observers
     */
    public void updateRemoteObservers(Map<RemoteObserver, Integer> observers) {
        this.manager.updateRemoteObservers(observers);
    }

    /**
     * Update the remote objects in the registry when their owner has changed.
     *
     * @param port, the port of the new owner
     * @throws RemoteException if the node is unreachable
     * @throws NotBoundException if the registry is unreachable
     */
    public void updateRemoteObjects(int port) throws RemoteException, NotBoundException {
        this.manager.updateRemoteObjects(port);
    }

    /**
     * Reset the local reference to the remote board with the new updated one.
     *
     * @throws RemoteException if the node is unreachable
     * @throws NotBoundException if the registry is unreachable
     */
    public void reset() throws RemoteException, NotBoundException {
        this.remoteBoard = (RemoteBoard) LocateRegistry.getRegistry(Starter.REGISTRY_PORT+ this.id-1).lookup("boardStatus");
    }
}
