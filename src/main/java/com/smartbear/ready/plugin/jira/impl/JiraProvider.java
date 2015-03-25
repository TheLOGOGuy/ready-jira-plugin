package com.smartbear.ready.plugin.jira.impl;

import com.atlassian.jira.rest.client.api.GetCreateIssueMetadataOptions;
import com.atlassian.jira.rest.client.api.GetCreateIssueMetadataOptionsBuilder;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.MetadataRestClient;
import com.atlassian.jira.rest.client.api.OptionalIterable;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.BasicProject;
import com.atlassian.jira.rest.client.api.domain.CimFieldInfo;
import com.atlassian.jira.rest.client.api.domain.CimIssueType;
import com.atlassian.jira.rest.client.api.domain.CimProject;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueType;
import com.atlassian.jira.rest.client.api.domain.Priority;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;
import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.settings.Settings;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.smartbear.ready.plugin.jira.settings.BugTrackerPrefs;
import com.smartbear.ready.plugin.jira.settings.BugTrackerSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class JiraProvider implements SimpleBugTrackerProvider {
    private static final Logger logger = LoggerFactory.getLogger(JiraProvider.class);

    private final static String BUG_TRACKER_ISSUE_KEY_NOT_SPECIFIED = "Issue key not specified";
    private final static String BUG_TRACKER_FILE_NAME_NOT_SPECIFIED = "File name not specified";

    private ModelItem activeElement;
    private JiraRestClient restClient = null;
    private BugTrackerSettings bugTrackerSettings;
    static private JiraProvider instance = null;

    public static JiraProvider getProvider (){
        if (instance == null){
            instance = new JiraProvider();
        }
        return instance;
    }

    public static void freeProvider(){
        instance = null;
    }

    private JiraProvider() {
        bugTrackerSettings = getBugTrackerSettings();
        if (!settingsComplete(bugTrackerSettings)) {
            logger.error("Bug tracker settings are not completely specified.");
            UISupport.showErrorMessage("Bug tracker settings are not completely specified.");
            return;
        }
        final AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
        try {
            restClient = factory.createWithBasicHttpAuthentication(new URI(bugTrackerSettings.getUrl()), bugTrackerSettings.getLogin(), bugTrackerSettings.getPassword());
        } catch (URISyntaxException e) {
            logger.error("Incorrectly specified bug tracker URI.");
            UISupport.showErrorMessage("Incorrectly specified bug tracker URI.");
        }
    }

    public String getName() {
        return "Jira Bug Tracker provider";
    }

    private JiraApiCallResult<Iterable<BasicProject>> getAllProjects() {
        try {
            return new JiraApiCallResult<Iterable<BasicProject>>(restClient.getProjectClient().getAllProjects().get());
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
            return new JiraApiCallResult<Iterable<BasicProject>>(e);
        } catch (ExecutionException e) {
            logger.error(e.getMessage());
            return new JiraApiCallResult<Iterable<BasicProject>>(e);
        }
    }

    public List<String> getAvailableProjects() {
        JiraApiCallResult<Iterable<BasicProject>> projects = getAllProjects();
        if (!projects.isSuccess()) {
            return new ArrayList<String>();
        }

        List<String> projectNames = new ArrayList<String>();
        for (BasicProject project : projects.getResult()) {
            projectNames.add(project.getKey());
        }

        return projectNames;
    }

    private JiraApiCallResult<Project> getProjectByKey(String key) {
        try {
            return new JiraApiCallResult<Project>(restClient.getProjectClient().getProject(key).get());
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
            return new JiraApiCallResult<Project>(e);
        } catch (ExecutionException e) {
            logger.error(e.getMessage());
            return new JiraApiCallResult<Project>(e);
        }
    }

    private JiraApiCallResult<OptionalIterable<IssueType>> getIssueTypes(String projectKey) {
        JiraApiCallResult<Project> project = getProjectByKey(projectKey);
        if (!project.isSuccess()) {
            return new JiraApiCallResult<OptionalIterable<IssueType>>(project.getError());
        }
        return new JiraApiCallResult<OptionalIterable<IssueType>>(project.getResult().getIssueTypes());
    }

    public List<String> getAvailableIssueTypes(String projectKey) {
        JiraApiCallResult<OptionalIterable<IssueType>> result = getIssueTypes(projectKey);
        if (!result.isSuccess()) {
            return new ArrayList<String>();
        }

        List<String> issueTypeList = new ArrayList<String>();
        OptionalIterable<IssueType> issueTypes = result.getResult();
        for (IssueType issueType : issueTypes) {
            issueTypeList.add(issueType.getName());
        }

        return issueTypeList;
    }

    private JiraApiCallResult<Iterable<Priority>> getAllPriorities() {
        final MetadataRestClient client = restClient.getMetadataClient();
        try {
            return new JiraApiCallResult<Iterable<Priority>>(client.getPriorities().get());
        } catch (InterruptedException e) {
            return new JiraApiCallResult<Iterable<Priority>>(e);
        } catch (ExecutionException e) {
            return new JiraApiCallResult<Iterable<Priority>>(e);
        }
    }

    private Priority getPriorityByName(String priorityName) {
        JiraApiCallResult<Iterable<Priority>> priorities = getAllPriorities();
        if (!priorities.isSuccess()) {
            return null;
        }
        for (Priority priority : priorities.getResult()) {
            if (priority.getName().equals(priorityName)) {
                return priority;
            }
        }
        return null;
    }

    public List<String> getPriorities() {
        JiraApiCallResult<Iterable<Priority>> priorities = getAllPriorities();
        if (!priorities.isSuccess()) {
            return new ArrayList<String>();
        }

        List<String> prioritiesAll = new ArrayList<String>();
        for (Priority currentPriority : priorities.getResult()) {
            prioritiesAll.add(currentPriority.getName());
        }

        return prioritiesAll;
    }

    private JiraApiCallResult<IssueType> getIssueTypeByKey(String projectKey, String issueKey) {
        JiraApiCallResult<OptionalIterable<IssueType>> issueTypes = getIssueTypes(projectKey);
        if (!issueTypes.isSuccess()) {
            return new JiraApiCallResult<IssueType>(issueTypes.getError());
        }
        for (IssueType issueType : issueTypes.getResult()) {
            if (issueType.getName().equals(issueKey)) {
                return new JiraApiCallResult<IssueType>(issueType);
            }
        }
        return null;
    }

    public Issue getIssue(String key) {
        try {
            return restClient.getIssueClient().getIssue(key).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return null;
    }

    public JiraApiCallResult<Map<String, CimFieldInfo>> getRequiredFields(String projectKey, String issueType) {
        GetCreateIssueMetadataOptions options = new GetCreateIssueMetadataOptionsBuilder()
                .withExpandedIssueTypesFields()
                .withProjectKeys(projectKey)
                .build();
        try {
            Iterable<CimProject> cimProjects = restClient.getIssueClient().getCreateIssueMetadata(options).get();
            for (CimProject project : cimProjects) {
                Iterable<CimIssueType> issueTypes = project.getIssueTypes();
                for (CimIssueType currentIssueType : issueTypes) {
                    if (currentIssueType.getName().equals(issueType)) {
                        Map<String, CimFieldInfo> fields = currentIssueType.getFields();
                        Map<String, CimFieldInfo> requiredFields = new HashMap<>();
                        for (Map.Entry<String, CimFieldInfo> entry : fields.entrySet()) {
                            if (entry.getValue().isRequired()) {
                                requiredFields.put(entry.getKey(), entry.getValue());
                            }
                        }
                        return new JiraApiCallResult<Map<String, CimFieldInfo>>(requiredFields);
                    }
                }
            }
        } catch (InterruptedException e) {
            return new JiraApiCallResult<Map<String, CimFieldInfo>>(e);
        } catch (ExecutionException e) {
            return new JiraApiCallResult<Map<String, CimFieldInfo>>(e);
        }

        return new JiraApiCallResult<Map<String, CimFieldInfo>>(new HashMap<String, CimFieldInfo>());
    }

    @Override
    public BugTrackerIssueCreationResult createIssue(String projectKey, String issueKey, String priority, String summary, String description, Map<String, String> extraRequiredValues) {
        //https://bitbucket.org/atlassian/jira-rest-java-client/src/75a64c9d81aad7d8bd9beb11e098148407b13cae/test/src/test/java/samples/Example1.java?at=master
        //http://www.restapitutorial.com/httpstatuscodes.html
        if (restClient == null) {
            return new BugTrackerIssueCreationResult("Incorrectly specified bug tracker URI.");//TODO: correct message
        }

        BasicIssue basicIssue = null;
        try {
            JiraApiCallResult<IssueType> issueType = getIssueTypeByKey(projectKey, issueKey);
            if (!issueType.isSuccess()) {
                return new BugTrackerIssueCreationResult(issueType.getError().getMessage());
            }

            IssueInputBuilder issueInputBuilder = new IssueInputBuilder(projectKey, issueType.getResult().getId());
            issueInputBuilder.setIssueType(issueType.getResult());
            issueInputBuilder.setProjectKey(projectKey);
            issueInputBuilder.setSummary(summary);
            issueInputBuilder.setDescription(description);
            issueInputBuilder.setPriority(getPriorityByName(priority));
            for (final Map.Entry<String, String> extraRequiredValue : extraRequiredValues.entrySet()) {
                if (extraRequiredValue.getKey().equals("components")) {
                    issueInputBuilder.setComponentsNames(new Iterable<String>() {
                        @Override
                        public Iterator<String> iterator() {
                            return new Iterator<String>() {
                                boolean hasValue = true;

                                @Override
                                public boolean hasNext() {
                                    return hasValue;
                                }

                                @Override
                                public String next() {
                                    hasValue = false;
                                    return extraRequiredValue.getValue();
                                }

                                @Override
                                public void remove() {

                                }
                            };
                        }
                    });
                } else {
                    issueInputBuilder.setFieldValue(extraRequiredValue.getKey(), extraRequiredValue.getValue());
                }
            }
            Promise<BasicIssue> issue = restClient.getIssueClient().createIssue(issueInputBuilder.build());
            basicIssue = issue.get();
        } catch (InterruptedException e) {
            return new BugTrackerIssueCreationResult(e.getMessage());
        } catch (ExecutionException e) {
            return new BugTrackerIssueCreationResult(e.getMessage());
        }

        return new BugTrackerIssueCreationResult(basicIssue);
    }

    protected void finalize() throws Throwable {
        try {
            if (restClient != null) {
                restClient.close();
            }
        } catch (IOException e) {
        }
    }

    @Override
    public BugTrackerAttachmentCreationResult attachFile(URI attachmentUri, String fileName, InputStream inputStream) {
        if (attachmentUri == null) {
            return new BugTrackerAttachmentCreationResult(BUG_TRACKER_ISSUE_KEY_NOT_SPECIFIED);
        }
        if (StringUtils.isNullOrEmpty(fileName)) {
            return new BugTrackerAttachmentCreationResult(BUG_TRACKER_FILE_NAME_NOT_SPECIFIED);
        }

        try {
            restClient.getIssueClient().addAttachment(attachmentUri, inputStream, fileName).get();
        } catch (InterruptedException e) {
            return new BugTrackerAttachmentCreationResult(e.getMessage());
        } catch (ExecutionException e) {
            return new BugTrackerAttachmentCreationResult(e.getMessage());
        }

        return new BugTrackerAttachmentCreationResult();//everything is ok
    }

    public InputStream getSoapUIExecutionLog() {
        return getExecutionLog("com.eviware.soapui");
    }

    public InputStream getServiceVExecutionLog() {
        return getExecutionLog("com.smartbear.servicev");
    }

    public InputStream getLoadUIExecutionLog() {
        return getExecutionLog("com.eviware.loadui");
    }

    public InputStream getReadyApiLog() {
        return getExecutionLog("com.smartbear.ready");
    }

    public void setActiveItem(ModelItem element) {
        activeElement = element;
    }

    public String getActiveItemName() {
        return activeElement.getName();
    }

    public InputStream getRootProject() {
        WsdlProject project = findActiveElementRootProject(activeElement);
        return new ByteArrayInputStream(project.getConfig().toString().getBytes(StandardCharsets.UTF_8));
    }

    public boolean settingsComplete(BugTrackerSettings settings) {
        return !(settings == null ||
                StringUtils.isNullOrEmpty(settings.getUrl()) ||
                StringUtils.isNullOrEmpty(settings.getLogin()) ||
                StringUtils.isNullOrEmpty(settings.getPassword()));
    }

    public boolean settingsComplete() {
        BugTrackerSettings settings = getBugTrackerSettings();
        return settingsComplete(settings);
    }

    public BugTrackerSettings getBugTrackerSettings() {
        if (bugTrackerSettings == null) {
            Settings soapuiSettings = SoapUI.getSettings();
            bugTrackerSettings = new BugTrackerSettings(soapuiSettings.getString(BugTrackerPrefs.DEFAULT_URL, ""),
                    soapuiSettings.getString(BugTrackerPrefs.LOGIN, ""),
                    soapuiSettings.getString(BugTrackerPrefs.PASSWORD, ""));
        }
        return bugTrackerSettings;
    }

    private WsdlProject findActiveElementRootProject(ModelItem activeElement) {
        return ModelSupport.getModelItemProject(activeElement);
    }

    private InputStream getExecutionLog(String loggerName) {
        org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(loggerName);
        try {
            return (InputStream) new FileInputStream(log.getName());
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
        }
        return null;
    }
}