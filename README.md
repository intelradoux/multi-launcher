multi-launcher
==============

Jenkins plugin to schedule parametrized build

You can schedule build with some parameter 

![Launchers view](https://github.com/intelradoux/multi-launcher/raw/master/img/launchers.png)

Note:
This plugin use a Quartz scheduler (http://quartz-scheduler.org/ ), not the jenkins internal scheduler. (need heavy refactoring if you want to use the internal scheduler....)

NOTE: If security is enabled, the user "cron" must have access to (all) the project.
