package hudson.plugins.emailext.plugins.content;

import hudson.ExtensionList;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.TaskListener;
import hudson.plugins.analysis.core.AbstractResultAction;
import hudson.plugins.analysis.util.model.AbstractAnnotation;
import hudson.plugins.analysis.util.model.FileAnnotation;
import hudson.plugins.analysis.util.model.Priority;
import hudson.plugins.checkstyle.CheckStyleResultAction;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.JellyTemplateConfig.JellyTemplateConfigProvider;
import hudson.plugins.emailext.plugins.EmailToken;
import hudson.plugins.emailext.plugins.EmailTrigger;
import hudson.plugins.emailext.plugins.trigger.StaticCodeWarningChangeTrigger;
import hudson.plugins.pmd.PmdResultAction;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.jelly.*;
import org.jenkinsci.lib.configprovider.ConfigProvider;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.xml.sax.InputSource;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@EmailToken
public class JellyScriptContent extends AbstractEvalContent {

    public static final String MACRO_NAME = "JELLY_SCRIPT";
    private static final String DEFAULT_HTML_TEMPLATE_NAME = "html";
    private static final String DEFAULT_TEMPLATE_NAME = DEFAULT_HTML_TEMPLATE_NAME;
    private static final String JELLY_EXTENSION = ".jelly";
    
    @Parameter
    public String template = DEFAULT_TEMPLATE_NAME;

    public JellyScriptContent() {
        super(MACRO_NAME);
    }
    
    @Override
    public String evaluate(AbstractBuild<?, ?> build, TaskListener listener, String macroName)
            throws MacroEvaluationException, IOException, InterruptedException {
        InputStream inputStream = null;

        try {
            inputStream = getFileInputStream(build.getWorkspace(), template, JELLY_EXTENSION);
            return renderContent(build, inputStream, listener);
        } catch (JellyException e) {
            return "JellyException: " + e.getMessage();
        } catch (FileNotFoundException e) {
            return generateMissingFile("Jelly", template);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    @Override
    protected Class<? extends ConfigProvider> getProviderClass() {
        return JellyTemplateConfigProvider.class;
    }

    private String renderContent(AbstractBuild<?, ?> build, InputStream inputStream, TaskListener listener)
            throws JellyException, IOException {
        JellyContext context = createContext(new ScriptContentBuildWrapper(build), build, listener);
        Script script = context.compileScript(new InputSource(inputStream));
        if (script != null) {
            return convert(build, context, script);
        }
        return null;
    }

    private String convert(AbstractBuild<?, ?> build, JellyContext context, Script script)
            throws JellyTagException, IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(16 * 1024);
        XMLOutput xmlOutput = XMLOutput.createXMLOutput(output);
        script.run(context, xmlOutput);
        xmlOutput.flush();
        xmlOutput.close();
        output.close();
        return output.toString(getCharset(build));
    }

    private JellyContext createContext(Object it, AbstractBuild<?, ?> build, TaskListener listener) {
        JellyContext context = new JellyContext();
        ExtendedEmailPublisherDescriptor descriptor = Jenkins.getActiveInstance().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        context.setVariable("it", it);
        AbstractProject<?,?> project  = build.getProject();
        AbstractBuild<?, ?> oldBuild = null;
        for(Publisher publisher : project.getPublishersList()) {
        	if(publisher instanceof ExtendedEmailPublisher) {
        		for(EmailTrigger trigger: ((ExtendedEmailPublisher) publisher).getConfiguredTriggers()) {
        			if(trigger instanceof StaticCodeWarningChangeTrigger) {
        				oldBuild = project.getBuild(((StaticCodeWarningChangeTrigger) trigger).getBuildId());
        			}
        		}
        	}
        }
        if(oldBuild != null) {
        	List<StaticCodeWarningChange> stats = new ArrayList<>();
        	StaticCodeWarningChange change = populateDiffCounts(build, oldBuild, PmdResultAction.class);
        	if(change != null) {
        		stats.add(change);
        	}
        	change = populateDiffCounts(build, oldBuild, CheckStyleResultAction.class);
        	if(change != null) {
        		stats.add(change);
        	}
        	if(!stats.isEmpty()) {
        		context.setVariable("oldBuildId", oldBuild.getId());
        		SimpleDateFormat yyyyMMdd_Format = new SimpleDateFormat("yyyy-MM-dd");
        		context.setVariable("oldBuildDate", yyyyMMdd_Format.format(oldBuild.getTimestamp().getTime()));
        		context.setVariable("diffStats", stats);
        	}
        }
        
        context.setVariable("build", build);
        context.setVariable("project", build.getParent());
        context.setVariable("logger", listener.getLogger());
        context.setVariable("rooturl", descriptor.getHudsonUrl());
        return context;
    }

	private <T extends AbstractResultAction>  StaticCodeWarningChange populateDiffCounts(AbstractBuild<?, ?> build, AbstractBuild<?, ?> oldBuild,
			Class<T> class1) {
		StaticCodeWarningChange change = null;
		hudson.plugins.analysis.core.BuildResult buildResultOld = oldBuild.getAction(class1).getResult();
		hudson.plugins.analysis.core.BuildResult buildResultNew = build.getAction(class1).getResult();
		
		if(buildResultNew != null && buildResultOld != null) {
			change = new StaticCodeWarningChange();
			change.setType(oldBuild.getAction(class1).getDisplayName());
			change.setTotalWarnings(buildResultNew.getNumberOfWarnings() - buildResultOld.getNumberOfWarnings());
			change.setHighWarnings(buildResultNew.getNumberOfHighPriorityWarnings() - buildResultOld.getNumberOfHighPriorityWarnings());
			change.setNormalWarnings(buildResultNew.getNumberOfNormalPriorityWarnings() - buildResultOld.getNumberOfNormalPriorityWarnings());
			change.setLowWarnings(buildResultNew.getNumberOfLowPriorityWarnings() - buildResultOld.getNumberOfLowPriorityWarnings());
			change.setErrors(diffErrors(buildResultOld.getAnnotations(), buildResultNew.getAnnotations()));
		}
		return change;
	}

	private  <T extends AbstractResultAction> List<FileAnnotation> diffErrors(Collection<FileAnnotation> collection, Collection<FileAnnotation> collection2) {
		List<FileAnnotation> error = new ArrayList<>();
		if(collection2 != null) {
			error.addAll(collection2);
		}
		if(collection != null) {
			error.removeAll(collection);
		}
		Iterator<FileAnnotation> iterator = error.iterator();
		while (iterator.hasNext()) {
			AbstractAnnotation fileAnnotation = (AbstractAnnotation) iterator.next();
			if(Priority.NORMAL.compareTo(fileAnnotation.getPriority()) < 0) {
				iterator.remove();
			}
		}
		return error;
	}
}
