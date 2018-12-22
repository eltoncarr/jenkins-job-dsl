package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator

class MongoAgentsUpdate {
    public static def job = {
        dslFactory, extraVars ->
        dslFactory.job("Monitoring" + "/mongo-agents-update") {
            logRotator common_logrotator
            parameters {
                stringParam('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git')
                stringParam('CONFIGURATION_BRANCH', 'master')
                stringParam('CONFIGURATION_INTERNAL_REPO', extraVars.get('CONFIGURATION_INTERNAL_REPO',"git@github.com:edx/${deployment}-internal.git"),
                    'Git repo containing internal overrides')
                stringParam('CONFIGURATION_INTERNAL_BRANCH', extraVars.get('CONFIGURATION_INTERNAL_BRANCH', 'master'),
                    'e.g. tagname or origin/branchname')
                stringParam('CONFIGURATION_SECURE_REPO', extraVars.get('CONFIGURATION_SECURE_REPO',"git@github.com:edx-ops/${deployment}-secure.git"),
                    'Secure Git repo .')
                stringParam('CONFIGURATION_SECURE_BRANCH', extraVars.get('CONFIGURATION_SECURE_BRANCH', 'master'),
                    'e.g. tagname or origin/branchname')
            }

            multiscm {
                git {
                    remote {
                        url('$CONFIGURATION_REPO')
                        branch('$CONFIGURATION_BRANCH')
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('configuration')
                    }
                }
                git {
                    remote {
                        url('$CONFIGURATION_INTERNAL_REPO')
                        branch('$CONFIGURATION_INTERNAL_BRANCH')
                            if (gitCredentialId) {
                                credentials(gitCredentialId)
                            }
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('configuration-internal')
                    }
                }
                git {
                    remote {
                        url('$CONFIGURATION_SECURE_REPO')
                        branch('$CONFIGURATION_SECURE_BRANCH')
                            if (gitCredentialId) {
                                credentials(gitCredentialId)
                            }
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('configuration-internal')
                    }
                }
            }

            steps {
                virtualenv {
                    pythonName('System-CPython-3.5')
                    nature("shell")
                    systemSitePackages(false)

                    command(
                        dslFactory.readFileFromWorkspace("devops/resources/mongo-agents-update.sh")
                    )
                }
            }

            publishers {
                mailer(extraVars.get('NOTIFY_ON_FAILURE'), false, false)

            }
        }
    }
}
