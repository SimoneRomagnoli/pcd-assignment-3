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


public class Controller {

    private final RemoteObserver observer;

    private final PuzzleBoard puzzleBoard;

    private BoardStatus boardStatus;

    private final OwnershipManager manager;

    private final int id;

    public Controller(int id, BoardStatus boardStatus) {
        this.id = id;
        this.boardStatus = boardStatus;

        this.puzzleBoard = new PuzzleBoard(this, id == 1);
        this.puzzleBoard.setVisible(true);
        this.observer = new RemoteObserverImpl(this, this.id);

        this.manager = new OwnershipManager(this.id, this);

        try {
            UnicastRemoteObject.exportObject(observer, 0);
            this.boardStatus.addRemoteObserver(this.observer, this.id);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void select(SerializableTile tile) throws RemoteException, NotBoundException {
        try {
            this.boardStatus.select(tile, this.id);
        } catch (RemoteException starterFailed) {
            BoardStatus board = new BoardStatusImpl(this.puzzleBoard.getSerializableTiles());
            BoardStatus boardStub = (BoardStatus) UnicastRemoteObject.exportObject(board, 0);
            this.boardStatus = board;
            this.boardStatus.setRemoteObservers(this.manager.getObservers());
            Registry registry = LocateRegistry.getRegistry(Starter.REGISTRY_PORT + this.id - 1);
            registry.rebind("boardStatus", boardStub);

            this.manager.propagate();
            select(tile);
        }
    }

    public void update() {
        try {
            this.puzzleBoard.updateBoard();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void loadInitialBoard(List<SerializableTile> tiles) {
        try {
            this.boardStatus.loadCurrentTiles(tiles);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public List<SerializableTile> getSerializableTiles() throws RemoteException {
        return this.boardStatus.getTiles();
    }

    public void updateRemoteObservers(Map<RemoteObserver, Integer> observers) {
        this.manager.updateRemoteObservers(observers);
    }

    public void updateRemoteObjects(int port) throws RemoteException, NotBoundException {
        this.manager.updateRemoteObjects(port);
    }

    public void reset() throws RemoteException, NotBoundException {
        this.boardStatus = (BoardStatus) LocateRegistry.getRegistry(Starter.REGISTRY_PORT+ this.id-1).lookup("boardStatus");
        //this.boardStatus.addRemoteObserver(this.observer);
    }
}
