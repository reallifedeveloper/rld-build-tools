RLD-BUILD-TOOLS
===============

Java support classes and configuration files that are used only during build-time, not runtime, e.g., code to support testing.

Before building, you need to install the [parent POM](https://github.com/reallifedeveloper/rld-parent).

To build with all quality checks enabled:

    mvn -DcheckAll clean install

To create a Maven site with documentation, including Javadoc, in `target/site/index.html`:

    mvn -P coverage clean integration-test site

For more information, see <http://reallifedeveloper.com/>.
