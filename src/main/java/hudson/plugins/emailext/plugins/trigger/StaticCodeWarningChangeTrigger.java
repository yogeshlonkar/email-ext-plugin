package hudson.plugins.emailext.plugins.trigger;

import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TaskListener;
import hudson.plugins.analysis.core.AbstractResultAction;
import hudson.plugins.analysis.core.BuildResult;
import hudson.plugins.analysis.core.ParserResult;
import hudson.plugins.analysis.util.StringPluginLogger;
import hudson.plugins.checkstyle.CheckStyleResultAction;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.EmailTriggerDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider;
import hudson.plugins.pmd.PmdPublisher;
import hudson.plugins.pmd.PmdResult;
import hudson.plugins.pmd.PmdResultAction;
import hudson.tasks.Publisher;
import net.sf.json.JSONObject; 

public class StaticCodeWarningChangeTrigger extends EmailTrigger {

	public static final String TRIGGER_NAME = "Static Code Warning Increase";

	protected String buildId;

	@DataBoundConstructor
	public StaticCodeWarningChangeTrigger(List<RecipientProvider> recipientProviders, String recipientList,
			String replyTo, String subject, String body, String attachmentsPattern, int attachBuildLog,
			String contentType, String buildId) {
		super(recipientProviders, recipientList, replyTo, subject, body, attachmentsPattern, attachBuildLog,
				contentType);
		this.buildId = buildId;
	}

	@Override
	public boolean trigger(AbstractBuild<?, ?> build, TaskListener listener) {
		listener.getLogger().println("[Email-ext] checking pmd/checkstyle high warnings change");
		try {
			if(buildId != null && !buildId.trim().isEmpty()) {
				AbstractProject<?, ?> project = build.getProject();
				AbstractBuild<?, ?> oldBuild = project.getBuild(buildId);
				return isWarningChange(oldBuild, build, PmdResultAction.class, listener) || isWarningChange(oldBuild, build, CheckStyleResultAction.class, listener);
			}
		} catch (Exception e) {
			listener.getLogger().println("[Email-ext] error occured " +e.getMessage());
			e.printStackTrace();
		}
		return true;
	}

	private <T extends AbstractResultAction> boolean isWarningChange(AbstractBuild<?, ?> oldBuild, AbstractBuild<?, ?> build, Class<T> class1, TaskListener listener) {
		listener.getLogger().println("[Email-ext] checking  " + class1.getName());
		if(oldBuild.getAction(class1) != null && build.getAction(class1) != null) {
			BuildResult buildResultOld = oldBuild.getAction(class1).getResult();
			BuildResult buildResultNew = build.getAction(class1).getResult();
			if(buildResultNew != null && buildResultOld != null) {
				listener.getLogger().println("[Email-ext] high priority warnings, old count:" + buildResultOld.getNumberOfHighPriorityWarnings() + " new count:" + buildResultNew.getNumberOfHighPriorityWarnings());
				if(buildResultNew.getNumberOfHighPriorityWarnings() <= buildResultOld.getNumberOfHighPriorityWarnings()) {
					return false;
				}
			}
		}
		return true;
	}

	public String getBuildId() {
		return buildId;
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject formData) {
		super.configure(req, formData);
		if (formData.containsKey("buildId")) {
			this.buildId = formData.optString("buildId", "");
		}
		return true;
	}

	@Extension
	public static final class DescriptorImpl extends EmailTriggerDescriptor {

		public DescriptorImpl() {
			addDefaultRecipientProvider(new DevelopersRecipientProvider());
		}

		@Override
		public String getDisplayName() {
			return TRIGGER_NAME;
		}

		@Override
		public EmailTrigger createDefault() {
			return _createDefault();
		}
	}
}
