package org.terrakube.api.plugin.vcs;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.terrakube.api.repository.JobRepository;
import org.terrakube.api.repository.WebhookRepository;
import org.terrakube.api.repository.WorkspaceRepository;
import org.terrakube.api.rs.job.Job;
import org.terrakube.api.rs.template.Template;
import org.terrakube.api.rs.vcs.Vcs;
import org.terrakube.api.rs.webhook.Webhook;
import org.terrakube.api.rs.webhook.WebhookType;
import org.terrakube.api.rs.workspace.Workspace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.terrakube.api.plugin.scheduler.ScheduleJobService;
import org.terrakube.api.plugin.vcs.provider.github.GitHubWebhookService;
import org.terrakube.api.plugin.vcs.provider.gitlab.GitLabWebhookService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;

@AllArgsConstructor
@Slf4j
@Service
public class WebhookService {

    WebhookRepository webhookRepository;
    WorkspaceRepository workspaceRepository;
    GitHubWebhookService gitHubWebhookService;
    GitLabWebhookService gitLabWebhookService;
    JobRepository jobRepository;
    ScheduleJobService scheduleJobService;


    @Transactional
    public String processWebhook(String webhookId, String jsonPayload,Map<String, String> headers) {
        String result = "";
        Webhook webhook = webhookRepository.getReferenceById(UUID.fromString(webhookId));
        if(webhook == null){
            log.error("Webhook not found {}", webhookId);
            return result;
        }
        webhook.setType(WebhookType.WORKSPACE);
        switch (webhook.getType()) {
            case WORKSPACE:
                // The webhook instance has a reference to the workspace
                Workspace workspace = workspaceRepository.getReferenceById(UUID.fromString(webhook.getReferenceId()));
                Vcs vcs = workspace.getVcs();

                // if the VCS is empty we cannot process the webhook
                if(vcs == null) {
                    log.error("VCS not found for workspace {}", workspace.getId());
                    return result;
                }
                else{
                    WebhookResult webhookResult = new WebhookResult();
                    String base64WorkspaceId = Base64.getEncoder().encodeToString(workspace.getId().toString().getBytes(StandardCharsets.UTF_8));
                    switch (vcs.getVcsType()) {
                        case GITHUB:
                             webhookResult = gitHubWebhookService.processWebhook(jsonPayload, headers,base64WorkspaceId);
                            break;
                        case GITLAB:
                             webhookResult = gitLabWebhookService.processWebhook(jsonPayload, headers,base64WorkspaceId);
                            break;
                        default:
                            break;
                    }

                    log.info("webhook result {}", webhookResult);

                    // if the webhook is a valid request we can create a new job
                    if(webhookResult.isValid()){
                        // only execute the job if the branch is the same
                        if(webhookResult.getBranch().equals(workspace.getBranch()))
                        {
                           ObjectMapper objectMapper = new ObjectMapper();
                            try {
                                // The template id is stored in the webhook template mapping, basicall its a map with event and templateId
                                JsonNode rootNode = objectMapper.readTree(webhook.getTemplateMapping());
                                log.info("webhook event {}", webhookResult.getEvent());
                                log.info(rootNode.toString());
                                String templateId = rootNode.path(webhookResult.getEvent()).asText();
                                log.info("Template ID is: " + templateId);
                                if(templateId.isEmpty()){
                                    log.error("Template not found for event {}", webhookResult.getEvent());
                                    return result;
                                }
                                Job job = new Job();
                                job.setOrganization(workspace.getOrganization());
                                job.setWorkspace(workspace);
                                job.setTemplateReference(templateId);
                                job.setCreatedBy(webhookResult.getCreatedBy());
                                job.setUpdatedBy(webhookResult.getCreatedBy());
                                Date triggerDate = new Date(System.currentTimeMillis());
                                job.setCreatedDate(triggerDate);
                                job.setUpdatedDate(triggerDate);
                                job.setVia(webhookResult.getVia());
                                Job savedJob = jobRepository.save(job);
                                scheduleJobService.createJobContext(savedJob);
                            } catch (Exception e) {
                                log.error("Error creating the job", e);
                            }

                        }
                    }
                }
                break;
            case MODULE:
            // In future we can use this to validate the module for a new version or update the code asynchronously
                break;
            default:
                break;
        }
        return result;
    }


 @Transactional
 public void createWorkspaceWebhook(Workspace workspace) {
    String webhookRemoteId = "";

  if(workspace.getVcs() != null){
   // Get template id
    List<Template> templates = workspace.getOrganization().getTemplate();
    String templateId = "";
    for (Template template : templates) {
        // search for the template "Plan and apply"
        if ("Plan and apply".equals(template.getName())) {
            templateId = template.getId().toString();
            break;
        }
    }

    if (templateId.isEmpty()) {
        log.warn("Template 'Plan and apply' not found , getting first template");
        templateId = templates.get(0).getId().toString();
    } 

    // Template id is a json with the mapping of the event and the template id. 
    // In a future release we can trigger a different template for each event
    String templateMapping ="{\"push\":\"" + templateId +"\"}";
    log.info("templateMapping {}", templateMapping);
    // save the webhook
    Webhook webhook = new Webhook();
    webhook.setType(WebhookType.WORKSPACE);
    webhook.setReferenceId(workspace.getId().toString());
    webhook.setTemplateMapping(templateMapping);
    Webhook savedWebhook = webhookRepository.save(webhook);

    //
    Vcs vcs = workspace.getVcs();
    switch (vcs.getVcsType()) {
        case GITHUB:
             webhookRemoteId = gitHubWebhookService.createWebhook(workspace,savedWebhook.getId().toString());
            break;
        case GITLAB:
             webhookRemoteId = gitLabWebhookService.createWebhook(workspace,savedWebhook.getId().toString());
        default:
            break;
    }

    if(webhookRemoteId.isEmpty()){
        log.error("Error creating the webhook");
        return;
    }

    savedWebhook.setRemoteHookId(webhookRemoteId);
    // Save the updated webhook
    webhookRepository.save(savedWebhook);
  }
 }

}
