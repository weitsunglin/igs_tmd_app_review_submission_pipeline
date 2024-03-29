pipeline {
    agent any

    parameters {
        string(name: 'BUILDER_NAME', defaultValue: 'weitsunglin', description: '你的名字')
        string(name: 'NEW_IOS_VERSION', defaultValue: '1.1.xxx', description: '版本號:安卓單數1.1.175/IOS雙數1.1.184')
    }

    environment {
        MSSQL_SERVER = 'db-gt-test24.gametower.com.tw'  //db-gt-test24.gametower.com.tw、10.100.20.17
        MSSQL_DATABASE = 'GameTower2_TMD'
        TMD_DB = 'tmd_db'
        REMOTE_CREDENTIALS ='remote4463_login'
        NEWIOSVERSION = ''
        PRE_VERSION = "0.0.000"
    }
    
    stages {
        stage('Parameter Check') {
            steps {
                script {
                    if (params.NEW_IOS_VERSION == '1.1.xxx') {
                        error("The NEW_IOS_VERSION parameter is set to '1.1.xxx', which is a placeholder and not a valid version name. Pipeline execution is terminated to prevent build configuration errors.")
                    }
                }
            }
        }

        stage('Caculate per version') {
            steps {
                script { 
                    try {
                        NEWIOSVERSION = params.NEW_IOS_VERSION  

                        def versionParts = NEWIOSVERSION.tokenize('.')
                        def major = versionParts[0] as int
                        def minor = versionParts[1] as int
                        def patch = (versionParts[2] as int) - 2
                        PRE_VERSION = "${major}.${minor}.${patch}"
                    } catch (Exception e) {
                        echo "Error in 'Caculate per version' stage: ${e.getMessage()}"
                    }
                }
            }
        }
        
        stage('Insert 二測 DB LatestSvnRevision') {
            steps {
                script {
                    MSSQL_SERVER = 'db-gt-test24.gametower.com.tw'

                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'REMOTE_PASSWORD', usernameVariable: 'REMOTE_USER')]) {
                        def remote = getRemoteConfig("192.168.44.63", REMOTE_USER, REMOTE_PASSWORD)
                        withCredentials([usernamePassword(credentialsId: TMD_DB, usernameVariable: 'DB_ACCOUNT', passwordVariable: 'DB_PASSWORD')]) {
                            try {
                                def insert_dbCommand = "sqlcmd -S ${MSSQL_SERVER} -d ${MSSQL_DATABASE} -U ${DB_ACCOUNT} -P ${DB_PASSWORD} -Q \"INSERT INTO [GameTower2_TMD].[dbo].[Client_LatestSvnRevision] SELECT '${NEWIOSVERSION}', [path], [revision] FROM [GameTower2_TMD].[dbo].[Client_LatestSvnRevision] where [deviceVersion] = '${PRE_VERSION}'\""
                                sshCommand remote: remote, command: insert_dbCommand

                                def confirm_dbCommand = "sqlcmd -S ${MSSQL_SERVER} -d ${MSSQL_DATABASE} -U ${DB_ACCOUNT} -P ${DB_PASSWORD} -Q \"SELECT TOP 999 * FROM Client_LatestSvnRevision WHERE [deviceVersion] = '${NEWIOSVERSION}'\""
                                sshCommand remote: remote, command: confirm_dbCommand
                            } catch (Exception e) {
                                echo "Caught an exception in 'Insert DB LatestSvnRevision' stage: ${e.getMessage()}"
                                throw e
                            }
                        }
                    }
                }
            }
        }

        stage('Insert 外部 DB LatestSvnRevision') {
            steps {
                script {
                    MSSQL_SERVER = '10.100.20.17'

                    withCredentials([usernamePassword(credentialsId: REMOTE_CREDENTIALS, passwordVariable: 'REMOTE_PASSWORD', usernameVariable: 'REMOTE_USER')]) {
                        def remote = getRemoteConfig("192.168.44.63", REMOTE_USER, REMOTE_PASSWORD)
                        withCredentials([usernamePassword(credentialsId: TMD_DB, usernameVariable: 'DB_ACCOUNT', passwordVariable: 'DB_PASSWORD')]) {
                            try {
                                def insert_dbCommand = "sqlcmd -S ${MSSQL_SERVER} -d ${MSSQL_DATABASE} -U ${DB_ACCOUNT} -P ${DB_PASSWORD} -Q \"INSERT INTO [GameTower2_TMD].[dbo].[Client_LatestSvnRevision] SELECT '${NEWIOSVERSION}', [path], [revision] FROM [GameTower2_TMD].[dbo].[Client_LatestSvnRevision] where [deviceVersion] = '${PRE_VERSION}'\""
                                sshCommand remote: remote, command: insert_dbCommand

                                def confirm_dbCommand = "sqlcmd -S ${MSSQL_SERVER} -d ${MSSQL_DATABASE} -U ${DB_ACCOUNT} -P ${DB_PASSWORD} -Q \"SELECT TOP 999 * FROM Client_LatestSvnRevision WHERE [deviceVersion] = '${NEWIOSVERSION}'\""
                                sshCommand remote: remote, command: confirm_dbCommand
                            } catch (Exception e) {
                                echo "Caught an exception in 'Insert DB LatestSvnRevision' stage: ${e.getMessage()}"
                                throw e
                            }
                        }
                    }
                }
            }
        }

    }
    
    post {
        always {
            script {
                if (currentBuild.result == 'SUCCESS') {
                    emailext(
                        subject: "SUCCESS Add 二測及外部 DB LatestSvnRevision",
                        body: "SUCCESS",
                        to: "${params.BUILDER_NAME}@igs.com.tw"
                    )
                } 
                else {
                    emailext(
                        subject: "Failure Add 二測及外部  DB LatestSvnRevision",
                        body: "Failure",
                        to: "${params.BUILDER_NAME}@igs.com.tw"
                    )
                }
            }
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