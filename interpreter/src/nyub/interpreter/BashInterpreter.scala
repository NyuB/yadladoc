package nyub.interpreter

import java.io.{BufferedReader, InputStreamReader, PrintWriter}
import java.lang.ref.Cleaner
import java.nio.file.{Files, Path}
import scala.collection.mutable.ArrayBuffer

class BashInterpreter(val process: Process, val onClose: () => Unit)
    extends Interpreter
    with AutoCloseable:

    override def eval(line: String): Seq[String] =
        sendLine(line)
        // Use a marker to identify the last line, embed a display of the command return code
        val salt = prepareSalt(line)
        sendLine(s"echo '${salt}' [$$?]")
        val res = ArrayBuffer.empty[String]
        var lastLine = read.readLine()
        while !lastLine.contains(salt) do
            res.addOne(lastLine)
            lastLine = read.readLine()
        val returnCode = lastLine.split(s"${salt} ")(1)
        // Only print return code if not zero
        if returnCode != "[0]" then res.addOne(returnCode)
        res.toSeq

    override def close(): Unit = clean.clean()
    private val clean: Cleaner.Cleanable =
        BashInterpreter.cleaner.register(
          this,
          { () =>
              this.process.destroyForcibly(): @annotation.nowarn
              onClose()
          }
        )

    private val read = BufferedReader(
      InputStreamReader(process.getInputStream())
    )

    private val write = PrintWriter(process.getOutputStream())

    private def sendLine(line: String) =
        write.print(line)
        write.print("\n")
        write.flush()

    private var commandCount = 0
    private def prepareSalt(line: String) =
        val salt = s"${line} ${commandCount}".hashCode().toHexString
        commandCount += 1
        salt

object BashInterpreter:
    class Factory(private val bashPath: Path) extends InterpreterFactory:
        override def create(): Interpreter =
            val tempDir = Files.createTempDirectory("BashInterpreter")
            val process = ProcessBuilder(bashPath.toAbsolutePath().toString())
                .directory(tempDir.toFile())
                .redirectErrorStream(true)
                .start()
            BashInterpreter(process, () => rmRf(tempDir))

    private val cleaner = Cleaner.create()
    private def rmRf(path: Path): Unit =
        if path.toFile().isFile() then
            path.toFile().delete(): @annotation.nowarn
        else if path.toFile().isDirectory() then
            path.toFile().list().map(path.resolve(_)).foreach(rmRf)
            path.toFile().delete(): @annotation.nowarn
