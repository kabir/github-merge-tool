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
import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;

import org.kabir.github.merges.common.Util;


public class UserConfig implements Serializable, Comparable<UserConfig> {

    private static final long serialVersionUID = 1L;

    private final File rootDirectory;
    private final File checkoutsDirectory;
    private final File dataDirectory;
    private final String userName;
    private final boolean admin;
    private final Set<GitRepositoryDetails> checkouts = new TreeSet<>();

    public UserConfig(File appRootDirectory, String userName, boolean admin) {
        this.userName = userName;
        this.admin = admin;
        rootDirectory = Util.createDirectoryAndMakeSureExists(appRootDirectory, userName);
        checkoutsDirectory = Util.createDirectoryAndMakeSureExists(rootDirectory, "checkouts");
        dataDirectory = Util.createDirectoryAndMakeSureExists(rootDirectory, "data");
        for (File file : dataDirectory.listFiles()){
            if (file.isDirectory()){
                GitRepositoryDetails details = GitRepositoryDetails.load(this, file.getName());
                checkouts.add(details);
            }
        }
    }

    public static File getUserRootDirectory(File appRootDirectory, String userName) {
        return new File(appRootDirectory, userName);
    }

    public String getUserName() {
        return userName;
    }

    public boolean isAdmin() {
        return admin;
    }

    public File getCheckoutsDirectory() {
        return checkoutsDirectory;
    }

    public File getDataDirectory() {
        return dataDirectory;
    }

    public Set<GitRepositoryDetails> getCheckouts() {
        return checkouts;
    }

    public void addCheckout(GitRepositoryDetails detail) {
        checkouts.add(detail);
    }

    public void deleteCheckout(String name) {
        GitRepositoryDetails remove = null;
        for (GitRepositoryDetails detail : checkouts) {
            if (detail.getName().equals(name)) {
                remove = detail;
            }
        }
        if (remove != null){
            checkouts.remove(remove);
        }
    }

    @Override
    public int compareTo(UserConfig other) {
        return userName.compareTo(other.userName);
    }

}
