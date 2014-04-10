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
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Model;
import javax.enterprise.inject.Produces;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kabir.github.merges.common.Util;
import org.kabir.github.merges.data.BranchStatus;
import org.kabir.github.merges.data.GitRepositoryDetails;
import org.kabir.github.merges.data.Loaded;
import org.kabir.github.merges.data.LoggedIn;
import org.kabir.github.merges.data.UserConfig;

@Model
@SessionScoped
public class GitRepositoryManager implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    @LoggedIn
    private UserConfig userConfig;

    @Inject
    FacesContext facesContext;

    private GitRepositoryDetails gitRepositoryDetails = new GitRepositoryDetails();
    private BranchStatus branchStatus;

    private GitHubHelper gitHubHelper;


    public GitRepositoryManager() {
    }


    private boolean initGitHubHelperAndAuthenticate() {
        if (gitHubHelper == null || gitHubHelper.getGitRepositoryDetails().getName() == null || !gitHubHelper.getGitRepositoryDetails().getName().equals(gitRepositoryDetails.getName())) {
            gitHubHelper = new GitHubHelper(gitRepositoryDetails);
            return gitHubHelper.checkAuthenticated();
        }
        return true;
    }

    @Produces
    @Loaded
    public GitRepositoryDetails getGitRepositoryDetails(){
        return gitRepositoryDetails;
    }

    GitHubHelper getGitHubHelper() {
        return gitHubHelper;
    }

    public void setGitRepositoryDetails(GitRepositoryDetails gitRepositoryDetails) {
        this.gitRepositoryDetails = gitRepositoryDetails;
    }

    public BranchStatus getBranchStatus() {
        return branchStatus;
    }

    public String initForClone() {
        this.gitRepositoryDetails = new GitRepositoryDetails();
        return "new-clone";
    }

    public String cloneUserRepository() {
        //TODO these should be picked up by the regexp in the validator
        String gitUrl = gitRepositoryDetails.getGitUrl();
        if (!gitUrl.endsWith(".git") || gitUrl.indexOf('/') == -1){
            facesContext.addMessage(null, new FacesMessage(gitUrl + " does not apear to be a valid git repository"));
            return "error";
        }

        if (!initGitHubHelperAndAuthenticate()) {
            facesContext.addMessage(null, new FacesMessage("Wrong username/password"));
            return "error";
        }

        File cloneDirectory = new File(userConfig.getCheckoutsDirectory(), gitRepositoryDetails.getName());

        if (cloneDirectory.exists()){
            facesContext.addMessage(null, new FacesMessage("Clone '" + gitRepositoryDetails.getName() + "' already exists."));
            return "existing";
        }

        Git git = null;
        BranchManager branchManager = null;
        try {
            git = Git.cloneRepository()
                    .setURI(gitUrl)
                    .setCredentialsProvider(getCredentialsProvider())
                    .setProgressMonitor(new TextProgressMonitor())
                    .setDirectory(cloneDirectory)
                    .call();

            //This checkouts the main and testing branches from the remote
            branchManager = new BranchManager(git, gitRepositoryDetails.getMainBranch(), gitRepositoryDetails.getTestingBranch());

            //Store the git repository metadata
            gitRepositoryDetails.store(userConfig);
            userConfig.addCheckout(gitRepositoryDetails);
        } catch (Exception e) {
            Util.recursiveDelete(cloneDirectory);
            facesContext.addMessage(null, new FacesMessage("Error cloning the repository " + e.getMessage()));
            return "error";
        }

        branchStatus = new BranchStatus(branchManager);

        return "view";
    }

    public void deleteCheckout(String name) {
        File file = new File(userConfig.getCheckoutsDirectory(), name);
        Util.recursiveDelete(file);
        file = new File(userConfig.getDataDirectory(), name);
        Util.recursiveDelete(file);
        userConfig.deleteCheckout(name);
    }

    public void fetchUpstream() {
        try {
            branchStatus.getManager().fetch();
        } catch (GitAPIException e) {
            facesContext.addMessage(null, new FacesMessage("Error fetching remote: " + e.getMessage()));
        }
    }

    public void rebaseLocalMainOnUpstreamMain() {
        BranchManager manager = branchStatus.getManager();
        try {
            manager.rebaseBranch(manager.getLocalMainName(), manager.getRemoteMainName());
        } catch (GitAPIException e) {
            facesContext.addMessage(null, new FacesMessage("Error rebasing " + e.getMessage()));
        }
    }

    public void pushLocalMainToUpstreamMain() {
        BranchManager manager = branchStatus.getManager();
        try {
            manager.pushBranch(manager.getLocalMainName(), false);
        } catch (GitAPIException e) {
            facesContext.addMessage(null, new FacesMessage("Error pushing " + e.getMessage()));
        }
    }

    public void resetHardLocalMainToUpstreamMain() {
        BranchManager manager = branchStatus.getManager();
        try {
            manager.resetBranch(manager.getLocalMainName(), manager.getRemoteMainName(), ResetType.HARD);
        } catch (GitAPIException e) {
            facesContext.addMessage(null, new FacesMessage("Error resetting " + e.getMessage()));
        }
    }

    public void rebaseLocalTestingOnUpstreamMain() {
        BranchManager manager = branchStatus.getManager();
        try {
            manager.rebaseBranch(manager.getLocalTestingName(), manager.getRemoteMainName());
        } catch (GitAPIException e) {
            facesContext.addMessage(null, new FacesMessage("Error rebasing " + e.getMessage()));
        }
    }

    public void rebaseLocalTestingOnUpstreamTesting() {
        BranchManager manager = branchStatus.getManager();
        try {
            manager.rebaseBranch(manager.getLocalTestingName(), manager.getRemoteTestingName());
        } catch (GitAPIException e) {
            facesContext.addMessage(null, new FacesMessage("Error rebasing " + e.getMessage()));
        }
    }

    public void pushLocalTestingToUpstreamTesting() {
        BranchManager manager = branchStatus.getManager();
        try {
            manager.pushBranch(manager.getLocalTestingName(), false);
        } catch (GitAPIException e) {
            facesContext.addMessage(null, new FacesMessage("Error pushing " + e.getMessage()));
        }
    }

    public void mergeFFOnlyLocalTestingToLocalMain() {
        BranchManager manager = branchStatus.getManager();
        try {
            MergeResult result = manager.mergeBranch(manager.getLocalMainName(), manager.getLocalTestingName(), FastForwardMode.FF_ONLY);
            if (!result.getMergeStatus().isSuccessful()) {
                facesContext.addMessage(null, new FacesMessage("Error merging " + result.getFailingPaths()));
            }
        } catch (GitAPIException e) {
            facesContext.addMessage(null, new FacesMessage("Error merging " + e.getMessage()));
        }
    }

    public void resetHardLocalTestingToUpstreamMain() {
        BranchManager manager = branchStatus.getManager();
        try {
            manager.resetBranch(manager.getLocalTestingName(), manager.getRemoteMainName(), ResetType.HARD);
        } catch (GitAPIException e) {
            facesContext.addMessage(null, new FacesMessage("Error resetting " + e.getMessage()));
        }
    }

    public void resetHardLocalTestingToUpstreamTesting() {
        BranchManager manager = branchStatus.getManager();
        try {
            manager.resetBranch(manager.getLocalTestingName(), manager.getRemoteTestingName(), ResetType.HARD);
        } catch (GitAPIException e) {
            facesContext.addMessage(null, new FacesMessage("Error resetting " + e.getMessage()));
        }
    }

    public void pushForceLocalTestingToUpstreamTesting() {
        BranchManager manager = branchStatus.getManager();
        try {
            manager.pushBranch(manager.getLocalTestingName(), true);
        } catch (GitAPIException e) {
            facesContext.addMessage(null, new FacesMessage("Error pushing " + e.getMessage()));
        }
    }

    public String viewCheckout(String name) {
        if (this.gitRepositoryDetails == null || this.gitRepositoryDetails.getName() == null || !this.gitRepositoryDetails.getName().equals(name)) {
            this.gitRepositoryDetails = GitRepositoryDetails.load(userConfig, name);
        }
        if (this.gitRepositoryDetails.getPassword() == null) {
            return "password";
        }
        if (!initGitHubHelperAndAuthenticate()) {
            facesContext.addMessage(null, new FacesMessage("Wrong password"));
            return "password";
        }

        File checkoutDir = Util.getExistingDirectory(new File(userConfig.getCheckoutsDirectory(), name).getAbsolutePath());
        Git git = null;
        BranchManager branchManager = null;
        try {
            Repository repository = new FileRepositoryBuilder()
                .setWorkTree(checkoutDir)
                .readEnvironment()
                .build();
            git = Git.wrap(repository);
            branchManager = new BranchManager(git, gitRepositoryDetails.getMainBranch(), gitRepositoryDetails.getTestingBranch());
        } catch (Exception e){
            facesContext.addMessage(null, new FacesMessage("Error loading the repository " + e.getMessage()));
            return "error";
        }

        branchStatus = new BranchStatus(branchManager);
        return "view";
    }

    public void newMerge() {

    }

    public Set<GitRepositoryDetails> getCheckouts(){
        return userConfig.getCheckouts();
    }

    private CredentialsProvider getCredentialsProvider() {
        return new UsernamePasswordCredentialsProvider(gitRepositoryDetails.getUserName(), gitRepositoryDetails.getPassword());
    }


    public class BranchManager {
        private final Git git;
        private final String mainName;
        private final String testingName;
        private final String localMainName;
        private final String localTestingName;
        private final String remoteMainName;
        private final String remoteTestingName;

        private BranchManager(Git git, String mainName, String testingName) throws GitAPIException {
            this.git = git;
            this.mainName = mainName;
            this.testingName = testingName;
            localMainName = "refs/heads/" + mainName;
            localTestingName = "refs/heads/" + testingName;
            remoteMainName  = "refs/remotes/origin/" + mainName;
            remoteTestingName = "refs/remotes/origin/" + testingName;

            Ref remoteMainRef = getRef(remoteMainName);
            Ref remoteTestingRef = getRef(remoteTestingName);

            if (remoteMainRef == null) {
                throw new RuntimeException("Could not find remote main branch " + mainName);
            }
            if (remoteTestingRef == null) {
                throw new RuntimeException("Could not find remote testing branch" + testingName);
            }
            if (getRef(localMainName) == null) {
                checkoutBranch(mainName, remoteMainName);
            }
            if (getRef(localTestingName) == null) {
                checkoutBranch(testingName, remoteTestingName);
            }
        }

        Ref getRef(String branchName) {
            try {
                return git.getRepository().getRef(branchName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Ref checkoutBranch(String localName, String remote) throws GitAPIException {
            return git.checkout()
                    .setCreateBranch(true)
                    .setName(localName)
                    .setStartPoint(remote)
                    .setUpstreamMode(SetupUpstreamMode.NOTRACK)
                    .call();
        }

        Ref checkoutBranch(String localName, Ref remote) throws GitAPIException {
            return checkoutBranch(localName, remote.getName());
        }

        void deleteBranchIfExists(String localName) throws GitAPIException {
            try {
                if (git.getRepository().resolve(localName) != null) {
                    git.checkout()
                        .setName(localMainName)
                        .call();
                    git.branchDelete()
                        .setBranchNames(localName)
                        .setForce(true)
                        .call();
                }
            } catch (RevisionSyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        RebaseResult rebaseBranch(String branch, String ontoBranch) throws GitAPIException {
            git.checkout()
                .setName(branch)
                .call();

            return git.rebase()
                .setUpstream(ontoBranch)
                .call();
        }

        void fetch() throws GitAPIException {
            git.fetch()
                .setCredentialsProvider(getCredentialsProvider())
                .call();
        }

        void resetBranch(String branch, String toBranch, ResetType type) throws GitAPIException {
            git.checkout()
                .setName(branch)
                .call();

            git.reset()
                .setMode(type)
                .setRef(toBranch)
                .call();
        }

        Iterable<PushResult> pushBranch(String localBranch, boolean force) throws GitAPIException {
            return git.push()
                .setCredentialsProvider(getCredentialsProvider())
                .setForce(force)
                .add(localBranch)
                .call();
        }

        MergeResult mergeBranch(String branch, String fromBranch, FastForwardMode mode) throws GitAPIException {
            git.checkout()
                .setName(branch)
                .call();

            MergeResult result = git.merge()
                .setFastForward(mode)
                .include(getRef(fromBranch))
                .call();
            return result;
        }


        public String getMainName() {
            return mainName;
        }

        public String getTestingName() {
            return testingName;
        }

        public String getLocalMainName() {
            return localMainName;
        }

        public String getLocalTestingName() {
            return localTestingName;
        }

        public String getRemoteMainName() {
            return remoteMainName;
        }

        public String getRemoteTestingName() {
            return remoteTestingName;
        }

        public String getSHA1(String branchName) {
            return getRef(branchName).getObjectId().getName();
        }

        public RevCommit getLastCommit(String branchName) {
            try {
                return git.log()
                        .add(getRef(branchName).getObjectId())
                        .setMaxCount(1)
                        .call()
                        .iterator()
                        .next();
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }

        public String fetchPullRequest(Integer id) throws GitAPIException {
            RefSpec refSpec = new RefSpec("+refs/pull/" + id + "/head:refs/remotes/origin/pr/" + id);
            git.fetch()
                    .setProgressMonitor(new TextProgressMonitor())
                    .setRefSpecs(refSpec)
                    .setThin(false)
                    .setCredentialsProvider(getCredentialsProvider())
                    .call();
            try {
                return git.getRepository().getRef("refs/remotes/origin/pr/" + id).getName();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public Map<Integer, Ref> fetchPullRequsts(Collection<Integer> ids) throws GitAPIException, IOException {
            FetchCommand command = git.fetch()
                    .setProgressMonitor(new TextProgressMonitor())
                    .setThin(false)
                    .setCredentialsProvider(getCredentialsProvider());

            for (Integer id : ids) {
                command.getRefSpecs().add(new RefSpec("+refs/pull/" + id + "/head:refs/remotes/origin/pr/" + id));
            }

            FetchResult result = command.call();

            Map<Integer, Ref> refs = new HashMap<>();
            for (Integer id : ids) {
                refs.put(id, git.getRepository().getRef("refs/remotes/origin/pr/" + id));
            }
            return refs;
        }

        public Git getGit() {
            return git;
        }
    }
}
