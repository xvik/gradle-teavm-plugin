package example

import org.teavm.jso.browser.*

/*
 * https://github.com/konsoletyper/teavm/tree/master/samples/kotlin
 */

fun main(args : Array<String>) {
    val document = Window.current().document

    document.getElementById("hello-kotlin").addEventListener("click") { Window.alert("Hello, developer!") }
}
