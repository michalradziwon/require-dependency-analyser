import java.io.PrintWriter

object TxtExporter extends Exporter {

  def export(graph: Seq[(String, String)], filename: String) {
    Some(new PrintWriter(filename)).foreach{p => p.write(graph.mkString("\n")); p.close}
    println(s"Exported graph in 'txt' format to file '${new java.io.File(".").getCanonicalPath().replaceAll("\\\\", "/") + "/" + filename}'.")
  }
}