pipeline {
  agent {
    node {
      label 'maven'
    }

  }
  stages {
    stage('拉取代碼') {
      steps {
        git(credentialsId: 'gitee-id', url: 'yoururl', branch: 'master', changelog: true, poll: false)
        sh 'echo 正在構建 $PROJECT_VERSION  版本號: $PROJECT_NAME 將會提交給 $REGISTRY 鏡像倉庫'
        container('maven') {
          sh 'mvn clean install -Dmaven.test.skip=true -gs `pwd`/mvn-settings.xml'
        }
      }
    }
    stage('sonar代碼質量分析') {
      steps {
        container('maven') {
          withCredentials([string(credentialsId: "$SONAR_CREDENTIAL_ID", variable: 'SONAR_TOKEN')]) {
            withSonarQubeEnv('sonar') {
              sh 'echo 當前目標 `pwd`'
              sh "mvn sonar:sonar -gs `pwd`/mvn-settings.xml -Dsonar.branch=$BRANCH_NAME -Dsonar.login=$SONAR_TOKEN"
            }
          }
          timeout(time: 1, unit: 'HOURS') {
            waitForQualityGate true
          }
        }
      }
    }

    stage ('構建鏡像-推送鏡像') {
      steps {
          container ('maven') {
              sh 'mvn -Dmaven.test.skip=true -gs `pwd`/mvn-settings.xml clean package'
              sh 'cd $PROJECT_NAME && docker build -f Dockerfile -t $REGISTRY/$DOCKERHUB_NAMESPACE/$PROJECT_NAME:SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER .'
              withCredentials([usernamePassword(passwordVariable : 'DOCKER_PASSWORD' ,usernameVariable : 'DOCKER_USERNAME' ,credentialsId : "$DOCKER_CREDENTIAL_ID" ,)]) {
                  sh 'echo "$DOCKER_PASSWORD" | docker login $REGISTRY -u "$DOCKER_USERNAME" --password-stdin'
                  sh 'docker tag  $REGISTRY/$DOCKERHUB_NAMESPACE/$PROJECT_NAME:SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER $REGISTRY/$DOCKERHUB_NAMESPACE/$PROJECT_NAME:latest '
                  sh 'docker push  $REGISTRY/$DOCKERHUB_NAMESPACE/$PROJECT_NAME:latest '
              }
          }
      }
    }

    stage('部署到k8s') {
      steps {
        input(id: "deploy-to-dev-$PROJECT_NAME", message: "是否將 $PROJECT_NAME 部署到集群中?")
        kubernetesDeploy(configs: "$PROJECT_NAME/deploy/**", enableConfigSubstitution: true, kubeconfigId: "$KUBECONFIG_CREDENTIAL_ID")
      }
    }

  stage('發布版本'){
      when{
        expression{
          return params.PROJECT_VERSION =~ /v.*/
        }
      }
      steps {
          container ('maven') {
            input(id: 'release-image-with-tag', message: '發布當前版本鏡像嗎?')
            sh 'docker tag  $REGISTRY/$DOCKERHUB_NAMESPACE/$PROJECT_NAME:SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER $REGISTRY/$DOCKERHUB_NAMESPACE/$PROJECT_NAME:$PROJECT_VERSION '
            sh 'docker push  $REGISTRY/$DOCKERHUB_NAMESPACE/$PROJECT_NAME:$PROJECT_VERSION '
              withCredentials([usernamePassword(credentialsId: "$GITEE_CREDENTIAL_ID", passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                sh 'git config --global user.email "kubesphere@yunify.com" '
                sh 'git config --global user.name "kubesphere" '
                sh 'git tag -a $PROJECT_NAME-$PROJECT_VERSION -m "$PROJECT_VERSION" '
                sh 'git push http://$GIT_USERNAME:$GIT_PASSWORD@gitee.com/$GITEE_ACCOUNT/gumall.git --tags --ipv4'
              }

      }
      }
    }
  }
  environment {
    DOCKER_CREDENTIAL_ID = 'dockerhub-id'
    GITEE_CREDENTIAL_ID = 'gitee-id'
    KUBECONFIG_CREDENTIAL_ID = 'demo-kubeconfig'
    REGISTRY = 'docker.io'
    DOCKERHUB_NAMESPACE = 'yourdockerhub'
    GITEE_ACCOUNT = 'youraccount'
    SONAR_CREDENTIAL_ID = 'sonar-qube'
    BRANCH_NAME = 'master'
  }
  parameters {
    string(name: 'PROJECT_VERSION', defaultValue: 'v0.0Beta', description: '')
    string(name: 'PROJECT_NAME', defaultValue: 'gumall-gateway', description: '')
  }
}
