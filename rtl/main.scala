import circt.stage.ChiselStage
import org.scalatest.run

object VerilogMain extends App {
  println("====== Emit SystemVerilog file ======")
  ChiselStage.emitSystemVerilogFile(new SystolicArray, Array("--target-dir", "gen/misc", "--full-stacktrace"))

  println("====== Run Tests ======")
  run(new TestSystolicArray)
}
