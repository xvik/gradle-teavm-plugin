package example;

import org.teavm.flavour.templates.BindTemplate;
import org.teavm.flavour.widgets.ApplicationTemplate;

/**
 * Flavour archetype example: https://github.com/konsoletyper/teavm-flavour/tree/master/archetype
 *
 * @author Vyacheslav Rusakov
 * @since 12.01.2023
 */
@BindTemplate("example/client.html")
public class Client extends ApplicationTemplate {
    private String userName = "";

    public static void main(String[] args) {
        Client client = new Client();
        client.bind("application-content");
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
