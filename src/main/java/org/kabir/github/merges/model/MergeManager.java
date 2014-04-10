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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.inject.Model;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.RebaseCommand.Operation;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.kabir.github.merges.common.Util;
import org.kabir.github.merges.data.LoggedIn;
import org.kabir.github.merges.data.MergeIds;
import org.kabir.github.merges.data.MergeState;
import org.kabir.github.merges.data.OldMerge;
import org.kabir.github.merges.data.UserConfig;
import org.kabir.github.merges.model.GitRepositoryManager.BranchManager;

@Model
public class MergeManager {
    private static final String IDS_PROP = "ids";
    private static final String RESOLUTION = "resolution";
    private static final String ACTIVE_MERGE = "active-merge.txt";
    private static final String MERGE_PREFIX = "merge-";

    @Inject
    private GitRepositoryManager gitRepositoryManager;

    @Inject
    @LoggedIn
    private UserConfig userConfig;

    @Inject
    private MergeIds mergeIds;

    @Inject
    FacesContext facesContext;

    public String newMerge() {
        MergeContext context = new MergeContext();
        try {
            context.doMerge();
            if (context.isSuccess()) {
                return "view";
            }
        } catch (Exception e) {
            facesContext.addMessage(null, new FacesMessage("An error occurred merging the pull requests " + e.getMessage()));
        }
        return "error";
    }

    public String viewActiveMerge() {
        loadMergeFromFile(getActiveMergeFile());
        return "view";
    }

    public String viewOldMerge(String name) {
        loadMergeFromFile(getOldMergeFile(name));
        mergeIds.setName(name);
        return "view";
    }

    public void pushToTestingBranch() {
        BranchManager mgr = gitRepositoryManager.getBranchStatus().getManager();
        try {
            mgr.pushBranch(mgr.getLocalTestingName(), false);
        } catch (GitAPIException e) {
            facesContext.addMessage(null, new FacesMessage("Error pushing " + mgr.getTestingName() + " branch: " + e.getMessage()));
        }
        //Reload the PRs
        setActiveMergeState(MergeState.TESTING);
        viewActiveMerge();
    }

    public String closeMerge() {
        getActiveMergeFile().delete();
        return "closed";
    }

    public String closeAndPushMerge() {
        BranchManager mgr = gitRepositoryManager.getBranchStatus().getManager();
        try {
            MergeResult result = mgr.mergeBranch(mgr.getLocalMainName(), mgr.getLocalTestingName(), FastForwardMode.FF_ONLY);
            if (!result.getMergeStatus().isSuccessful()){
                facesContext.addMessage(null, new FacesMessage("Error merging " + mgr.getTestingName() + " to "
                        + mgr.getMainName() + " testing branch: " + result.getFailingPaths()));
                return "error";
            }
        } catch (GitAPIException e) {
            facesContext.addMessage(null, new FacesMessage("Error merging " + mgr.getTestingName() + " to "
                    + mgr.getMainName() + " testing branch: " + e.getMessage()));
            return "error";
        }
        try {
            mgr.pushBranch(mgr.getLocalMainName(), false);
        } catch (GitAPIException e) {
            facesContext.addMessage(null, new FacesMessage("Error pushing " + mgr.getMainName() + " branch: " + e.getMessage()));
        }

        Properties props = setActiveMergeState(MergeState.MERGED);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmz");
        File activeMergeFile = getActiveMergeFile();
        File replaced = new File(activeMergeFile.getParentFile(), MERGE_PREFIX + format.format(new Date()));
        activeMergeFile.renameTo(replaced);

        List<Integer> ids = readIdsFromProperty(props);
        for (Integer id : ids) {
            try {
                gitRepositoryManager.getGitHubHelper().closeMergedPullRequest(id);
            } catch (IOException e) {
                facesContext.addMessage(null, new FacesMessage("Error closing pr " + id + ": " + e.getMessage()));
            }
        }
        return "closed";
    }

    public boolean isMergeInProgress() {
        return getActiveMergeFile().exists();
    }

    public List<OldMerge> getAllMerges() {
        Set<OldMerge> merges = new TreeSet<>(new Comparator<OldMerge>() {
            @Override
            public int compare(OldMerge o1, OldMerge o2) {
                //Reverse name order
                return o1.getName().compareTo(o2.getName()) * -1;
            }
        });
        for (File file : getActiveMergeFile().getParentFile().listFiles()) {
            if (file.getName().startsWith(MERGE_PREFIX)) {
                Properties activeMergeProps = Util.loadProperties(file);
                merges.add(new OldMerge(file.getName().substring(MERGE_PREFIX.length()), MergeState.fromString(activeMergeProps.getProperty(RESOLUTION))));
            }
        }
        return new ArrayList<>(merges);
    }

