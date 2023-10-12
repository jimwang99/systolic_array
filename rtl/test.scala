import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

trait SystolicArrayBehavior {
  this: AnyFlatSpec with ChiselScalatestTester =>

  def createSequentialInputs(numRow: Int, numCol: Int, numElem: Int): (Array[Array[Int]], Array[Array[Int]]) = {
    val a: Array[Array[Int]] = Array.ofDim[Int](numRow, numElem)
    val b: Array[Array[Int]] = Array.ofDim[Int](numCol, numElem)

    for (i <- 0 until numRow; k <- 0 until numElem) a(i)(k) = i * numElem + k
    for (j <- 0 until numCol; k <- 0 until numElem) b(j)(k) = j * numElem + k

    (a, b)
  }

  def createRandomInputs(numRow: Int, numCol: Int, numElem: Int): (Array[Array[Int]], Array[Array[Int]]) = {
    val a: Array[Array[Int]] = Array.ofDim[Int](numRow, numElem)
    val b: Array[Array[Int]] = Array.ofDim[Int](numCol, numElem)

    val rand = new scala.util.Random
    for (i <- 0 until numRow; k <- 0 until numElem) a(i)(k) = rand.nextInt(100)
    for (j <- 0 until numCol; k <- 0 until numElem) b(j)(k) = rand.nextInt(100)

    (a, b)
  }

  def createFeedInputs(a: Array[Array[Int]], b: Array[Array[Int]]): (Array[Array[Int]], Array[Array[Int]]) = {
    val numRow = a.length
    val numCol = b.length
    val numElem = a(0).length
    assert(a(0).length == b(0).length)

    val totalCycle = numElem + numRow.max(numCol) - 1

    val fa: Array[Array[Int]] = Array.ofDim[Int](numRow, totalCycle)
    val fb: Array[Array[Int]] = Array.ofDim[Int](numCol, totalCycle)
    for (i <- 0 until numRow; k <- 0 until totalCycle) fa(i)(k) = -1
    for (j <- 0 until numCol; k <- 0 until totalCycle) fb(j)(k) = -1

    for (i <- 0 until numRow; k <- 0 until numElem) fa(i)(i + k) = a(i)(k)
    for (j <- 0 until numCol; k <- 0 until numElem) fb(j)(j + k) = b(j)(k)

    (fa, fb)
  }

  def mac(inA: Array[Array[Int]], inB: Array[Array[Int]]): Array[Array[Int]] = {
    val numRow = inA.length
    val numCol = inB.length
    val numElem = inA(0).length
    assert(inA(0).length == inB(0).length)

    val out: Array[Array[Int]] = Array.ofDim[Int](numRow, numCol)

    for (i <- 0 until numRow; j <- 0 until numCol) {
      out(i)(j) = 0
      for (k <- 0 until numElem) {
        out(i)(j) += inA(i)(k) * inB(j)(k)
      }
    }

    out
  }
}

class TestSystolicArray extends AnyFlatSpec with SystolicArrayBehavior with ChiselScalatestTester {
  def feed(dut: SystolicArray, inA: Array[Array[Int]], inB: Array[Array[Int]]): Unit = {
    val numRow = inA.length
    val numCol = inB.length
    val numElem = inA(0).length

    val totalCycle = numElem + numRow.max(numCol) - 1

    for (c <- 0 until totalCycle) {
      println(s"> cycle=$c")
      for (i <- 0 until numRow) {
        dut.io.inA(i).valid.poke(1)
      }
      for (j <- 0 until numCol) {
        dut.io.inB(j).valid.poke(1)
      }
      dut.clock.step()
    }
  }

  // def flush(dut: SystolicArray, numRow:Int,)
  for ((numRow, numCol, numElem) <- List((2, 3, 4))) {
    s"Systolic array of size $numRow x $numCol" should "pass with sequantial inputs" in {
      test(new SystolicArray(numRow, numCol)) { dut =>
        val ab = createSequentialInputs(numRow, numCol, numElem)
        val a = ab._1
        val b = ab._2
        println("> a = ")
        a.foreach(e => println(e.mkString(", ")))
        println("> b = ")
        b.foreach(e => println(e.mkString(", ")))

        val fab = createFeedInputs(a, b)
        val fa = fab._1
        val fb = fab._2
        println("> fa = ")
        fa.foreach(e => println(e.mkString(", ")))
        println("> fb = ")
        fb.foreach(e => println(e.mkString(", ")))

        feed(dut, fa, fb)
      }
    }
  }
}
