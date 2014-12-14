trait Exporter {
  def export(graph: Seq[(String, String)], filename: String)
}