    private void loadMergeFromFile(File file) {
        Properties activeMergeProps = Util.loadProperties(file);
        List<Integer> ids = readIdsFromProperty(activeMergeProps);
        List<MergeIds.PullRequestInfo> prInfos = new ArrayList<>();
        for (Integer id : ids){
            prInfos.add(createPullRequestInfo(id));
        }
        mergeIds.setPullRequestInfos(prInfos);
        mergeIds.setState(MergeState.fromString(activeMergeProps.getProperty(RESOLUTION)));
    }


    private Properties setActiveMergeState(MergeState state) {
        File activeMergeFile = getActiveMergeFile();
        Properties props = Util.loadProperties(activeMergeFile);
        props.put(RESOLUTION, state.getStateString());
        Util.writeProperties(props, activeMergeFile);
        return props;
    }

    private File getActiveMergeFile() {
        File dataDir = Util.createDirectoryAndMakeSureExists(userConfig.getDataDirectory(), gitRepositoryManager.getGitRepositoryDetails().getName());
        return new File(dataDir, ACTIVE_MERGE);
    }

    private File getOldMergeFile(String name) {
        File dataDir = Util.createDirectoryAndMakeSureExists(userConfig.getDataDirectory(), gitRepositoryManager.getGitRepositoryDetails().getName());
        return new File(dataDir, MERGE_PREFIX + name);
    }

    private void addIdToIdsProperty(Properties props, Integer id) {
        String idsProp = props.getProperty(IDS_PROP);
        if (idsProp == null) {
            idsProp = String.valueOf(id);
        } else {
            idsProp += "," + id;
        }
        props.put(IDS_PROP, idsProp);
    }

    private List<Integer> readIdsFromProperty(Properties props){
        List<Integer> list = new ArrayList<>();
        String ids = props.getProperty(IDS_PROP);
        if (ids != null) {
            for (String id : ids.split(",")) {
                list.add(Integer.valueOf(id));
            }
        }
        return list;
    }

