multi-launcher
==============

Jenkins plugin to schedule parametrized build

You can schedule build with some parameter 

![Launchers view](https://github.com/intelradoux/multi-launcher/raw/master/img/launchers.png)

Note:
This plugin use a Quartz scheduler (http://quartz-scheduler.org/ ), not the jenkins internal scheduler. (need heavy refactoring if you want to use the internal scheduler....)

You need to create parameter, save the job and reopen the configuration to be able to schedule job with parameter.
