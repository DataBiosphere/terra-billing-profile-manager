# Getting Started

These instructions assume you use MacOS, and that you are on the internal Broad
network or the VPN. If the VPN is not installed, follow the instructions
[at this link](https://broad.io/vpn).

> During this process, you will need your GitHub username, password, and
personal access token for multiple steps, so make sure to have those handy. If
you don't have those yet, see the section below, otherwise you can skip to
[Request Required Access](#2-request-required-access).

## 1. Create a GitHub account

GitHub is where the Broad stores our code and projects. Sign up to this service
with your personal or Broad email: https://github.com/join

Create a [personal access token](https://help.github.com/en/github/authenticating-to-github/creating-a-personal-access-token-for-the-command-line)
so you can interact with GitHub on the command line.

## 2. Request Required Access

Ensure that you have access to the required team resources. If you encounter a
permission error, it is likely because you are missing appropriate access.

- DataBiosphere: Join the `#github` Slack channel, click the lightning bolt in
  the channel header, and select `Join DataBiosphere`. Ask in '#dsp-devops-champions' to be added to the BroadWrite team in the DataBiosphere organization.

## 3. Connect accounts

> Make sure 2-factor authentication (2FA) is activated on your [Broad](https://broad.io/2fa)
and [GitHub](https://github.com/settings/security) account before starting this
process!

Connect your GitHub account to your Broad profile:

1. Go to [Broad people](https://people.broadinstitute.org/me) and select the
   **My Profile** tab.
2. Link your profile to GitHub by clicking under **Other Profiles**.
3. [Check if the account is successfully linked](https://github.broadinstitute.org/).
4. To avoid being overwhelmed with notifications, [add your Broad email address](https://github.com/settings/emails),
   [route the notifications](https://github.com/settings/notifications) to that
   email, and [unfollow projects](https://github.com/watching) that are not
   relevant to your team.
 
## 4. Install Homebrew

[Homebrew](https://brew.sh/) is a [package manager](https://en.wikipedia.org/wiki/Package_manager)
which enables the installation of software using a single, convenient command
line interface. To automatically install development tools necessary for the
team, a [Brewfile](https://github.com/Homebrew/homebrew-bundle) is used:

```
/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
curl -LO https://raw.githubusercontent.com/DataBiosphere/terra-billing-profile-manager/main/docs/Brewfile
brew bundle --no-lock install
```

Running the `brew` command above will install the following tools:

1. [Git](https://git-scm.com/) is a version control tool for tracking changes in
   projects and code.
2. [git-secrets](https://github.com/awslabs/git-secrets) prevents developers
   from committing passwords and secrets to git.
3. [jq](https://stedolan.github.io/jq/) is a command line JSON processing tool.
4. [Docker](https://www.docker.com/) is a tool to deliver software in packages
   called containers. Docker for MacOS also includes [Kubernetes](https://kubernetes.io/),
   which deploys groups of containers together in clusters.
5. [Vault](https://www.vaultproject.io/) is an encrypted database used to store
   many of the team's secrets such as keys and passwords.
6. [Google Cloud SDK](https://cloud.google.com/sdk) is a command-line interface
   to Google Cloud services. Once it is installed, you'll need to allow auth access
   and configure Docker to connect to the appropriate Google Cloud endpoint when
   necessary, which is done with the configuration below.
7. [IntelliJ IDEA](https://www.jetbrains.com/idea/) is an integrated development
   environment (IDE) for Java. There are two versions available: **Ultimate** (paid)
   and **Community** (open-source). We recommend the Ultimate Edition to Broad
   employees for its database navigation capabilities. Alternatively, the Community
   Edition has all the features needed for development, and this version can be
   installed by switching `intellij-idea` with `intellij-idea-ce` in the Brewfile.
8. [Temurin](https://adoptium.net/) is the code name for the free and
   open-source version of Java 17 provided by the Eclipse Adoptium Working Group.

Unfortunately, some manual configuration is also necessary:

```
# configure vault
export VAULT_ADDR=https://clotho.broadinstitute.org:8200

# launch docker desktop - this installs docker in /usr/local/bin
open -a docker

# configure google-cloud-sdk
gcloud auth login
gcloud auth application-default login
gcloud auth configure-docker

# ensure that git-secrets patterns are installed
git clone https://github.com/broadinstitute/dsp-appsec-gitsecrets-client.git
./dsp-appsec-gitsecrets-client/gitsecrets.sh
```
## 5. Create GitHub token

The GitHub token verifies team permissions. This token is necessary for the next
step, [Login to Vault](#8-login-to-vault). To create a token:

1. Go to the [GitHub Personal Access Token](https://github.com/settings/tokens)
   page and click **Generate new token**.
2. Give the token a descriptive name, **only** give it the following two scopes
   and then click **Generate token**.
*  `read:org` scope under `admin:org`
*  `workflow` (this is used to kick off GitHub actions from the command line)
3. Store this token in a file:

```
GITHUB_TOKEN=<<GITHUB TOKEN VALUE>>
echo $GITHUB_TOKEN > ~/.github-token
```

## 6. Login to Vault

Vault access tokens can be obtained using the GitHub token from earlier as
follows:

```
vault login -method=github token=$(cat ~/.github-token)
```

> Vault access tokens expire after 30 days, so if you get a `403` error trying
to use `vault`, re-run the `vault login` command to refresh your access token.

## 7. Code Checkout

> It may be useful to create a folder for Broad projects in your home directory.

Download the team's projects:

```
git clone git@github.com:DataBiosphere/terra-billing-profile-manager
git clone git@github.com:broadinstitute/terraform-ap-deployments
git clone git@github.com:broadinstitute/terra-helmfile
```
