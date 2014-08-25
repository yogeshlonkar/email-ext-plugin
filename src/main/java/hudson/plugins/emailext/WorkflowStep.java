package hudson.plugins.emailext;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Workflow step for the Extended Email Plugin.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class WorkflowStep extends AbstractStepImpl {

    private String triggerName;
    private String recipientList;
    private String replyto;
    private String subject;
    private String content;
    private String contentType;
    private String attachments;
    private boolean attachBuildlog;
    private boolean compressBuildlog;
    private String presendScript;
    private boolean saveOutput;

    @DataBoundConstructor
    public WorkflowStep(String triggerName, String recipientList, String replyto, String subject, String content,
                        String contentType, String attachments, boolean attachBuildlog, boolean compressBuildlog,
                        String presendScript, boolean saveOutput) {
        this.triggerName = triggerName;
        this.recipientList = recipientList;
        this.replyto = replyto;
        this.subject = subject;
        this.content = content;
        this.contentType = contentType;
        this.attachments = attachments;
        this.attachBuildlog = attachBuildlog;
        this.compressBuildlog = compressBuildlog;
        this.presendScript = presendScript;
        this.saveOutput = saveOutput;
    }

    @Override
    public StepDescriptor getDescriptor() {
        return super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(WorkflowStepStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            // TODO: name kinda sucks
            return "emailext";
        }

        @Override
        public String getDisplayName() {
            return "Email Extension";
        }
    }

    public static class WorkflowStepStepExecution extends AbstractSynchronousStepExecution<Void> {

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient Launcher launcher;

        @StepContextParameter
        private transient TaskListener listener;

        @Inject
        WorkflowStep step;

        @Override
        protected Void run() throws Exception {
            WorkflowPublisher workflowPublisher =
                    new WorkflowPublisher(step.triggerName, step.recipientList, step.replyto, step.subject, step.content,
                            step.contentType, step.attachments, step.attachBuildlog, step.compressBuildlog, step.presendScript,
                            step.saveOutput);
            workflowPublisher.perform(build, ws, launcher, listener);
            return null;
        }
    }
}



