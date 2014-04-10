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

package org.kabir.github.merges.model;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.ValidationException;

import org.kabir.github.merges.common.NotAuthorizedException;
import org.kabir.github.merges.common.UserAlreadyExistsException;
import org.kabir.github.merges.common.Util;
import org.kabir.github.merges.data.UserConfig;

/**
 * The main config for our application
 *
 */
@ApplicationScoped
public class ApplicationConfigBean {

    private static final String MERGES = "merges";
    private static final String USERS = "users.properties";
    private static final String ROLES = "admins.properties";
    private final File persistentDirectory;
    private final File usersFile;
    private final File rolesFile;

    private final Properties users;
    private final Properties roles;

    public ApplicationConfigBean(){
        String dataDirName = System.getenv("OPENSHIFT_DATA_DIR");
        if (dataDirName == null){
            throw new IllegalArgumentException("Environment variable $OPENSHIFT_DATA_DIR not found");
        }
        File dataDir = Util.getExistingDirectory(dataDirName);
        boolean fresh = !new File(dataDir, MERGES).exists();
        persistentDirectory = Util.createDirectoryAndMakeSureExists(dataDir, MERGES);
        usersFile = new File(persistentDirectory, USERS);
        rolesFile = new File(persistentDirectory, ROLES);
        if (fresh){
            try {
                usersFile.createNewFile();
                rolesFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        users = Util.loadProperties(usersFile);
        roles = Util.loadProperties(rolesFile);
        if (fresh) {
            try {
                createUserConfig("admin", "admin", true);
            } catch (UserAlreadyExistsException ignore) {
                throw new RuntimeException("Something has gone badly wrong. The 'admin' user exists in a fresh install");
            }
        }
    }

    public synchronized UserConfig loadUserConfig(String userName, String password) {
        String hashedPassword = Util.md5(password);
        String storedHashedPassword = users.getProperty(userName);
        if (!hashedPassword.equals(storedHashedPassword)){
            return null;
        }
        boolean admin = "true".equals(roles.getProperty(userName));
        return new UserConfig(persistentDirectory, userName, admin);
    }

    public synchronized UserConfig createUserConfig(UserConfig loggedInUser, String userName, String password, boolean admin) throws NotAuthorizedException, UserAlreadyExistsException, ValidationException {
        if (!loggedInUser.isAdmin()){
            throw new NotAuthorizedException();
        }
        return createUserConfig(userName, password, admin);
    }

    private synchronized UserConfig createUserConfig(String userName, String password, boolean admin) throws UserAlreadyExistsException, ValidationException {
        if (users.getProperty(userName) != null) {
            throw new UserAlreadyExistsException("There is already a user called '" + userName + "'");
        }
        if (userName.contains(" ")){
            throw new ValidationException("User name should not contain spaces");
        }
        users.put(userName, Util.md5(password));
        Util.writeProperties(users, usersFile);

        roles.put(userName, Boolean.valueOf(admin).toString());
        Util.writeProperties(roles, rolesFile);
        return new UserConfig(persistentDirectory, userName, admin);
    }

    public synchronized void removeUserConfig(UserConfig loggedInUser, String userName) throws NotAuthorizedException {
        if (!loggedInUser.isAdmin()){
            throw new NotAuthorizedException();
        }
        roles.remove(userName);
        Util.writeProperties(roles, rolesFile);
        users.remove(userName);
        Util.writeProperties(users, usersFile);
        Util.recursiveDelete(UserConfig.getUserRootDirectory(persistentDirectory, userName));
    }

    public synchronized void changeOwnPassword(UserConfig loggedInUser, String password)  {
        users.put(loggedInUser.getUserName(), Util.md5(password));
        Util.writeProperties(users, usersFile);
    }

    public Set<UserConfig> listUsers(UserConfig loggedInUser) throws NotAuthorizedException {
        if (!loggedInUser.isAdmin()){
            throw new NotAuthorizedException();
        }
        Set<UserConfig> set = new TreeSet<UserConfig>(new Comparator<UserConfig>() {
            @Override
            public int compare(UserConfig o1, UserConfig o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getUserName(), o2.getUserName());
            }
        });
        for (Object key : users.keySet()) {
            String username = (String)key;
            boolean admin = Boolean.valueOf(roles.getProperty(username, "false"));
            set.add(new UserConfig(persistentDirectory, username, admin));
        }
        return set;
    }

    public void toggleAdmin(UserConfig loggedInUser, String userName) throws NotAuthorizedException {
        if (!loggedInUser.isAdmin()){
            throw new NotAuthorizedException();
        }
        boolean admin = "true".equals(roles.getProperty(userName));
        roles.put(userName, String.valueOf(!admin));
        Util.writeProperties(roles, rolesFile);
    }


    public void adminChangePassword(UserConfig loggedInUser, String userName, String password) throws NotAuthorizedException {
        if (!loggedInUser.isAdmin()){
            throw new NotAuthorizedException();
        }
        users.put(userName, Util.md5(password));
        Util.writeProperties(users, usersFile);
    }
}
