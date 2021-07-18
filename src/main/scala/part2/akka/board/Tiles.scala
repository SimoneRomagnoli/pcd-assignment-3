package part2.akka.board

import java.awt.event.{MouseAdapter, MouseEvent}
import java.awt.{Color, Image}
import javax.swing.{BorderFactory, ImageIcon, JButton}

/**
 * Contains classes for handling puzzle tiles, both logically and graphically.
 *
 */
object Tiles {

  /**
   * Class that logically represents a tile.
   *
   * @param image, the portion of image that a tile draws
   * @param originalPosition, the tile's correct position in the puzzle
   * @param currentPosition, the tile's current position in the puzzle
   * @param selected, true if the tile is currently selected by someone
   */
  case class Tile(image: Image, originalPosition: Int, var currentPosition: Int, var selected: Boolean = false) extends Comparable[Tile] {
    def setCurrentPosition(newPosition: Int): Unit =
      currentPosition = newPosition

    def isInRightPlace: Boolean =
      currentPosition == originalPosition

    /**
     * Comparison needed for correctly sorting a list of tiles.
     *
     * @param other, another tile
     * @return a comparison between two tiles
     */
    override def compareTo(other: Tile): Int =
      if (currentPosition < other.currentPosition) -1
      else if (currentPosition == other.currentPosition) 0
      else 1
  }

  /**
   * Class that graphically represent a tile and draws colors.
   *
   * @param color, the tile border color
   * @param tile, the selected tile on the board
   */
  case class TileButton(color: Color, tile: Tile) extends JButton(new ImageIcon(tile.image)) {
    addMouseListener(new MouseAdapter() {
      override def mouseClicked(e: MouseEvent): Unit = {
        if (!tile.selected)
          setBorder(BorderFactory.createLineBorder(color, 3))
      }
    })
  }

}
