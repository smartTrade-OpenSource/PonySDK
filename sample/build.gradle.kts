dependencies {
    implementation(project(":ponysdk"))
    implementation(libs.spring.context)
    implementation(libs.json.api)
    implementation(libs.servlet.api)
    implementation(libs.slf4j.api)
    implementation(libs.bundles.jetty.ws)
    implementation(libs.java2html)
    implementation(libs.commons.beanutils)
    implementation(libs.commons.io)
    implementation(libs.commons.fileupload)
    implementation(libs.gson)

    runtimeOnly(libs.spring.aop)
    runtimeOnly(libs.spring.expression)
    runtimeOnly(libs.gwt.codeserver)
    runtimeOnly(libs.bundles.slf4j)
    runtimeOnly(libs.bundles.log4j)
}

tasks.register<JavaExec>("runCodeServer") {
    classpath += project(":ponysdk").configurations.getByName("gwtDev")
    classpath += project(":ponysdk").configurations.getByName("gwt")
    classpath += project(":ponysdk").sourceSets["main"].java.sourceDirectories
    classpath += sourceSets["main"].java.sourceDirectories
    mainClass.set("com.google.gwt.dev.codeserver.CodeServer")
    args(
        "-src", "../ponysdk/src/main/java",
        "-generateJsInteropExports", "-noincremental",
        "-style", "DETAILED",
        "com.ponysdk.core.PonyTerminal"
    )
    jvmArgs("-server", "-Xmx1024m")
}

tasks.register<JavaExec>("runSampleSpring") {
    group = ApplicationPlugin.APPLICATION_GROUP

    classpath = sourceSets["main"].runtimeClasspath
    classpath += project(":ponysdk").sourceSets["main"].java.sourceDirectories
    classpath += files(layout.buildDirectory.dir("gwt/gwt").get())
    classpath += files(layout.buildDirectory.dir("resources"))
    classpath += files(layout.buildDirectory.dir("resources/main"))
    mainClass.set("com.ponysdk.impl.spring.MainSpring")
    jvmArgs(
        "-Xmx1g",
        "-Xms1g",
        "-XX:-DisableExplicitGC",
        "-Xlog:safepoint=info,gc*=info,gc+age=trace:file=run_sample_spring_gc.log:time,uptime,level,tags:filesize=10M,filecount=100",
        "-Dlog4j.configurationFile=etc/log4j2.xml",
        "-agentlib:jdwp=transport=dt_socket,server=y,address=8888,suspend=n",
        "-Xmn7500M",
    )
    //jvmArgs = ["-server", "-Xmx512m", "-Dlog4j.configurationFile=etc/log4j2.xml", "-agentlib:jdwp=transport=dt_socket,server=y,address=8888,suspend=n"]
}

tasks.register<JavaExec>("runSampleTrading") {
    group = ApplicationPlugin.APPLICATION_GROUP
    classpath = sourceSets["main"].runtimeClasspath
    classpath += files(layout.buildDirectory.dir("/gwt/debug/gwt"))
    classpath += files(layout.buildDirectory.dir("/gwt/prod/gwt"))
    classpath += files(layout.buildDirectory.dir("/resources/etc"))
    mainClass.set("com.ponysdk.impl.java.Main")
    jvmArgs(
        "-server",
        "-Xmx512m",
        "-Dponysdk.application.id=trading",
        "-Dponysdk.application.name='Trading sample'",
        "-Dponysdk.application.description='Trading Sample'",
        "-Dponysdk.application.context.name=trading",
        "-Dponysdk.entry.point.class=com.ponysdk.sample.client.TradingSampleEntryPoint",
        "-Dponysdk.application.javascripts=script/ponysdk.js;script/widget.js;script/sample.js;script/less.js",
        "-Dponysdk.application.stylesheets=css/sample.less;css/ponysdk.less"
    )
}
