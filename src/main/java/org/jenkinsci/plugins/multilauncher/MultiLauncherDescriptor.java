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
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.AbstractProject;
import hudson.model.Job;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONObject;

import org.jenkinsci.plugins.multilauncher.data.Launcher;
import org.jenkinsci.plugins.multilauncher.utils.LauncherTrigger;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public final class MultiLauncherDescriptor extends JobPropertyDescriptor {
	private List<Launcher> launchers = new ArrayList<Launcher>();

	public MultiLauncherDescriptor() {
		super(MultiLauncher.class);

		LauncherTrigger.isInitialized();
	}

	@DataBoundConstructor
	public MultiLauncherDescriptor(List<Launcher> launchers) {
		setLaunchers(launchers);
	}

	public List<Launcher> getTargets() {
		return launchers;
	}

	public void setLaunchers(List<Launcher> launchers) {
		this.launchers = new ArrayList<Launcher>(launchers);
	}

	@Override
	public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends Job> jobType) {
		return AbstractProject.class.isAssignableFrom(jobType);
	}

	@Override
	public String getDisplayName() {
		return "Multi Launcher";
	}

	@Override
	public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData)
			throws hudson.model.Descriptor.FormException {
		List<Ancestor> ancestors = req.getAncestors();
		Ancestor item = ancestors.get(ancestors.size() - 1);
		AbstractProject<?, ?> j = (AbstractProject<?, ?>) item.getObject();
		MultiLauncher property = j.getProperty(MultiLauncher.class);

		return property;
	}
}
