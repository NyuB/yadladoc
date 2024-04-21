package nyub.yadladoc

import java.nio.file.{Path, Paths}

trait Properties:
    def get(key: String): Option[String]
    final def getPath(key: String): Option[Path] = get(key).map(Paths.get(_))
    def getOrDefault(key: String)(default: String): String
    final def getPathOrDefault(key: String)(default: Path): Path =
        getPath(key).getOrElse(default)

    def all: List[(String, String)]
    final def extendedWith(overridingWith: Properties): Properties =
        val extended = Map(all*) ++ Map(overridingWith.all*)
        Properties.ofMap(extended)

object Properties:
    def apply(pairs: (String, String)*) = ofMap(pairs.toMap)
    def empty = ofMap(Map.empty)
    def ofMap(map: Map[String, String]): Properties = PropertyMap(map)
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
        override def get(key: String): Option[String] = map.get(key)
        override def getOrDefault(key: String)(default: String): String =
            map.getOrElse(key, default)

        override def equals(other: Any): Boolean =
            if !other.isInstanceOf[Properties] then false
            else other.asInstanceOf[Properties].all.toMap.equals(map)

        override def toString(): String = s"PropertyMap{${map.toString()}}"
