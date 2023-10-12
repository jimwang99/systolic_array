import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

trait SystolicArrayBehavior {
  this: AnyFlatSpec with ChiselScalatestTester =>

  def createSequentialInputs(numRow: Int, numCol: Int, numElem: Int): (Array[Array[Int]], Array[Array[Int]]) = {
    println(s"createSequentialInputs() numRow=$numRow numCol=$numCol numElem=$numElem")
    val a: Array[Array[Int]] = Array.ofDim[Int](numRow, numElem)
    val b: Array[Array[Int]] = Array.ofDim[Int](numCol, numElem)

    for (i <- 0 until numRow; k <- 0 until numElem) a(i)(k) = i * numElem + k + 100
    for (j <- 0 until numCol; k <- 0 until numElem) b(j)(k) = j * numElem + k + 200

    (a, b)
  }

  def createRandomInputs(numRow: Int, numCol: Int, numElem: Int): (Array[Array[Int]], Array[Array[Int]]) = {
    println(s"createRandomInputs() numRow=$numRow numCol=$numCol numElem=$numElem")
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

    val totalCycle = numElem + numRow + numCol - 1
    println(s"createFeedInputs() numRow=$numRow numCol=$numCol numElem=$numElem totalCycle=$totalCycle")

    val fa: Array[Array[Int]] = Array.ofDim[Int](numRow, totalCycle)
    val fb: Array[Array[Int]] = Array.ofDim[Int](numCol, totalCycle)
    for (i <- 0 until numRow; k <- 0 until totalCycle) fa(i)(k) = -1
    for (j <- 0 until numCol; k <- 0 until totalCycle) fb(j)(k) = -1

    for (i <- 0 until numRow; k <- 0 until numElem) fa(i)(i + k) = a(i)(k)
    for (j <- 0 until numCol; k <- 0 until numElem) fb(j)(j + k) = b(j)(k)

    (fa, fb)
  }

  def mac(a: Array[Array[Int]], b: Array[Array[Int]]): Array[Array[Int]] = {
    val numRow = a.length
    val numCol = b.length
    val numElem = a(0).length
    assert(a(0).length == b(0).length)
    println(s"mac() numRow=$numRow numCol=$numCol numElem=$numElem")

    val z: Array[Array[Int]] = Array.ofDim[Int](numRow, numCol)

    for (i <- 0 until numRow; j <- 0 until numCol) {
      z(i)(j) = 0
      for (k <- 0 until numElem) {
        z(i)(j) += a(i)(k) * b(j)(k)
      }
    }

    z 
  }
}

class TestSystolicArray extends AnyFlatSpec with SystolicArrayBehavior with ChiselScalatestTester {

  def feed(dut: SystolicArray, inA: Array[Array[Int]], inB: Array[Array[Int]], dataWidth: Int = 32): Unit = {
    val numRow = inA.length
    val numCol = inB.length
    val totalCycle = inA(0).length

    println(s"feed() numRow=$numRow numCol=$numCol totalCycle=$totalCycle")

    dut.io.flush.poke(false.B)
    for (c <- 0 until totalCycle) {
      if (Config.TRACE) println(s"> cycle=$c")
      for (i <- 0 until numRow) {
        var valid: Boolean = false
        var data: Int = 0
        if (inA(i)(c) < 0) {
          valid = false
          data = 42
        } else {
          valid = true
          data = inA(i)(c)
        }
        if (Config.TRACE) println(s"  A($i)($c): valid=$valid data=$data")
        dut.io.inA(i).valid.poke(valid)
        dut.io.inA(i).bits.poke(data.U(dataWidth.W))
      }
      for (j <- 0 until numCol) {
        var valid: Boolean = false
        var data: Int = 0
        if (inB(j)(c) < 0) {
          valid = false
          data = 42
        } else {
          valid = true
          data = inB(j)(c)
        }
        if (Config.TRACE) println(s"  B($j)($c): valid=$valid data=$data")
        dut.io.inB(j).valid.poke(valid)
        dut.io.inB(j).bits.poke(data.U(dataWidth.W))
      }
      dut.clock.step()
    }
  }

  def flush(dut: SystolicArray, outRef: Array[Array[Int]]): Unit = {
    val numRow: Int = outRef.length
    val numCol: Int = outRef(0).length
    val totalCycle: Int = numCol
    println(s"flush() numRow=$numRow numCol=$numCol totalCycle=$totalCycle")

    dut.io.flush.poke(true.B)
    for (i <- 0 until numRow) dut.io.inA(i).valid.poke(0)
    for (j <- 0 until numCol) dut.io.inB(j).valid.poke(0)

    for (c <- 0 until totalCycle) {
      if (Config.TRACE) println(s"> cycle=$c")

      val j = numCol - 1 - c
      for (i <- 0 until numRow) {
        if (Config.TRACE) println(s">> i=$i j=$j out=" + dut.io.out(i).bits.peek().litValue + " ref=" + outRef(i)(j))
        dut.io.out(i).bits.expect(outRef(i)(j))
      }

      dut.clock.step()
    }
  }

  for ((numRow, numCol, numElem) <- List((2, 3, 4), (8, 8, 8))) {
    s"Systolic array of size $numRow x $numCol" should "pass with sequantial inputs" in {
      test(new SystolicArray(numRow, numCol)) { dut =>
        val ab = createSequentialInputs(numRow, numCol, numElem)
        val a = ab._1
        val b = ab._2
        if (Config.DEBUG) println("> a = ")
        if (Config.DEBUG) a.foreach(e => println(e.mkString(", ")))
        if (Config.DEBUG) println("> b = ")
        if (Config.DEBUG) b.foreach(e => println(e.mkString(", ")))

        val fab = createFeedInputs(a, b)
        val fa = fab._1
        val fb = fab._2
        if (Config.DEBUG) println("> fa = ")
        if (Config.DEBUG) fa.foreach(e => println(e.mkString(", ")))
        if (Config.DEBUG) println("> fb = ")
        if (Config.DEBUG) fb.foreach(e => println(e.mkString(", ")))

        feed(dut, fa, fb)

        val z = mac(a, b)
        if (Config.DEBUG) println("> z = ")
        if (Config.DEBUG) z.foreach(e => println(e.mkString(", ")))

        flush(dut, z)
      }
    }
  }
}
