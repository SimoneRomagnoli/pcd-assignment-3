package part2.akka.board

import akka.actor.typed.ActorRef
import Tiles.{Tile, TileButton}
import part2.akka.actors.Player.{PlayerMessage, SelectedCell}

import java.awt.image.{BufferedImage, CropImageFilter, FilteredImageSource}
import java.awt.{BorderLayout, Color, GridLayout, Image}
import java.io.File
import javax.imageio.ImageIO
import javax.swing._
import scala.util.Random

object Puzzle {

  /**
   * Maps a player id to a unique color.
   *
   */
  val playersToColors: Map[Int, Color] =
    Map(
      (1, Color.green), (2, Color.blue), (3, Color.red),
      (4, Color.orange), (5, Color.magenta), (6, Color.yellow),
      (7, Color.cyan), (8, Color.pink), (9, Color.darkGray), (0, Color.lightGray)
    )


  /**
   * Graphical user interface of the puzzle board.
   *
   * @param localId                    , the id of the player that starts the gui
   * @param rows                          , number of puzzle rows
   * @param cols                          , number of puzzle columns
   * @param starter                    , true if the player starts a new game, false if the player joins a game
   * @param currentPositions  , list of current positions of puzzle tiles
   * @param selectionList        , list of identifiers of players that selected a tile
   * @param player                      , a reference to the local Player actor
   * @param tiles                        , list of tiles in the board
   */
  case class PuzzleBoard(localId: Int, rows: Int, cols: Int, starter: Boolean, currentPositions: List[Int], var selectionList: List[Int], player: ActorRef[PlayerMessage], var tiles: List[Tile] = List()) extends JFrame {
    setTitle("Puzzle")
    setResizable(false)
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

    val board = new JPanel()
    board.setBorder(BorderFactory.createLineBorder(Color.gray))
    board.setLayout(new GridLayout(rows, cols, 0, 0))
    getContentPane.add(board, BorderLayout.CENTER)

    createTiles()
    paintPuzzle()
    this.setVisible(true)

    /**
     * Create the tiles list:
     * if the player is a starter, randomly select an order of tiles,
     * if the player is a joiner, use the current positions list.
     *
     */
    def createTiles(): Unit = {
      val imgPath: String = "res/numbers-smaller.png"
      val img: BufferedImage = ImageIO.read(new File(imgPath))
      if (img equals null) {
        JOptionPane.showMessageDialog(this, "Could not load image")
      } else {
        val width = img.getWidth
        val height = img.getHeight

        if (starter) {
          var position = 0
          var randomPositions: List[Int] = LazyList.iterate(0)(_ + 1).take(rows * cols).toList
          randomPositions = Random.shuffle(randomPositions)
          for (i <- 0 until rows; j <- 0 until cols) {
            val imagePortion: Image = createImage(new FilteredImageSource(img.getSource,
              new CropImageFilter(j * width / cols, i * height / rows, width / cols, height / rows)))

            tiles = tiles appended Tile(imagePortion, position, randomPositions(position))
            position += 1
          }
        } else {

          var position = 0
          for (i <- 0 until rows; j <- 0 until cols) {
            val imagePortion: Image = createImage(new FilteredImageSource(img.getSource,
              new CropImageFilter(j * width / cols, i * height / rows, width / cols, height / rows)))

            tiles = tiles appended Tile(imagePortion, position, currentPositions(position))
            position += 1
          }
        }
      }
    }

    /**
     * Checks if the current solution is correct.
     *
     */
    def checkSolution(): Unit = {
      if (tiles.forall(t => t.isInRightPlace))
        JOptionPane.showMessageDialog(this, "Puzzle Completed!")
    }

    /**
     * Redraw the puzzle on the gui.
     *
     */
    def paintPuzzle(): Unit = {
      board.removeAll()
      tiles = tiles.sorted

      tiles.zipWithIndex.foreach {
        case (tile, index) =>
          val btn = TileButton(playersToColors(localId), tile)
          board.add(btn)
          val color: Color =
            if (selectionList(index) == 0) Color.gray
            else playersToColors(selectionList(index) % 10)
          btn.setBorder(BorderFactory.createLineBorder(color, 3))
          btn.addActionListener(_ => {
            if (selectionList(index) == 0) {
              val timestamp: Double = System.currentTimeMillis()
              player ! SelectedCell(tile.currentPosition, timestamp)
            }
          })
      }

      pack()
    }

    /**
     * Update the gui with a new selection
     *
     * @param selectedPosition  , the index of the position selected
     * @param remoteId                  , the identifier of the player that selected it
     */
    def remoteSelection(selectedPosition: Int, remoteId: Int): Unit = {
      if (selectionList(selectedPosition) == 0) {
        if (!selectionList.contains(remoteId)) {
          selectionList = selectionList.patch(selectedPosition, Seq(remoteId), 1)
          tiles.filter(tile => tile.currentPosition == selectedPosition).head.selected = true
        } else {
          val firstSelection = selectionList.indexOf(remoteId)
          selectionList = selectionList.patch(firstSelection, Seq(0), 1)

          val tile1: Tile = tiles.filter(tile => tile.currentPosition == firstSelection).head
          val tile2: Tile = tiles.filter(tile => tile.currentPosition == selectedPosition).head
          swap(tile1, tile2)
          tile1.selected = false
        }
        paintPuzzle()
        checkSolution()
      }
    }

    /**
     * Unselect all tiles that are selected by the specified unreachable player.
     *
     * @param id, the id of the unreachable player
     */
    def unselectAll(id: Int): Unit = {
      if(selectionList.contains(id)) {
        selectionList = selectionList.patch(selectionList.indexOf(id), Seq(0), 1)
        paintPuzzle()
      }
    }

    /**
     * Swaps two selected tiles.
     *
     * @param t1  , first tile
     * @param t2  , second tile
     */
    def swap(t1: Tile, t2: Tile): Unit = {
      val position = t1.currentPosition
      t1.setCurrentPosition(t2.currentPosition)
      t2.setCurrentPosition(position)
    }

  }

  /**
   * Wrapper of gui arguments
   *
   * @param rows                          , number of puzzle rows
   * @param cols                          , number of puzzle columns
   * @param starter                    , true if the player starts a new game, false if the player joins a game
   * @param currentPositions  , list of current positions of puzzle tiles
   * @param selectionList        , list of identifiers of players that selected a tile
   */
  case class PuzzleOptions(rows: Int = 3, cols: Int = 5, starter: Boolean, currentPositions: List[Int] = List(), var selectionList: List[Int] = List()) {
    if (selectionList.isEmpty)
      selectionList = LazyList.continually(0).take(rows * cols).toList
  }

}
