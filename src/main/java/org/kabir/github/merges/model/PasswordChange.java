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

import java.io.Serializable;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.kabir.github.merges.data.LoggedIn;
import org.kabir.github.merges.data.Password;
import org.kabir.github.merges.data.UserConfig;


@RequestScoped
@Named
public class PasswordChange implements Serializable {

   private static final long serialVersionUID = 7965455427888195913L;

   @Inject
   private Password password;

   @Inject
   private ApplicationConfigBean applicationConfig;

   @Inject
   @LoggedIn
   private UserConfig currentUser;

   @Inject
   FacesContext facesContext;

   public void change() throws Exception {
       applicationConfig.changeOwnPassword(currentUser, password.getPassword());
       facesContext.addMessage(null, new FacesMessage("Password changed!"));
   }
}
