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

end PropertiesSuite
