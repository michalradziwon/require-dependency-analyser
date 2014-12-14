import java.util.concurrent.atomic.AtomicInteger
import java.io.PrintWriter

object CytoscapeExporter extends Exporter {
  def export(graph: Seq[(String, String)], filename: String) = {

    val vertexSet = graph.map(e => Set(e._1, e._2)).flatten.toSet
    val actions = vertexSet.filter(e => e.endsWith("Actions") || e.endsWith("Action"))
    val stores = vertexSet.filter(e => e.endsWith("Store"))
    val other = vertexSet -- actions -- stores

    var seq = new AtomicInteger
    val elements = ("""
  {
    nodes: [ 
      { data: { id: 'Actions' } },
      { data: { id: 'Stores' } },
      { data: { id: 'Components' } },""" +
      (actions.map(node => "{ data: { id: '" + node + "', parent: 'Actions' } }") ++
        stores.map(node => "{ data: { id: '" + node + "', parent: 'Stores' } }") ++
        other.map(node => "{ data: { id: '" + node + "', parent: 'Components' } }")).mkString(",")
        + """
    ],
    edges: [""" +
        graph.map(e => "{ data: { id: 'id_" + seq.incrementAndGet() + "', source: '" + e._1 + "', target: '" + e._2 + "' } }").mkString(",") +
        """]
  }""")

    var script = """$(function () { // on dom ready
  
  cy = cytoscape({
    container: document.getElementById('cy'),
    
    style: [
      {
        selector: 'node',
        css: {
          'content': 'data(id)',
          'text-valign': 'center',
          'text-halign': 'center'
        }
      },
      {
        selector: '$node > node',
        css: {
          'padding-top': '10px',
          'padding-left': '10px',
          'padding-bottom': '10px',
          'padding-right': '10px',
          'text-valign': 'top',
          'text-halign': 'center'
        }
      },
      {
        selector: 'edge',
        css: {
          'target-arrow-shape': 'triangle'
        }
      },
      {
        selector: ':selected',
        css: {
          'background-color': 'black',
          'line-color': 'black',
          'target-arrow-color': 'black',
          'source-arrow-color': 'black'
        }
      }
    ],
    
    elements: """ + elements + """,
    
    layout: {
      name: 'cose',
      padding: 5
    }
  });
  
}); // on dom ready"""

    var html = """<!DOCTYPE html>
<html>
<head>
<link href="style.css" rel="stylesheet" />
<meta charset=utf-8 />
<title>Cytoscape.js initialisation</title>
<script src="http://ajax.googleapis.com/ajax/libs/jquery/1/jquery.min.js"></script>
<script src="http://cytoscape.github.io/cytoscape.js/api/cytoscape.js-latest/cytoscape.min.js"></script>
<script>""" + script + """</script>
<style>body { 
  font: 14px helvetica neue, helvetica, arial, sans-serif;
}

#cy {
  height: 100%;
  width: 100%;
  position: absolute;
  left: 0;
  top: 0;
}</style>
</head>
<body>
<div id="cy"></div>
</body>
</html>"""

    Some(new PrintWriter(filename)).foreach { p => p.write(html); p.close }
    println(s"Exported graph in 'CytoscapeJs' format to file '${new java.io.File(".").getCanonicalPath().replaceAll("\\\\", "/") + "/" + filename}'.")
  }
}

