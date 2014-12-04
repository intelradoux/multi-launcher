package org.jenkinsci.plugins.multilauncher;

import hudson.model.Action;
import hudson.model.AbstractProject;
import hudson.model.Descriptor.FormException;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.listeners.ItemListener;
import hudson.util.FormApply;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jenkinsci.plugins.multilauncher.data.Launcher;
import org.jenkinsci.plugins.multilauncher.data.LauncherParameterValue;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class MultiLauncherAction implements Action {
	private final AbstractProject<?, ?> target;

	public MultiLauncherAction(final AbstractProject<?, ?> target) {
		this.target = target;
	}

	@Override
	public String getIconFileName() {
		return "/plugin/multi-launcher/img/multilaunch.png";
	}

	@Override
	public String getDisplayName() {
		return "Parametrized Build";
	}

	@Override
	public String getUrlName() {
		return "multiLauncher";
	}

	public AbstractProject<?, ?> getTarget() {
		return target;
	}

	public List<ParameterDefinition> getParameterDefinitions() {
		ParametersDefinitionProperty property = target.getProperty(ParametersDefinitionProperty.class);
		if (property != null && property.getParameterDefinitions() != null) {
			return property.getParameterDefinitions();
		}
		return new ArrayList<ParameterDefinition>();
	}

	public List<Launcher> getLaunchers() {
		MultiLauncher property = target.getProperty(MultiLauncher.class);
		if (property != null && property.getLaunchers() != null) {
			return property.getLaunchers();
		}
		return new ArrayList<Launcher>();
	}

	public List<ParameterDefinition> getParameterDefinitions(Launcher l) throws IOException, InterruptedException {
		MultiLauncher property = target.getProperty(MultiLauncher.class);
		if (property != null && property.getLaunchers() != null) {
			return calcParameterDefinitions(l);
		}
		return getParameterDefinitions();
	}

	private List<ParameterDefinition> calcParameterDefinitions(Launcher l) throws IOException, InterruptedException {
		List<ParameterDefinition> definitions = getParameterDefinitions();
		if (l == null || l.getParameter() == null || l.getParameter().isEmpty()) {
			return definitions;
		}

		List<ParameterDefinition> modifiedOne = new ArrayList<ParameterDefinition>();
		boolean found = false;
		for (ParameterDefinition p : definitions) {
			found = false;
			for (LauncherParameterValue v : l.getParameter()) {
				if (v.getName().equals(p.getName())) {
					modifiedOne.add(p.copyWithDefaultValue(p.createValue(null, v.getValue())));
					found = true;
					break;
				}
			}
			if (!found) {
				modifiedOne.add(p);
			}
		}
		return modifiedOne;
	}

	public void doSave(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, FormException {
		JSONObject json = req.getSubmittedForm();
		MultiLauncher desc = newInstanceML(req, json);

		MultiLauncher property = target.getProperty(MultiLauncher.class);
		target.removeProperty(property);
		if (desc != null) {
			target.addProperty(desc);
		}
		target.save();
		ItemListener.fireOnUpdated(target);
	    // Redirect to the plugin index page
		FormApply.success("..").generateResponse(req, rsp, this);
	}

	public MultiLauncher newInstanceML(StaplerRequest req, JSONObject formData) throws FormException {
		List<Launcher> launchers = new ArrayList<Launcher>();
		if (formData != null && !formData.isNullObject()) {
			JSON launchersData = (JSON) formData.get("launchers");
			if (launchersData != null && !launchersData.isEmpty()) {
				if (launchersData.isArray()) {
					JSONArray launchersArrayData = (JSONArray) launchersData;
					launchers.addAll(req.bindJSONToList(Launcher.class, launchersArrayData));
				} else {
					JSONObject launchersObjectData = (JSONObject) launchersData;
					launchers.add(req.bindJSON(Launcher.class, launchersObjectData));
				}
			}
		}

		Set<String> ids = new HashSet<String>();

		for (int i = 0; i < launchers.size(); i++) {
			Launcher l = launchers.get(i);

			FormValidation fc = doCheckCron(l.getCron());
			if (fc.kind != Kind.OK) {
				throw new FormException(Messages.multilauncher_form_cron_error(l.getCron()), "cron." + i);
			}
			FormValidation fi = doCheckId(l.getId());
			if (fi.kind != Kind.OK) {
				throw new FormException(Messages.multilauncher_form_id_error(), "id." + i);
			}
			if (ids.contains(l.getId())) {
				throw new FormException(Messages.multilauncher_form_id_duplicate(l.getId()), "id." + i);
			}
			ids.add(l.getId());
		}

		MultiLauncher notificationProperty = new MultiLauncher(launchers);
		return notificationProperty;
	}

	public FormValidation doCheckCron(@QueryParameter(value = "cron", fixEmpty = true) String cron) {
		if (cron == null) {
			return FormValidation.warning(Messages.multilauncher_validate_cron_warn());

		}
		if (!org.quartz.CronExpression.isValidExpression(cron)) {
			return FormValidation.error(Messages.multilauncher_validate_cron_error());
		}
		return FormValidation.ok();
	}

	public FormValidation doCheckId(@QueryParameter(value = "id", fixEmpty = true) String id) {
		if (id == null || id.trim().equals("")) {
			return FormValidation.error(Messages.multilauncher_validate_id_error());
		}
		return FormValidation.ok();
	}

}
