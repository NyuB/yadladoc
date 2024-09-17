import $ivy.`com.goyeau::mill-scalafix::0.4.1`
import com.goyeau.mill.scalafix.ScalafixModule

import mill._, scalalib._

object Versions {
    val scala = "3.4.1"
    val munit = "1.0.0"
    val munit_diff = munit
}

trait SharedConfiguration extends ScalaModule with ScalafixModule {
    override def scalaVersion: T[String] = Versions.scala
    override def scalacOptions: T[Seq[String]] =
        Seq(
          "-deprecation",
          "-Werror",
          "-Wimplausible-patterns",
          "-Wnonunit-statement",
          "-WunstableInlineAccessors",
          "-Wunused:all",
          "-Wvalue-discard",
          "-Xlint:all"
        )

    trait Tests extends ScalaTests with TestModule.Munit {
        override def ivyDeps = super.ivyDeps() ++ Agg(
          ivy"org.scalameta::munit:${Versions.munit}",
          ivy"org.scalameta::munit-scalacheck:${Versions.munit}"
        )

        override def moduleDeps = super.moduleDeps ++ Seq(assert_extensions)
    }

}

object assert_extensions extends ScalaModule with SharedConfiguration {
    override def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"org.scalameta::munit:${Versions.munit}",
      ivy"org.scalameta::munit-scalacheck:${Versions.munit}"
    )

    object test extends Tests
}

object filesystem extends ScalaModule with SharedConfiguration {
    object test extends Tests
}

object interpreter extends ScalaModule with SharedConfiguration {
    object test extends Tests
}

object yadladoc extends ScalaModule with SharedConfiguration {
    override def moduleDeps = super.moduleDeps ++ Seq(filesystem, interpreter)
    override def ivyDeps = super.ivyDeps() ++ Agg(
      ivy"org.scalameta::munit-diff::${Versions.munit_diff}"
    )

    object test extends Tests
}

object yadladoc_app extends ScalaModule with SharedConfiguration {
    override def moduleDeps = super.moduleDeps ++ Seq(yadladoc)
    def ydocJar = T {
        os.copy(
          assembly().path,
          millSourcePath / os.up / "usage" / "ydoc.jar",
          replaceExisting = true
        )
    }

}
