package example

import org.teavm.jso.browser.Window
import org.teavm.jso.dom.events._
import org.teavm.jso.dom.html._

/*
 * https://github.com/konsoletyper/teavm/tree/master/samples/scala
 */
object Client {
  def main(args: Array[String]) {
      val doc = HTMLDocument.current

      doc.getElementById("hello-scala").listenClick((e: MouseEvent) => { Window.alert("Hello, developer!") })
  }
}
