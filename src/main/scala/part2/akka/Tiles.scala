package part2.akka

import java.awt.event.{MouseAdapter, MouseEvent}
import java.awt.{Color, Image}
import javax.swing.{BorderFactory, ImageIcon, JButton}

object Tiles {

  case class Tile(image: Image, originalPosition: Int, var currentPosition: Int, var selected:Boolean = false) extends Comparable[Tile] {
    override def compareTo(other: Tile): Int =
      if (currentPosition < other.currentPosition) -1
      else if (currentPosition == other.currentPosition) 0
      else 1

    def setCurrentPosition(newPosition: Int): Unit =
      currentPosition = newPosition

    def isInRightPlace: Boolean =
      currentPosition == originalPosition
  }

  case class TileButton(color:Color, tile: Tile) extends JButton(new ImageIcon(tile.image)) {
    addMouseListener(new MouseAdapter() {
      override def mouseClicked(e: MouseEvent): Unit = {
        if(!tile.selected)
          setBorder(BorderFactory.createLineBorder(color, 3))
      }
    })
  }

  case class SelectionManager(var selectedTile: Tile = null, var selectionActive: Boolean = false) {

    trait Listener {
      def swapAction(): Unit
    }

    def selectTile(tile: Tile, listener: Listener): Unit = {
      if (selectionActive) {
        selectionActive = false
        swap(selectedTile, tile)
        listener.swapAction()
      } else {
        selectionActive = true
        selectedTile = tile
      }
    }

    def swap(t1: Tile, t2: Tile): Unit = {
      val position = t1.currentPosition
      t1.setCurrentPosition(t2.currentPosition)
      t2.setCurrentPosition(position)
    }
  }

}
