GitHub Merge Tool
=================

Allows you to process pull requests from your browser, and any handhelp device. Data is stored on the server so you can initiate testing from one device, and complete the merge from another.

I host this on OpenShift but you can run it on any Java EE 7 app server. To use OpenShift
* Go to openshift.com, login/create an account, and create an application using the 'WildFly 8' cartridge
* Make a not of the source code url for your application (e.g. ssh://123456789abcdefgh9999999@thing-thing.rhcloud.com/~/git/thing.git/)
* Fork this repository in github
* `git clone git@github.com:<your-github-username>/github-merge-tool.git`
* `cd github-merge-tool`
* `git remote add openshift ssh://123456789abcdefgh9999999@thing-thing.rhcloud.com/~/git/thing.git/`
* `git push --force openshift master`
And you should be good to go.

Initial setup
-------------
If you are *not* running on OpenShift, you will need to create a directory somewhere on your file system and make the OPENSHIFT_DATA_DIR environment variable point to it, e.g:
    export OPENSHIFT_DATA_DIR=/some/where/on/your/system
 
The OPENSHIFT_DATA_DIR is used for the properties files containing the user's log in details. Each user also has a their own folder for their git checkouts.
 
On first use the app will create an account for you with
    username:admin
    password:admin
Change the password by logging in and choosing 'Settings'. 

User management
---------------

If you wish to add more users you can do so from 'Manage Users'. Currently the only distinction between an admin and a non-admin is that an admin can manage users.

Repositories
------------
Go to 'My Repositories' and you'll se a list of repositories

### Cloning a new repository

You will be asked to enter:
* *A name for the Git repository clone* - this is just the id of the repository for the tool
* *Git repository URL* - The name of the git repository URL, use HTTPS, e.g.: https://github.com/someorg/somerepo.git
* *Your user name for the Git repository* - This should be the name of a user with push access to the repository
* *Your password for the git repository* - The password of the git repository user. In an attempt to keep things secure, this password will not be stored, only for the duration of your session.
* *The name of the branch to merge to* - This is the name of the branch which we should merge to once we are done processing pull requests.
* *The name of the branch to run tests on* - This is the name of the branch where we push merges in progress to be picked up by the CI.

Then sit and wait, checking out a large repository will take some time.

### Viewing an existing repository

Click the name of the repostitory in the list. You might have to enter your github password, since we don't store it anywhere, only in memory for the duration of the session.

You will see the repository details and the local and remote versions of your 'testing' and 'main' branches, along with the ability to perform common branch maintenance tasks. 

#### Initiating a new merge

To start a new merge, click the link towards the bottom of the page. You can only have one in-progress merge per repository at any time. 

Enter the numbers of the pull request ids you want to merge, and press ‘Merge’. Again, this will take some time depending on what you’re merging so be patient. You will now get taken to a status page of the merge. You can leave this page, now that you have an in-progress merge the link mentioned in the previous paragraph will take you back to this status page. You can also view the in-progress merge from another device etc. If something went wrong with the merge there will be an error message (need to do a bit more work on getting proper error messages :-) ) The typical workflow is:

* If the merge went wrong, and you want to stop - select ‘Close’ and use the links on the repository overview page to reset your branches.
* If the merge was ok, or you want to merge what passed - select ‘Test’ which will push the merge up to the branch used by the CI to test merges
* If testing went wrong - you will probably want to close the merge and reset the branches
* If testing passed and you want to merge to the main branch - select ‘Merge’ (again this may take some time). This will merge into the local copy of the main branch, and push to the remote main branch and close the pull requests for you.

‘Merged’ merges are kept in history accessible from the repository overview page, and you can come back in and view links to the pull requests (handy if you need to manage issue trackers linked from the pull requests).

Currently the only merge strategy supported is rebase-only.







