package org.terrakube.executor.plugin.tfstate.configuration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.*;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.terrakube.client.TerrakubeClient;
import org.terrakube.executor.plugin.tfstate.TerraformState;
import org.terrakube.executor.plugin.tfstate.TerraformStatePathService;
import org.terrakube.executor.plugin.tfstate.aws.AwsTerraformStateImpl;
import org.terrakube.executor.plugin.tfstate.aws.AwsTerraformStateProperties;
import org.terrakube.executor.plugin.tfstate.azure.AzureTerraformStateImpl;
import org.terrakube.executor.plugin.tfstate.azure.AzureTerraformStateProperties;
import org.terrakube.executor.plugin.tfstate.gcp.GcpTerraformStateImpl;
import org.terrakube.executor.plugin.tfstate.gcp.GcpTerraformStateProperties;
import org.terrakube.executor.plugin.tfstate.local.LocalTerraformStateImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
@AllArgsConstructor
@Configuration
@EnableConfigurationProperties({
        TerraformStateProperties.class,
        AzureTerraformStateProperties.class,
        AwsTerraformStateProperties.class,
        GcpTerraformStateProperties.class
})
@ConditionalOnMissingBean(TerraformState.class)
public class TerraformStateAutoConfiguration {

    @Bean
    public TerraformState terraformState(TerrakubeClient terrakubeClient, TerraformStateProperties terraformStateProperties, AzureTerraformStateProperties azureTerraformStateProperties, AwsTerraformStateProperties awsTerraformStateProperties, GcpTerraformStateProperties gcpTerraformStateProperties, TerraformStatePathService terraformStatePathService) {
        TerraformState terraformState = null;

        if (terraformStateProperties != null)
            switch (terraformStateProperties.getType()) {
                case AzureTerraformStateImpl:
                    BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                            .connectionString(
                                    String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;EndpointSuffix=core.windows.net",
                                            azureTerraformStateProperties.getStorageAccountName(),
                                            azureTerraformStateProperties.getStorageAccessKey())
                            ).buildClient();

                    terraformState = AzureTerraformStateImpl.builder()
                            .resourceGroupName(azureTerraformStateProperties.getResourceGroupName())
                            .storageAccountName(azureTerraformStateProperties.getStorageAccountName())
                            .storageContainerName(azureTerraformStateProperties.getStorageContainerName())
                            .storageAccessKey(azureTerraformStateProperties.getStorageAccessKey())
                            .blobServiceClient(blobServiceClient)
                            .terrakubeClient(terrakubeClient)
                            .terraformStatePathService(terraformStatePathService)
                            .build();
                    break;
                case AwsTerraformStateImpl:
                    AmazonS3 s3client = null;

                    if (awsTerraformStateProperties.getEndpoint() != "") {
                        ClientConfiguration clientConfiguration = new ClientConfiguration();
                        clientConfiguration.setSignerOverride("AWSS3V4SignerType");

                        AWSCredentials credentials = new BasicAWSCredentials(
                                awsTerraformStateProperties.getAccessKey(),
                                awsTerraformStateProperties.getSecretKey()
                        );
                        AWSStaticCredentialsProvider awsStaticCredentialsProvider = new AWSStaticCredentialsProvider(credentials);

                        s3client = AmazonS3ClientBuilder
                                .standard()
                                .withClientConfiguration(clientConfiguration)
                                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsTerraformStateProperties.getEndpoint(), awsTerraformStateProperties.getRegion()))
                                .withCredentials(awsStaticCredentialsProvider)
                                .withPathStyleAccessEnabled(true)
                                .build();
                    } else if (awsTerraformStateProperties.isEnableRoleAuthentication()) {
                        log.info("Using Role Authentication");
                        s3client = AmazonS3ClientBuilder.standard()
                                .withCredentials(new DefaultAWSCredentialsProviderChain())
                                .withRegion(awsTerraformStateProperties.getRegion())
                                .build();
                    } else {
                        AWSCredentials credentials = new BasicAWSCredentials(
                                awsTerraformStateProperties.getAccessKey(),
                                awsTerraformStateProperties.getSecretKey()
                        );
                        AWSStaticCredentialsProvider awsStaticCredentialsProvider = new AWSStaticCredentialsProvider(credentials);

                        s3client = AmazonS3ClientBuilder
                                .standard()
                                .withCredentials(awsStaticCredentialsProvider)
                                .withRegion(Regions.fromName(awsTerraformStateProperties.getRegion()))
                                .build();
                    }

                    terraformState = AwsTerraformStateImpl.builder()
                            .s3client(s3client)
                            .endpoint(awsTerraformStateProperties.getEndpoint() != "" ? awsTerraformStateProperties.getEndpoint(): null)
                            .bucketName(awsTerraformStateProperties.getBucketName())
                            .accessKey(awsTerraformStateProperties.getAccessKey())
                            .secretKey(awsTerraformStateProperties.getSecretKey())
                            .region(Regions.fromName(awsTerraformStateProperties.getRegion()))
                            .includeBackendKeys(awsTerraformStateProperties.isIncludeBackendKeys())
                            .terrakubeClient(terrakubeClient)
                            .terraformStatePathService(terraformStatePathService)
                            .build();
                    break;
                case GcpTerraformStateImpl:
                    try {
                        log.info("GCP Credentials Base64 {} length", gcpTerraformStateProperties.getCredentials().length());
                        Credentials gcpCredentials = GoogleCredentials.fromStream(
                                new ByteArrayInputStream(
                                        Base64.decodeBase64(gcpTerraformStateProperties.getCredentials())
                                )
                        );
                        Storage gcpStorage = StorageOptions.newBuilder()
                                .setCredentials(gcpCredentials)
                                .setProjectId(gcpTerraformStateProperties.getProjectId())
                                .build()
                                .getService();

                        terraformState = GcpTerraformStateImpl.builder().storage(gcpStorage)
                                .terraformStatePathService(terraformStatePathService)
                                .bucketName(gcpTerraformStateProperties.getBucketName())
                                .credentials(gcpTerraformStateProperties.getCredentials())
                                .terrakubeClient(terrakubeClient)
                                .build();
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                    break;
                default:
                    terraformState = LocalTerraformStateImpl.builder()
                            .terrakubeClient(terrakubeClient)
                            .terraformStatePathService(terraformStatePathService)
                            .build();
            }
        else
            terraformState = LocalTerraformStateImpl.builder()
                    .terrakubeClient(terrakubeClient)
                    .terraformStatePathService(terraformStatePathService)
                    .build();
        return terraformState;
    }
}
