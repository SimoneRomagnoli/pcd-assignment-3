package part2.akka.board

import akka.actor.typed.ActorRef
import part2.akka.actors.PuzzleBehaviors.SelectionGuardian.{SelectedCell, Selection}
import Tiles.{SelectionManager, Tile, TileButton}
import part2.akka.StarterMain

import java.awt.image.{BufferedImage, CropImageFilter, FilteredImageSource}
import java.awt.{BorderLayout, Color, GridLayout, Image}
import java.io.File
import javax.imageio.ImageIO
import javax.swing._
import scala.util.Random

case class PuzzleBoard(localId:Int, rows: Int, cols: Int, starter:Boolean, currentPositions:List[Int], var selectionList:List[Int], selectionGuardian:ActorRef[Selection], var tiles: List[Tile] = List(), selectionManager:SelectionManager = SelectionManager()) extends JFrame {
  setTitle("Puzzle")
  setResizable(false)
  setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

  val board = new JPanel()
  board.setBorder(BorderFactory.createLineBorder(Color.gray))
  board.setLayout(new GridLayout(rows, cols, 0, 0))
  getContentPane.add(board,BorderLayout.CENTER)

  createTiles()
  paintPuzzle()
  this.setVisible(true)

  private def createTiles(): Unit = {
    val imgPath:String = "res/numbers-smaller.png"
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

  private def checkSolution(): Unit = {
    if (tiles.forall(t => t.isInRightPlace))
      JOptionPane.showMessageDialog(this, "Puzzle Completed!")
  }

  def paintPuzzle(): Unit = {
    board.removeAll()
    tiles = tiles.sorted

    tiles.zipWithIndex.foreach {
      case (tile, index) =>
        val btn = TileButton(StarterMain.playersToColors(localId),tile)
        board.add(btn)
        val color:Color =
          if(selectionList(index) == 0) Color.gray
          else StarterMain.playersToColors(selectionList(index))
        btn.setBorder(BorderFactory.createLineBorder(color, 3))
        btn.addActionListener(_ => {
          if(selectionList(index) == 0)
            selectionGuardian ! SelectedCell(tile.currentPosition)
        })
    }

    pack()
    //setLocationRelativeTo(null)
  }

  def remoteSelection(selectedPosition:Int, remoteId:Int): Unit = {
    if(!selectionList.contains(remoteId)) {
      selectionList = selectionList.patch(selectedPosition, Seq(remoteId), 1)
      tiles.filter(tile => tile.currentPosition == selectedPosition).head.selected = true
    } else {
      val firstSelection = selectionList.indexOf(remoteId)
      selectionList = selectionList.patch(firstSelection, Seq(0), 1)

      val tile1:Tile = tiles.filter(tile => tile.currentPosition == firstSelection).head
      val tile2:Tile = tiles.filter(tile => tile.currentPosition == selectedPosition).head
      selectionManager.swap(tile1, tile2)
      tile1.selected = false
    }
    paintPuzzle()
    checkSolution()
  }

  def localSelection(selectedPosition:Int, localId:Int): Unit = {
    if(!selectionList.contains(localId)) {
      selectionList = selectionList.patch(selectedPosition, Seq(localId), 1)
      tiles.filter(tile => tile.currentPosition == selectedPosition).head.selected = true
    } else {
      val firstSelection = selectionList.indexOf(localId)
      selectionList = selectionList.patch(firstSelection, Seq(0), 1)
      tiles.filter(tile => tile.currentPosition == firstSelection).head.selected = false
    }
    selectionManager.selectTile(tiles.filter(tile => tile.currentPosition==selectedPosition).head, () => {
      paintPuzzle()
      checkSolution()
    })
  }
}

case class PuzzleOptions(rows: Int, cols: Int, starter:Boolean, currentPositions:List[Int] = List(), var selectionList:List[Int] = List()) {
  if(selectionList.isEmpty)
    selectionList = LazyList.continually(0).take(rows * cols).toList
}
