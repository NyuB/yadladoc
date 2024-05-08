import mill._, scalalib._

trait SharedConfiguration extends ScalaModule {
    override def scalaVersion: T[String] = "3.4.1"
    override def scalacOptions: T[Seq[String]] = Seq("-deprecation")
}

object assert_extensions extends ScalaModule with SharedConfiguration {
    def ivyDeps = Agg(
        ivy"org.scalameta::munit:0.7.29",
        ivy"org.scalameta::munit-scalacheck:0.7.29",
    )
    object test extends ScalaTests with TestModule.Munit { }
}

object yadladoc extends ScalaModule with SharedConfiguration {
    object test extends ScalaTests with TestModule.Munit {
        override def moduleDeps = super.moduleDeps ++ Seq(assert_extensions)
        override def ivyDeps = Agg(
            ivy"org.scalameta::munit:0.7.29",
            ivy"org.scalameta::munit-scalacheck:0.7.29",
        )
    }
}

object yadladoc_app extends ScalaModule with SharedConfiguration {
    override def moduleDeps = super.moduleDeps ++ Seq(yadladoc)
    def ydocJar = T {
        os.copy(assembly().path, millSourcePath / os.up / "usage" / "ydoc.jar", replaceExisting = true)
    }
}
