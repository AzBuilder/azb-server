package org.terrakube.api.plugin.scheduler.job.tcl.executor.ephemeral;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.terrakube.api.plugin.scheduler.job.tcl.executor.ExecutorContext;
import org.terrakube.api.rs.job.Job;

import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class EphemeralExecutorService {

    private static final String NODE_SELECTOR = "EPHEMERAL_CONFIG_NODE_SELECTOR_TAGS";
    private static final String SERVICE_ACCOUNT = "EPHEMERAL_CONFIG_SERVICE_ACCOUNT";
    private static final String ANNOTATIONS = "EPHEMERAL_CONFIG_ANNOTATIONS";
    private static final String CONFIG_MAP_NAME = "EPHEMERAL_CONFIG_MAP_NAME";
    private static final String CONFIG_MAP_PATH = "EPHEMERAL_CONFIG_MAP_MOUNT_PATH";


    KubernetesClient kubernetesClient;
    EphemeralConfiguration ephemeralConfiguration;

    public boolean sendToEphemeralExecutor(Job job, ExecutorContext executorContext) {
        final String jobName = "job-" + job.getId();
        deleteEphemeralJob(job);
        log.info("Ephemeral Executor Image {}, Job: {}, Namespace: {}, NodeSelector: {}", ephemeralConfiguration.getImage(), jobName, ephemeralConfiguration.getNamespace(), ephemeralConfiguration.getNodeSelector());
        SecretEnvSource secretEnvSource = new SecretEnvSource();
        secretEnvSource.setName(ephemeralConfiguration.getSecret());
        EnvFromSource envFromSource = new EnvFromSource();
        envFromSource.setSecretRef(secretEnvSource);
        final List<EnvFromSource> executorEnvVarFromSecret = Arrays.asList(envFromSource);

        EnvVar executorFlagBatch = new EnvVar();
        executorFlagBatch.setName("EphemeralFlagBatch");
        executorFlagBatch.setValue("true");

        EnvVar executorFlagBatchJsonContent = new EnvVar();
        try {
            executorFlagBatchJsonContent.setName("EphemeralJobData");
            ObjectMapper mapper = new ObjectMapper();
            String jobJson = mapper.writeValueAsString(executorContext);
            executorFlagBatchJsonContent.setValue(Base64.getEncoder().encodeToString(jobJson.getBytes("UTF-8")));
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        final List<EnvVar> executorEnvVarFlags = Arrays.asList(executorFlagBatch, executorFlagBatchJsonContent);

        Optional<String> nodeSelector = Optional.ofNullable(executorContext.getEnvironmentVariables().getOrDefault(NODE_SELECTOR, null));
        Map<String, String> nodeSelectorInfo = new HashMap();
        log.info("Custom Node selector: {} {}", nodeSelector.isPresent(), nodeSelector.isEmpty());
        if(nodeSelector.isPresent()) {
            log.info("Using custom node selector information");
            for (String nodeSelectorData : nodeSelector.get().split(";")) {
                String[] info = nodeSelectorData.split("=");
                nodeSelectorInfo.put(info[0], info[1]);
            }
        } else {
            log.info("Using default node selector information");
            nodeSelectorInfo = ephemeralConfiguration.getNodeSelector();
        }

        Optional<String> annotationsInfo = Optional.ofNullable(executorContext.getEnvironmentVariables().getOrDefault(ANNOTATIONS, null));
        Map<String, String> annotations = new HashMap();
        log.info("Custom Annotations: {}", annotationsInfo.isPresent());
        if(annotationsInfo.isPresent()) {
            for (String annotationData : annotationsInfo.get().split(";")) {
                String[] info = annotationData.split("=");
                annotations.put(info[0], info[1]);
            }
        }

        Optional<String> serviceAccountInfo = Optional.ofNullable(
                executorContext.getEnvironmentVariables().getOrDefault(SERVICE_ACCOUNT, null));
        String serviceAccount = serviceAccountInfo.orElse(null);

        // Volume and VolumeMount for ConfigMap if specified
        List<Volume> volumes = new ArrayList<>();
        List<VolumeMount> volumeMounts = new ArrayList<>();

        Optional<String> configMapNameOpt = Optional.ofNullable(executorContext.getEnvironmentVariables().get(CONFIG_MAP_NAME));
        Optional<String> configMapMountPathOpt = Optional.ofNullable(executorContext.getEnvironmentVariables().get(CONFIG_MAP_PATH));
        if (configMapNameOpt.isPresent()) {
            String configMapName = configMapNameOpt.get();
            String mountPath = configMapMountPathOpt.orElse("/tmp");  // Default mount path if not specified

            // Define ConfigMap volume
            Volume configMapVolume = new Volume();
            configMapVolume.setName("config-volume");
            ConfigMapVolumeSource configMapVolumeSource = new ConfigMapVolumeSource();
            configMapVolumeSource.setName(configMapName);
            configMapVolume.setConfigMap(configMapVolumeSource);
            volumes.add(configMapVolume);

            // Define VolumeMount for the container
            VolumeMount configMapMount = new VolumeMount();
            configMapMount.setName("config-volume");
            configMapMount.setMountPath(mountPath);
            volumeMounts.add(configMapMount);
        }

        io.fabric8.kubernetes.api.model.batch.v1.Job k8sJob = new JobBuilder()
                .withApiVersion("batch/v1")
                .withNewMetadata()
                .withName(jobName)
                .withNamespace(ephemeralConfiguration.getNamespace())
                .withLabels(Collections.singletonMap("jobId", executorContext.getJobId()))
                .withLabels(Collections.singletonMap("organizationId", executorContext.getOrganizationId()))
                .withLabels(Collections.singletonMap("workspaceId", executorContext.getWorkspaceId()))
                .withAnnotations(annotations)
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()
                .withNodeSelector(nodeSelectorInfo)
                .withServiceAccountName(serviceAccount)
                .withVolumes(volumes)
                .addNewContainer()
                .withName(jobName)
                .withEnvFrom(executorEnvVarFromSecret)
                .withImage(ephemeralConfiguration.getImage())
                .withEnv(executorEnvVarFlags)
                .withVolumeMounts(volumeMounts)
                .endContainer()
                .withRestartPolicy("Never")
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();

        log.info("Running ephemeral job");
        kubernetesClient.batch().v1().jobs().inNamespace(ephemeralConfiguration.getNamespace()).createOrReplace(k8sJob);
        return true;
    }

    public void deleteEphemeralJob(Job job){
        try {
            io.fabric8.kubernetes.api.model.batch.v1.Job jobK8s = kubernetesClient
                    .batch()
                    .jobs()
                    .inNamespace(ephemeralConfiguration.getNamespace())
                    .withName("job-" + job.getId())
                    .get();

            if (jobK8s != null) {
                kubernetesClient
                        .batch()
                        .jobs()
                        .inNamespace(ephemeralConfiguration.getNamespace())
                        .withName("job-" + job.getId())
                        .delete();
            }

        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }
}
