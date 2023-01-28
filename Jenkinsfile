pipeline {
	options {
		timeout(time: 40, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
		timestamps()
	}
	agent {
		label 'centos-latest'
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'openjdk-jdk17-latest'
	}
	stages {
		stage('Build') {
			steps {
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh '''
						mvn clean verify --batch-mode -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						-Pbree-libs -Papi-check \
						-Dmaven.test.error.ignore=true -Dmaven.test.failure.ignore=true
					'''
				}
			}
			post {
				always {
					archiveArtifacts(allowEmptyArchive: true, artifacts: '*.log, \
						*/target/work/data/.metadata/*.log, \
						*/tests/target/work/data/.metadata/*.log, \
						apiAnalyzer-workspace/.metadata/*.log')
					publishIssues issues:[scanForIssues(tool: java()), scanForIssues(tool: mavenConsole())]
					junit '**/target/surefire-reports/*.xml'
				}
			}
		}
	}
}
