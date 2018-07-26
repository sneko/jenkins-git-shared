#!groovy

/**
 * Configure Git credentials from the 'scm' information available in the script context.
 * Credentials are transmitted to the git command line utility via the GIT_ASKPASS technic.
 * 
 * Usage:
 *
 *    withGit {
 *        sh 'git ...'
 *    }
 *
 * Note: only the first userRemoteConfig is considered. The step is therefore suitable when
 *       accessing a single Git repository.
 *
 * Note2: the GIT_ASKPASS script is invoked twice: once for the username, and a second time 
 *        for the password each time with a different first argument:
 *        When asking for the username, the first argument is: 
 *               Username for '<repository url>'
 *        When asking for the password, the first argument is:
 *               Password for '<repository url>'
 *
 */
def call(Closure body) {

    // Get the credentialsId from the scm configuration
    //
    def remoteConfig = scm?.userRemoteConfigs[0]
    assert remoteConfig : 'No credentials found in SCM configuration'
    def credentialsId = remoteConfig.credentialsId
    assert credentialsId : 'No credentials found in SCM configuration'

    echo "Using GIT_ASKPASS to set credentials '${credentialsId}' for repository '${remoteConfig.url}'"


    // Create the GIT_ASKPASS script
    //
    def tempDir = mktemp('withGit')
    try {
        def filename = null

        withCredentials([usernamePassword(credentialsId: credentialsId, usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASSWORD')]) {
            if (utils.isUnixLike()) {
                filename = createGitAskPassUnix(tempDir)
            }
            else {
                filename = createGitAskPassWindows(tempDir)
            }
        }

        // Point the "GIT_ASKPASS" envvar to the script and invoke the body
        //
        withEnv(["GIT_ASKPASS=${filename}"]) {
            if (body) {
                body.call()
            }
        }
    }
    finally {
        rmdir tempDir
    }
}


def createGitAskPassUnix(path) {
    def script = 
"""#!/bin/sh
case \"\$1\" in
  Username*) echo '${GIT_USER}';;
  Password*) echo '${GIT_PASSWORD}';;
esac
"""

    def filename = path + "/gitpass.sh"
    writeFile file: filename, text: script
    sh "chmod +x ${filename}"

    return filename
}


def createGitAskPassWindows(path) {
    def script = 
"""@set arg=%~1
@if (%arg:~0,8%)==(Username) echo ${escapeWindowsCharsForUnquotedString(GIT_USER)}
@if (%arg:~0,8%)==(Password) echo ${escapeWindowsCharsForUnquotedString(GIT_PASSWORD)}
"""

    def filename = path + "\\gitpass.bat"
    writeFile file: filename, text: script

    return filename
}


def escapeWindowsCharsForUnquotedString(String str) {
    // Quote special characters for Windows Batch Files
    // See: http://stackoverflow.com/questions/562038/escaping-double-quotes-in-batch-script
    // See: http://ss64.com/nt/syntax-esc.html
    def quoted =  str.replace("%", "%%")
                    .replace("^", "^^")
                    .replace(" ", "^ ")
                    .replace("\t", "^\t")
                    .replace("\\", "^\\")
                    .replace("&", "^&")
                    .replace("|", "^|")
                    .replace("\"", "^\"")
                    .replace(">", "^>")
                    .replace("<", "^<");
    return quoted;
}
