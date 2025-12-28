//package com.lz.logging.test;
//
//import com.lz.logging.autoconfigure.ElasticsearchLoggingAutoConfiguration;
//import com.lz.logging.autoconfigure.ElasticsearchLoggingProperties;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//
//@RunWith(SpringRunner.class)
//@TestPropertySource(properties = {"es.logging.enabled=true"}) // 启用elasticsearch日志记录
//@SpringBootTest(classes = ElasticsearchLoggingAutoConfiguration.class)
//public class ElasticsearchLoggingStarterTest {
//
//    @Autowired(required = false)
//    private ElasticsearchLoggingProperties properties;
//
//    @Test
//    public void contextLoads() {
//        // 测试配置是否正确加载
//        assertThat(properties).isNotNull();
//        assertThat(properties.isEnabled()).isTrue();
//        assertThat(properties.getHosts()).isEqualTo("localhost:9200");
//    }
//
//    @Test
//    public void testPropertiesDefaultValues() {
//        assertThat(properties.getMinLevel()).isEqualTo("INFO");
//        assertThat(properties.getEnvironment()).isEqualTo("dev");
//        assertThat(properties.getIndex()).isEqualTo("app-logs");
//        assertThat(properties.isAsync()).isTrue();
//        assertThat(properties.getQueueSize()).isEqualTo(10000);
//    }
//}
