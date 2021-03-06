import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.ext.VertexNameProvider
import org.jgrapht.ext.EdgeNameProvider
import org.jgrapht.ext.GraphMLExporter
import java.io.FileWriter
import org.jgrapht.graph.AbstractGraph
import org.jgrapht.graph.Pseudograph

object GraphmlExporter extends Exporter {

  def export(graph: Seq[(String, String)], filename: String) = saveToGraphML(createGraph(graph), filename)

  def createGraph(edges: Seq[(String, String)]) = {
    val graph = new Pseudograph[String, DefaultEdge](classOf[DefaultEdge]);
    val vertexSet = edges.map(e => Set(e._1, e._2)).flatten.toSet
    vertexSet.foreach(graph.addVertex(_))
    edges.foreach(edge => graph.addEdge(edge._1, edge._2))
    graph
  }

  def saveToGraphML(graph: Graph[String, DefaultEdge], filename: String) = {
    val vertexIDProvider = new VertexNameProvider[String]() {
      def getVertexName(vertex: String) = vertex
    };
    val vertexNameProvider = new VertexNameProvider[String]() {
      def getVertexName(vertex: String) = vertex
    };
    val edgeIDProvider = new EdgeNameProvider[DefaultEdge]() {
      def getEdgeName(edge: DefaultEdge) = graph.getEdgeSource(edge) + " ] " + graph.getEdgeTarget(edge)
    };
    val edgeLabelProvider = new EdgeNameProvider[DefaultEdge]() {
      def getEdgeName(edge: DefaultEdge) = edge + ""
    };
    val exporter = new GraphMLExporter[String, DefaultEdge](vertexIDProvider, vertexNameProvider,
      edgeIDProvider, edgeLabelProvider)

    exporter.export(new FileWriter(filename), graph)
    println(s"Exported graph in 'GraphML' format to file '${new java.io.File(".").getCanonicalPath().replaceAll("\\\\", "/") + "/" + filename}'.")
  }
}