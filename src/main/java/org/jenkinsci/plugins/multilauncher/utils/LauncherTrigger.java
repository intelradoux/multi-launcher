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

import hudson.model.ParameterValue;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;

import java.util.ArrayList;
import java.util.List;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.multilauncher.MultiLauncher;
import org.jenkinsci.plugins.multilauncher.data.Launcher;
import org.jenkinsci.plugins.multilauncher.data.LauncherParameterValue;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LauncherTrigger implements Job {
	private static final Logger LOG = LoggerFactory.getLogger(LauncherTrigger.class);

	private static Scheduler SCHEDULER = init();

	private static Scheduler init() {
		try {
			SchedulerFactory sf = new StdSchedulerFactory();
			Scheduler sched = sf.getScheduler();
			sched.start();
			return sched;
		} catch (Exception e) {
			LOG.error("Cannot start scheduler for parameter build scheduler", e);
			return null;
		}
	}

	public static boolean isInitialized() {
		return SCHEDULER != null;
	}

	public static void removeTrigger(hudson.model.Job<?, ?> project) {
		removeTrigger(project.getName());
	}

	public static void removeTrigger(String projectName) {
		try {
			for (JobKey jk : SCHEDULER.getJobKeys(GroupMatcher.<JobKey> groupEquals(projectName))) {
				SCHEDULER.deleteJob(jk);
			}
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}

	public static void triggerJob(Launcher launcher, hudson.model.Job<?, ?> project) {
		JobDetail job = JobBuilder.newJob(LauncherTrigger.class)
.usingJobData("launcher", launcher.getId())
				.usingJobData("project", project.getName()).withIdentity(launcher.getId(), project.getName()).build();

		CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(launcher.getId(), project.getName())
				.withSchedule(CronScheduleBuilder.cronSchedule(launcher.getCron())).build();

		try {
			SCHEDULER.scheduleJob(job, trigger);
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap data = context.getMergedJobDataMap();
		String projectName = data.getString("project");
		String launchName=data.getString("launcher");

		// Get the project name...
		AbstractProject<?, ?> project = findProject(projectName);

		// If no project name then project has been removed... Removing scheduled task.
		if (project == null) {
			removeTrigger(projectName);
		}

		// Get parameter build from project.
		MultiLauncher builds = project.getProperty(MultiLauncher.class);

		// No build : configuration has been modified and parameter build has been removed.
		// Removing task manually
		if (builds == null) {
			removeTrigger(project);
			return;
		}

		// Get the launcher for this task
		Launcher launcher = getLauncher(builds, project, launchName);

		// I guess there a way to have launcher rewriten at the same time this task is scheduled.
		if (launcher == null) {
			return;
		}

		;
		// From parameter of the build, create value.
		List<ParameterValue> values = buildParameterValue(project, launcher);

		Jenkins.getInstance().getQueue()
				.schedule(project, 0, new ParametersAction(values), new CauseAction(new ParameterTimerTriggerCause()));

	}

	private List<ParameterValue> buildParameterValue(AbstractProject<?, ?> project, Launcher launcher) {
		List<ParameterValue> values = new ArrayList<ParameterValue>();

		if (launcher.getParameter() == null) {
			return values;
		}

		ParametersDefinitionProperty defaultProp = project.getProperty(ParametersDefinitionProperty.class);
		List<ParameterDefinition> paramDef = defaultProp.getParameterDefinitions();
		if (paramDef == null) {
			return values;
		}

		for (ParameterDefinition parameterDefinition : paramDef) {
			for (LauncherParameterValue v : launcher.getParameter()) {
				if (v.getName().equals(parameterDefinition.getName())) {

					try {
						values.add(parameterDefinition.createValue(null, v.getValue()));
					} catch (Exception e) {
						LOG.warn("Parameter invalid... Going to use the default one");
					}
				}
			}
		}
		return values;
	}

	private AbstractProject<?, ?> findProject(String projectName) {
		for (AbstractProject<?, ?> a : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
			if (projectName.equals(a.getName())) {
				return a;
			}
		}
		return null;
	}

	private Launcher getLauncher(MultiLauncher builds, AbstractProject<?, ?> project, String launcher) {
		for (Launcher l : builds.getLaunchers()) {
			if (l.getId().equals(launcher)) {
				return l;
			}
		}
		return null;
	}
	
	
	
	public static class ParameterTimerTriggerCause extends Cause {
        @Override
        public String getShortDescription() {
			return "Parameter build scheduler";
        }

        @Override
        public boolean equals(Object o) {
			return o instanceof ParameterTimerTriggerCause;
        }

        @Override
        public int hashCode() {
			return 4;
        }
    }
}

