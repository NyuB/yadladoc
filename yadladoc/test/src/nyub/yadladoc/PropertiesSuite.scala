package nyub.yadladoc

import nyub.assert.AssertExtensions

class PropertiesSuite extends munit.FunSuite with AssertExtensions:
    test("Single property line"):
        Properties.ofLine("foo.bar=baz") isEqualTo Properties(
          "foo.bar" -> "baz"
        )

    test("Property access"):
        val properties = Properties("foo.bar" -> "baz")
        properties.getOrDefault("foo.bar")("goo") isEqualTo "baz"
        properties.get("zzz") isEqualTo None
        properties.getOrDefault("zzz")("Z") isEqualTo "Z"

    test("Extend with disjoint properties"):
        val base = Properties("foo.bar" -> "baz")
        val extend = Properties("foo.goo" -> "zoo")
        base.extendedWith(extend) isEqualTo Properties(
          "foo.bar" -> "baz",
          "foo.goo" -> "zoo"
        )

    test("Extend overrides common properties"):
        val base = Properties("key" -> "baseValue")
        val extend = Properties("key" -> "overrideValue")
        base.extendedWith(extend)
            .getOrDefault("key")("") isEqualTo "overrideValue"

end PropertiesSuite
