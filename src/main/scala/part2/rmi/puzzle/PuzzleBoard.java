package part2.rmi.puzzle;

import part2.rmi.Starter;
import part2.rmi.controller.Controller;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PuzzleBoard extends JFrame {

    private final static Map<Integer, Color> COLORS = Map.of(
                1, Color.green, 2, Color.blue, 3, Color.red,
                4, Color.orange, 5, Color.magenta, 6, Color.yellow,
                7, Color.cyan, 8, Color.pink, 9, Color.darkGray
            );


    final JPanel board;

	final int rows = Starter.ROWS;
	final int cols = Starter.COLS;
	final String img = Starter.IMG;

	private final boolean starter;

    private final Controller controller;

    private final List<Tile> tiles = new ArrayList<>();
	
    public PuzzleBoard(Controller controller, boolean starter) {
    	setTitle("Puzzle");
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.controller = controller;
        this.starter = starter;

        this.board = new JPanel();
        board.setBorder(BorderFactory.createLineBorder(Color.gray));
        board.setLayout(new GridLayout(rows, cols, 0, 0));
        getContentPane().add(board, BorderLayout.CENTER);

        createTiles();

        try {
            paintPuzzle();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    
    private void createTiles() {
		final BufferedImage image;
        
        try {
            image = ImageIO.read(new File(img));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not load image", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final int imageWidth = image.getWidth(null);
        final int imageHeight = image.getHeight(null);

        int position = 0;

        List<Integer> randomPositions = new ArrayList<>();
        if(starter) {
            IntStream.range(0, rows*cols).forEach(randomPositions::add);
            Collections.shuffle(randomPositions);
        } else {
            try {
                randomPositions = this.controller.getCurrentPositions();
                System.out.println("Current positions (sorted) are "+randomPositions);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
            	final Image imagePortion = createImage(new FilteredImageSource(image.getSource(),
                        new CropImageFilter(j * imageWidth / cols,
                        					i * imageHeight / rows, 
                        					(imageWidth / cols),
                        					imageHeight / rows)));

                tiles.add(new Tile(imagePortion, position, randomPositions.get(position)));
                position++;
            }
        }

        this.controller.loadCurrentPositions(this.tiles.stream().map(Tile::getCurrentPosition).collect(Collectors.toList()));
	}

	private List<Integer> getSortedDestinations(List<Integer> currentPositions) {
        List<Integer> destinations = new ArrayList<>();
        for(Integer i: currentPositions) {
            destinations.add(currentPositions.indexOf(i));
        }
        return destinations;
    }
    
    private void paintPuzzle() throws RemoteException {
    	this.board.removeAll();

    	Collections.sort(tiles);

        List<Integer> selectedList = new ArrayList<>(controller.getSelectedList());

        tiles.forEach(tile -> {
    		final TileButton btn = new TileButton(tile);
            this.board.add(btn);
            int id = selectedList.get(tiles.indexOf(tile));
            btn.setBorder(BorderFactory.createLineBorder(id == 0 ? Color.gray : COLORS.get(id)));
            btn.addActionListener(actionListener -> {
                try {
                    controller.select(tile.getCurrentPosition());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                /*
                selectionManager.selectTile(tile, () -> {
            		paintPuzzle();
                	checkSolution();
            	});

                 */
            });
    	});
    	
    	pack();
        setLocationRelativeTo(null);
    }

    private void checkSolution() {
    	if(tiles.stream().allMatch(Tile::isInRightPlace)) {
    		JOptionPane.showMessageDialog(this, "Puzzle Completed!", "", JOptionPane.INFORMATION_MESSAGE);
    	}
    }

    public void updateBoard() {
        try {
            /*
            List<Integer> currentPositions = this.controller.getCurrentPositions();
            for(int i = 0; i < currentPositions.size(); i++) {
                tiles.get(i).setCurrentPosition(currentPositions.get(i));
            }

             */
            paintPuzzle();
            checkSolution();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }
}
