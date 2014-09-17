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


import hudson.model.JobProperty;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.multilauncher.data.Launcher;
import org.jenkinsci.plugins.multilauncher.data.LauncherParameterValue;
import org.kohsuke.stapler.DataBoundConstructor;

public class MultiLauncher extends JobProperty<Job<?, ?>> {
	private final List<Launcher> launchers;

	@DataBoundConstructor
	public MultiLauncher(List<Launcher> launchers) {
		this.launchers = new ArrayList<Launcher>(launchers);
	}

	public List<Launcher> getLaunchers() {
		return launchers;
	}

	public static List<ParameterDefinition> getDefaultParameterDefinitions(Job<?, ?> job) {
		ParametersDefinitionProperty property = (ParametersDefinitionProperty) job
				.getProperty(ParametersDefinitionProperty.class);
		if (property != null && property.getParameterDefinitions() != null) {
			return property.getParameterDefinitions();
		}
		return new ArrayList<ParameterDefinition>();
	}

	public List<ParameterDefinition> getParameterDefinitions(Launcher l) throws IOException, InterruptedException {
		List<ParameterDefinition> definitions = getDefaultParameterDefinitions(owner);
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

	@Override
	public MultiLauncherDescriptor getDescriptor() {
		return (MultiLauncherDescriptor) super.getDescriptor();
	}
}
