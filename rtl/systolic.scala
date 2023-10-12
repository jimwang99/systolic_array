import chisel3._
import chisel3.util._

class SystolicArrayProcElem(dataWidth: Int = 32) extends Module {
  val io = IO(new Bundle {
    val inA = Flipped(ValidIO(UInt(dataWidth.W)))
    val inB = Flipped(ValidIO(UInt(dataWidth.W)))
    val outA = ValidIO(UInt(dataWidth.W))
    val outB = ValidIO(UInt(dataWidth.W))
    val flush = Input(Bool())
  })

  val regValidA = RegInit(0.U(1.W)); regValidA := io.inA.valid
  val regValidB = RegInit(0.U(1.W)); regValidB := io.inB.valid
  val regDataA = RegEnable(io.inA.bits, io.inA.valid)
  val regDataB = RegEnable(io.inB.bits, io.inB.valid)

  val regAcc = RegInit(0.U(dataWidth.W))

  when (io.inA.valid & io.inB.valid & (io.flush === 0.U)) {
    regAcc := regAcc + io.inA.bits * io.inB.bits
  }

  when (io.flush === 0.U) {
    io.outA.valid := regValidA
    io.outA.bits := regDataA
  } .otherwise {
    io.outA.valid := 1.U
    io.outA.bits := regDataA
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
    val pe = Module(new SystolicArrayProcElem(dataWidth))

    pe.io.outA.valid <> validH(i)(j)
    pe.io.outA.bits <> dataH(i)(j)
    pe.io.outB.valid <> validV(i)(j)
    pe.io.outB.bits <> dataV(i)(j)

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

    pe
  }

  val regFlush = RegNext(io.flush, 0.U(1.W))

  for (i <- 0 until numRow) {
    io.out(i).valid := regFlush
    io.out(i).bits := dataH(i).last
  }
}
