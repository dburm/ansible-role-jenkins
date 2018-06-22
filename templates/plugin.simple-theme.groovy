#!groovy

import jenkins.model.Jenkins

class Actions {
    Actions(out) {
        this.out = out
    }

    def out
    def instance = Jenkins.instance
    def extensions = hudson.model.PageDecorator.all()
    Boolean changed = false

    def classLoader = instance.pluginManager.uberClassLoader

    def defaultTheme = [
            css_url: '',
            extra_css: '',
            js_url: '',
            favicon_url: ''
        ]

    Boolean compareObjects( Object a, b) {
        return Jenkins.XSTREAM.toXML(a) == Jenkins.XSTREAM.toXML(b)
    }

    void configure(params) {
        def themeParams = defaultTheme + params
        String themeClassName = 'org.codefirst.SimpleThemeDecorator'
        def theme = extensions.findByName(themeClassName)
        def newTheme
        try {
            newTheme = classLoader.loadClass(themeClassName).newInstance()
        } catch (ClassNotFoundException ex) { }
        if (newTheme) {
            newTheme.cssUrl = themeParams.css_url
            newTheme.cssRules = themeParams.extra_css
            newTheme.jsUrl = themeParams.js_url
            newTheme.faviconUrl = themeParams.favicon_url
            if (!compareObjects(theme, newTheme)) {
                extensions.remove(theme)
                extensions.add(newTheme)
                changed = true
            }
        }
    }
}

def params = new groovy.json.JsonSlurperClassic()
    .parseText('''{{ jenkins.theme | to_json}}''')
def actions = new Actions(out)

actions.configure(params)

if (actions.changed) {
    actions.instance.save()
    println 'CHANGED'
} else {
    println 'EXISTS'
}
