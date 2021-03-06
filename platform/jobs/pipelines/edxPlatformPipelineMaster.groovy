package platform

import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_CANCEL_BUILDS_ON_UPDATE

Map publicBokchoyJobConfig = [
    open: true,
    jobName: 'edx-platform-bokchoy-pipeline-master',
    repoName: 'edx-platform',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'bokchoy',
    branch: 'master'
]

Map privateBokchoyJobConfig = [
    open: false,
    jobName: 'edx-platform-bokchoy-pipeline-master_private',
    repoName: 'edx-platform-private',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'bokchoy',
    branch: 'security-release'
]

Map publicLettuceJobConfig = [
    open: true,
    jobName: 'edx-platform-lettuce-pipeline-master',
    repoName: 'edx-platform',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'lettuce',
    branch: 'master'
]

Map privateLettuceJobConfig = [
    open: false,
    jobName: 'edx-platform-lettuce-pipeline-master_private',
    repoName: 'edx-platform-private',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'lettuce',
    branch: 'security-release'
]

Map publicPythonJobConfig = [
    open: true,
    jobName: 'edx-platform-python-pipeline-master',
    repoName: 'edx-platform',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'python',
    branch: 'master'
]

Map privatePythonJobConfig = [
    open: false,
    jobName: 'edx-platform-python-pipeline-master_private',
    repoName: 'edx-platform-private',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'python',
    branch: 'security-release'
]

Map publicQualityJobConfig = [
    open: true,
    jobName: 'edx-platform-quality-pipeline-master',
    repoName: 'edx-platform',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'quality',
    branch: 'master'
]

Map privateQualityJobConfig = [
    open: false,
    jobName: 'edx-platform-quality-pipeline-master_private',
    repoName: 'edx-platform-private',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'quality',
    branch: 'security-release'
]

List jobConfigs = [
    publicBokchoyJobConfig,
    privateBokchoyJobConfig,
    publicLettuceJobConfig,
    privateLettuceJobConfig,
    publicPythonJobConfig,
    privatePythonJobConfig,
    publicQualityJobConfig,
    privateQualityJobConfig
]

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    // This is the job DSL responsible for creating the main pipeline job.
    pipelineJob(jobConfig.jobName) {

        definition {

            if (!jobConfig.open.toBoolean()) {
                authorization GENERAL_PRIVATE_JOB_SECURITY()
            }
            logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)
            environmentVariables(
                REPO_NAME: "${jobConfig.repoName}",
                BRANCH_NAME: "${jobConfig.branch}"
            )

            triggers {
                githubPush()
            }

            cpsScm {
                scm {
                    git {
                        extensions {
                            cloneOptions {
                                honorRefspec(true)
                                noTags(true)
                                shallow(true)
                            }
                            sparseCheckoutPaths {
                                sparseCheckoutPaths {
                                    sparseCheckoutPath {
                                        path(jobConfig.jenkinsFileDir)
                                    }
                                }
                            }
                        }
                        remote {
                            credentials('jenkins-worker')
                            github("edx/${jobConfig.repoName}", 'ssh', 'github.com')
                            refspec("+refs/heads/${jobConfig.branch}:refs/remotes/origin/${jobConfig.branch}")
                            branch(jobConfig.branch)
                        }
                    }
                }
                scriptPath(jobConfig.jenkinsFileDir + '/' + jobConfig.jenkinsFileName)
            }
        }
    }
}
