pipeline {
    agent any

     parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: '你的名字')
        string(name: 'NEW_VERSION', defaultValue: '1.1.xxx', description: '版本號:IOS雙數1.1.184')
    }

    environment {
        REMOTE4463_CREDENTIALS ='remote4463_login'
        SVN_INSTALL_PATH = '/opt/homebrew/bin/svn'
        svnCommitMessage = ''
        filePath = ''
        PROJ_ANDROID_URL = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/"
        SVM_CREDENTIALS_ID = '97731a4e-685d-4356-8c2b-d902c44ed6e9'
        versionsUpdated = false
        GAMEENV = 'iOS'
        REMOTE_CREDENTIALS ='remote4463_login'
    }

    stages {
         stage('Parameter Check') {
            steps {
                script {
                    if (params.NEW_VERSION == '1.1.xxx') {
                        error("The NEW_VERSION parameter is set to '1.1.xxx', which is a placeholder and not a valid version name. Pipeline execution is terminated to prevent build configuration errors.")
                    }
                    SVN = "${env.SVN_INSTALL_PATH}/svn"

                    def versionCode = params.NEW_VERSION.tokenize('.').last() as Integer
                    def isEven = versionCode % 2 == 0
                    
                    if ((isEven && GAMEENV != 'iOS') || (!isEven && GAMEENV != 'Android')) {
                        error("The selected GAMEENV is incompatible with the NEW_VERSION. For even versions, select 'iOS'. For odd versions, select 'Android'. Pipeline execution is terminated to prevent build configuration errors.")
                    }
                }
            }
        }

        stage('Update Version Info') {
            steps {
                script {
                    try {
                        filePath = '/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/targetVersion.json'
                        
                        def versionsFile = readFile filePath
                        def versions = readJSON text: versionsFile
                        String platform = GAMEENV
                    
                        String newVersion = params.NEW_VERSION

                        if (versions.containsKey(platform)) {
                            if (!versions[platform].contains(newVersion)) {
                                versions[platform].add(newVersion)
                                def updatedVersions = writeJSON returnText: true, json: versions
                                writeFile file: filePath, text: updatedVersions

                                svnCommitMessage = "Updated targetversion from 2412 jenkins ${platform} version to ${newVersion}"
                                versionsUpdated = true
                            } else {
                                versionsUpdated = false
                            }
                        } else {
                            versionsUpdated = false
                        }
                    } catch (Exception e) {
                        echo "Error in 'Update Version Info': ${e.getMessage()}"
                    }
                }
            }
        }


        stage('SVN commit target version') {
            steps {
                script {
                    dir(PROJ_ANDROID_URL) {
                        if (versionsUpdated) {
                            try {
                                def commitMessage = "jenkins 2412 auto commit target version"
                                svnCommit(filePath, commitMessage)
                            } catch (Exception e) {
                                echo "Error in 'SVN commit target version': ${e.getMessage()}"
                            }
                        } else {
                            echo 'No version updates to commit.'
                        }
                    }
                }
            }
        }


        stage('ReplaceBatchFile') {
            steps {
                script {
                    try {
                        def svn = "/opt/homebrew/bin/svn"                    
                        withCredentials([usernamePassword(credentialsId: SVM_CREDENTIALS_ID, usernameVariable: 'SVN_USER', passwordVariable: 'SVN_PASSWORD')]) {
                            sh """
                            ${svn} update /Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/targetVersion.json --username \$SVN_USER --password \$SVN_PASSWORD --non-interactive
                            """
                        }
                        sh "chmod +x /Users/tmd/Documents/tmd/SouthPark/Jenkins/SyncFileTemp/replace_jenkins.sh"
                        sh "/Users/tmd/Documents/tmd/SouthPark/Jenkins/SyncFileTemp/replace_jenkins.sh ${GAMEENV}"
                    } catch (Exception e) {
                        echo "Error in 'ReplaceBatchFile': ${e.getMessage()}"
                    }
                }
            }
        }


        stage('Move ffsBatch') {
            steps {
                script {
                    try {
                        def destinationFolder = ""
                        if (GAMEENV == 'Android') {
                            destinationFolder= "/Volumes/TMD_PUBLIC/TMD/testing/android/"
                        } else {
                            destinationFolder= "/Volumes/TMD_PUBLIC/TMD/testing/ios/"
                        }
                        def sourceFolder = "/Users/tmd/Documents/tmd/SouthPark/Jenkins/SyncFileTemp/Output/"
                        sh "cp -r ${sourceFolder}* ${destinationFolder}"
                    } catch (Exception e) {
                        echo "Error in 'Move ffsBatch': ${e.getMessage()}"
                    }
                }
            }
        }


        stage('Create ffs_batch') {
            steps {
                script {
                    try {
                        dir(PROJ_ANDROID_URL) {
                            def ffsContent = generateFfsBatchContent(params.NEW_VERSION)
                            writeFile file: 'TMD_testing_new_version_to_4055.ffs_batch', text: ffsContent

                            def destinationFolder = "/Volumes/TMD_PUBLIC/TMD/testing/"
                            def sourceFolder = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/TMD_testing_new_version_to_4055.ffs_batch"
                            sh "cp -r ${sourceFolder}* ${destinationFolder}"
                        }
                    } catch (Exception e) {
                        echo "Error in 'Create ffs_batch': ${e.getMessage()}"
                    }
                }
            }
        }

        stage('Create new Version resources in 4463') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'REMOTE_PASSWORD', usernameVariable: 'REMOTE_USER')]) {
                        try {
                            def remote = getRemoteConfig("192.168.44.63", REMOTE_USER, REMOTE_PASSWORD)
                            def sourceFolder = GAMEENV == 'iOS' ? "D:\\TMD_PUBLIC\\TMD\\testing\\ios" : "D:\\TMD_PUBLIC\\TMD\\testing\\android"
                            def newVersionFolder = "D:\\TMD_PUBLIC\\TMD\\testing\\${params.NEW_VERSION}"
                            def checkAndCopyCmd = "if not exist \"${newVersionFolder}\" (mkdir \"${newVersionFolder}\" && xcopy /E /I \"${sourceFolder}\\*\" \"${newVersionFolder}\")"
                            sshCommand remote: remote, command: checkAndCopyCmd
                        } catch (Exception e) {
                            echo "Error in 'Create new Version resources in 4463': ${e.getMessage()}"
                        }
                    }
                }
            }
        }


        stage('Move testing game resources to 4055') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'REMOTE_PASSWORD', usernameVariable: 'REMOTE_USER')]) {
                        def remote = getRemoteConfig("192.168.44.63", REMOTE_USER, REMOTE_PASSWORD)
                        try {
                            sshCommand remote: remote, command: "D:/TMD_PUBLIC/TMD/testing/on_TMD_testing_new_version_to_4055.bat"
                        } catch (Exception e) {
                            echo "Error in 'Move testing game resources to 4055': ${e.getMessage()}"
                        }
                    }
                }
            }
        }
        
        stage('Move WebEnv to 4055') {
            steps {
                script {

                    dir(PROJ_ANDROID_URL) {
                        def ffsContent = moveWebEnvTo4055(params.NEW_VERSION)
                        writeFile file: '4055_webEnv.ffs_batch', text: ffsContent

                        def destinationFolder = "/Volumes/TMD_PUBLIC/TMD/testing/"
                        def sourceFolder = "/Users/tmd/Documents/tmd/SouthPark/ClientCocos/proj.android/4055_webEnv.ffs_batch"
                        sh "cp -r ${sourceFolder}* ${destinationFolder}"
                    }

                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'REMOTE_PASSWORD', usernameVariable: 'REMOTE_USER')]) {
                        def remote = getRemoteConfig("192.168.44.63", REMOTE_USER, REMOTE_PASSWORD)
                        try {
                            sshCommand remote: remote, command: "D:/TMD_PUBLIC/TMD/testing/4055_webEnv.bat"
                        } catch (Exception e) {
                            echo "Error in 'Move testing game resources to 4055': ${e.getMessage()}"
                        }
                    }
                }
            }
        }

        stage('Good guy will clean environment') {
            steps {
                script {
                    dir(PROJ_ANDROID_URL) {
                        try {
                            echo 'Removing TMD_testing_new_version_to_4055.ffs_batch'
                            sh "rm -f TMD_testing_new_version_to_4055.ffs_batch"
                        } catch (Exception e) {
                            echo "Error removing TMD_testing_new_version_to_4055.ffs_batch: ${e.getMessage()}"
                        }
                    }

                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'REMOTE_PASSWORD', usernameVariable: 'REMOTE_USER')]) {
                        def remote = getRemoteConfig("192.168.44.63", REMOTE_USER, REMOTE_PASSWORD)
                        try {
                            sshCommand remote: remote, command: "rmdir /s /q \"D:/TMD_PUBLIC/TMD/testing/${params.NEW_VERSION}\""
                        } catch (Exception e) {
                            echo "Error cleaning environment on remote server: ${e.getMessage()}"
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            emailext(
                subject: "${currentBuild.currentResult} Update freefilesync data and testing data",
                body: "${currentBuild.currentResult}, Update freefilesync data and testing data",
                to: "${params.BUILDER_NAME}@igs.com.tw"
            )
        }
    }
}

