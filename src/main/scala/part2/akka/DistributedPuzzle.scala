package part2.akka

object DistributedPuzzle extends App {
  val imgPath: String = "res/bletchley-park-mansion.jpg"
  val puzzleBoard = PuzzleBoard(3, 5, imgPath)
}
