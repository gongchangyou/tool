import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = "com.sample;"
typeMapping = [
    (~/bigint/)                      : "Long",
  (~/(?i)int/)                      : "Int",
  (~/(?i)float|double|decimal|real/): "Double",
  (~/(?i)datetime|timestamp/)       : "java.sql.Timestamp",
  (~/(?i)date/)                     : "java.sql.Date",
  (~/(?i)time/)                     : "java.sql.Time",
  (~/(?i)/)                         : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
  SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
  def className = javaName(table.getName(), true)
  new File(dir, className + "MysqlModel.kt").withPrintWriter { out -> generate(out, className, table) }
}

def generate(out, className, table) {
  out.println "package $packageName"
  out.println ""
  out.println "import org.springframework.data.annotation.Id"
  out.println "import org.springframework.data.relational.core.mapping.Column"
  out.println "import org.springframework.data.relational.core.mapping.Table"
  out.println ""
  out.println ""
  out.println "@Table(\""+ table.getName() + "\")"
  out.println "data class " + className + "MysqlModel ("
  out.println ""

    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        def comment = col.getComment()
        if (comment != ""){
            out.println "  /**"
            out.println "   * ${comment}"
            out.println "   */"
        }

        if (DasUtil.isPrimary(col)) {
            out.println "  @Id"
        }

        out.println "  @Column(\"${col.getName()}\")"
        out.println "  var ${javaName(col.getName(), false)}: $typeStr ?=null,"
    }


  out.println ""
  out.println ")"
}

def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())
    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    fields += [[
                 name : javaName(col.getName(), false),
                 type : typeStr,
                 annos: col.getComment()]]
  }
}

def javaName(str, capitalize) {
  def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
    .collect { Case.LOWER.apply(it).capitalize() }
    .join("")
    .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
  capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