def getRemoteConfig(host, user, password) {
    def remote = [:]
    remote.name = user
    remote.host = host
    remote.user = user
    remote.password = password
    remote.allowAnyHosts = true
    return remote
}

def svnCommit(String filePath, String commitMessage) {
    def svn = "${env.SVN_INSTALL_PATH}"

    withCredentials([usernamePassword(credentialsId: SVM_CREDENTIALS_ID, usernameVariable: 'SVN_USER', passwordVariable: 'SVN_PASSWORD')]) {
        sh "${svn} commit -m '${commitMessage}' --username ${SVN_USER} --password ${SVN_PASSWORD} ${filePath}"
    }
}

def generateFfsBatchContent(String newVersion) {
    String template = '''<?xml version="1.0" encoding="utf-8"?>
<FreeFileSync XmlType="BATCH" XmlFormat="19">
    <Notes/>
    <Compare>
        <Variant>Content</Variant>
        <Symlinks>Exclude</Symlinks>
        <IgnoreTimeShift/>
    </Compare>
    <Synchronize>
        <Variant>Mirror</Variant>
        <DetectMovedFiles>false</DetectMovedFiles>
        <DeletionPolicy>RecycleBin</DeletionPolicy>
        <VersioningFolder Style="Replace"/>
    </Synchronize>
    <Filter>
        <Include>
            <Item>*</Item>
        </Include>
        <Exclude>
            <Item>\\System Volume Information\\</Item>
            <Item>\\$Recycle.Bin\\</Item>
            <Item>\\RECYCLER\\</Item>
            <Item>\\RECYCLED\\</Item>
            <Item>*\\desktop.ini</Item>
            <Item>*\\thumbs.db</Item>
            <Item>*\\.Ds_Store</Item>
            <Item>*\\LocalVersion.json</Item>
            <Item>*\\AllVersion.json</Item>
            <Item>*\\CheckVersion.json</Item>
            <Item>*\\update.ffs_batch</Item>
            <Item>*\\update.bat</Item>
        </Exclude>
        <SizeMin Unit="None">0</SizeMin>
        <SizeMax Unit="None">0</SizeMax>
        <TimeSpan Type="None">0</TimeSpan>
    </Filter>
    <FolderPairs>
        <Pair>
            <Left>D:\\TMD_PUBLIC\\TMD\\testing\\_newVersion_</Left>
            <Right>\\\\10.100.40.55\\webgame\\Game\\TMD_mobile_test\\data\\_newVersion_</Right>
        </Pair>
    </FolderPairs>
    <Errors Ignore="true" Retry="0" Delay="5"/>
    <PostSyncCommand Condition="Completion"/>
    <LogFolder/>
    <EmailNotification Condition="Always"/>
    <GridViewType>Action</GridViewType>
    <Batch>
        <ProgressDialog Minimized="false" AutoClose="true"/>
        <ErrorDialog>Show</ErrorDialog>
        <PostSyncAction>None</PostSyncAction>
    </Batch>
</FreeFileSync>'''

    template = template.replaceAll("_newVersion_", newVersion)
    
    return template
}





