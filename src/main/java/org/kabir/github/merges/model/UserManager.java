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

import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Model;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.validation.ValidationException;

import org.kabir.github.merges.common.NotAuthorizedException;
import org.kabir.github.merges.common.UserAlreadyExistsException;
import org.kabir.github.merges.data.Credentials;
import org.kabir.github.merges.data.LoggedIn;
import org.kabir.github.merges.data.Password;
import org.kabir.github.merges.data.UserConfig;

@Model
@RequestScoped
public class UserManager {
    @Inject
    @LoggedIn
    private UserConfig user;

    @Inject
    private ApplicationConfigBean applicationConfig;

    @Inject
    private FacesContext facesContext;

    public Set<UserConfig> getUsers() {
        try {
            return applicationConfig.listUsers(user);
        } catch (NotAuthorizedException e){
            facesContext.addMessage(null, new FacesMessage("You are not authorised to view users"));
        }
        return Collections.emptySet();
    }

    public boolean isSelf(UserConfig currentUser) {
        return currentUser.getUserName().equals(user.getUserName());
    }

    public String addUser(Credentials credentials) {
        try {
            applicationConfig.createUserConfig(user, credentials.getUsername(), credentials.getPassword(), credentials.isAdmin());
            return "list";
        } catch (ValidationException | UserAlreadyExistsException e) {
            facesContext.addMessage(null, new FacesMessage(e.getMessage()));
        } catch (NotAuthorizedException e){
            facesContext.addMessage(null, new FacesMessage("You are not authorised to add users"));
        }
        return "error";
    }

    public void deleteUser(String userName) {
        try {
            applicationConfig.removeUserConfig(user, userName);
        } catch (NotAuthorizedException e) {
            facesContext.addMessage(null, new FacesMessage("You are not authorised to remove users"));
        }
    }

    public void toggleAdmin(String userName) {
        try {
            applicationConfig.toggleAdmin(user, userName);
        } catch (NotAuthorizedException e) {
            facesContext.addMessage(null, new FacesMessage("You are not authorised to change users"));
        }
    }

    public String adminChangePassword(Password password) {
        try {
            applicationConfig.adminChangePassword(user, password.getUserName(), password.getPassword());
            return "success";
        } catch (NotAuthorizedException e) {
            facesContext.addMessage(null, new FacesMessage("You are not authorised to change user passwords"));
        }
        return "error";
    }
}
