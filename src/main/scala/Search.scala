object Search {

  /**
   * Program arguments: ${projectPath} ${ignoredPaths} ${ignoredModules}
   * 	${projectPath} - absolute path to analysed project path directory
   *  	${ignoredPaths} - project's subpath that will be ignored during a graph generation; separated with a colon
   *   	${ignoredModules} - modules skipped during a graph generation; separated by a colon
   *   	TODO: TO BE IMPLEMENTED: ${outputFormat} - generated graph's output format; one of ["GraphML", "txt"]
   * 	Example program arguments: ~/projects/Project1/src js/__tests__ js/environment/Logger
   */
  def main(args: Array[String]) {
    runTests();
    val projectPath = if (args.isEmpty) "." else args(0)
    val ignoredPaths = if (args.length >= 2) args(1).split(":").toSet[String].map(p => new java.io.File(projectPath + "/" + p)) else Set[java.io.File]()
    val ignoredModules = if (args.length >= 3) args(2).split(":").toSet[String] else Set[String]()

    println("IGNORED PATHS:")
    ignoredPaths.foreach(println(_))
    println("")
    println("IGNORED MODULES:")
    ignoredModules.foreach(println(_))
    println("")

    val jsFiles = recursivelyListFiles(new java.io.File(projectPath)).filter(byExtension(List("js", "jsx")))
    val notIgnoredJsFiles = jsFiles.filter(file => ignoredPaths.filter(ignoredPath => file.getCanonicalPath().startsWith(ignoredPath.getCanonicalPath())).isEmpty)

    // Array[(java.io.File, List[String])] - array of tuples (file, line[]) with all .js and .jsx files
    val jsFilesContent = notIgnoredJsFiles.map(file => (file, scala.io.Source.fromFile(file).getLines.toList))

    // Array[(String, List[String])] - array of tuples (jsFilePathRelativeToProjectPath, dependecyRelativePath[])
    val jsFilesWithDependencies = jsFilesContent.map(e => (useSlashInsteadOfBackslash(e._1.getAbsolutePath().substring(projectPath.length() + 1)), e._2.map(extractRequiredFiles).flatten)).filterNot(_._2.isEmpty)

    // similar to the previous one, but without global dependencies
    val jsFileWithDependenciesWithoutGlobalDependencies = jsFilesWithDependencies.map(e => (e._1, e._2.filter(_.startsWith("."))))

    // Array[(String, String)] - array with pairs (file,dependencyFile); file names are normalized (.js extenstion is removed)
    var dependencies = jsFileWithDependenciesWithoutGlobalDependencies.map(e => e._2.map(dep => (e._1, joinPaths(e._1, dep, projectPath)))).flatten
    var normalizedDependencies = dependencies.map(e => (normalizeFileName(e._1), normalizeFileName(e._2)))
    var normalizedDependenciesWithoutIgnoredModules = normalizedDependencies.filter(e => !ignoredModules.contains(e._1) && !ignoredModules.contains(e._2))

    println(s"There is ${jsFilesContent.size} files with .js and .jsx extenstions")
    println(s"There is ${jsFileWithDependenciesWithoutGlobalDependencies.size} JS files with declared 'require' dependencies")

    GraphmlExporter.export(normalizedDependenciesWithoutIgnoredModules, "dependencies.graphml")
    TxtExporter.export(normalizedDependenciesWithoutIgnoredModules, "depencencies.txt")

  }

  /**
   * if file does not end with .js it means that it is .js by default
   */
  def normalizeFileName(fileName: String) = if (fileName.endsWith(".js")) fileName.dropRight(".js".length) else fileName

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
    e(joinPaths("""js\main.js""", """./Service.js""", """C:\src\Project1\app\"""), """js/Service.js""")
    try { joinPaths("""js\main.js""", """/absolute/path/not/supported/Service.js""", """C:\src\Project1\app\"""); throw new IllegalStateException("Should throw exception here"); } catch { case e: UnsupportedOperationException => {} }

    println("Tests ok")
  }

}
