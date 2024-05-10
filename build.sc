import mill._, scalalib._

object Versions {
    val scala = "3.4.1"
    val munit = "0.7.29"
}

trait SharedConfiguration extends ScalaModule {
    override def scalaVersion: T[String] = Versions.scala
    override def scalacOptions: T[Seq[String]] = Seq("-deprecation")

    trait Tests extends ScalaTests with TestModule.Munit {
        override def ivyDeps = super.ivyDeps() ++ Agg(
            ivy"org.scalameta::munit:${Versions.munit}",
            ivy"org.scalameta::munit-scalacheck:${Versions.munit}",
        )
        override def moduleDeps = super.moduleDeps ++ Seq(assert_extensions)
    }
}

object assert_extensions extends ScalaModule with SharedConfiguration {
    override def ivyDeps = super.ivyDeps() ++ Agg(
        ivy"org.scalameta::munit:${Versions.munit}",
        ivy"org.scalameta::munit-scalacheck:${Versions.munit}",
    )
    object test extends Tests { }
}

object filesystem extends ScalaModule with SharedConfiguration {
    object test extends Tests
}

object interpreter extends ScalaModule with SharedConfiguration {
    object test extends Tests 
}

object yadladoc extends ScalaModule with SharedConfiguration {
    override def moduleDeps = super.moduleDeps ++ Seq(filesystem)

    object test extends Tests
}

object yadladoc_app extends ScalaModule with SharedConfiguration {
    override def moduleDeps = super.moduleDeps ++ Seq(yadladoc)
    def ydocJar = T {
        os.copy(assembly().path, millSourcePath / os.up / "usage" / "ydoc.jar", replaceExisting = true)
    }
}
