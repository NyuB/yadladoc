package nyub.yadladoc

import java.nio.file.{Path, Paths}

/** Key-value pairs properties
  */
trait Properties:
    def get(key: String): Option[String]
    final def getPath(key: String): Option[Path] = get(key).map(Paths.get(_))
    def getOrDefault(key: String)(default: String): String
    final def getPathOrDefault(key: String)(default: Path): Path =
        getPath(key).getOrElse(default)

    def all: List[(String, String)]
    def toMap = all.toMap

    /** Merge these properties with `overridingWith` properties
      *
      * If a property key is present in both map, the property from the argument
      * overrides the base one.
      *
      * @param overridingWith
      *   the property to extend these properties with. Property with the same
      *   key override the base ones
      * @return
      *   these properties extended and overriden with the argument properties
      */
    final def extendedWith(overridingWith: Properties): Properties =
        val extended = toMap ++ overridingWith.toMap
        Properties.ofMap(extended)

object Properties:
    /** Build properties from the given key-value pairs
      *
      * @param pairs
      *   key-value properties
      */
    def apply(pairs: (String, String)*) = ofMap(pairs.toMap)

    /** @return
      *   an empty property set
      */
    def empty = ofMap(Map.empty)

    /** Build properties from the given key-value pairs
      *
      * @param map
      *   key-value properties
      */
    def ofMap(map: Map[String, String]): Properties = PropertyMap(map)

    /** Build properties from the given formatted line
      *
      * The property must be space separated key-value pairs
      *
      * The key value pairs must be '=' separated strings without white space
      *
      * @example
      *   ```
      *   // Nominal
      *   ofLine("a=b").get("a") // Some("b")
      *   ofLine("a=b    c.d=e.f").toMap // Map("a" -> "b", "c.d" -> "e.f")
      *   // Format caveats
      *   ofLine("a=b c").get("c") // None
      *   ofLine("a=b=c").get("a") // Some("b=c")
      *   ```
      *
      * @param line
      *   space separated '=' seprated key value pairs
      */
    def ofLine(line: String): Properties =
        val map = line
            .split("\\s")
            .map(_.split("="))
            .filter(_.length == 2)
            .map(pair => pair(0) -> pair(1))
            .toMap
        ofMap(map)

    private class PropertyMap(private val map: Map[String, String])
        extends Properties:
        override def all: List[(String, String)] = map.toList
        override def toMap = map
        override def get(key: String): Option[String] = map.get(key)
        override def getOrDefault(key: String)(default: String): String =
            map.getOrElse(key, default)

        override def equals(other: Any): Boolean =
            if !other.isInstanceOf[Properties] then false
            else other.asInstanceOf[Properties].all.toMap.equals(map)

        override def toString(): String = s"PropertyMap{${map.toString()}}"
