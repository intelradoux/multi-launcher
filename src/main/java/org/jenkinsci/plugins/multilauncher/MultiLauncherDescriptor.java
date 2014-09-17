/*
 * The MIT License
 * 
 * Copyright (c) 2011, Jesse Farinacci
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.multilauncher;

import hudson.Extension;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Job;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jenkins.model.ParameterizedJobMixIn.ParameterizedJob;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jenkinsci.plugins.multilauncher.data.Launcher;
import org.jenkinsci.plugins.multilauncher.utils.LauncherTrigger;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public final class MultiLauncherDescriptor extends JobPropertyDescriptor {
	private List<Launcher> launchers = new ArrayList<Launcher>();

	public MultiLauncherDescriptor() {
		super(MultiLauncher.class);
		load();

		LauncherTrigger.isInitialized();
	}

	@DataBoundConstructor
	public MultiLauncherDescriptor(List<Launcher> launchers) {
		setLaunchers(launchers);
	}


	public boolean isEnabled() {
		return !launchers.isEmpty();
	}

	public List<Launcher> getTargets() {
		return launchers;
	}

	public void setLaunchers(List<Launcher> launchers) {
		this.launchers = new ArrayList<Launcher>(launchers);
	}

	@Override
	public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends Job> jobType) {
		return ParameterizedJob.class.isAssignableFrom(jobType);
	}

	@Override
	public String getDisplayName() {
		return "Multi Launcher";
	}

	   

	@Override
	public MultiLauncher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
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

	@Override
	public boolean configure(StaplerRequest req, JSONObject formData) {
		// req.bindJSON(this, formData);
		save();
		return true;
	}
}
