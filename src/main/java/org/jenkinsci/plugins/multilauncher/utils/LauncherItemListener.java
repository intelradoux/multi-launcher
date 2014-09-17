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
package org.jenkinsci.plugins.multilauncher.utils;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.AbstractProject;
import hudson.model.Job;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.multilauncher.MultiLauncher;
import org.jenkinsci.plugins.multilauncher.data.Launcher;

@Extension
public class LauncherItemListener extends hudson.model.listeners.ItemListener {
	@Override
	public void onDeleted(Item item) {
		if (item instanceof Job) {
			LauncherTrigger.removeTrigger((Job<?, ?>) item);
		}
	}

	@Override
	public void onRenamed(Item item, String oldName, String newName) {
		if (item instanceof AbstractProject) {
			AbstractProject<?, ?> job = (AbstractProject<?, ?>) item;

			LauncherTrigger.removeTrigger(oldName);
			launchScheduler(job);
		}
	}

	@Override
	public void onLoaded() {
		for (Item item : Jenkins.getInstance().getAllItems()) {
			if (item instanceof AbstractProject) {
				launchScheduler((AbstractProject<?, ?>) item);
			}
		}
	}

	@Override
	public void onUpdated(Item item) {
		if (item instanceof AbstractProject) {
			AbstractProject<?, ?> job = (AbstractProject<?, ?>) item;

			LauncherTrigger.removeTrigger(job);
			launchScheduler(job);
		}
	}

	private void launchScheduler(AbstractProject<?, ?> job) {
		MultiLauncher property = (MultiLauncher) job.getProperty(MultiLauncher.class);
		if (property == null || property.getLaunchers() == null) {
			return;
		}
		if (job.isDisabled()) {
			return;
		}
		for (Launcher l : property.getLaunchers()) {
			LauncherTrigger.triggerJob(l, job);
		}
	}
}
