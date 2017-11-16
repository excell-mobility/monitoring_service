package monitoring;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import monitoring.component.MonitoringService;
import monitoring.controller.MonitoringController;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.VendorExtension;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.ApiKeyVehicle;
import springfox.documentation.swagger.web.SecurityConfiguration;
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
          	.apis(RequestHandlerSelectors.any()) 
          	.paths(PathSelectors.regex("/v1/monitoring"))
          	.build()
          .genericModelSubstitutes(ResponseEntity.class)
          .protocols(Sets.newHashSet("https"))
          //.host("localhost:43444")
          //.host("141.64.5.234/excell-monitoring-api")
          .host("dlr-integration.minglabs.com/api/v1/service-request/monitoringservice")
          .securitySchemes(Lists.newArrayList(apiKey()))
          .securityContexts(Lists.newArrayList(securityContext()))
          .apiInfo(apiInfo())
          ;
    }
    
	private ApiKey apiKey() {
		return new ApiKey("api_key", "Authorization", "header");
	}
	
    private SecurityContext securityContext() {
        return SecurityContext.builder()
            .securityReferences(defaultAuth())
            .forPaths(PathSelectors.regex("/*.*"))
            .build();
    }

    private List<SecurityReference> defaultAuth() {
    	List<SecurityReference> ls = new ArrayList<>();
    	AuthorizationScope authorizationScope
    		= new AuthorizationScope("global", "accessEverything");
    	AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
    	authorizationScopes[0] = authorizationScope;
    	SecurityReference s = new SecurityReference("api_key", authorizationScopes);
    	ls.add(s);
    	return ls;
    }

	@Bean
	public SecurityConfiguration security() {
		return new SecurityConfiguration(null, null, null, null, "Token", ApiKeyVehicle.HEADER, "Authorization", ",");
	}
    
    private ApiInfo apiInfo() {
		ApiInfo apiInfo = new ApiInfo(
          "ExCELL Monitoring API",
          "Diese API liefert einen Status-Report für ein getracktes Gerät A in Hinblick auf das Erreichen eines Ortes B."
          + " Der Report besteht aus mehreren Elementen: Die Koordinaten der aktuellen Position, die vorgeschlagene Route zum Ziel, die errechnete Verspätung und eine Meldung ob B erreicht ist."
          + " Intern fragt der Monitoring Service die aktuelle Postion des Gerätes über den ExCELL Tracking Service ab."
          + " Die verwendete deviceID muss dafür im Tracking Service hinterlegt sein."
          + "\n"
          + "\nThis API provides a status report for a monitored device A with regards to a target location B."
          + " It returns the coordinates of locations A and B, the proposed route between them, calculates the delay and tells you when B is reached."
          + " Internally the Monitoring Service queries the ExCELL Tracking Service for the current position."
          + " Please, make sure that the ID used here as first parameter also exists in the Tracking Service.\n",
          "Version 1.0",
          "Use only for testing",
          new Contact(
        		  "Felix Kunde, Stephan Pieper",
        		  "https://projekt.beuth-hochschule.de/magda/poeple",
        		  "fkunde@beuth-hochschule"),
          "Apache 2",
          "http://www.apache.org/licenses/LICENSE-2.0",
          new ArrayList<VendorExtension>());
        return apiInfo;
    }
}