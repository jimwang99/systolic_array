import chisel3._
import chisel3.util._

class SystolicArrayProcElem(dataWidth: Int = 32, rowIdx: Int = 0, colIdx: Int = 0) extends Module {
  val io = IO(new Bundle {
    val inA = Flipped(ValidIO(UInt(dataWidth.W)))
    val inB = Flipped(ValidIO(UInt(dataWidth.W)))
    val outA = ValidIO(UInt(dataWidth.W))
    val outB = ValidIO(UInt(dataWidth.W))
    val flush = Input(Bool())
  })

  // registers
  val regValidA = RegInit(0.U(1.W)); regValidA := io.inA.valid
  val regValidB = RegInit(0.U(1.W)); regValidB := io.inB.valid
  val regDataA = RegEnable(io.inA.bits, io.inA.valid)
  val regDataB = RegEnable(io.inB.bits, io.inB.valid)

  // accumulator
  val regAcc = RegInit(0.U(dataWidth.W))

  when (io.flush === 0.U) {
    when (io.inA.valid & io.inB.valid) {
      regAcc := regAcc + io.inA.bits * io.inB.bits
      if (Config.DEBUG) printf(cf"PE [$rowIdx][$colIdx] inA=${io.inA.bits} inB=${io.inB.bits} regAcc=$regAcc\n")
    }
  } .otherwise {
    regAcc := io.inA.bits
  }

  // output
  io.outA.valid := regValidA
  when (io.flush === 0.U) {
    io.outA.bits := regDataA
  } .otherwise {
    io.outA.bits := regAcc
    if (Config.DEBUG) printf(cf"PE [$rowIdx][$colIdx] outA=${io.outA.bits}\n")
  }
  io.outB.valid := regValidB
  io.outB.bits := regDataB
}

class SystolicArray(numRow: Int = 4, numCol: Int = 8, dataWidth: Int = 32) extends Module {
  val io = IO(new Bundle {
    val inA = Flipped(Vec(numRow, ValidIO(UInt(dataWidth.W))))
    val inB = Flipped(Vec(numCol, ValidIO(UInt(dataWidth.W))))
    val out = Vec(numRow, ValidIO(UInt(dataWidth.W)))
    val flush = Input(Bool())
  })

  val validV = Wire(Vec(numRow, Vec(numCol, Bool())))
  val dataV = Wire(Vec(numRow, Vec(numCol, UInt(dataWidth.W))))
  val validH = Wire(Vec(numRow, Vec(numCol, Bool())))
  val dataH = Wire(Vec(numRow, Vec(numCol, UInt(dataWidth.W))))

  val pes = for (i <- 0 until numRow; j <- 0 until numCol) yield {
    val pe = Module(new SystolicArrayProcElem(dataWidth, i, j))

    if (i == 0) {
      pe.io.inB <> io.inB(j)
    } else {
      pe.io.inB.valid <> validV(i-1)(j)
      pe.io.inB.bits <> dataV(i-1)(j)
    }
    if (j == 0) {
      pe.io.inA <> io.inA(i)
    } else {
      pe.io.inA.valid <> validH(i)(j-1)
      pe.io.inA.bits <> dataH(i)(j-1)
    }

    pe.io.flush <> io.flush

    pe.io.outB.valid <> validV(i)(j)
    pe.io.outB.bits <> dataV(i)(j)
    pe.io.outA.valid <> validH(i)(j)
    pe.io.outA.bits <> dataH(i)(j)

    pe
  }


  for (i <- 0 until numRow) {
    io.out(i).valid := io.flush
    io.out(i).bits := dataH(i).last
    when (io.flush === 1.U) {
      if (Config.DEBUG) printf(cf"SA io.out[$i]=${io.out(i).bits}\n")
    }
  }
}
