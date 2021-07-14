package part2.rmi.local;

public class PuzzleClient {

    public static final int ROWS = 3;
    public static final int COLS = 5;
    public static final String IMG_PATH = "res/numbers-smaller.png";

    private final PuzzleBoard puzzleBoard;

    public PuzzleClient() {
        this.puzzleBoard = new PuzzleBoard(ROWS, COLS, IMG_PATH);
    }

}