def moveWebEnvTo4055(String newVersion) {
    String template = '''<?xml version="1.0" encoding="utf-8"?>
<FreeFileSync XmlType="BATCH" XmlFormat="19">
    <Notes/>
    <Compare>
        <Variant>TimeAndSize</Variant>
        <Symlinks>Exclude</Symlinks>
        <IgnoreTimeShift/>
    </Compare>
    <Synchronize>
        <Variant>Update</Variant>
        <DetectMovedFiles>false</DetectMovedFiles>
        <DeletionPolicy>RecycleBin</DeletionPolicy>
        <VersioningFolder Style="Replace"/>
    </Synchronize>
    <Filter>
        <Include>
            <Item>*</Item>
        </Include>
        <Exclude/>
        <SizeMin Unit="None">0</SizeMin>
        <SizeMax Unit="None">0</SizeMax>
        <TimeSpan Type="None">0</TimeSpan>
    </Filter>
    <FolderPairs>
        <Pair>
            <Left>D:\\TMD_PUBLIC\\TMD\\testing\\4055_webEnv</Left>
            <Right>\\\\10.100.40.55\\webgame\\Game\\TMD_mobile_test\\data\\_newVersion_\\Inanna\\InannaLua</Right>
        </Pair>
    </FolderPairs>
    <Errors Ignore="true" Retry="0" Delay="5"/>
    <PostSyncCommand Condition="Completion"/>
    <LogFolder/>
    <EmailNotification Condition="Always"/>
    <GridViewType>Action</GridViewType>
    <Batch>
        <ProgressDialog Minimized="false" AutoClose="true"/>
        <ErrorDialog>Show</ErrorDialog>
        <PostSyncAction>None</PostSyncAction>
    </Batch>
</FreeFileSync>'''

    template = template.replaceAll("_newVersion_", newVersion)
    
    return template
}














