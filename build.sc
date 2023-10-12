// import Mill dependency
import mill._
import mill.define.Sources
import mill.modules.Util
import mill.scalalib.TestModule.ScalaTest
import scalalib._

trait ChiselModule extends ScalaModule {
  override def scalaVersion = "2.13.8"
  override def sources = T.sources(millSourcePath)
  override def ivyDeps = Agg(
    ivy"org.chipsalliance::chisel:5.0.0",
    ivy"edu.berkeley.cs::chiseltest:5.0.0"
  )
  override def scalacPluginIvyDeps = Agg(
    ivy"org.chipsalliance:::chisel-plugin:5.0.0",
  )
}

object rtl extends ChiselModule {
}
