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

import org.kabir.github.merges.model.GitRepositoryManager.BranchManager;


public class BranchStatus {

    private final BranchManager branchManager;

    public BranchStatus(BranchManager branchManager){
        this.branchManager = branchManager;
    }

    public BranchManager getManager() {
        return branchManager;
    }

    public String getMainName() {
        return branchManager.getMainName();
    }

    public String getTestingName() {
        return branchManager.getTestingName();
    }

    public String getLocalMainId(){
        return getCommitId(branchManager.getLocalMainName());
    }

    public String getLocalTestingId(){
        return getCommitId(branchManager.getLocalTestingName());
    }

    public String getRemoteMainId(){
        return getCommitId(branchManager.getRemoteMainName());
    }

    public String getRemoteTestingId(){
        return getCommitId(branchManager.getRemoteTestingName());
    }

    private String getCommitId(String branchName) {
        return branchManager.getLastCommit(branchName).getId().getName().substring(0, 10);
    }

    public String getLocalMainMsg(){
        return getCommitMsg(branchManager.getLocalMainName());
    }

    public String getLocalTestingMsg(){
        return getCommitMsg(branchManager.getLocalTestingName());
    }

    public String getRemoteMainMsg(){
        return getCommitMsg(branchManager.getRemoteMainName());
    }

    public String getRemoteTestingMsg(){
        return getCommitMsg(branchManager.getRemoteTestingName());
    }

    private String getCommitMsg(String branchName) {
        String msg = branchManager.getLastCommit(branchName).getShortMessage();
        if (msg.length() > 50) {
            msg = msg.substring(0, 50);
        }
        return msg;
    }

}
