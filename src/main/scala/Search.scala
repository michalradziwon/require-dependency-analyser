object Search {
  def main(args: Array[String]) {
    runTests();
    val projectPath = if (args.isEmpty) "." else args(0)
    val jsFiles = recursivelyListFiles(new java.io.File(projectPath)).filter(byExtension(List("js", "jsx")))

    // Array[(java.io.File, List[String])] - array of tuples (file, line[]) with all .js and .jsx files
    val jsFilesContent = jsFiles.map(file => (file, scala.io.Source.fromFile(file).getLines.toList))

    // Array[(String, List[String])] - array of tuples (jsFilePathRelativeToProjectPath, dependecyRelativePath[])
    val jsFilesWithDependencies = jsFilesContent.map(e => (useSlashInsteadOfBackslash(e._1.getAbsolutePath().substring(projectPath.length() + 1)), e._2.map(extractRequiredFiles).flatten)).filterNot(_._2.isEmpty)

    // Array[(String, String)] - array with pairs (file,dependencyFile)
    var dependencies = jsFilesWithDependencies.map(e => e._2.map(dep => (e._1, joinPaths(e._1, dep, projectPath)))).flatten

    println(s"There is ${jsFilesContent.size} files with .js and .jsx extenstions")
    println(s"There is ${jsFilesWithDependencies.size} JS files with declared 'require' dependencies")
    jsFilesWithDependencies.foreach(e => { println(e._1); e._2.foreach(f => println("   " + f)) })
    println("\n" * 5)
    dependencies.foreach(println)
    
    val graph = GraphmlExporter.createGraph(dependencies);
    GraphmlExporter.saveToGraphML(graph, "dependencies.graphml");

    // TODO to be finished!!!!
    // TODO add following functionality:
    // * if file does not end with .js it means that it is .js by default ... it should be handled
    // * optional ignoring of external (3rd party) dependencies
  }

  def useSlashInsteadOfBackslash(path: String) = path.replaceAll("\\\\", "/")
  def recursivelyListFiles(f: java.io.File): Array[java.io.File] = f.listFiles ++ f.listFiles.filter(_.isDirectory).flatMap(recursivelyListFiles)
  def byExtension(acceptedExtensions: List[String]) = { f: java.io.File => acceptedExtensions.contains(f.getPath().split("\\.").last) }
  def extractRequiredFiles(s: String) = """.*require\(["']([^\)]*)["']\).*""".r.findAllIn(s).matchData.map(_.group(1)).toList
  def joinPaths(path: String, relative: String, base: String) =
    if ('/'.equals(relative(0)))
      throw new UnsupportedOperationException("Absoulute paths are not supported. " + relative)
    else
      new java.io.File(base + "/" + path + "/../" + relative).getCanonicalPath().replace(new java.io.File(base).getCanonicalPath(), "").replaceAll("\\\\", "/").substring(1) // TODO temporary ugly impl. use scalax.file.Path from scala io

  /**
   * TESTS. each line contains different test
   */
  def runTests() {
    def assertEquals(a: Any, b: Any) = if (!a.equals(b)) throw new IllegalStateException(s"\n${a} \n not equal to: \n${b}")
    val e = assertEquals _

    e(List(new java.io.File("/dev/dir/Main.js"), new java.io.File("/dev/dir/Main.jsx"), new java.io.File("/dev/dir/Main.java")).filter(byExtension(List("js", "jsx"))), List(new java.io.File("/dev/dir/Main.js"), new java.io.File("/dev/dir/Main.jsx")))

    e(extractRequiredFiles("""require("./File.js")"""), List("./File.js"))
    e(extractRequiredFiles("""require('../File.js')"""), List("../File.js"))
    e(extractRequiredFiles("""requ('../File.js')"""), List())
    e(extractRequiredFiles("""require '../File.js')"""), List())
    e(extractRequiredFiles("""require('../File.js',)"""), List())

    e(joinPaths("""js\service\BatchSpec.js""", """./../Service""", new java.io.File(".").getCanonicalPath()), """js/Service""")
    e(joinPaths("""js\service\BatchSpec.js""", """./Service""", new java.io.File(".").getCanonicalPath()), """js/service/Service""")
    e(joinPaths("""js\service\BatchSpec.js""", """../Service""", new java.io.File(".").getCanonicalPath()), """js/Service""")
    e(joinPaths("""js\service\BatchSpec.js""", """../../lib/CrazyLib.js""", new java.io.File(".").getCanonicalPath()), """lib/CrazyLib.js""")
    e(joinPaths("""js\main.js""", """./Service.js""", """C:\CodeMobile\MobileWebDemo\app\"""), """js/Service.js""")
    try { joinPaths("""js\main.js""", """/absolute/path/not/supported/Service.js""", """C:\CodeMobile\MobileWebDemo\app\"""); throw new IllegalStateException("Should throw exception here"); } catch { case e: UnsupportedOperationException => {} }

    println("Tests ok")
  }

}
