package com.xreal.db.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.xreal.db.repository")
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;
    
    @Value("${spring.elasticsearch.username}")
    private String username;

    @Value("${spring.elasticsearch.password}")
    private String password;
    
    @Value("${spring.ai.vectorstore.elasticsearch.dimensions}")
    private int dimensions;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = createRestClient();
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
        );
        return new ElasticsearchClient(transport);
    }

    private RestClient createRestClient() {
        try {
            URI uri = URI.create(elasticsearchUri);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            
            // 如果没有指定端口，使用默认端口
            if (port == -1) {
                port = "https".equalsIgnoreCase(scheme) ? 443 : 9200;
            }
            
            // 创建HttpHost
            HttpHost httpHost = new HttpHost(host, port, scheme);
            
            // 创建RestClientBuilder
            RestClientBuilder builder = RestClient.builder(httpHost);
            
            // 配置认证
            if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password)
                );
                
                builder.setHttpClientConfigCallback(httpClientBuilder -> {
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    
                    // 如果是HTTPS，配置SSL
                    if ("https".equalsIgnoreCase(scheme)) {
                        try {
                            SSLContext sslContext = SSLContexts.custom()
                                    .loadTrustMaterial((chain, authType) -> true) // 信任所有证书
                                    .build();
                            httpClientBuilder.setSSLContext(sslContext);
                            httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                            throw new RuntimeException("Failed to configure SSL", e);
                        }
                    }
                    
                    return httpClientBuilder;
                });
            } else if ("https".equalsIgnoreCase(scheme)) {
                // 即使没有认证信息，HTTPS仍需要配置SSL
                builder.setHttpClientConfigCallback(httpClientBuilder -> {
                    try {
                        SSLContext sslContext = SSLContexts.custom()
                                .loadTrustMaterial((chain, authType) -> true) // 信任所有证书
                                .build();
                        httpClientBuilder.setSSLContext(sslContext);
                        httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to configure SSL", e);
                    }
                    return httpClientBuilder;
                });
            }
            
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Elasticsearch RestClient", e);
        }
    }
    
    @Bean
    public ElasticsearchConverter elasticsearchConverter(SimpleElasticsearchMappingContext mappingContext) {
        MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);
        converter.setConversions(new CustomElasticsearchConversions(dimensions));
        return converter;
    }
    
    @Bean
    public SimpleElasticsearchMappingContext elasticsearchMappingContext() {
        return new SimpleElasticsearchMappingContext();
    }
}