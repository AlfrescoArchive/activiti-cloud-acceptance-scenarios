pipeline {
  agent {
    label "jenkins-maven"
  }
  environment {
    ORG = 'almerico'
    APP_NAME = 'activiti-cloud-acceptance-scenarios'
    CHARTMUSEUM_CREDS = credentials('jenkins-x-chartmuseum')
    GATEWAY_HOST = "activiti-cloud-gateway.jx-staging.35.228.195.195.nip.io"
    SSO_HOST = "activiti-keycloak.jx-staging.35.228.195.195.nip.io"
    REALM = "activiti"
  }
  stages {
    stage('CI Build and push snapshot') {
      when {
        branch 'PR-*'
      }
      environment {
        PREVIEW_VERSION = "0.0.0-SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER"
        PREVIEW_NAMESPACE = "$APP_NAME-$BRANCH_NAME".toLowerCase()
        HELM_RELEASE = "$PREVIEW_NAMESPACE".toLowerCase()
      }
      steps {
        container('maven') {
          sh "mvn versions:set -DnewVersion=$PREVIEW_VERSION"
          dir('charts/preview') {
            sh "make preview"
            sh "jx preview --app $APP_NAME --dir ../.."
          }
          
          // sh "mvn clean install -DskipTests && mvn -pl '!apps-acceptance-tests,!multiple-runtime-acceptance-tests,!security-policies-acceptance-tests' clean verify"
          sh "mvn clean install -DskipTests"
          
          dir('charts/preview') {
            sh "make delete"
            //sh "jx delete preview --app $APP_NAME"
          }
          

          // sh "mvn install"
          // sh "export VERSION=$PREVIEW_VERSION && skaffold build -f skaffold.yaml"
          // sh "jx step post build --image $DOCKER_REGISTRY/$ORG/$APP_NAME:$PREVIEW_VERSION"
          
        }
      }
    }
    stage('Build Release') {
      when {
        branch 'master'
      }
      steps {
        container('maven') {
          // ensure we're not on a detached head
          sh "git checkout master"
          sh "git config --global credential.helper store"
          sh "jx step git credentials"
          sh "mvn clean install -DskipTests && mvn -pl '!apps-acceptance-tests,!multiple-runtime-acceptance-tests,!security-policies-acceptance-tests' clean verify"
 
          // so we can retrieve the version in later steps
          // sh "echo \$(jx-release-version) > VERSION"
          // sh "mvn versions:set -DnewVersion=\$(cat VERSION)"
          // sh "jx step tag --version \$(cat VERSION)"
          // sh "mvn clean deploy"
          // sh "export VERSION=`cat VERSION` && skaffold build -f skaffold.yaml"
          // sh "jx step post build --image $DOCKER_REGISTRY/$ORG/$APP_NAME:\$(cat VERSION)"
        }
      }
    }
    // stage('Promote to Environments') {
    //   when {
    //     branch 'master'
    //   }
    //   steps {
    //     container('maven') {
    //       dir('charts/activiti-cloud-acceptance-scenarios') {
    //         sh "jx step changelog --version v\$(cat ../../VERSION)"

    //         // release the helm chart
    //         sh "jx step helm release"

    //         // promote through all 'Auto' promotion Environments
    //         sh "jx promote -b --all-auto --timeout 1h --version \$(cat ../../VERSION)"
    //       }
    //     }
    //   }
    // }
  }
  post {
        always {
          cleanWs()
        }
  }
}
