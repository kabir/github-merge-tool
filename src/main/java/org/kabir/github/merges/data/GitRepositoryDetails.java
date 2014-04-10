/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.kabir.github.merges.data;

import java.io.File;
import java.util.Properties;

import javax.validation.constraints.NotNull;

import org.kabir.github.merges.common.Util;

public class GitRepositoryDetails implements Comparable<GitRepositoryDetails>{

    private static final String REPOSITORY_SETTINGS = "settings.properties";
    private static final String MAIN = "main";
    private static final String TESTING = "testing";
    private static final String GIT_URL = "gitUrl";
    private static final String USERNAME = "userName";

    @NotNull
    private String name;

    @NotNull
    private String gitUrl;

    @NotNull
    private String userName;

    @NotNull
    private String password;

    @NotNull
    private String mainBranch;

    @NotNull
    private String testingBranch;

    public GitRepositoryDetails() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public String getGitUrlShort() {
        if (gitUrl.endsWith(".git") && gitUrl.contains("/")){
            int i = gitUrl.lastIndexOf('/');
            return gitUrl.substring(i, gitUrl.length());
        } else {
            return "-";
        }
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMainBranch() {
        return mainBranch;
    }

    public void setMainBranch(String mainBranch) {
        this.mainBranch = mainBranch;
    }

    public String getTestingBranch() {
        return testingBranch;
    }

    public void setTestingBranch(String testingBranch) {
        this.testingBranch = testingBranch;
    }

    public void store(UserConfig userConfig) {
        //Store everything apart from the password
        File dataDir = Util.createDirectoryAndMakeSureExists(userConfig.getDataDirectory(), name);
        Properties properties = new Properties();
        properties.put(GIT_URL, gitUrl);
        properties.put(USERNAME, userName);
        properties.put(MAIN, mainBranch);
        properties.put(TESTING, testingBranch);
        Util.writeProperties(properties, new File(dataDir, REPOSITORY_SETTINGS));

    }

    public static GitRepositoryDetails load(UserConfig userConfig, String name){
        File dataDir = Util.createDirectoryAndMakeSureExists(userConfig.getDataDirectory(), name);
        File settings = new File(dataDir, REPOSITORY_SETTINGS);
        Properties properties = Util.loadProperties(settings);
        GitRepositoryDetails details = new GitRepositoryDetails();
        details.setName(name);
        details.gitUrl = properties.getProperty(GIT_URL);
        details.userName = properties.getProperty(USERNAME);
        details.mainBranch = properties.getProperty(MAIN);
        details.testingBranch = properties.getProperty(TESTING);
        return details;
    }

    @Override
    public int compareTo(GitRepositoryDetails o) {
        return this.name.compareTo(o.name);
    }
}
