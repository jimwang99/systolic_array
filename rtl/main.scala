import circt.stage.ChiselStage
import org.scalatest.run

object Config {
  // val DEBUG = true
  val DEBUG = false

  // val TRACE = true
  val TRACE = false
}

object VerilogMain extends App {
  println("====== Emit SystemVerilog file ======")
  ChiselStage.emitSystemVerilogFile(new SystolicArray, Array("--target-dir", "gen/misc", "--full-stacktrace"))

  println("====== Run Tests ======")
  run(new TestSystolicArray)
}