    private MergeIds.PullRequestInfo createPullRequestInfo(int id){
        PullRequest pr;
        try {
            pr = gitRepositoryManager.getGitHubHelper().getPullRequest(id);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return createPullRequestInfo(id, pr);
    }

    private MergeIds.PullRequestInfo createPullRequestInfo(int id, PullRequest pr) {
        return new MergeIds.PullRequestInfo(id, pr.getHtmlUrl(), pr.getTitle(), pr.getClosedAt() == null ? "open" : "closed");
    }

    private final class MergeContext {
        private boolean error;

        boolean isSuccess() {
            return !error;
        }

        void doMerge() {
            final BranchManager mgr = gitRepositoryManager.getBranchStatus().getManager();
            final Map<Integer, PullRequest> validPullRequests = getAllValidPullRequests();
            final List<MergeIds.PullRequestInfo> infos = new ArrayList<>();
            try {
                mgr.fetch();
                gitRepositoryManager.rebaseLocalTestingOnUpstreamMain();
            } catch (Exception fetchException){
                facesContext.addMessage(null, new FacesMessage("Error fetching from remote " + fetchException));
                error = true;
                return;
            }
            //TODO - Check that the ids are the same and take action if not

            final Map<Integer, Ref> pullRequestRefs;
            try {
                pullRequestRefs = mgr.fetchPullRequsts(validPullRequests.keySet());
            } catch (Exception fetchException) {
                facesContext.addMessage(null, new FacesMessage("Error fetching pull requests " + fetchException));
                error = true;
                return;
            }

            Properties activeMergeProps = new Properties();
            for (Integer id : validPullRequests.keySet()) {
                final Ref prRef = pullRequestRefs.get(id);
                final String localPrBranchName = "pull_request__" + id;

                try {
                    mgr.deleteBranchIfExists(localPrBranchName);
                    mgr.checkoutBranch(localPrBranchName, prRef);
                } catch (Exception checkoutError) {
                    validPullRequests.remove(id);
                    facesContext.addMessage(null, new FacesMessage("Error checking out pull request " + id +  ": " + checkoutError));
                    continue;
                }

                try {
                    String rebaseError = null;
                    try {
                        RebaseResult result = mgr.rebaseBranch(localPrBranchName, mgr.getLocalTestingName());
                        if (!result.getStatus().isSuccessful()) {
                            rebaseError = result.toString();
                        }
                    } catch (Exception rebaseException) {
                        rebaseError = rebaseException.getMessage();
                    } finally {
                        if (rebaseError != null) {
                            validPullRequests.remove(id);
                            facesContext.addMessage(null, new FacesMessage("Error rebasing pull request " + id +  ": " + rebaseError));
                            try {
                                mgr.getGit().rebase().setOperation(Operation.ABORT).call();
                            } catch (Exception abortError){
                                facesContext.addMessage(null, new FacesMessage("Could not abort rebase for pull request " + id +  ": " + abortError));
                            }
                            continue;
                        }
                    }

                    String mergeError = null;
                    try {
                        MergeResult result = mgr.mergeBranch(mgr.getLocalTestingName(), localPrBranchName, FastForwardMode.FF_ONLY);
                        if (!result.getMergeStatus().isSuccessful()){
                            mergeError = result.toString();
                        }
                    } catch (Exception mergeException){
                        mergeError = mergeException.getMessage();
                    } finally {
                        if (mergeError != null) {
                            validPullRequests.remove(id);
                            facesContext.addMessage(null, new FacesMessage("Error merging branch for pull request " + id +  " back into " + mgr.getLocalTestingName()));
                            continue;
                        }
                    }

                    addIdToIdsProperty(activeMergeProps, id);
                } finally {
                    try {
                        mgr.deleteBranchIfExists(localPrBranchName);
                    } catch (Exception deleteException) {
                        facesContext.addMessage(null, new FacesMessage("Error deleting branch for pull request " + id + ". Including it in the merge anyway"));
                    }
                }

                PullRequest pr = validPullRequests.get(id);
                infos.add(createPullRequestInfo(id, pr));
            }

            try {
                activeMergeProps.setProperty(RESOLUTION, MergeState.NEW.getStateString());
                Util.writeProperties(activeMergeProps, getActiveMergeFile());
            } catch (Exception e) {
                error = true;
                facesContext.addMessage(null, new FacesMessage("Error writing active merges file: " + e.getMessage()));
                return;
            }
            mergeIds.setPullRequestInfos(infos);
        }

        private Map<Integer, PullRequest> getAllValidPullRequests() {
            //Get the pull request ids the user wants to merge into pullRequestIds
            List<Integer> pullRequestIds = getPullRequestsIdsFromString();
            if (error) {
                return null;
            }
            //Validate the pull requests, and store the valid ones in validPullRequests.
            return validatePullRequestIds(pullRequestIds);
        }

        private List<Integer> getPullRequestsIdsFromString() {
            List<Integer> pullRequestIds = new ArrayList<>();
            try {
                for (String line : mergeIds.getPullRequestIds().split("\n")) {
                    for (String id : line.split(",")) {
                        id = id.trim();
                        if (id.length() > 0){
                            pullRequestIds.add(Integer.valueOf(id));
                        }
                    }
                }
            } catch (NumberFormatException e) {
                facesContext.addMessage(null, new FacesMessage("Pull request ids must be numberic"));
                error = true;
            }
            return pullRequestIds;
        }


        /**
         * Checks that the pull requests exist, if it does not exist or is closed, remove it from the list to be merged
         * but keep trying to merge the rest.
         */
        private Map<Integer, PullRequest> validatePullRequestIds(List<Integer> pullRequestIds) {
            Map<Integer, PullRequest> validPullRequests = new LinkedHashMap<Integer, PullRequest>();
            Set<Integer> invalid = new TreeSet<>();
            Set<Integer> closed = new TreeSet<>();
            for (Integer id : pullRequestIds) {

                PullRequest pr;
                try {
                     pr = gitRepositoryManager.getGitHubHelper().getPullRequest(id);
                } catch (IOException e) {
                    //Could be a bit stricter about the type of exception here...
                    invalid.add(id);
                    continue;
                }

                if (pr.getClosedAt() != null) {
                    closed.add(id);
                    continue;
                }
                validPullRequests.put(id, pr);
            }

            if (invalid.size() > 0) {
                facesContext.addMessage(null, new FacesMessage("The following pull request ids do not appear to be valid " + invalid));
            }
            if (closed.size() > 0) {
                facesContext.addMessage(null, new FacesMessage("The following pull requests are already closed " + closed));
            }
            return validPullRequests;
        }
    }
}
