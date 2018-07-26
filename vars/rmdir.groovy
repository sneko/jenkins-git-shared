#!groovy

// Recursively delete the directory specified by the 'dir' argument.
// Noop if the directory does not exist.
//
// same as :
//    dir(<dirname>) {
//       deleteDir()
//    }
// Except these two steps add "annoying" log messages

def call(String dir) {
	assert dir : 'directory to delete is required'

	def cwd = getContext(hudson.FilePath.class)
	assert cwd : 'A node must be allocated to use rmdir()'
	
	cwd.child(dir).deleteRecursive()
}