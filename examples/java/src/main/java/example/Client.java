package example;

import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * Teavm sample: https://github.com/konsoletyper/teavm/tree/master/samples/hello
 *
 * @author Vyacheslav Rusakov
 * @since 11.01.2023
 */
public class Client {
    private static HTMLDocument document = Window.current().getDocument();
    private static HTMLButtonElement helloButton = document.getElementById("hello-button").cast();
    private static HTMLElement responsePanel = document.getElementById("response-panel");
    private static HTMLElement thinkingPanel = document.getElementById("thinking-panel");

    private Client() {
    }

    public static void main(String[] args) {
        helloButton.listenClick(evt -> sayHello());
    }

    private static void sayHello() {
        helloButton.setDisabled(true);
        thinkingPanel.getStyle().setProperty("display", "");
        XMLHttpRequest xhr = XMLHttpRequest.create();
        xhr.onComplete(() -> receiveResponse(xhr.getStatus() == 200 ? xhr.getResponseText() : xhr.getStatusText()));
        xhr.open("GET", "hello");
        xhr.send();
    }

    private static void receiveResponse(String text) {
        HTMLElement responseElem = document.createElement("div");
        responseElem.appendChild(document.createTextNode(text));
        responsePanel.appendChild(responseElem);
        helloButton.setDisabled(false);
        thinkingPanel.getStyle().setProperty("display", "none");
    }
}
