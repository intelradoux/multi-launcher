package org.jenkinsci.plugins.multilauncher;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;
import hudson.model.AbstractProject;

import java.util.Collection;
import java.util.Collections;

@Extension
public class MultiLauncherActionFactory extends TransientProjectActionFactory {

	@SuppressWarnings("rawtypes")
	@Override
	public Collection<? extends Action> createFor(AbstractProject target) {
		return Collections.singleton(new MultiLauncherAction(target));

	}


}
