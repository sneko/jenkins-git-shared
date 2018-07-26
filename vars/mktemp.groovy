#!groovy

/**
 * Create a temporary directory in the temp area reserved for this build.
 * Return the absolute path of the created temp directory
 */
def call(String prefix) {
    // get workspace as a FilePath
    def ws = getContext(hudson.FilePath.class)
    assert ws : 'A node must be allocated to use mktemp()'
    
    // get the temp dir associated with workspace and make sure 
    // it exists
    def wsTemp = hudson.slaves.WorkspaceList.tempDir(ws)
    wsTemp.mkdirs()

    // create a temp dir inside the workspace's temp area
    if (!prefix) {
        prefix = 'build'
    }
    return wsTemp.createTempDir(prefix, '').getRemote()
}
