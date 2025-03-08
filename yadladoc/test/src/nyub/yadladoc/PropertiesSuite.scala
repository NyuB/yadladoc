package nyub.yadladoc

import nyub.assert.AssertExtensions

class PropertiesSuite extends munit.FunSuite with AssertExtensions:
    test("Single property line"):
        Properties.ofLine("foo.bar=baz") `is equal to` Properties(
          "foo.bar" -> "baz"
        )

    test("Property access"):
        val properties = Properties("foo.bar" -> "baz")
        properties.getOrDefault("foo.bar")("goo") `is equal to` "baz"
        properties.get("zzz") `is equal to` None
        properties.getOrDefault("zzz")("Z") `is equal to` "Z"

    test("Extend with disjoint properties"):
        val base = Properties("foo.bar" -> "baz")
        val extend = Properties("foo.goo" -> "zoo")
        base.extendedWith(extend) `is equal to` Properties(
          "foo.bar" -> "baz",
          "foo.goo" -> "zoo"
        )

    test("Extend overrides common properties"):
        val base = Properties("key" -> "baseValue")
        val extend = Properties("key" -> "overrideValue")
        base.extendedWith(extend)
            .getOrDefault("key")("") `is equal to` "overrideValue"

    test("Extend with disjoint property"):
        val base = Properties("foo.bar" -> "baz")
        base.extendedWith("foo.goo" -> "zoo") `is equal to` Properties(
          "foo.bar" -> "baz",
          "foo.goo" -> "zoo"
        )

    test("Extend overrides common property"):
        val base = Properties("key" -> "baseValue")
        base.extendedWith("key" -> "overrideValue")
            .getOrDefault("key")("") `is equal to` "overrideValue"

    test("Quoted property value"):
        Properties
            .ofLine("key=\"Spaced value\"")
            .getOrDefault("key")("") `is equal to` "Spaced value"

    test("Dashes and underscores in key/value"):
        Properties
            .ofLine("some_dashed-key=some_dashed-value")
            .getOrDefault("some_dashed-key")(
              ""
            ) `is equal to` "some_dashed-value"

    test("Path components in value"):
        Properties
            .ofLine("key=some/path\\file")
            .getOrDefault("key")(
              ""
            ) `is equal to` "some/path\\file"

end PropertiesSuite
