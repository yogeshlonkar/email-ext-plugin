package hudson.plugins.emailext.plugins.content;

import java.util.List;

import hudson.plugins.analysis.util.model.FileAnnotation;

/**
 * store difference and erros 
 * @author yogesh lonkar <lonkar.yogeshr@gmail.com>
 *
 */
public class StaticCodeWarningChange {

	/** PMD warnings or check style warnings */
	private String type;

	/** difference total warnings*/
	private int totalWarnings;
	
	/** difference high warnings*/
	private int highWarnings;
	
	/** difference normal warnings*/
	private int normalWarnings;
	
	/** difference low warnings*/
	private int lowWarnings;
	
	private List<FileAnnotation> errors;

	public int getTotalWarnings() {
		return totalWarnings;
	}

	public void setTotalWarnings(int totalWarnings) {
		this.totalWarnings = totalWarnings;
	}

	public int getHighWarnings() {
		return highWarnings;
	}

	public void setHighWarnings(int highWarnings) {
		this.highWarnings = highWarnings;
	}

	public int getNormalWarnings() {
		return normalWarnings;
	}

	public void setNormalWarnings(int normalWarnings) {
		this.normalWarnings = normalWarnings;
	}

	public int getLowWarnings() {
		return lowWarnings;
	}

	public void setLowWarnings(int lowWarnings) {
		this.lowWarnings = lowWarnings;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<FileAnnotation> getErrors() {
		return errors;
	}

	public void setErrors(List<FileAnnotation> errors) {
		this.errors = errors;
	}

}
