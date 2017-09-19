package monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;

//import com.google.common.collect.Sets;

import monitoring.component.MonitoringService;
import monitoring.controller.MonitoringController;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableScheduling
@EnableSwagger2
@ComponentScan(basePackageClasses = {
		MonitoringController.class,
		MonitoringService.class
	})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    @Bean
    public Docket monitoringApi() { 
        return new Docket(DocumentationType.SWAGGER_2)
          .groupName("excell-monitoring-api")
          .select()
          	//.apis(RequestHandlerSelectors.any()) 
          	//.paths(PathSelectors.any())
          .build()
          .genericModelSubstitutes(ResponseEntity.class)
          //.protocols(Sets.newHashSet("https"))
//          .host("localhost:43444")
          .host("141.64.5.234/excell-monitoring-api")
          .apiInfo(apiInfo())
          ;
    }
    
    private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
          "ExCELL Monitoring API",
          "This API provides information about position and status of a monitored sensor",
          "Version 1.0",
          "Use only for testing",
          "fkunde@beuth-hochschule",
          "Apache 2",
          "http://www.apache.org/licenses/LICENSE-2.0");
        return apiInfo;
    }
}