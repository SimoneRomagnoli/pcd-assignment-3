package part2.akka

import akka.actor.typed.ActorRef
import part2.akka.PuzzleBehaviors.SelectionGuardian.{SelectedCell, Selection}
import part2.akka.Tiles.{SelectionManager, Tile, TileButton}

import java.awt.image.{BufferedImage, CropImageFilter, FilteredImageSource}
import java.awt.{BorderLayout, Color, GridLayout, Image}
import java.io.File
import javax.imageio.ImageIO
import javax.swing._
import scala.util.Random

case class PuzzleBoard(rows: Int, cols: Int, starter:Boolean, currentPositions:List[Int], var selectionList:List[Int], selectionGuardian:ActorRef[Selection], var tiles: List[Tile] = List(), selectionManager:SelectionManager = SelectionManager()) extends JFrame {
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

  private def createTiles(): Unit = {
    val imgPath:String = "res/numbers.png"
    val img: BufferedImage = ImageIO.read(new File(imgPath))
    if (img equals null) {
      JOptionPane.showMessageDialog(this, "Could not load image")
    } else {
      val width = img.getWidth
      val height = img.getHeight

      if(starter) {
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

  private def paintPuzzle(): Unit = {
    board.removeAll()
    tiles = tiles.sorted

    for (tile <- tiles) {
      val btn = TileButton(tile)
      board.add(btn)
      btn.setBorder(BorderFactory.createLineBorder(Color.gray))
      btn.addActionListener(_ => {
        selectionGuardian ! SelectedCell(tile.currentPosition)
        selectionManager.selectTile(tile, () => {
          paintPuzzle()
          checkSolution()
        })
      })
    }

    pack()
    setLocationRelativeTo(null)
  }

  private def checkSolution(): Unit = {
    if (tiles.forall(t => t.isInRightPlace))
      JOptionPane.showMessageDialog(this, "Puzzle Completed!")
  }
}

case class PuzzleOptions(rows: Int, cols: Int, starter:Boolean, currentPositions:List[Int] = List(), var selectionList:List[Int] = List()) {
  selectionList = LazyList.continually(0).take(rows * cols).toList
}
