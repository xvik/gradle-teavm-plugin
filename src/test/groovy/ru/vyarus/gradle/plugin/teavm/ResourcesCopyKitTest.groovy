package ru.vyarus.gradle.plugin.teavm

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * @author Vyacheslav Rusakov
 * @since 12.01.2023
 */
class ResourcesCopyKitTest extends AbstractKitTest {

    def "Check mixed resources support"() {
        setup:
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.teavm'
            }
            
            repositories { mavenCentral() }
            dependencies {
                implementation "org.teavm:teavm-classlib:\${teavm.version}"
                implementation "org.teavm.flavour:teavm-flavour-widgets:0.2.1"
                implementation "org.teavm.flavour:teavm-flavour-rest:0.2.1"
                implementation "com.fasterxml.jackson.core:jackson-annotations:2.5.4"
            }

            teavm {
                mixedResources = true
                debug = true
                mainClass = 'example.Client'
            }

        """
        file('src/main/java/example/Client.java')  << """
package example;

import org.teavm.flavour.templates.BindTemplate;
import org.teavm.flavour.widgets.ApplicationTemplate;

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
"""

        file('src/main/java/example/client.html')  << """
<div>
    <label>Please, enter your name</label>:
    <input type="text" html:value="userName" html:change="userName"/>
</div>

<div>
    Hello, <i><html:text value="userName"/></i>
</div>
"""

        when: "run task"
        debug()
        BuildResult result = run('compileTeavm')

        then: "task successful"
        result.task(':compileTeavm').outcome == TaskOutcome.SUCCESS
        result.output.contains('Output file successfully built')
        result.output.contains("""Mixed resources mode for source set 'main': 
\tsrc/main/java""".replace('/', File.separator))
    }

    def "Check build fails without mixed resources"() {
        setup:
        build """
            plugins {
                id 'java'
                id 'ru.vyarus.teavm'
            }
            
            repositories { mavenCentral() }
            dependencies {
                implementation "org.teavm:teavm-classlib:\${teavm.version}"
                implementation "org.teavm.flavour:teavm-flavour-widgets:0.2.1"
                implementation "org.teavm.flavour:teavm-flavour-rest:0.2.1"
                implementation "com.fasterxml.jackson.core:jackson-annotations:2.5.4"
            }

            teavm {
                mainClass = 'example.Client'
            }

        """
        file('src/main/java/example/Client.java')  << """
package example;

import org.teavm.flavour.templates.BindTemplate;
import org.teavm.flavour.widgets.ApplicationTemplate;

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
"""

        file('src/main/java/example/client.html')  << """
<div>
    <label>Please, enter your name</label>:
    <input type="text" html:value="userName" html:change="userName"/>
</div>

<div>
    Hello, <i><html:text value="userName"/></i>
</div>
"""

        when: "run task"
        debug()
        BuildResult result = runFailed('compileTeavm')

        then: "task failed"
        result.task(':compileTeavm').outcome == TaskOutcome.FAILED
        result.output.contains('ERROR: Can\'t create template for example.Client: template example/client.html was not found')
    }
}
