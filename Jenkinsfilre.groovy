pipeline {
    agent any
    tools {
        maven "MAVEN"
        jdk "JDK"
    }   
    stages {
        stage("Code Checkout from GitLab") {
            steps {
                git branch: 'master',
                    credentialsId: 'gitlab_access_token',
                        url: 'http://your-ip-here:10080/root/test-project.git'
                }
            }
        }
    stages {
        stage('Code Quality Check via SonarQube') {
            steps {
                script {
                def scannerHome = tool 'sonarqube';
                    withSonarQubeEnv("sonarqube-container") {
                    sh "${tool("sonarqube")}/bin/sonar-scanner \
                    -Dsonar.projectKey=test-node-js \
                    -Dsonar.sources=. \
                    -Dsonar.css.node=. \
                    -Dsonar.host.url=http://your-ip-here:9000 \
                    -Dsonar.login=your-generated-token-from-sonarqube-container"
               }
           }
       }
   }
}
    stages {
        stage('Initialize'){
            steps{
                echo "PATH = ${M2_HOME}/bin:${PATH}"
                echo "M2_HOME = /opt/maven"
            }
        }
    }
        stages {
            stage('Build') {
                steps {
                    dir("/var/lib/jenkins/workspace/demopipelinetask/my-app") {
                    sh 'mvn -B -DskipTests clean package'
                }
            }
        }
    }
    post {
       always {
          junit(
        allowEmptyResults: true,
        testResults: '*/test-reports/.xml'
      )
      }
   } 
        stage('Upload to AWS') {
              steps {
                  withAWS(region:'ap-south-1',credentials:'wach-jenkins-cred') {
                  sh 'echo "Uploading content with AWS creds"'
                      s3Upload(pathStyleAccessEnabled: true, payloadSigningEnabled: true, file:'app.py', bucket:'jenkins-s3-bucket-wach')
                    }
                }
            }
}
