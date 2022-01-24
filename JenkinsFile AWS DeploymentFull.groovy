environment {
	CHECKRESULT=""
}

pipeline {
	agent {label "awsjenklinux"}
	tools{
	    maven 'Maven 3.2.5'
	}
    options {
        buildDiscarder(logRotator(numToKeepStr: '5',daysToKeepStr:'16'))
        timeout(time: 30, unit: 'MINUTES')
    }
    parameters {

        string(name: 'GIT_REPO', defaultValue: 'https://test.com/scm/myproject.git', description: '')
        choice(choices: ['YES','NO'], description: 'default to YES', name: 'DeployFlag')
        choice(choices: ['cn-east-1','cn-west-2'], description: '', name: 'REGION')
        choice(choices: ['111011193316', '2222215333393'], description: '', name: 'USER_ID')
       
        choice(choices: ['dev', 'qa', 'prd'], description: '', name: 'RUN_ENV')
        choice(choices: ['module1','module2','module3','module4'], description: '', name: 'application_dir')
        choice(choices: ['s3-code-package','s3-code-package-bg'], description: '', name: 'bucket')

    }
    environment {
        PROJECT_NAME = "myproject"
        JENKIN_ROLE_NAME='role-Deploy'
        ROOT_BACKEND_PATH = "${WORKSPACE}"
        MAVEN_HOME = "${tool 'Maven 3.5.3 -Linux -Windows'}"
        
        VERSION_NUMBER = "0.0.${BUILD_NUMBER}.${RUN_ENV}"
        mvn_package_name = "target/${application_dir}-0.0.1-SNAPSHOT.zip"
        application_dirs = "${PROJECT_NAME}/${application_dir}"
    }
    stages {

        stage('PullCode'){
            steps{
                configFileProvider([configFile(fileId: '123a1944-605d-2280-a1ec-5e1115bb01fc', variable: 'pullcode')]) {
                    script{
                        def pullcode = load "$pullcode"
                        pullcode.call()
                    }
                }
            }
          
        }

        stage( 'SecurityTest' ) {
            when {
                environment ignoreCase: true, name: 'SKIP_SECURITY_TEST', value: 'NO'
            }
            agent {label "redhat_linux"}
            steps{
                configFileProvider([configFile(fileId: 'e9432a23-8e11-22e1-aa60-123439512fe1', variable: 'checkmarx')]) {
                    script{
                        def checkmarx = load "$checkmarx"
                        checkmarx.call()
                    }
                }
            }
        }
        
        stage('PackageToS3' ) {
	        when {
                environment ignoreCase: true, name: 'DeployFlag', value: 'YES'
                beforeAgent true
            }
            steps{
                configFileProvider([configFile(fileId: 'fd712345-1ef1-4ba3-a311-66666aa899bd', variable: 'buildPackage')]) {
                    script{

                        def UPLOAD_SCRIPT = '''
                            cd myproject
                            ${MAVEN_HOME}/bin/mvn package -U -Dmaven.test.skip=true -am
                            cd ../
                            source terraform_scripts/role.sh ${USER_ID} ${JENKIN_ROLE_NAME}
                            aws s3 cp ${application_dirs}/${mvn_package_name} s3://${bucket}/code-package/${application_dirs}_${VERSION_NUMBER}.zip
                            '''
                        sh UPLOAD_SCRIPT
                        echo "VERSION_NUMBER:  ${VERSION_NUMBER}"
                    }
                }
            }
        }
    }
}