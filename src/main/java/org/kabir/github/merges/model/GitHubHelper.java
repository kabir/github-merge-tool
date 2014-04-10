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

import java.io.IOException;
import java.io.Serializable;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.UserService;
import org.kabir.github.merges.data.GitRepositoryDetails;

public class GitHubHelper implements Serializable {

    private static final long serialVersionUID = 1L;

    private GitRepositoryDetails details;
    private GitHubClient client;
    private RepositoryId repoId;

    public GitHubHelper(GitRepositoryDetails details) {
        this.details = details;
        this.client = GitHubClient.createClient(details.getGitUrl());
        client.setCredentials(details.getUserName(), details.getPassword());

        String url = details.getGitUrl();
        if (url.endsWith(".git")) {
            //Does not seem to like the '.git' at the end
            url = url.substring(0, url.length() - 4);
        }
        if (repoId == null) {
            repoId = RepositoryId.createFromUrl(url);
        }
    }

    GitRepositoryDetails getGitRepositoryDetails() {
        return details;
    }

    public PullRequest getPullRequest(int prId) throws IOException {
        PullRequestService prService = new PullRequestService(client);
        return prService.getPullRequest(repoId, prId);
    }

    public boolean checkAuthenticated() {
        UserService userService = new UserService(client);
        try {
            userService.getEmails();
        } catch (IOException e) {
            if (e instanceof RequestException){
                if(((RequestException)e).getStatus() == 401) {
                    return false;
                }
            }
            throw new RuntimeException(e);
        }
        return true;
    }

    public void closeMergedPullRequest(Integer prId) throws IOException {
        PullRequest pr = getPullRequest(prId);
        if (pr.getClosedAt() == null) {
            PullRequestService prService = new PullRequestService(client);
            IssueService issueService = new IssueService(client);
            issueService.createComment(repoId, prId, "Merged");
            pr.setState("closed");
            prService.editPullRequest(repoId, pr);
        }
    }
}
