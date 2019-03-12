
package com.ponysdk.core;

import java.io.File;

/*============================================================================
 *
 * Copyright (c) 2000-2008 Smart Trade Technologies. All Rights Reserved.
 *
 * This software is the proprietary information of Smart Trade Technologies
 * Use is subject to license terms. Duplication or distribution prohibited.
 *
 *============================================================================*/

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * DistributedTestTask DOCUMENT ME
 *
 * @author ldanesi
 */
public class DistributedTestTask implements Callable<Integer>, Serializable {

    private String main;
    private String args;
    private String vmargs;
    private String workingDirectory;
    private Collection<String> classpath;

    @Override
    public Integer call() throws Exception {
        final ProcessBuilder pb = new ProcessBuilder(buildCommandLine());

        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        final Process p = pb.start();
        return 10;
    }

    private List<String> buildCommandLine() {
        final List<String> commandLine = new ArrayList<>();
        // Process First
        commandLine.add("java");
        // VM Args
        commandLine.add(vmargs);
        // Classpath
        commandLine.add("-cp");
        commandLine.add(String.join(File.pathSeparator, classpath));
        // Main
        commandLine.add(main);
        // Args
        commandLine.add(args);
        return commandLine;
    }

    public void setMain(String main) {
        this.main = main;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public void setVmargs(String vmargs) {
        this.vmargs = vmargs;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public void setClasspath(Collection<String> classpath) {
        this.classpath = classpath;
    }

}
