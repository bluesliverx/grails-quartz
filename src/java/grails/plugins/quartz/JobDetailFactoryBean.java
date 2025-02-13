/*
 * Copyright (c) 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grails.plugins.quartz;

import org.quartz.*;
import org.quartz.impl.matchers.KeyMatcher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * Simplified version of Spring's <a href='http://static.springframework.org/spring/docs/2.5.x/api/org/springframework/scheduling/quartz/MethodInvokingJobDetailFactoryBean.html'>MethodInvokingJobDetailFactoryBean</a>
 * that avoids issues with non-serializable classes (for JDBC storage).
 *
 * @author <a href='mailto:beckwithb@studentsonly.com'>Burt Beckwith</a>
 * @author Sergey Nebolsin (nebolsin@gmail.com)
 * @since 0.3.2
 */
public class JobDetailFactoryBean implements FactoryBean, InitializingBean, ApplicationContextAware {
	private ApplicationContext applicationContext;

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

    public static final transient String JOB_NAME_PARAMETER = "org.grails.plugins.quartz.grailsJobName";

    private String name;
    private String group;
    private boolean concurrent;
    private boolean volatility;
    private boolean durability;
    private boolean requestsRecovery;
    private String[] jobListenerNames;
    private JobDetail jobDetail;

    /**
     * Set the name of the job.
     * <p>Default is the bean name of this FactoryBean.
     *
     * @param name name of the job
     * @see org.quartz.JobDetail#getKey
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Set the group of the job.
     * <p>Default is the default group of the Scheduler.
     *
     * @param group group name of the job
     * @see org.quartz.JobDetail#getKey
     * @see org.quartz.Scheduler#DEFAULT_GROUP
     */
    public void setGroup(final String group) {
        this.group = group;
    }

    /**
     * Set a list of JobListener names for this job, referring to
     * non-global JobListeners registered with the Scheduler.
     * <p>A JobListener name always refers to the name returned
     * by the JobListener implementation.
     *
     * @param names array of job listener names which should be applied to the job
     * @see SchedulerFactoryBean#setJobListeners
     * @see org.quartz.JobListener#getName
     */
    public void setJobListenerNames(final String[] names) {
        this.jobListenerNames = names;
    }

    @Required
    public void setConcurrent(final boolean concurrent) {
        this.concurrent = concurrent;
    }

    @Required
    public void setVolatility(boolean volatility) {
        this.volatility = volatility;
    }

    @Required
    public void setDurability(boolean durability) {
        this.durability = durability;
    }

    @Required
    public void setRequestsRecovery(boolean requestsRecovery) {
        this.requestsRecovery = requestsRecovery;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() {

        if (name == null) {
            throw new IllegalStateException("name is required");
        }

        if (group == null) {
            throw new IllegalStateException("group is required");
        }

        // Consider the concurrent flag to choose between stateful and stateless job.
        Class jobClass = (concurrent ? GrailsJobFactory.GrailsJob.class : GrailsJobFactory.StatefulGrailsJob.class);

        // Build JobDetail instance.
        jobDetail = JobBuilder.newJob(jobClass)
            .withIdentity(name, group)
            .storeDurably(durability)
       		.requestRecovery(requestsRecovery)
			.usingJobData(JOB_NAME_PARAMETER, name)
			.build();

        // Register job listener names.
		Scheduler quartzScheduler = (Scheduler)applicationContext.getBean("quartScheduler");
		try {
		ListenerManager listenerManager = quartzScheduler.getListenerManager();
        if (jobListenerNames != null) {
            for (String jobListenerName : jobListenerNames) {
                listenerManager.addJobListenerMatcher(jobListenerName, KeyMatcher.keyEquals(jobDetail.getKey()));
            }
        }
		} catch(Exception ex) {
			//TODO Handle exception when retrieving listener
		}
    }

    /**
     * {@inheritDoc}
     *
     * @see org.springframework.beans.factory.FactoryBean#getObject()
     */
    public Object getObject() {
        return jobDetail;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.springframework.beans.factory.FactoryBean#getObjectType()
     */
    public Class getObjectType() {
        return JobDetail.class;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.springframework.beans.factory.FactoryBean#isSingleton()
     */
    public boolean isSingleton() {
        return true;
    }
}
