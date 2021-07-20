package part2.rmi.puzzle;

import part2.rmi.Starter;
import part2.rmi.controller.Controller;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.io.File;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The graphical user interface of the game.
 * It is a local representation of the remote game.
 *
 */
public class PuzzleBoard extends JFrame {

    private final static Map<Integer, Color> COLORS = Map.of(
                1, Color.green, 2, Color.blue, 3, Color.red,
                4, Color.orange, 5, Color.magenta, 6, Color.yellow,
                7, Color.cyan, 8, Color.pink, 9, Color.darkGray, 0, Color.lightGray
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
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.board = new JPanel();
        board.setBorder(BorderFactory.createLineBorder(Color.gray));
        board.setLayout(new GridLayout(rows, cols, 0, 0));
        this.getContentPane().add(board, "Center");
        this.createTiles();
    }

    /**
     * Updates a board with the current global state.
     *
     * @throws RemoteException if the node is unreachable
     */
    public void updateBoard() throws RemoteException {
        List<SerializableTile> serializableTiles = controller.getSerializableTiles();

        for(Tile tile: this.tiles) {
            final SerializableTile twinTile = serializableTiles
                    .stream()
                    .filter(t -> t.getOriginalPosition() == tile.getOriginalPosition())
                    .collect(Collectors.toList())
                    .get(0);

            tile.setCurrentPosition(twinTile.getCurrentPosition());
            tile.select(twinTile.getSelection());
        }

        paintPuzzle();
        checkSolution();
    }

    /**
     * Get the current local state of the tiles.
     *
     * @return the current tiles in the local board
     */
    public List<SerializableTile> getSerializableTiles(){
        return tiles.stream().map(Tile::getSerializableTile).collect(Collectors.toList());
    }

    private void createTiles(){
        BufferedImage image;
        try {
            image = ImageIO.read(new File(this.img));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not load image", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int position = 0;
        List<Integer> randomPositions = new ArrayList<>();
        IntStream.range(0, this.rows * this.cols).forEach(randomPositions::add);
        Collections.shuffle(randomPositions);

        for(int i = 0; i < this.rows; ++i) {
            for(int j = 0; j < this.cols; ++j) {
                Image imagePortion = this.createImage(new FilteredImageSource(image.getSource(), new CropImageFilter(j * imageWidth / this.cols, i * imageHeight / this.rows, imageWidth / this.cols, imageHeight / this.rows)));
                this.tiles.add(new Tile(imagePortion, position, randomPositions.get(position), 0));
                ++position;
            }
        }

        if(starter){
            this.paintPuzzle();
            controller.loadInitialBoard(this.getSerializableTiles());
        } else {
            try {
                this.updateBoard();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void paintPuzzle() {
        this.board.removeAll();
        Collections.sort(this.tiles);

        this.tiles.forEach((tile) -> {
            TileButton btn = new TileButton(tile);
            this.board.add(btn);
            final int id = tile.getSelection();
            btn.setBorder(BorderFactory.createLineBorder(id == 0 ? Color.GRAY : COLORS.get(id % 10), 3));
            btn.addActionListener((actionListener) -> {
                if(!tile.alreadySelected()){
                    try {
                        this.controller.select(tile.getSerializableTile());
                    } catch (RemoteException | NotBoundException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        this.pack();
    }

    private void checkSolution() {
        if (this.tiles.stream().allMatch(Tile::isInRightPlace)) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Puzzle Completed!", "", JOptionPane.INFORMATION_MESSAGE));
        }

    }
}
