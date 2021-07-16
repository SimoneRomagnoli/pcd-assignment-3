package part2.rmi.puzzle;

import jnr.ffi.annotations.In;
import part2.rmi.Starter;
import part2.rmi.controller.Controller;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PuzzleBoard extends JFrame {

    private final static Map<Integer, Color> COLORS = Map.of(
                1, Color.green, 2, Color.blue, 3, Color.red,
                4, Color.orange, 5, Color.magenta, 6, Color.yellow,
                7, Color.cyan, 8, Color.pink, 9, Color.darkGray
            );


    final int rows = Starter.ROWS;
	final int cols = Starter.COLS;
	final String img = Starter.IMG;

	private final boolean starter;

    private final Controller controller;

    private final List<Tile> tiles = new ArrayList<>();
    private final JPanel board;

    public PuzzleBoard(Controller controller, boolean starter) {
        this.starter = starter;
        this.controller = controller;
        this.setTitle("Puzzle");
        this.setResizable(false);
        this.setDefaultCloseOperation(3);
        this.board = new JPanel();
        board.setBorder(BorderFactory.createLineBorder(Color.gray));
        board.setLayout(new GridLayout(rows, cols, 0, 0));
        this.getContentPane().add(board, "Center");
        this.createTiles();
        this.paintPuzzle();
    }

    private void createTiles(){
        BufferedImage image;
        try {
            image = ImageIO.read(new File(this.img));
        } catch (IOException var10) {
            System.out.println(var10);
            JOptionPane.showMessageDialog(this, "Could not load image", "Error", 0);
            return;
        }

        int imageWidth = image.getWidth((ImageObserver)null);
        int imageHeight = image.getHeight((ImageObserver)null);
        int position = 0;
        List<Integer> randomPositions = new ArrayList();

        if(starter){
            final List<Integer> randomGenerator = new ArrayList<>();
            IntStream.range(0, this.rows * this.cols).forEach((item) -> {
                randomGenerator.add(item);
            });
            randomPositions = new ArrayList<>(randomGenerator);
            Collections.shuffle(randomPositions);
        }else{
            try {
                randomPositions = controller.getSerializableTiles().stream().map(SerializableTile::getCurrentPosition).collect(Collectors.toList());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        for(int i = 0; i < this.rows; ++i) {
            for(int j = 0; j < this.cols; ++j) {
                Image imagePortion = this.createImage(new FilteredImageSource(image.getSource(), new CropImageFilter(j * imageWidth / this.cols, i * imageHeight / this.rows, imageWidth / this.cols, imageHeight / this.rows)));
                this.tiles.add(new Tile(imagePortion, position, randomPositions.get(position), 0));
                ++position;
            }
        }

        //Solo se sono lo starter devo dire comunicare al BoardStatus l'initial board
        if(starter){
            controller.loadInitialBoard(this.getSerializableTiles());
        }
    }

    private void paintPuzzle() {
        this.board.removeAll();
        Collections.sort(this.tiles);

        this.tiles.forEach((tile) -> {
            TileButton btn = new TileButton(tile);
            this.board.add(btn);
            final int id = tile.getSelection();
            btn.setBorder(BorderFactory.createLineBorder(id == 0 ? Color.GRAY : COLORS.get(id)));
            btn.addActionListener((actionListener) -> {
                // Solo se la tile non è già selezionata
                if(!tile.alreadySelected()){
                    try {
                        this.controller.select(tile.getSerializableTile());
                        this.checkSolution();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        this.pack();
        this.setLocationRelativeTo((Component)null);
    }

    private void checkSolution() {
        if (this.tiles.stream().allMatch(Tile::isInRightPlace)) {
            JOptionPane.showMessageDialog(this, "Puzzle Completed!", "", 1);
        }

    }
    public void updateBoard() throws RemoteException {
        List<SerializableTile> serializableTiles = controller.getSerializableTiles();
        for(int i=0;i<serializableTiles.size();i++){
            tiles.get(i).setCurrentPosition(serializableTiles.get(i).getCurrentPosition());
            tiles.get(i).select(serializableTiles.get(i).getSelection());
        }
        paintPuzzle();
    }

    public List<SerializableTile> getSerializableTiles(){
        return tiles.stream().map(Tile::getSerializableTile).collect(Collectors.toList());
    }
}
